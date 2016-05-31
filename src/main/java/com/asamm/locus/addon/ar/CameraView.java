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
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Iterator;
import java.util.List;

import locus.api.utils.Logger;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    // tag for logger
	private static final String TAG = "CameraView";

    // default angle for camera
    public static final float DEFAULT_VIEW_ANGLE = 45;

    protected List<Camera.Size> mPreviewSizeList;
    protected List<Camera.Size> mPictureSizeList;

    private SurfaceHolder mHolder;
	private Camera mCamera;

    // horizontal view angle
	private static float mAngleH;
    // vertical view angle
	private static float mAngleV;

    public CameraView(Context context) {
		super(context);
		init();
	}
	
    public CameraView(Context context, AttributeSet attr) {
    	super(context, attr);
    	init();
    }

    /**
     * Initialize main parameters.
     */
    private void init() {
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
        mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // obtain camera
        mCamera = Camera.open(0);

        // store parameters
        Camera.Parameters cameraParams = mCamera.getParameters();
        mPreviewSizeList = cameraParams.getSupportedPreviewSizes();
        mPictureSizeList = cameraParams.getSupportedPictureSizes();
        mAngleH = DEFAULT_VIEW_ANGLE;
        mAngleV = DEFAULT_VIEW_ANGLE;
    }

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Logger.logW(TAG, "surfaceCreated(" + holder + ")");
		try {
			mCamera.setPreviewDisplay(mHolder);
		} catch (Exception e) {
            Logger.logE(TAG, "surfaceCreated(" + holder + ")", e);
			cleanCamera();
		}
	}

    @Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Logger.logW(TAG, "surfaceChanged(" + holder + ", " + format + ", " + w + ", " + h + ")");
		try {
			Camera.Parameters parameters = mCamera.getParameters();
			try {
				// get supported size
				List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();

				// preview form factor
				float ff = (float) w / h;

				// holder for the best form factor and size
				float bff = 0;
				int bestWidth = 0;
				int bestHeight = 0;
				Iterator<Camera.Size> itr = supportedSizes.iterator();

				// we look for the best preview size, it has to be the closest to the
				// screen form factor, and be less wide than the screen itself
				// the latter requirement is because the HTC Hero with update 2.1 will
				// report camera preview sizes larger than the screen, and it will fail
				// to initialize the camera other devices could work with previews
				// larger than the screen though
				while (itr.hasNext()) {
					Camera.Size element = itr.next();
					// current form factor
					float cff = (float) element.width / element.height;
					// check if the current element is a candidate to replace
					// the best match so far
					// current form factor should be closer to the bff
					// preview width should be less than screen width
					// preview width should be more than current bestw
					// this combination will ensure that the highest resolution
					// will win
					if ((ff - cff <= ff - bff) && (element.width <= w) && (element.width >= bestWidth)) {
						bff = cff;
						bestWidth = element.width;
						bestHeight = element.height;
					}
				}

				// Some Samsung phones will end up with bestw and besth = 0
				// because their minimum preview size is bigger then the screen size.
				// In this case, we use the default values: 480x320
				if ((bestWidth == 0) || (bestHeight == 0)) {
					bestWidth = 480;
					bestHeight = 320;
				}
				parameters.setPreviewSize(bestWidth, bestHeight);
			} catch (Exception ex) {
				parameters.setPreviewSize(480, 320);
			}

            // set parameters and start preview
			mCamera.setParameters(parameters);
			mCamera.startPreview();

			// set angles to degree
            float angleH = mCamera.getParameters().getHorizontalViewAngle();
            float angleV = mCamera.getParameters().getVerticalViewAngle();
            if (angleH > DEFAULT_VIEW_ANGLE && angleH < 180) {
                CameraView.mAngleH = angleH;
            }
            if (angleV > DEFAULT_VIEW_ANGLE && angleV < 180) {
                CameraView.mAngleV = angleV;
            }
		} catch (Exception e) {
            Logger.logE(TAG, "surfaceChanged(" + holder + ", " + format + ", " +
                    w + ", " + h + ")", e);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Logger.logW(TAG, "surfaceDestroyed(" + holder + ")");
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		cleanCamera();
	}

    /**
     * Clear reference to camera.
     */
    private void cleanCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception ignore) {
            }
            try {
                mCamera.release();
            } catch (Exception ignore) {
            }
            mCamera = null;
        }
    }

    /**
     * Get current defined horizontal view angle.
     * @return horizontal angle
     */
    public static float getAngleHorizontal() {
        return mAngleH;
    }
}
