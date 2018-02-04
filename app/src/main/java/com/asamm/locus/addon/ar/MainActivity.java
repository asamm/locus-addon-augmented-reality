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

import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.asamm.locus.addon.ar.utils.Matrix;

import locus.api.utils.Const;
import menion.android.locus.addon.ar.R;

public class MainActivity extends Activity implements SensorEventListener, OnTouchListener {

    // tag for logger
	private static final String TAG = "MainActivity";

	// ID for a request on permission for camera
	private static final int REQUEST_CODE_CAMERA_PERMISSION 					= 1001;

	// public context of whole AR
	public static ArContent arContent;
	
	// main view for displaying
	private AugmentedView aView;
	// zoom in button
	private ImageButton btnZoomIn; 
	// zoom out button
	private ImageButton btnZoomOut;

	// container for camera view
	private FrameLayout mCameraContainer;
	
	private float RTmp[] = new float[9];
	private float mR[] = new float[9];
	private float I[] = new float[9];
	private float grav[] = new float[3];
	private float mag[] = new float[3];

	private SensorManager sensorMgr;
    private Sensor sensorGrav, sensorMag;

	private int rHistIdx = 0;
	private Matrix tempR = new Matrix();
	private Matrix finalR = new Matrix();
	private Matrix smoothR = new Matrix();
	private Matrix histR[] = new Matrix[50];
	private Matrix m1 = new Matrix();
	private Matrix m2 = new Matrix();
	private Matrix m3 = new Matrix();

    // store density for later faster re-use
	private static float mDensity = -1.0f;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // set content
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_main);

		// get reference to views
		mCameraContainer = (FrameLayout)
				findViewById(R.id.frame_layout_camera_container);

        // create instance of context
		arContent = new ArContent(this);
		aView = (AugmentedView)
                findViewById(R.id.augmentedView);

		// set density for recompute sizes
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDensity = metrics.density;
		
		// initialize 'home' button
		ImageButton btnHome = (ImageButton)
                findViewById(R.id.btn_home);
		btnHome.setOnClickListener(new View.OnClickListener() {

            @Override
			public void onClick(View v) {
				MainActivity.this.finish();
			}
		});

        // set button for 'zoom in'
		btnZoomIn = (ImageButton)
                findViewById(R.id.btn_zoom_in);
		btnZoomIn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ArContent.zoomIn();
				manageZoomButtons();
			}
		});

        // set button for 'zoom out'
		btnZoomOut = (ImageButton)
                findViewById(R.id.btn_zoom_out);
		btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArContent.zoomOut();
                manageZoomButtons();
            }
        });

        // finally handle received data
		arContent.handleIntent(getIntent());

		// set camera
		setCameraView();

        // set style of buttons
        manageZoomButtons();
	}

	// CAMERA & PERMISSIONS

	/**
	 * Check if user has correct permission to access camera and display it or request for
	 * a permission.
	 */
	private void setCameraView() {
		// check if camera is already visible
		if (mCameraContainer.getChildCount() == 1 &&
				mCameraContainer.getChildAt(0) instanceof CameraView) {
			return;
		}

		// test permission
		if (!isPermissionGranted(this, Manifest.permission.CAMERA)) {
			requestPermission(this,
					new String[] {Manifest.permission.CAMERA},
					REQUEST_CODE_CAMERA_PERMISSION);

			// insert empty layout with some info
			// TODO
		} else {
			// insert camera view
			CameraView cameraView = new CameraView(this);
			mCameraContainer.removeAllViews();
			mCameraContainer.addView(cameraView,
					FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.MATCH_PARENT);
		}
	}

	/**
	 * Check if certain permission is granted.
	 * @param act current activity
	 * @param permission permission to check
	 * @return <code>true</code> if is granted
	 */
	private boolean isPermissionGranted(Activity act, String permission) {
		int granted = ContextCompat.checkSelfPermission(act, permission);
		return granted == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Request certain permissions.
	 * @param act current activity
	 * @param permissions permission we require
	 * @param requestCode request code (8-bit)
	 */
	private void requestPermission(Activity act, String[] permissions, int requestCode) {
		ActivityCompat.requestPermissions(act, permissions, requestCode);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
			@NonNull String permissions[], @NonNull int[] grantResults) {
		if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
			// handle result
			setCameraView();
		}
	}

	/**
     * Set state (enabled/disabled) of zoom buttons.
     */
	private void manageZoomButtons() {
		btnZoomIn.setEnabled(ArContent.isZoomInAllowed());
		btnZoomOut.setEnabled(ArContent.isZoomOutAllowed());
	}

    /**
     * Get pixels converted to DPI value.
     * @param pixels pixel value
     * @return pixels in DPI format
     */
	public static float getDpPixels(float pixels) {
		if (mDensity == -1.0f) {
            return pixels;
        }
		return mDensity * pixels;
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		arContent.handleIntent(intent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		try {
			try {
				sensorMgr.unregisterListener(this, sensorGrav);
			} catch (Exception ignore) {}
			try {
				sensorMgr.unregisterListener(this, sensorMag);
			} catch (Exception ignore) {}
			sensorMgr = null;
		} catch (Exception e) {
			Log.e(TAG, "onPause()", e);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		try {
			// clear all events
			aView.clearEvents();

			// rotate
			double angleX = Math.toRadians(-90.0);
			double angleY = Math.toRadians(-90.0);
			
			// set matrix
			m1.set(1f, 0f, 0f,
					0f, (float) Math.cos(angleX), (float) -Math.sin(angleX),
					0f, (float) Math.sin(angleX), (float) Math.cos(angleX));
			
			if (getWindowManager().getDefaultDisplay().getRotation() == 1) { // ROTATION_90
				m2.set(1f, 0f, 0f,
						0f, (float) Math.cos(angleX), (float) -Math.sin(angleX),
						0f, (float) Math.sin(angleX), (float) Math.cos(angleX));
				m3.set((float) Math.cos(angleY), 0f, (float) Math.sin(angleY),
						0f, 1f, 0f,
						(float) -Math.sin(angleY), 0f, (float) Math.cos(angleY));	
			} else {
				m2.set((float) Math.cos(angleX), 0f, (float) Math.sin(angleX),
						0f, 1f, 0f,
						(float) -Math.sin(angleX), 0f, (float) Math.cos(angleX));
				m3.set(1f, 0f, 0f,
						0f, (float) Math.cos(angleY), (float) -Math.sin(angleY),
						0f, (float) Math.sin(angleY), (float) Math.cos(angleY));
			}
			

			for (int i = 0; i < histR.length; i++) {
				histR[i] = new Matrix();
			}

			sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

            List<Sensor> sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0) {
				sensorGrav = sensors.get(0);
			}

			sensors = sensorMgr.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
			if (sensors.size() > 0) {
				sensorMag = sensors.get(0);
			}

			sensorMgr.registerListener(this, sensorGrav,
					SensorManager.SENSOR_DELAY_GAME);
			sensorMgr.registerListener(this, sensorMag, 
					SensorManager.SENSOR_DELAY_GAME);
		} catch (Exception e) {
			Log.e(TAG, "onResume()", e);
			try {
				if (sensorMgr != null) {
					sensorMgr.unregisterListener(this, sensorGrav);
					sensorMgr.unregisterListener(this, sensorMag);
					sensorMgr = null;
				}
			} catch (Exception ignore) {}
		}
	}

    @Override
	public void onDestroy() {
		super.onDestroy();
		
		// clear all variables
		arContent.destroy();
	}

    // SENSOR EVENT LISTENER

    @Override
	public void onSensorChanged(SensorEvent evt) {
		try {
			if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				grav[0] = evt.values[0];
				grav[1] = evt.values[1];
				grav[2] = evt.values[2];
			} else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				mag[0] = evt.values[0];
				mag[1] = evt.values[1];
				mag[2] = evt.values[2];
			}

			// set rotation based on orientation
			SensorManager.getRotationMatrix(RTmp, I, grav, mag);
			if (getWindowManager().getDefaultDisplay().getRotation() == 0) {
				SensorManager.remapCoordinateSystem(RTmp,
						SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_Z, mR);
			} else {
				SensorManager.remapCoordinateSystem(RTmp,
						SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, mR);
			}
			
			// set transformation matrixes
			tempR.set(mR[0], mR[1], mR[2],
					mR[3], mR[4], mR[5],
					mR[6], mR[7], mR[8]);

			finalR.toIdentity();
			finalR.prod(arContent.m4);
			finalR.prod(m1);
			finalR.prod(tempR);
			finalR.prod(m3);
			finalR.prod(m2);
			finalR.invert(); 

			histR[rHistIdx].set(finalR);
			rHistIdx++;
			if (rHistIdx >= histR.length) {
				rHistIdx = 0;
			}

			smoothR.set(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
            for (Matrix aHistR : histR) {
                smoothR.add(aHistR);
            }
			smoothR.mult(1 / (float) histR.length);

            // store computed rotation to permanent matrix
            arContent.setRotationMatrix(smoothR);

            // refresh view
			aView.postInvalidate();
		} catch (Exception e) {
			Log.e(TAG, "onSensorCahnged()", e);
		}
	}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // TOUCH LISTENER

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		try {
			float xPress = me.getX();
			float yPress = me.getY();
			if (me.getAction() == MotionEvent.ACTION_UP) {
				aView.clickEvent(xPress, yPress);
			}
			
			return true;
		} catch (Exception ex) {
			return super.onTouchEvent(me);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}
}
