/*
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

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;

public class MarkerLabel {
	
	private static final int MAX_LINES = 2;
	private static final int MAX_WIDTH = 140;
	private static final int ARROW_WIDTH = 15;
	private static final int ARROW_HEIGHT = 20;
	private static final int BOTTOM_SPACE = 40;
	
	private int width;
	private int height;
	
	private Bitmap bitmap;
	
	private static Paint mPaintFill;
	private static Paint mPaintBorder;
	private static Paint mPaintText;
	private static Path mArrow;
	static {
		mPaintFill = new Paint();
		mPaintFill.setStyle(Paint.Style.FILL);
		mPaintFill.setColor(Color.argb(255, 255, 255, 255));
		mPaintFill.setAntiAlias(true);
		
		mPaintBorder = new Paint();
		mPaintBorder.setStyle(Paint.Style.STROKE);
		mPaintBorder.setColor(Color.argb(255, 0, 0, 0));
		mPaintBorder.setAntiAlias(true);
		
		mPaintText = new Paint();
		mPaintText.setTextSize(14.0f);
		mPaintText.setStyle(Paint.Style.FILL);
		mPaintText.setColor(Color.argb(255, 0, 0, 0));
		mPaintText.setAntiAlias(true);
		
		mArrow = new Path();
		mArrow.moveTo(0, 0);
		mArrow.lineTo(ARROW_WIDTH / 2, ARROW_HEIGHT);
		mArrow.lineTo(ARROW_WIDTH, 0);
	}

    /**
     * Basic constructor.
     * @param txtInit label title
     */
	public MarkerLabel(String txtInit) {
		if (txtInit == null || txtInit.length() == 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
			return;
		}
		
		// define size variables
		float padding = 3.0f;
		float areaWidth = MAX_WIDTH - 2 * padding;
		float areaHeight;
		float lineHeight = -mPaintText.ascent() + mPaintText.descent();

		// create array of text lines
		List<String> lineList = new ArrayList<>();
		BreakIterator boundary = BreakIterator.getWordInstance();
		boundary.setText(txtInit);

		int start = boundary.first();
		int end = boundary.next();
		int prevEnd = start;
		while (end != BreakIterator.DONE) {
			String line = txtInit.substring(start, end);
			String prevLine = txtInit.substring(start, prevEnd);
			float lineWidth = mPaintText.measureText(line);

            // check width
			if (lineWidth > areaWidth) {
				// if the first word is longer than lineWidth 
				// prevLine is empty and should be ignored
				if (prevLine.length() > 0) {
                    lineList.add(prevLine);
                }
				start = prevEnd;
			}

			prevEnd = end;
			end = boundary.next();

            // check number of lines
			if (lineList.size() == MAX_LINES - 1) {
                break;
            }
		}
		if (start != prevEnd) {
			String line = txtInit.substring(start, prevEnd) + "...";
			lineList.add(line);
		}

		// compute maxWidth
		float maxLineWidth = 0;
		for (int i = 0; i < lineList.size(); i++) {
			float lw = mPaintText.measureText(lineList.get(i));
			if (lw > maxLineWidth)
				maxLineWidth = lw;
		}
		
		areaWidth = maxLineWidth;
		areaHeight = lineHeight * lineList.size();

		width = (int) (areaWidth + 2 * padding);
		height = (int) (areaHeight + 2 * padding + ARROW_HEIGHT + BOTTOM_SPACE);
		
		// create canvas
		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		// draw top part
		RectF rectTop = new RectF(0, 0, width, height - ARROW_HEIGHT - BOTTOM_SPACE);
		c.drawRoundRect(rectTop, 5, 5, mPaintFill);
		c.drawRoundRect(rectTop, 5, 5, mPaintBorder);
		// draw bottom arrow
        c.save();
        c.translate((width - ARROW_WIDTH) / 2, height - ARROW_HEIGHT - BOTTOM_SPACE);
        c.clipPath(mArrow, Region.Op.REPLACE);
        c.drawPaint(mPaintFill);
        c.drawPath(mArrow, mPaintBorder);
        c.restore();
        
		// draw texts
		for (int i = 0; i < lineList.size(); i++) {
			c.drawText(lineList.get(i), padding,
					padding + lineHeight * i - mPaintText.ascent(), mPaintText);
		}
	}

	public Bitmap getTextImage() {
		return bitmap;
	}
	
	public void paint(Canvas c) {}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}
}
