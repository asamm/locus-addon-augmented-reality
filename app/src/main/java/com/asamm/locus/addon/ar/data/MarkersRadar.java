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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader.TileMode;
import android.location.Location;

import com.asamm.locus.addon.ar.ArContent;
import com.asamm.locus.addon.ar.CameraView;
import com.asamm.locus.addon.ar.AugmentedView;
import com.asamm.locus.addon.ar.MainActivity;

public class MarkersRadar extends ScreenObject {

	/** Radius in pixel on screen */
	public static float RADIUS = MainActivity.getDpPixels(70);
	/** background color */
	private static int bgColorLight = Color.argb(100, 200, 200, 200);
	/** background color */
	private static int bgColorDark = Color.argb(100, 0, 0, 0);
	/** point color */
	private static int colorPoints = Color.argb(200, 255, 255, 255);
	
	// paint object for background
	private static Paint mPaintBg;
	// paint object for points
	private static Paint mPaintPoint;
	
	// left line to draw view
	private ScreenLine2D lrl;
	// right line to draw view
	private ScreenLine2D rrl;
	
	public MarkersRadar() {
		// init paint object
		mPaintBg = new Paint();
		mPaintBg.setAntiAlias(true);
		mPaintBg.setStyle(Paint.Style.FILL);
        // set shader
		mPaintBg.setShader(new RadialGradient(
       			RADIUS, RADIUS, RADIUS, 
       			new int[] {bgColorLight, bgColorLight, bgColorDark, bgColorDark, bgColorLight, bgColorLight},
       			new float[] {0, 0.65f, 0.9f, 0.96f, 0.98f, 1.0f}, TileMode.CLAMP));
		// init paint for points
		mPaintPoint = new Paint();
		mPaintPoint.setAntiAlias(true);
		mPaintPoint.setColor(colorPoints);
		mPaintPoint.setStyle(Paint.Style.FILL);
		
		lrl = new ScreenLine2D();
		rrl = new ScreenLine2D();
	}
	
	@Override
	public void onLocationChanged(Location loc) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isInRange() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void paint(AugmentedView av, Canvas c) {
		// draw the radar
		c.drawCircle(RADIUS, RADIUS, RADIUS, mPaintBg);
		
		// draw lines
		lrl.set(0, -MarkersRadar.RADIUS);
		lrl.rotate(CameraView.getAngleHorizontal() / 2.0 + ArContent.getCurBearing());
		lrl.add(MarkersRadar.RADIUS, MarkersRadar.RADIUS);
		c.drawLine(lrl.x, lrl.y, RADIUS, RADIUS, mPaintPoint);
		
		rrl.set(0, -MarkersRadar.RADIUS);
		rrl.rotate(- CameraView.getAngleHorizontal() / 2.0 + ArContent.getCurBearing());
		rrl.add(MarkersRadar.RADIUS, MarkersRadar.RADIUS);
		c.drawLine(rrl.x, rrl.y, RADIUS, RADIUS, mPaintPoint);
		
		// put the markers in it
		float scale = ArContent.getRadius() / RADIUS;

		ArContent ar = MainActivity.arContent;
		int size = ar.getMarkers().size();
		for (int i = 0; i < size; i++) {
			Marker pm = ar.getMarkers().get(i);
			float x = pm.mLocationVec.x / scale;
			float y = pm.mLocationVec.z / scale;

			if (x * x + y * y < RADIUS * RADIUS) {
				c.drawRect(x + RADIUS - 1, y + RADIUS - 1,
						x + RADIUS + 1, y + RADIUS + 1, mPaintPoint);
			}
		}
	}
	
	@Override
	public float getHeight() {
		return RADIUS * 2;
	}

	@Override
	public float getWidth() {
		return RADIUS * 2;
	}
}
