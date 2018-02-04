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

import com.asamm.locus.addon.ar.AugmentedView;
import com.asamm.locus.addon.ar.utils.Vector3D;

import locus.api.objects.extra.Location;

public class GuideLine {

	private Location mLoc;
	private double mDist;
	private Vector3D locVec;
	private Vector3D locProj;
	
	private Vector3D origin = new Vector3D(0, 0, 0);
	
	// temp properties
	private Vector3D tmpA = new Vector3D();
	private Vector3D tmpB = new Vector3D();
	
	private Paint mPaint;
	
	private float angle1;
	private float angle2;
	private float angle3;
	
	public GuideLine(Location loc) {
		this.mLoc = loc;
		
		locVec = new Vector3D();
		locProj = new Vector3D();
		
		mPaint = new Paint();
		mPaint.setColor(Color.RED);
		mPaint.setStrokeWidth(5.0f);
		mPaint.setAntiAlias(true);
	}
	
	public void onLocationChanged(Location loc) {
//		UtilsGeo.convLocToVec(loc, mLoc, locVec); FIXME
		mDist = mLoc.distanceTo(loc);
	}

	public void calculatePoints(AugmentedView.Camera viewCam) {
		// calculate point position
		tmpA.set(origin); 
		tmpA.add(locVec);
		tmpA.sub(viewCam.lco);
		tmpA.prod(viewCam.transform);
		viewCam.projectPoint(tmpA, tmpB); //6
		locProj.set(tmpB); //7
	}
	
	public void paint(AugmentedView av, Canvas c) {
		// TODO create 3D guide line
//		float dx = locProj.x - av.screenW / 2;
//		float dy = locProj.y - av.screenH;
//		float dz = locProj.z - 0;
//		double x = Math.sqrt(dx * dx + dy * dy);
//		float dist = (float) Math.sqrt(x * x + dz * dz); 
//
//		float pitch = (float) Math.atan(locVec.y / mDist);
//		
//		Camera cam = new Camera();
//		//cam.rotateX((float) (-90 + Math.asin(dy / dist) * 90) - 5);
//		cam.rotateX(-90 + ArContext.getCurPitch() + pitch);
////		cam.rotateY(dy / dist * 90);
//		cam.rotateZ(dx / dist * 90);
//		Matrix matrix = new Matrix();
//		cam.getMatrix(matrix);
//		matrix.postTranslate(av.screenW / 2, c.getHeight() / 2);
//		
//		c.save();
//		c.setMatrix(matrix);
//		c.drawLine(0, 0, 0, dist, mPaint);
//		c.drawCircle(0, av.screenH / 2, 50.0f, mPaint);
//		c.restore();
		if (locProj.z < 0)
			c.drawLine(av.screenW / 2, av.screenH, locProj.x, locProj.y, mPaint);
	}
}
