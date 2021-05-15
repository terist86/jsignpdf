/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf.types;

import java.awt.Dimension;
import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Arrays;

import net.sf.jsignpdf.preview.FinalPropertyChangeSupport;

/**
 * Rectangle implementation based on relative positions.
 *
 * @author Josef Cacek
 */
public class RelRect implements Serializable {

	public enum ResizeDirection {
		N,
		NE,
		E,
		SE,
		S,
		SW,
		W,
		NW,
		NONE
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_COORDS = "coords";
	private final Float[] coords = new Float[] { 0f, 0f, 1f, 1f };

	private final Dimension imageSize = new Dimension(1, 1);

	private final FinalPropertyChangeSupport pcs = new FinalPropertyChangeSupport(this);

	/**
	 * Adds propertyChangeListener for this bean
	 *
	 * @param listener
	 */
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Returns relative coordinates of the startPoint [x1,y1] and endPoint
	 * [x2,y2] as Float array [x1,y1,x2,y2]
	 *
	 * @return
	 */
	public Float[] getCoords() {
		return coords;
	}

	/**
	 * Returns startPoint coordinates in the image.
	 *
	 * @return
	 */
	public Point getP1() {
		return makeImgPoint(0);
	}

	/**
	 * Returns endPoint coordinates in the image.
	 *
	 * @return
	 */
	public Point getP2() {
		return makeImgPoint(2);
	}

	/**
	 * Return if the rectangle is valid (i.e. both startPoint and endPoint are
	 * set correctly)
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean isValid() {
		for (final Float tmpCoord : coords) {
			if (tmpCoord == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Calculates point in the image.
	 *
	 * @param coordsOffset
	 *            use 0 for startPoint and 2 for endPoint.
	 * @return coordinates [x,y] of a point in the image
	 */
	private Point makeImgPoint(final int coordsOffset) {
		final int x = Math.round(coords[coordsOffset] * imageSize.width);
		final int y = imageSize.height - Math.round(coords[coordsOffset + 1] * imageSize.height);
		return new Point(x, y);
	}

	public void move(final Point center) {
		final Point p1 = getP1();
		final Point p2 = getP2();
		final int h = p2.y - p1.y;
		final int w = p2.x - p1.x;

		p1.x = center.x - w / 2;
		p1.y = center.y - h / 2;

		p2.x = p1.x + w;
		p2.y = p1.y + h;

		setStartPoint(p1);
		setEndPoint(p2);
	}

	public boolean pointInside(final Point point) {
		final Point p1 = getP1();
		final Point p2 = getP2();
		return p1.x <= point.x && p1.y <= point.y && point.x <= p2.x && point.y <= p2.y;
	}

	public ResizeDirection pointOnEdge(final Point point) {
		final Point p1 = getP1();
		final Point p2 = getP2();
		if (point.equals(p1)) {
			return ResizeDirection.NW;
		}
		if (point.equals(p2)) {
			return ResizeDirection.SE;
		}
		if (point.y == p1.y) {
			if (point.x == p2.x) {
				return ResizeDirection.NE;
			}
			return ResizeDirection.N;
		}
		if (point.y == p2.y) {
			if (point.x == p1.x) {
				return ResizeDirection.SW;
			}
			return ResizeDirection.S;
		}
		if (point.x == p1.x) {
			return ResizeDirection.W;
		}
		if (point.x == p2.x) {
			return ResizeDirection.E;
		}
		return ResizeDirection.NONE;
	}

	/**
	 * Removes propertyChangeListener from this bean
	 *
	 * @param listener
	 */
	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void resize(final Point point, final ResizeDirection resizeDirection) {
		final Point p1 = getP1();
		final Point p2 = getP2();
		switch (resizeDirection) {
		case N:
			p1.y = point.y;
			setStartPoint(p1);
			break;
		case NE:
			p1.y = point.y;
			setStartPoint(p1);
			p2.x = point.x;
			setEndPoint(p2);
			break;
		case E:
			p2.x = point.x;
			setEndPoint(p2);
			break;
		case SE:
			setEndPoint(point);
			break;
		case S:
			p2.y = point.y;
			setEndPoint(p2);
			break;
		case SW:
			p1.x = point.x;
			setStartPoint(p1);
			p2.y = point.y;
			setEndPoint(p2);
			break;
		case W:
			p1.x = point.x;
			setStartPoint(p1);
			break;
		case NW:
			setStartPoint(point);
			break;
		case NONE:
			break;
		default:
			break;
		}
	}

	/**
	 * Sets the end Point
	 *
	 * @param aPoint
	 *            the endPoint to set
	 */
	public void setEndPoint(final Point aPoint) {
		setRelPoint(aPoint, 2);
	}

	/**
	 * Sets new dimension. Minimal values for both sizes (width, height) are
	 * [1,1]
	 *
	 * @param newWidth
	 * @param newHeight
	 */
	public void setImgSize(int newWidth, int newHeight) {
		if (newWidth < 1) {
			newWidth = 1;
		}
		if (newHeight < 1) {
			newHeight = 1;
		}

		imageSize.setSize(newWidth, newHeight);
	}

	/**
	 * Sets coordinates of a relative point (startPoint or endPoint) based on given
	 * Point in the image.
	 *
	 * @param point  point with coordinates in the image
	 * @param offset use 0 for startPoint and 2 for endPoint.
	 */
	private void setRelPoint(final Point point, final int offset) {
		final Float[] oldVal = Arrays.copyOf(coords, coords.length);
		if (point == null) {
			coords[offset] = null;
			coords[offset + 1] = null;
		} else {
			coords[offset] = (float) point.x / imageSize.width;
			coords[offset + 1] = 1f - (float) point.y / imageSize.height;
		}
		pcs.firePropertyChange(PROPERTY_COORDS, oldVal, coords);
	}

	/**
	 * Sets the start Point
	 *
	 * @param aPoint the startPoint to set
	 */
	public void setStartPoint(final Point aPoint) {
		setRelPoint(aPoint, 0);
	}

}
