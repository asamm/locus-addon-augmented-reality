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
package com.asamm.locus.addon.ar.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.asamm.locus.addon.ar.ArContent;
import com.asamm.locus.addon.ar.AugmentedView;
import com.asamm.locus.addon.ar.MainActivity;
import com.asamm.locus.addon.ar.utils.Utils;
import com.asamm.locus.addon.ar.utils.UtilsGeo;
import com.asamm.locus.addon.ar.utils.Vector3D;

import locus.api.android.features.augmentedReality.UtilsAddonAR;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Waypoint;
import menion.android.locus.addon.ar.R;

public class Marker {

    // tag for logger
	private static final String TAG = "Marker";
	
	// max distance to behave as center
	private static final float MAX_CENTER_DIST = MainActivity.getDpPixels(30.0f) * MainActivity.getDpPixels(30.0f);
	// circle radius if no icon available
	private static final float CIRCLE_RADIUS = MainActivity.getDpPixels(15.0f);

    private static final float SCALE_HIGHLIGHT = 2.0f;

	// market id
	private long id;
	// marker variables
	private String mText;
	// icon for this marker
	private Bitmap mImg;
	// label object
	private MarkerLabel mLabel;

	// geo location
	private Location mLocationGeo;
	// location computed in vector to actual position
	protected Vector3D mLocationVec;
	// actual distance
	private double mDistance;

	// draw properties
	private boolean mIsVisible;
	// is marker in screen center
	private boolean isLookingAt;
	
	private Vector3D cMarker = new Vector3D();
	private Vector3D signMarker = new Vector3D();
	
	// temp properties
	private Vector3D vecA = new Vector3D();
	private Vector3D vecB = new Vector3D();
	private Vector3D vecMarker = new Vector3D();
    private Vector3D vecLabel = new Vector3D();
	
	private Vector3D origin = new Vector3D(0, 0, 0);
	private Vector3D upV = new Vector3D(0, 1, 0);
	
	// paint if no icon available 
	private static Paint mPaintCircle;
	static {
		mPaintCircle = new Paint();
		mPaintCircle.setColor(Color.RED);
		mPaintCircle.setStrokeWidth(3.0f);
		mPaintCircle.setStyle(Paint.Style.STROKE);
	}

    /**
     * Basic marker constructor.
     * @param p point object
     * @param img image of point
     */
	public Marker(Waypoint p, Bitmap img) {
		id = p.id;
		mText = p.getName();
		mImg = img;
		mLocationGeo = p.getLocation();
		mLocationVec = new Vector3D();
	}

    /**
     * Get text of current marker (name of point).
     * @return name of marker
     */
	public String getText(){
		return mText;
	}

    /**
     * Check if we are currently looking on this marker.
     * @return <code>true</code> if we are currently looking at it
     */
	public boolean isLookingAt() {
		return mIsVisible && isLookingAt;
	}

    /**
     * Prepare all parameters before we will paint marker.
     * @param viewCam current camera
     */
	public void prepareForPaint(AugmentedView.Camera viewCam) {
		// calculate point position
        vecA.set(origin);
        vecA.add(mLocationVec);
        vecA.sub(viewCam.lco);
        vecA.prod(viewCam.transform);
		viewCam.projectPoint(vecA, vecMarker);
		cMarker.set(vecMarker);

		// calculate camera
        mIsVisible = false;
		isLookingAt = false;

		if (cMarker.z < -1f) {
            mIsVisible = true;

			if (pointInside(cMarker.x, cMarker.y, 0, 0,
					viewCam.width, viewCam.height)) {

				float xDist = cMarker.x - viewCam.width / 2;
				float yDist = cMarker.y - viewCam.height / 2;
				float dist = xDist * xDist + yDist * yDist;
                if (dist < MAX_CENTER_DIST) {
					isLookingAt = true;
				}
			}
		}

        // calculate position for label
        vecB.set(upV);
        vecB.add(mLocationVec);
        vecB.sub(viewCam.lco);
        vecB.prod(viewCam.transform);
        if (isLookingAt) {
            viewCam.projectPoint(vecB, vecLabel, 0.0f,
                    -1 * SCALE_HIGHLIGHT * MainActivity.getDpPixels(24.0f));
        } else {
            viewCam.projectPoint(vecB, vecLabel, 0.0f,
                    -1 * MainActivity.getDpPixels(24.0f));
        }
        signMarker.set(vecLabel);
	}

    /**
     * Finally paint marker.
     * @param av current augmented view
     */
    public void paint(AugmentedView av) {
        // check visibility
        if (!mIsVisible) {
            return;
        }

        // prepare scale parameter
        float scale;
        if (isLookingAt) {
            scale = SCALE_HIGHLIGHT;
        } else {
            // set scale by distance from 0.50 - 1.50
            scale = (ArContent.getRadius() + cMarker.z) / ArContent.getRadius() + 0.50f;
        }

        // compute actual screen angle
        float currentAngle = Utils.getAngle(cMarker.x, cMarker.y, signMarker.x, signMarker.y) + 90;

        // draw label first
        if (mLabel == null) {
            mLabel = new MarkerLabel(mText);
        }
        av.drawBitmap(mLabel.getTextImage(), signMarker.x, signMarker.y, currentAngle,
                scale == SCALE_HIGHLIGHT ? SCALE_HIGHLIGHT : 1.0f);

        // paint img or circle when no image available
        if (mImg == null) {
            av.drawCircle(cMarker.x, cMarker.y, CIRCLE_RADIUS, mPaintCircle);
        } else {
            av.drawBitmap(mImg, cMarker.x, cMarker.y, currentAngle, scale);
        }
    }

    private static boolean pointInside(float P_x, float P_y,
            float r_x, float r_y, float r_w, float r_h) {
        return (P_x > r_x && P_x < r_x + r_w && P_y > r_y && P_y < r_y + r_h);
    }
	
	public double getDistance() {
		return mDistance;
	}
	
	public void onLocationChanged(Location loc) {
		UtilsGeo.convLocToVec(loc, mLocationGeo, mLocationVec);
		mDistance = loc.distanceTo(mLocationGeo);
//Log.d(TAG, "marker:" + mText + ", lcoationChanged, dist:" + mDistance + ", vec:" + mLocationVec + ", " + mLocationGeo.toString());
	}
	
	public boolean isInRange() {
		return mDistance < ArContent.getRadius();
	}



	public long getId() {
		return id;
	}

	public Location getLocation() {
		return mLocationGeo;
	}
	
	public boolean fClick(float x, float y) {
		boolean evtHandled = false;
		if (isClickValid(x, y)) {
			Log.d(TAG, "handleClick:" + mText);
			evtHandled = true;
		}
		return evtHandled;
	}
	
	ScreenLine2D pPt = new ScreenLine2D();

	//isClickValid(261.14685, 172.47269), 248.48828, 1108.4265, -40723.86, 46.11125, 149.0, 82.0

	private boolean isClickValid(float x, float y) {
		if (!mIsVisible)
			return false;
		
		float currentAngle = Utils.getAngle(cMarker.x, cMarker.y,
				signMarker.x, signMarker.y);

		pPt.x = x - signMarker.x;
		pPt.y = y - signMarker.y;
		pPt.rotate(Math.toRadians(-(currentAngle + 90)));
		pPt.x += signMarker.x;
		pPt.y += signMarker.y;
		
		float objX, objY, objW, objH;
		if (mImg != null) {
			objX = cMarker.x - mImg.getWidth();
			objY = cMarker.y - mImg.getHeight();
			objW = mImg.getWidth() * 2;
			objH = mImg.getHeight() * 2;	
		} else {
			objX = cMarker.x - CIRCLE_RADIUS;
			objY = cMarker.y - CIRCLE_RADIUS;
			objW = CIRCLE_RADIUS * 2;
			objH = CIRCLE_RADIUS * 2;
		}
		
//Log.d(TAG, "isClickValid(" + x + ", " +y + "), " + pPt.x + ", " + pPt.y + ", " + objX + ", " + objY + ", " + objW + ", " + objH);
		if (pPt.x > objX && pPt.x < objX + objW && pPt.y > objY
				&& pPt.y < objY + objH) {
			
			Activity activity = MainActivity.arContent.getMain();
			new AlertDialog.Builder(activity).
			setCancelable(true).
			setTitle(R.string.select).
			setMessage(activity.getString(R.string.select_marker, mText)).
			setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					MainActivity main = MainActivity.arContent.getMain();
					Intent intent = new Intent();
					intent.putExtra(UtilsAddonAR.RESULT_WPT_ID, id);
					main.setResult(Activity.RESULT_OK, intent);
					main.finish();
				}
			}).
			setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			}).
			show();
			return true;
		} else {
			return false;
		}
	}
}
