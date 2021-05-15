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
package net.sf.jsignpdf.preview;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import net.sf.jsignpdf.types.RelRect;
import net.sf.jsignpdf.types.RelRect.ResizeDirection;

/**
 * Resizeable image component with rectangle selection implementation. It
 * extends {@link JPanel} component and draws itself on the panel surface.
 *
 * @author Josef Cacek
 */
public class SelectionImage extends JPanel {

	/**
	 * Mouse adapter which stores current position of mouse and stores selection.
	 *
	 * @author Josef Cacek
	 */
	class SelMouseAdapter extends MouseAdapter implements MouseMotionListener {

		private final int btnCode;
		private boolean btnPressed = false;
		private RelRect.ResizeDirection resize = ResizeDirection.NONE;
		private boolean move = false;

		public SelMouseAdapter(final int aBtnCode) {
			btnCode = aBtnCode;
		}

		private boolean cursorAboveImage(final Point point) {
			return offsetX <= point.x && point.x <= offsetX + image.getWidth() && offsetY <= point.y
					&& point.y <= offsetY + image.getHeight();
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			final Point point = e.getPoint();
			if (!btnPressed || !cursorAboveImage(point)) {
				return;
			}
			final Point imgRelPoint = new Point(point.x - offsetX, point.y - offsetY);
			if (move) {
				relRect.move(imgRelPoint);
			} else if (resize != ResizeDirection.NONE) {
				relRect.resize(imgRelPoint, resize);
			} else {
				relRect.setEndPoint(imgRelPoint);
			}
			repaint();
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
			final Point point = e.getPoint();

			if (!cursorAboveImage(point)) {
				try {
					setCursor(Cursor.getSystemCustomCursor("Invalid.32x32"));
				} catch (final HeadlessException | AWTException e1) {
					LOGGER.error(e1);
				}
				return;
			}

			final Point imgRelPoint = new Point(point.x - offsetX, point.y - offsetY);
			if (relRect.pointInside(imgRelPoint)) {
				final Point startPoint = relRect.getP1();
				final Point endPoint = relRect.getP2();
				if (startPoint.equals(imgRelPoint)) {
					setCursor(Cursor.NW_RESIZE_CURSOR);
				} else if (endPoint.equals(imgRelPoint)) {
					setCursor(Cursor.SE_RESIZE_CURSOR);
				} else if (startPoint.x == imgRelPoint.x) {
					setCursor(Cursor.W_RESIZE_CURSOR);
				} else if (startPoint.y == imgRelPoint.y) {
					setCursor(Cursor.N_RESIZE_CURSOR);
				} else if (endPoint.x == imgRelPoint.x) {
					setCursor(Cursor.E_RESIZE_CURSOR);
				} else if (endPoint.y == imgRelPoint.y) {
					setCursor(Cursor.S_RESIZE_CURSOR);
				} else {
					setCursor(Cursor.MOVE_CURSOR);
				}
			} else {
				setCursor(Cursor.CROSSHAIR_CURSOR);
			}
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			final Point point = e.getPoint();
			if (e.getButton() != btnCode || !cursorAboveImage(point)) {
				return;
			}
			btnPressed = true;
			final Point imgRelPoint = new Point(point.x - offsetX, point.y - offsetY);
			if (relRect.pointInside(imgRelPoint)) {
				resize = relRect.pointOnEdge(imgRelPoint);
				if (resize == ResizeDirection.NONE) {
					move = true;
					relRect.move(imgRelPoint);
				} else {
					relRect.resize(imgRelPoint, resize);
				}
			} else {
				relRect.setStartPoint(imgRelPoint);
				relRect.setEndPoint(imgRelPoint);
			}
			repaint();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			final Point point = e.getPoint();
			if (e.getButton() != btnCode || !btnPressed || !cursorAboveImage(point)) {
				return;
			}
			btnPressed = false;
			final Point imgRelPoint = new Point(point.x - offsetX, point.y - offsetY);
			if (move) {
				move = false;
				relRect.move(imgRelPoint);
			} else if (resize != ResizeDirection.NONE) {
				relRect.resize(imgRelPoint, resize);
				resize = ResizeDirection.NONE;
			} else {
				relRect.setEndPoint(imgRelPoint);
			}
			repaint();
		}

	}

	private static final long serialVersionUID = 1L;

	/**
	 * Resizes given image to new dimension. It doesn't break original proportions.
	 *
	 * @param aImg    image to resize
	 * @param aWidth  new image width
	 * @param aHeight new image height
	 * @return resized image
	 */
	private static BufferedImage resize(final BufferedImage aImg, int aWidth, int aHeight) {
		if (aWidth < 1) {
			aWidth = 1;
		}
		if (aHeight < 1) {
			aHeight = 1;
		}

		final int w = aImg.getWidth();
		final int h = aImg.getHeight();
		final float rel = Math.min((float) aWidth / w, (float) aHeight / h);

		final BufferedImage dimg = new BufferedImage(Math.round(w * rel), Math.round(h * rel), aImg.getType());
		final Graphics2D g = dimg.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(aImg, 0, 0, dimg.getWidth(), dimg.getHeight(), 0, 0, w, h, null);
		g.dispose();
		return dimg;
	}

	private transient BufferedImage image = null;

	private transient BufferedImage originalImage = null;

	private int offsetX;
	private int offsetY;

	private final RelRect relRect = new RelRect();

	private Point currentPoint;

	private final Logger LOGGER = Logger.getLogger(SelectionImage.class);

	/**
	 * Default constructor.
	 */
	public SelectionImage() {
		final SelMouseAdapter tmpMouseAdapter = new SelMouseAdapter(MouseEvent.BUTTON1);
		addMouseListener(tmpMouseAdapter);
		addMouseMotionListener(tmpMouseAdapter);
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				createResizedImage();
			}

		});
	}

	/**
	 * Creates resized image and scales selection rectangle
	 */
	private void createResizedImage() {
		if (originalImage == null) {
			image = null;
		} else {
			image = resize(originalImage, getWidth(), getHeight());
			relRect.setImgSize(image.getWidth(), image.getHeight());
			offsetX = (getWidth() - image.getWidth()) / 2;
			offsetY = (getHeight() - image.getHeight()) / 2;
		}
		repaint();
	}

	/**
	 * Returns last mouse position
	 *
	 * @return the currentPoint
	 */
	public Point getCurrentPoint() {
		return currentPoint;
	}

	public RelRect getRelRect() {
		return relRect;
	}

	/**
	 * Returns true if image is set and selection is made
	 *
	 * @return
	 */
	public boolean isValidPosition() {
		return image != null && relRect != null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			g.drawImage(image, offsetX, offsetY, null);
			if (relRect.isValid()) {
				// Page (without rotation) 600x400: [50,100; 150,130]
				// rot 0: [50, 400-100, 150, 400-130]
				// rot 1 (90deg): [ 100, 50, 130, 150]
				// rot 2 (180deg): [ 600 - 50, 100, 600-150, 130]
				// rot 3 (270deg): [ 400 - 100, 600 - 150, 400 - 130, 600 - 150 ]
				final Point p1 = relRect.getP1();
				final Point p2 = relRect.getP2();
				g.drawRect(Math.min(p1.x, p2.x) + offsetX, Math.min(p1.y, p2.y) + offsetY, Math.abs(p2.x - p1.x),
						Math.abs(p2.y - p1.y));
			}
		}
	}

	public void setCursor(final int cursor) {
		setCursor(new Cursor(cursor));
	}

	/**
	 * Sets original image
	 *
	 * @param image
	 */
	public void setImage(final BufferedImage image) {
		originalImage = image;
		createResizedImage();
	}

}
