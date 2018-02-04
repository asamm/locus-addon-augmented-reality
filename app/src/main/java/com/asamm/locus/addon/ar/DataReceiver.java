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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DataReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		MainActivity.arContent.handleIntent(arg1);
	}
}
