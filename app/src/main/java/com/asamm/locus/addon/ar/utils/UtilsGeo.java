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
package com.asamm.locus.addon.ar.utils;

import locus.api.objects.extra.Location;
import locus.api.objects.extra.LocationCompute;

public class UtilsGeo {

	public static void convLocToVec(Location org, Location gp, Vector3D v) {
        float[] z = new float[1];
		z[0] = 0;
		LocationCompute.distanceBetween(org.getLatitude(), org.getLongitude(), gp
				.getLatitude(), org.getLongitude(), z);
        float[] x = new float[1];
		LocationCompute.distanceBetween(org.getLatitude(), org.getLongitude(), org
				.getLatitude(), gp.getLongitude(), x);
		// set correct altitude
        float y;
		if (!gp.hasAltitude() || !org.hasAltitude()) {
			y = 0;
		} else {
			y = (float) (gp.getAltitude() - org.getAltitude());
		}
		
		if (org.getLatitude() < gp.getLatitude()) {
			z[0] *= -1;
		}
		if (org.getLongitude() > gp.getLongitude()) {
			x[0] *= -1;
		}

		v.set(x[0], y, z[0]);
	}
}
