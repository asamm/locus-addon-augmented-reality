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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.asamm.locus.addon.ar.data.GuideLine;
import com.asamm.locus.addon.ar.data.Marker;
import com.asamm.locus.addon.ar.data.MarkersRadar;
import com.asamm.locus.addon.ar.data.ScreenObject;
import com.asamm.locus.addon.ar.data.ScreenText;
import com.asamm.locus.addon.ar.utils.Matrix;
import com.asamm.locus.addon.ar.utils.Utils;
import com.asamm.locus.addon.ar.utils.Vector3D;

import java.util.ArrayList;
import java.util.List;

import locus.api.objects.extra.Location;
import menion.android.locus.addon.ar.R;

public class AugmentedView extends View {

	// width of actual view
	public int screenW;
	// height of actual view
	public int screenH;
	// transformation camera class
	private Camera cam;
	// radar for markers
	private MarkersRadar mRadar;
	// screen text for range
	private ScreenText mTextDistance;
	// screen text for bearing
	private ScreenText mTextBearing;
	// guide line
	private GuideLine mGuideLine;
	
	// array for highlighted markers
	private List<Marker> mHighlighted;
	
	// last location
	private Location lastLoc;
	
	// actual screen click events
	private final List<ClickEvent> uiEvents = new ArrayList<>();
	
	public AugmentedView(Context context) {
		super(context);
		basicInit();
	}
	
    public AugmentedView(Context context, AttributeSet attr) {
    	super(context, attr);
    	basicInit();
    }
    
    private void basicInit() {
    	mHighlighted = new ArrayList<>();
    	mRadar = new MarkersRadar();
    	mTextDistance = new ScreenText(8);
    	mTextBearing = new ScreenText(8);
    }

	@Override
	public void draw(Canvas c) {
		super.draw(c);

		// set actual screen
		screenW = getWidth();
		screenH = getHeight();
		
		// set canvas for all mPaint methods
		this.c = c;
		
		// initialize core
		if (cam == null) {
			cam = new Camera(screenW, screenH);
		}

		// transform to camera
		ArContent ar = MainActivity.arContent;
		ar.getRotationMatrix(cam.transform);

		// set location
		if (lastLoc == null) {
			lastLoc = ar.actualLoc;
		}
		boolean updateLoc = false;
		if (lastLoc != ar.actualLoc) {
			lastLoc = ar.actualLoc;
			updateLoc = true;
		}
		
		// update and draw markers
		mHighlighted.clear();
		Marker guidedMarker = null;
		int size = ar.getMarkers().size();
		
		// initialize markers and find nearest one
		for (int i = 0; i < size; i++) {
			Marker ma = ar.getMarkers().get(i);
			if (ma.isInRange()) {
                // prepare point for draw
				ma.prepareForPaint(cam);

                // store currently guided marker
				if (ar.isMarkerGuided(ma)) {
                    guidedMarker = ma;
                    continue;
                }

                // store markers that needs highlight
                if (ma.isLookingAt()) {
                    mHighlighted.add(ma);
                    continue;
                }

                // draw basic marker
                ma.paint(this);
			}
		}
		
		// draw highlighted markers now
		for (Marker ma : mHighlighted) {
			ma.paint(this);
		}
		
		// draw guided marker on the end
		if (guidedMarker != null) {
			guidedMarker.paint(this);

			if (mGuideLine == null) {
				mGuideLine = new GuideLine(guidedMarker.getLocation());
				mGuideLine.onLocationChanged(ar.actualLoc);
			}

			if (updateLoc) {
				mGuideLine.onLocationChanged(lastLoc);
			}
			mGuideLine.calculatePoints(cam);
			mGuideLine.paint(this, c);
		}
		
		// set radar position
		int rx = getResources().getDimensionPixelSize(R.dimen.title_height) + 5;
		int ry = (int) (screenH - 2 * MarkersRadar.RADIUS) - 5;
		
		// draw radar
		paintObj(mRadar, rx, ry, -ArContent.getCurBearing(), 1);

		// draw bearing angle
		mTextBearing.setText(((int) ArContent.getCurBearing()) + "°");
		paintObj(mTextBearing, rx + MarkersRadar.RADIUS - mTextBearing.getWidth() / 2,
				screenH - 2 * MarkersRadar.RADIUS - mTextBearing.getHeight() - 5, 0, 1);
		
		// draw distance value
		mTextDistance.setText(Utils.formatDist(ArContent.getRadius()));
		paintObj(mTextDistance, rx + MarkersRadar.RADIUS - mTextDistance.getWidth() / 2,
				screenH - mTextDistance.getHeight() - 5, 0, 1);

		// handle all click events
		ClickEvent evt = null;
		synchronized (uiEvents) {
			if (uiEvents.size() > 0) {
				evt = uiEvents.get(0);
				uiEvents.remove(0);
			}
		}
		if (evt != null) {
			handleClickEvent(evt);
		}
 	}

	private boolean handleClickEvent(ClickEvent evt) {
		boolean evtHandled;
		ArContent ar = MainActivity.arContent;
		for (int i = ar.getMarkers().size() - 1; i >= 0; i--) {
			Marker pm = ar.getMarkers().get(i);
			evtHandled = pm.fClick(evt.x, evt.y);
			if (evtHandled) {
                return true;
            }
		}
		return false;
	}
	
	public void clickEvent(float x, float y) {
		synchronized (uiEvents) {
			uiEvents.add(new ClickEvent(x, y));
		}
	}

	public void clearEvents() {
		synchronized (uiEvents) {
			uiEvents.clear();
		}
	}
	
	/**************************************************/
	// PAINT PART
    /**************************************************/

    // stored canvas for current painting
	private Canvas c;
    // basic paint object
	private static Paint mPaintImg;
    // matrix for quick transformations
    private static android.graphics.Matrix mPaintMatrix;

	static {
		mPaintImg = new Paint();
		mPaintImg.setAntiAlias(true);
		mPaintImg.setFilterBitmap(true);
        mPaintMatrix = new android.graphics.Matrix();
	}


    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        c.drawCircle(cx, cy, radius, paint);
    }

	public void drawBitmap(Bitmap img, float x, float y, float rotation, float scale) {
        mPaintMatrix.reset();
        mPaintMatrix.preScale(scale, scale);
        mPaintMatrix.postTranslate(x - (img.getWidth() / 2 * scale), y - (img.getHeight() * scale));
        mPaintMatrix.postRotate(rotation, x, y);
		c.drawBitmap(img, mPaintMatrix, mPaintImg);
	}

	public void paintObj(ScreenObject obj, float x, float y, float rotation, float scale) {
		c.save();
		c.translate(x + obj.getWidth() / 2, y + obj.getHeight() / 2);
		c.rotate(rotation);
		c.scale(scale, scale);
		c.translate(- (obj.getWidth() / 2), - (obj.getHeight() / 2));
		obj.paint(this, c);
		c.restore();
	}

    /**
     * Container for camera parameters.
     */
	public class Camera {
		
		public int width;
		public int height;

		public Matrix transform = new Matrix();
		public Vector3D lco = new Vector3D();

		private float viewAngle;
		private float dist;

		public Camera(int width, int height) {
			this.width = width;
			this.height = height;

			transform.toIdentity();
			lco.set(0, 0, 0);

            // define parameters
			this.viewAngle = (float) Math.toRadians(CameraView.getAngleHorizontal());
            this.dist = (float) ((this.width / 2.0) / Math.tan(viewAngle / 2.0));
		}

		public void projectPoint(Vector3D orgPoint, Vector3D prjPoint) {
			projectPoint(orgPoint, prjPoint, 0.0f, 0.0f);
		}

        public void projectPoint(Vector3D orgPoint, Vector3D prjPoint, float addX, float addY) {
            prjPoint.x = dist * orgPoint.x / -orgPoint.z;
            prjPoint.y = dist * orgPoint.y / -orgPoint.z;
            prjPoint.z = orgPoint.z;
            prjPoint.x = prjPoint.x + addX + width / 2;
            prjPoint.y = -prjPoint.y + addY + height / 2;
        }
	}
	
	class ClickEvent {
		public float x, y;

		public ClickEvent(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
}
