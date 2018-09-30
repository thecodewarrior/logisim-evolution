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

package com.cburch.logisim.util;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

@SuppressWarnings("ConstantConditions")
public class GraphicsRenderer {
    private final Graphics graphics;
	private final LinkedList<Vec2i> polyline = new LinkedList<>();
	private final LinkedList<Vec2i> positionStack = new LinkedList<>();
	private Vec2i pos = new Vec2i(0, 0);
	private int width = 1;
	private final Vec2i offset;
	private Color color = Color.black;

	public GraphicsRenderer(Graphics g, int offsetX, int offsetY) {
		this.graphics = g;
		this.offset = new Vec2i(offsetX, offsetY);
	}

	public Vec2i getPos() {
		return new Vec2i(pos.x + offset.x, pos.y + offset.y);
	}

	public void run(String commands) {
	    LinkedList<String> elements = new LinkedList<>(Arrays.asList(commands.trim().split("\\s+|\\s*,\\s*")));

	    commandProcessing: while(elements.size() != 0) {
	    	final String command = elements.poll();
	    	switch(command) {
				case "M":
					moveAbsolute(Integer.parseInt(elements.poll()), Integer.parseInt(elements.poll()));
					break;
				case "m":
					move(Integer.parseInt(elements.poll()), Integer.parseInt(elements.poll()));
					break;
				case "L":
					lineToAbsolute(Integer.parseInt(elements.poll()), Integer.parseInt(elements.poll()));
					break;
				case "l":
					lineTo(Integer.parseInt(elements.poll()), Integer.parseInt(elements.poll()));
					break;
				default:
				    System.out.println("Error: unknown command name " + command);
					break commandProcessing;
			}
		}
		commit();
	}

	public GraphicsRenderer color(Color color) {
		this.color = color;
		return this;
	}

	public GraphicsRenderer moveAbsolute(int x, int y) {
	    commit();
	    pos.x = x;
	    pos.y = y;
	    return this;
	}

	public GraphicsRenderer move(int x, int y) {
		commit();
		pos.x += x;
		pos.y += y;
		return this;
	}

	public GraphicsRenderer pushPosition() {
	    positionStack.push(pos.clone());
		return this;
	}

	public GraphicsRenderer popPosition() {
		pos.copyFrom(positionStack.pop());
		return this;
	}

	public GraphicsRenderer peekPosition() {
		pos.copyFrom(positionStack.peek());
		return this;
	}

	public GraphicsRenderer discardPosition() {
		positionStack.pop();
		return this;
	}

	public GraphicsRenderer lineToAbsolute(int x, int y) {
		if(polyline.size() == 0) {
			polyline.push(getPos());
		}
		pos.x = x;
		pos.y = y;
		polyline.push(getPos());
		return this;
	}

	public GraphicsRenderer lineTo(int x, int y) {
		if(polyline.size() == 0) {
			polyline.push(getPos());
		}
		pos.x += x;
		pos.y += y;
		polyline.push(getPos());
		return this;
	}

	public GraphicsRenderer commit() {
		graphics.setColor(color);
		if(polyline.size() != 0) {
		    useWidth();
			graphics.drawPolyline(
			        polyline.stream().mapToInt(i -> i.x).toArray(),
					polyline.stream().mapToInt(i -> i.y).toArray(),
					polyline.size()
			);
			polyline.clear();
		}
		return this;
	}

	public GraphicsRenderer fill(Rectangle rectangle) {
	    commit();
		graphics.setColor(color);
		graphics.fillRect(rectangle.x + getPos().x, rectangle.y + getPos().y, rectangle.width, rectangle.height);
		return this;
	}

	public void drawCenteredArc(int r, int start, int dist) {
		commit();
		graphics.setColor(color);
		graphics.drawArc(getPos().x - r, getPos().y - r, 2 * r, 2 * r, start, dist);
	}

	public void drawCenteredText(String text) {
		drawText(text, H_CENTER, V_CENTER);
	}
	
	public void drawCenteredText(Font font, String text, Color fg, Color bg) {
		drawText(font, text, H_CENTER, V_CENTER, fg, bg);
	}
	
	public void drawCenteredColoredText(String text, Color fg, Color bg) {
		drawText(text, H_CENTER, V_CENTER, fg, bg);
	}

	public void drawText(Font font, String text, int halign, int valign, Color fg, Color bg) {
		Font oldfont = graphics.getFont();
		if (font != null)
			graphics.setFont(font);
		drawText(text, halign, valign, fg, bg);
		if (font != null)
			graphics.setFont(oldfont);
	}

	
	public void drawText(Font font, String text, int halign, int valign) {
		Font oldfont = graphics.getFont();
		if (font != null)
			graphics.setFont(font);
		drawText(text, halign, valign);
		if (font != null)
			graphics.setFont(oldfont);
	}

	public void drawText(String text, int halign, int valign) {
		commit();
		if (text.length() == 0)
			return;
		Rectangle bd = getTextBounds(text, halign, valign);
		graphics.drawString(text, bd.x + getPos().x, bd.y + getPos().y + graphics.getFontMetrics().getAscent());
	}
	
	public void drawText(String text, int halign, int valign, Color fg, Color bg) {
		commit();
		if (text.length() == 0)
			return;
		Rectangle bd = getTextBounds(text, halign, valign);
		if(graphics instanceof Graphics2D) {
			((Graphics2D) graphics).setPaint(bg);
			graphics.fillRect(bd.x + getPos().x, bd.y + getPos().y, bd.width, bd.height);
			((Graphics2D) graphics).setPaint(fg);
			((Graphics2D) graphics).drawString(text, bd.x + getPos().x, bd.y + getPos().y + graphics.getFontMetrics().getAscent());
		} else {
			graphics.drawString(text, bd.x + getPos().x, bd.y + getPos().y + graphics.getFontMetrics().getAscent());
		}
	}

	public Rectangle getTextBounds(Font font, String text, int halign, int valign) {
		if (graphics == null)
			return new Rectangle(0, 0, 0, 0);
		Font oldfont = graphics.getFont();
		if (font != null)
			graphics.setFont(font);
		Rectangle ret = getTextBounds(text, halign, valign);
		if (font != null)
			graphics.setFont(oldfont);
		return ret;
	}

	public Rectangle getTextBounds(String text, int halign, int valign) {
		if (graphics == null)
			return new Rectangle(0, 0, 0, 0);
		FontMetrics mets = graphics.getFontMetrics();
		int width = mets.stringWidth(text);
		int ascent = mets.getAscent();
		int descent = mets.getDescent();
		int height = ascent + descent;

		Rectangle ret = new Rectangle(0, 0, width, height);
		switch (halign) {
		case H_CENTER:
			ret.translate(-(width / 2), 0);
			break;
		case H_RIGHT:
			ret.translate(-width, 0);
			break;
		default:
			;
		}
		switch (valign) {
		case V_TOP:
			break;
		case V_CENTER:
			ret.translate(0, -(ascent / 2));
			break;
		case V_CENTER_OVERALL:
			ret.translate(0, -(height / 2));
			break;
		case V_BASELINE:
			ret.translate(0, -ascent);
			break;
		case V_BOTTOM:
			ret.translate(0, -height);
			break;
		default:
			;
		}
		return ret;
	}

	public void useWidth() {
		if (graphics instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) graphics;
			g2.setStroke(new BasicStroke((float) width));
		}
	}

	public void switchToWidth(int width) {
		commit();
		this.width = width;
	}

	public static final int H_LEFT = -1;

	public static final int H_CENTER = 0;

	public static final int H_RIGHT = 1;
	public static final int V_TOP = -1;

	public static final int V_CENTER = 0;
	public static final int V_BASELINE = 1;
	public static final int V_BOTTOM = 2;

	public static final int V_CENTER_OVERALL = 3;

	public void presetClockInput(Object attr) {
		if (attr.equals(StdAttr.TRIG_HIGH) || attr.equals(StdAttr.TRIG_LOW)) {
		    move(5, 0);
			drawText("E", H_LEFT, V_CENTER);
			move(-5, 0);
		} else {
		    run("m 0,-5 l 10,5 l -10,5 m 0,-5");
		}
		if (attr.equals(StdAttr.TRIG_FALLING) || attr.equals(StdAttr.TRIG_LOW)) {
			switchToWidth(2);
			useWidth();
			graphics.drawOval(getPos().x-10, getPos().y-5, 10, 10);
		} else {
			switchToWidth(2);
			run("l -10,0 m 10,0");
		}
	}

	public void presetValue(Value value, int halign, int valign) {
		int bits = value.getBitWidth().getWidth();
		String str = "";
		if (value.isFullyDefined()) {
			str = StringUtil.toHexString(bits, value.toIntValue());
		} else if(value.isErrorValue()) {
			int len = (bits + 3) / 4;
			for (int i = 0 ; i < len ; i++)
				str = str.concat("E");
		} else {
			int len = (bits + 3) / 4;
			for (int i = 0 ; i < len ; i++)
				str = str.concat("?");
		}
		presetTextOnPlate(str, halign, valign);
	}

	public void presetTextOnPlate(String str, int halign, int valign) {
		final int marginX = 3;
		final int marginY = 1;

		int shiftX = 0;
		int shiftY = 0;
		if(halign == H_LEFT) shiftX = marginX;
		if(halign == H_RIGHT) shiftX = -marginX;
		if(valign == V_TOP) shiftY = marginY;
		if(valign == V_BOTTOM) shiftY = -marginY;

		move(shiftX, shiftY);
		Rectangle bounds = getTextBounds(str, halign, valign);
		color(Color.lightGray);
		fill(new Rectangle(bounds.x - marginX, bounds.y - marginY, bounds.width + 2*marginX, bounds.height + 2*marginY));
		color(Color.black);
		drawText(str, halign, valign);

		move(-shiftX, -shiftY);
	}

}
