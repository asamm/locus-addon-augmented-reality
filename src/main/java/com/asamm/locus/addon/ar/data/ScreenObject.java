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
import android.location.Location;

import com.asamm.locus.addon.ar.AugmentedView;

public abstract class ScreenObject {

	public abstract void onLocationChanged(Location loc); 
	
	public abstract boolean isInRange();
	
	public abstract void paint(AugmentedView av, Canvas c);
	
	public abstract float getHeight();
	
	public abstract float getWidth();
}
