/*
 * Copyright 2012, Asamm Software, s. r. o.
 * 
 * This file is part of Locus - add-on AR project (LocusAddonAR).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.asamm.locus.addon.ar;

import android.content.Intent;
import android.hardware.GeomagneticField;

import com.asamm.locus.addon.ar.data.Marker;
import com.asamm.locus.addon.ar.utils.Matrix;
import com.asamm.locus.addon.ar.utils.Utils;
import com.asamm.locus.addon.ar.utils.Vector3D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import locus.api.android.features.augmentedReality.UtilsAddonAR;
import locus.api.android.objects.PackWaypoints;
import locus.api.android.utils.LocusConst;
import locus.api.android.utils.LocusUtils;
import locus.api.objects.Storable;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Waypoint;
import locus.api.utils.Logger;

public class ArContent {

    // tag for logger
	private static final String TAG = "ArContext";

    // lock for synchronization
    private final Object mLock;

	// main class of whole AR
	private MainActivity mAct;
	
	// actual addon location
	protected Location actualLoc;
	// current declination
	protected float declination = 0f;
	// matrix for 
	protected Matrix m4;
	// main rotation matrix
	private final Matrix mRotationM;
	
	// actual loaded points
	private final List<Marker> mMarkers;
	// actual guiding on marker id
	private long mMarkerGuideId;
	
	// minimum radius modify value (mRadiusDefault/RADIUS_MIN)
	public static final int RADIUS_MIN = 1;
	// maximum radius modify value (mRadiusDefault/RADIUS_MAX)
	public static final int RADIUS_MAX = 5;
	// actual radius modify value
	private static int radiusModify = RADIUS_MAX;
	
	// radius in metres to draw points
	private static float mRadiusDefault = 20000.0f;
	// actual radius
	private static float mRadiusActual;
	
	// actual bearing in degree
	private static float curBearing;
	// actual pitch in degree
	private static float curPitch;

    // flag if radius is computed
    private boolean mRadiusInitialized;

    /**
     * Main constructor.
     * @param main existing main activity
     */
	public ArContent(MainActivity main) {
		this.mAct = main;
		this.mLock = new Object();
        this.mRadiusInitialized = false;

		// initialize rotation matrix
        mRotationM = new Matrix();
        mRotationM.toIdentity();

		// create default empty data holder
        mMarkers = new ArrayList<>();

		// create default location
		actualLoc = new Location(TAG);

		// create rotation matrix
		m4 = new Matrix();
	}

    /**
     * Destroy current instance.
     */
	public void destroy() {
		mAct = null;
	}

    /**************************************************/
    // LOAD DATA
    /**************************************************/

    /**
     * Handle received data.
     * @param intent received intent
     */
	public void handleIntent(Intent intent) {
		try {
			// set new location
			synchronized (mLock) {
				if (intent.hasExtra(UtilsAddonAR.EXTRA_LOCATION)) {
					actualLoc = LocusUtils.getLocationFromIntent(
							intent, UtilsAddonAR.EXTRA_LOCATION);
				}
			}
		
			// initialize and load data from intent
			synchronized (mMarkers) {
				List<PackWaypoints> data = getDataFromIntent(intent);
				if (data.size() > 0) {
					// convert to markers
                    mMarkers.clear();
					for (PackWaypoints pd : data) {
						List<Waypoint> points = pd.getWaypoints();
						for (Waypoint p : points) {
							if (p == null) {
								continue;
							}
                            mMarkers.add(new Marker(p, pd.getBitmap()));
						}
					}
				}
			}

			// load marker guide id
			if (intent.hasExtra(UtilsAddonAR.EXTRA_GUIDING_ID)) {
                mMarkerGuideId = intent.getLongExtra(UtilsAddonAR.EXTRA_GUIDING_ID, -1L);
			}
		
			// now init location with actual points
			initLocation();
		} catch (Exception e) {
			Logger.logE(TAG, "handleIntent(" + intent + ")", e);
		}
	}

    /**
     * Initialize location.
     */
    private void initLocation() {
        synchronized (mLock) {
            // check location
            if (actualLoc.getLatitude() == 0.0 && actualLoc.getLongitude() == 0.0) {
                return;
            }

            // update location of all items
            int size = mMarkers.size();
            for (int i = 0; i < size; i++) {
                mMarkers.get(i).onLocationChanged(actualLoc);
            }

            // if first attempt to display data, set new radius
            if (!mRadiusInitialized && mMarkers.size() > 0) {
                mRadiusDefault = (float) (mMarkers.get(0).getDistance() * 1.1f);
                setRadius();
                mRadiusInitialized = true;
            }

            // compute this only if needed
            GeomagneticField gmf = new GeomagneticField(
                    (float) actualLoc.getLatitude(),
                    (float) actualLoc.getLongitude(),
                    (float) actualLoc.getAltitude(),
                    System.currentTimeMillis());

            double angleY = Math.toRadians(-gmf.getDeclination());
            m4.toIdentity();
            m4.set((float) Math.cos(angleY), 0f, (float) Math.sin(angleY),
                    0f, 1f, 0f,
                    (float) -Math.sin(angleY), 0f, (float) Math.cos(angleY));
            declination = gmf.getDeclination();
        }
    }

    /**
     * Get pack of waypoints from received intent.
     * @param intent received intent
     * @return pack of waypoints
     * @throws IOException
     */
    private static List<PackWaypoints> getDataFromIntent(Intent intent)
            throws IOException {
        List<PackWaypoints> data = new ArrayList<>();
        if (intent == null) {
            return data;
        }

        // read data from intent
        if (intent.hasExtra(LocusConst.INTENT_EXTRA_POINTS_DATA_ARRAY)) {
            byte[] rawData = intent.getByteArrayExtra(LocusConst.INTENT_EXTRA_POINTS_DATA_ARRAY);
            //noinspection unchecked
            data = (List<PackWaypoints>)
                    Storable.readList(PackWaypoints.class, rawData);
        }

        // return loaded data
        return data;
    }

    /**************************************************/
    // GET & SET TOOLS
    /**************************************************/

    /**
     * Get current rotation matrix. Better, copy current rotation to supplied matrix.
     * @param dest matrix where to copy content of current rotation
     */
	public void getRotationMatrix(Matrix dest) {
		synchronized (mRotationM) {
			dest.set(mRotationM);
		}
	}

    /**
     * Set values from certain matrix to current rotation matrix.
     * @param values matrix values
     */
    public void setRotationMatrix(Matrix values) {
        synchronized (mRotationM) {
            mRotationM.set(values);

            // compute parameters from matrix
            Matrix m = new Matrix();
            m.set(mRotationM);
            calcPitchBearing(m);
        }
    }

    /**
     * Compute pitch and bearing values for a new rotation matrix.
     * @param rotationM new rotation matrix
     */
    private void calcPitchBearing(Matrix rotationM) {
        Vector3D looking = new Vector3D();
        rotationM.transpose();
        looking.set(1, 0, 0);
        looking.prod(rotationM);
        ArContent.curBearing = (int) (Utils.getAngle(0, 0, looking.x, looking.z)  + 360 ) % 360;

        rotationM.transpose();
        looking.set(0, 1, 0);
        looking.prod(rotationM);
        ArContent.curPitch = -Utils.getAngle(0, 0, looking.y, looking.z);
    }

	public Location getLocation() {
		synchronized (mLock) {
			return actualLoc;
		}
	}

    /**
     * Get container with all fresh markers.
     * @return container with markers
     */
	public List<Marker> getMarkers() {
		return mMarkers;
	}

    /**
     * Get current bearing values (angle in degrees since north).
     * @return bearing angle
     */
	public static float getCurBearing() {
		return curBearing;
	}

    /**
     * Get current pitch value (angle in degrees, where '0' means vertical)
     * @return pitch angle
     */
	public static float getCurPitch() {
		return curPitch;
	}

    /**
     * Check if certain marker is currently target for guidance.
     * @param ma marker itself
     * @return <code>true</code> if is currently guidance target
     */
	public boolean isMarkerGuided(Marker ma) {
		return ma.getId() == mMarkerGuideId;
	}
	
	/**************************************************/
	// ZOOMING FUNCTIONS
    /**************************************************/

    /**
     * Get current computed radius.
     * @return current radius
     */
	public static float getRadius() {
		return ArContent.mRadiusActual;
	}
	
	public static void zoomIn() {
		if (radiusModify < RADIUS_MAX)
			radiusModify++;
		setRadius();
	}
	
	public static void zoomOut() {
		if (radiusModify > RADIUS_MIN)
			radiusModify--;
		setRadius();
	}
	
	public static boolean isZoomInAllowed() {
		return radiusModify < RADIUS_MAX;
	}
	
	public static boolean isZoomOutAllowed() {
		return radiusModify > RADIUS_MIN;
	}
	
	private static void setRadius() {
		mRadiusActual = mRadiusDefault * (radiusModify * 1.0f / RADIUS_MAX); 
	}

	public MainActivity getMain() {
		return mAct;
	}
}
