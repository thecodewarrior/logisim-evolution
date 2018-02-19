/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.std.wiring.DurationAttribute;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

// TODO repropagate when rows/cols change

public class Display extends InstanceFactory implements ITickingInstanceFactory {
	private static class State extends ClockState implements InstanceData, Cloneable {
	    private static int PIXELS_PER_UNIT = 3;

		private MemoryImageSource imageSource;
		private Image drawingImage;

		private Graphics graphics;
		private BufferedImage image;
		private byte[] data;

		private int rows;
		private int cols;
		private int fadeSpeed;

		public State(int rows, int cols) {
			this.rows = -1;
			this.cols = -1;
			updateSize(rows, cols);
		}

		@Override
		public Object clone() {
			try {
				State ret = (State) super.clone();
				//TODO: Get around to actually cloning this
				return ret;
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}

		private void drawLine(int x1, int y1, int x2, int y2, int brightness) {
			graphics.setColor(new Color(brightness, brightness, brightness));
			GraphicsUtil.switchToWidth(graphics, 1);
		    graphics.drawLine(x1, y1, x2, y2);
		}

		private boolean doTicks(long curTicks) {
			for(int i = 0; i < data.length; i++) {
				data[i] = (byte)Math.max(0, (data[i] & 0xFF) - fadeSpeed);
			}
			return true;
		}

		private void clear() {
			for(int i = 0; i < data.length; i++) {
				data[i] = 0;
			}
		}

		private void updateSize(int rows, int cols) {
			if (this.rows != rows || this.cols != cols) {
				this.rows = rows;
				this.cols = cols;

				image = new BufferedImage(cols*PIXELS_PER_UNIT, rows*PIXELS_PER_UNIT, BufferedImage.TYPE_BYTE_GRAY);
				data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
				Graphics2D graphics2D = (Graphics2D)image.getGraphics();
				this.graphics = graphics2D;
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				imageSource = new MemoryImageSource(cols*PIXELS_PER_UNIT, rows*PIXELS_PER_UNIT, IndexColorModel.getRGBdefault(), data, 0, cols*PIXELS_PER_UNIT);
				imageSource.setAnimated(true);

				drawingImage = Toolkit.getDefaultToolkit().createImage(imageSource);
			}
		}

		private void updateColors(Color fg, Color bg) {
			int[] palette = new int[256];
			for(int i = 0; i < 256; i++) {
				int r, g, b;
				r = ( fg.getRed() * i )/255;
				g = ( fg.getGreen() * i )/255;
				b = ( fg.getBlue() * i )/255;
				r += ( bg.getRed() * (255-i))/255;
				g += ( bg.getGreen() * (255-i))/255;
				b += ( bg.getBlue() * (255-i))/255;
				palette[i] = ((r & 0xFF) << 16) + ((g & 0xFF) << 8) + (b & 0xFF);
			}
			IndexColorModel model = new IndexColorModel(8, 256, palette, 0, false, -1, DataBuffer.TYPE_BYTE);
			imageSource.newPixels(data, model, 0, cols*PIXELS_PER_UNIT);
		}

		private void updateFadeSpeed(int speed) {
			this.fadeSpeed = speed;
		}

		// source: https://stackoverflow.com/a/42463677
		public class AdditiveComposite implements Composite {
			public AdditiveComposite() {
			}

			public CompositeContext createContext(ColorModel srcColorModel,
												  ColorModel dstColorModel, RenderingHints hints) {
				return new AdditiveCompositeContext();
			}
		}
		private static class AdditiveCompositeContext implements CompositeContext {
			public AdditiveCompositeContext() {
			}

			public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
				int[] pxSrc = new int[src.getNumBands()];
				int[] pxDst = new int[dstIn.getNumBands()];
				int chans = Math.min(src.getNumBands(), dstIn.getNumBands());

				for (int x = 0; x < dstIn.getWidth(); x++) {
					for (int y = 0; y < dstIn.getHeight(); y++) {
						pxSrc = src.getPixel(x, y, pxSrc);
						pxDst = dstIn.getPixel(x, y, pxDst);

						int alpha = pxSrc.length > 3 ? pxSrc[3] : 255;

						for (int i = 0; i < 3 && i < chans; i++) {
							pxDst[i] = Math.min(255, (pxSrc[i] * alpha / 255) + (pxDst[i]));
							dstOut.setPixel(x, y, pxDst);
						}
					}
				}
			}

			@Override public void dispose() { }
		}
	}

	private static final Attribute<Integer> ATTR_MATRIX_COLS = Attributes
			.forIntegerRange("matrixcols", Strings.getter("ioMatrixCols"), 1, 32);
	private static final Attribute<Integer> ATTR_MATRIX_ROWS = Attributes
			.forIntegerRange("matrixrows", Strings.getter("ioMatrixRows"), 1, 32);
	private static final Attribute<Integer> ATTR_FADE_SPEED = Attributes
			.forIntegerRange("fadespeed", Strings.getter("ioFadeSpeed"), 0, 255);

	public Display() {
		super("Display", Strings.getter("displayComponent"));
		setIconName("dotmat.gif");
		setAttributes(new Attribute<?>[] {
				ATTR_MATRIX_COLS,
				ATTR_MATRIX_ROWS,
				Io.ATTR_ON_COLOR,
				Io.ATTR_OFF_COLOR,
				ATTR_FADE_SPEED
		}, new Object[] {
				8,
				8,
				Color.GREEN,
				Color.DARK_GRAY,
				4
		});
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if(attr == ATTR_MATRIX_COLS || attr == ATTR_MATRIX_ROWS) {
			instance.recomputeBounds();
		}
		updatePorts(instance);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		int cols = attrs.getValue(ATTR_MATRIX_COLS);
		int rows = attrs.getValue(ATTR_MATRIX_ROWS);
		int width = 10 * cols,
				height = 10 * rows + 20;
		return Bounds.create(0, -height, width, height);
	}

	private State getState(InstanceState state) {
		int cols = state.getAttributeValue(ATTR_MATRIX_COLS);
		int rows = state.getAttributeValue(ATTR_MATRIX_ROWS);
		Color fg = state.getAttributeValue(Io.ATTR_ON_COLOR);
		Color bg = state.getAttributeValue(Io.ATTR_OFF_COLOR);
		int fadeSpeed = state.getAttributeValue(ATTR_FADE_SPEED);

		State data = (State) state.getData();
		if (data == null) {
			data = new State(rows, cols);
			data.updateColors(fg, bg);
			data.updateFadeSpeed(fadeSpeed);
			state.setData(data);
		} else {
			data.updateSize(rows, cols);
			data.updateColors(fg, bg);
			data.updateFadeSpeed(fadeSpeed);
		}
		return data;
	}

	@Override
	public void paintInstance(InstancePainter painter) {

		State data = getState(painter);
		long ticks = painter.getTickCount();
		Bounds bounds = painter.getBounds();

		boolean showState = painter.getShowState();
		Graphics g = painter.getGraphics();

		g.drawImage(data.drawingImage, bounds.getX(), bounds.getY(), 10*data.cols, 10*data.rows, null, null);

//		int displayX = bounds.getX();
//		int displayY = bounds.getY();
//		int rows = data.rows;
		int cols = data.cols;
//		for (int row = 0; row < rows; row++) {
//			for (int col = 0; col < cols; col++) {
//				int x = displayX + 10 * col;
//				int y = displayY + 10 * row;
//				if (showState) {
//					Color c = data.getColor(row, col);
//					g.setColor(c);
//					g.fillRect(x, y, 10, 10);
//				} else {
//					g.setColor(Color.GRAY);
//					g.fillOval(x + 1, y + 1, 8, 8);
//				}
//			}
//		}
		g.setColor(Color.BLACK);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
		GraphicsUtil.switchToWidth(g, 1);
		g.drawPolyline(new int[] {
				bounds.getX(),
				bounds.getX() + 10,
				bounds.getX()
		}, new int[] {
				bounds.getY() + bounds.getHeight() - 15,
				bounds.getY() + bounds.getHeight() - 10,
				bounds.getY() + bounds.getHeight() - 5
		}, 3);

		painter.drawPorts();
	}

	@Override
	public boolean tick(InstanceState state) {
	    return getState(state).doTicks(state.getTickCount());
	}

	@Override
	public void propagate(InstanceState state) {
		int rows = state.getAttributeValue(ATTR_MATRIX_ROWS);
		int cols = state.getAttributeValue(ATTR_MATRIX_COLS);

		State data = getState(state);

		int port = 0;

		Object triggerType = state.getAttributeValue(StdAttr.TRIGGER);
		boolean triggered = data.updateClock(state.getPortValue(port++), triggerType);
		if(!triggered) return;

		Value enable = state.getPortValue(port++);
		if(!enable.isFullyDefined() || enable.toIntValue() == 0) return;

		Value x1v = state.getPortValue(port++);
		Value y1v = state.getPortValue(port++);
		Value x2v = state.getPortValue(port++);
		Value y2v = state.getPortValue(port++);
		Value brightness = state.getPortValue(port++);

		if(!(x1v.isFullyDefined() && y1v.isFullyDefined() && x2v.isFullyDefined() && y2v.isFullyDefined() && brightness.isFullyDefined())) return;

		// equivalent to (x1v / 256) * (rowsOrCols * PX_PER_UNIT), but adjusted to make the integer math work
		int x1 = (x1v.toIntValue() * cols * State.PIXELS_PER_UNIT) / 256;
		int y1 = (y1v.toIntValue() * rows * State.PIXELS_PER_UNIT) / 256;
		int x2 = (x2v.toIntValue() * cols * State.PIXELS_PER_UNIT) / 256;
		int y2 = (y2v.toIntValue() * rows * State.PIXELS_PER_UNIT) / 256;

		data.drawLine(x1, y1, x2, y2, brightness.toIntValue());
	}

	private void updatePorts(Instance instance) {
		ArrayList<Port> ports = new ArrayList<Port>();

		Port port;

		port = new Port(0, -10, Port.INPUT, 1); // clock
		port.setToolTip(Strings.getter("displayClockTip"));
		ports.add(port);

		port = new Port(10, 0, Port.INPUT, 1); // enable
		port.setToolTip(Strings.getter("displayEnableTip"));
		ports.add(port);

		port = new Port(30, 0, Port.INPUT, 8); // X1
		port.setToolTip(Strings.getter("displayX1Tip"));
		ports.add(port);

		port = new Port(40, 0, Port.INPUT, 8); // Y1
		port.setToolTip(Strings.getter("displayY1Tip"));
		ports.add(port);


		port = new Port(60, 0, Port.INPUT, 8); // X2
		port.setToolTip(Strings.getter("displayX1Tip"));
		ports.add(port);

		port = new Port(70, 0, Port.INPUT, 8); // Y2
		port.setToolTip(Strings.getter("displayY1Tip"));
		ports.add(port);

		port = new Port(90, 0, Port.INPUT, 8); // brightness
		port.setToolTip(Strings.getter("displayBrightnessTip"));
		ports.add(port);

		Port[] arr = ports.toArray(new Port[] {});
		instance.setPorts(arr);
	}
}
