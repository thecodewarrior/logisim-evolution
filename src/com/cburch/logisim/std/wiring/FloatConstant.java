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

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FloatConstant extends InstanceFactory {
	private static class ConstantAttributes extends AbstractAttributeSet {
		private Direction facing = Direction.EAST;
		private float value = 0;

		@Override
		protected void copyInto(AbstractAttributeSet destObj) {
			ConstantAttributes dest = (ConstantAttributes) destObj;
			dest.facing = this.facing;
			dest.value = this.value;
		}

		@Override
		public List<Attribute<?>> getAttributes() {
			return ATTRIBUTES;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <V> V getValue(Attribute<V> attr) {
			if (attr == StdAttr.FACING)
				return (V) facing;
			if (attr == ATTR_VALUE)
				return (V) Double.valueOf(value);
			return null;
		}

		@Override
		public <V> void setValue(Attribute<V> attr, V value) {
			if (attr == StdAttr.FACING) {
				facing = (Direction) value;
			} else if (attr == ATTR_VALUE) {
				this.value = (float)((Double) value).doubleValue();
			} else {
				throw new IllegalArgumentException("unknown attribute " + attr);
			}
			fireAttributeValueChanged(attr, value,null);
		}
	}

	public static final Attribute<Double> ATTR_VALUE = Attributes
			.forDouble("value", Strings.getter("constantFloatValueAttr"));

	public static InstanceFactory FACTORY = new FloatConstant();

	private static final Color BACKGROUND_COLOR = new Color(230, 230, 230);

	private static final List<Attribute<?>> ATTRIBUTES = Arrays
			.asList(StdAttr.FACING, ATTR_VALUE);

	public FloatConstant() {
		super("FloatConstant", Strings.getter("floatConstantComponent"));
		setFacingAttribute(StdAttr.FACING);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new ConstantAttributes();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		Bounds ret = null;
		if (facing == Direction.EAST) {
			ret = Bounds.create(-76, -8, 76, 16);
		} else if (facing == Direction.WEST) {
			ret = Bounds.create(0, -8, 76, 16);
		} else if (facing == Direction.SOUTH) {
			ret = Bounds.create(-38, -16, 76, 16);
		} else if (facing == Direction.NORTH) {
			ret = Bounds.create(-38, 0, 76, 16);
		}
		if (ret == null) {
			throw new IllegalArgumentException("unrecognized arguments " + facing);
		}
		return ret;
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			instance.recomputeBounds();
		} else if (attr == ATTR_VALUE) {
			instance.fireInvalidated();
		}
	}

	private String valueToText(float v) {
		int width = 9;
		String vStr = String.format("% ." + width + "g", v);
		if(vStr.contains("e")) {
			String exponent = vStr.substring(vStr.indexOf("e"));
			String number = vStr.substring(0, vStr.indexOf("e"));
			vStr = number.substring(0, Math.min(number.length(), width-exponent.length())) + exponent;
		}
		vStr = vStr.replaceAll("\\.?0+$", "");
		return vStr + "f";
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		float v = (float)painter.getAttributeValue(ATTR_VALUE).doubleValue();
		String vStr = valueToText(v);

		Bounds bds = getOffsetBounds(painter.getAttributeSet());
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.fillOval(-2, -2, 5, 5);
		GraphicsUtil.drawCenteredText(g, vStr, bds.getX() + bds.getWidth() / 2,
				bds.getY() + bds.getHeight() / 2);
	}

	//
	// painting methods
	//
	@Override
	public void paintIcon(InstancePainter painter) {
		int pinx = 16;
		int piny = 9;
		Direction dir = painter.getAttributeValue(StdAttr.FACING);
		if (dir == Direction.EAST) {
		} // keep defaults
		else if (dir == Direction.WEST) {
			pinx = 4;
		} else if (dir == Direction.NORTH) {
			pinx = 9;
			piny = 4;
		} else if (dir == Direction.SOUTH) {
			pinx = 9;
			piny = 16;
		}

		Graphics g = painter.getGraphics();
		g.setFont(g.getFont().deriveFont(9.0f));
		GraphicsUtil.drawCenteredText(g, "Xf", 10, 9);
		g.fillOval(pinx, piny, 3, 3);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Bounds bds = painter.getOffsetBounds();
		float floatValue = (float)painter.getAttributeValue(ATTR_VALUE).doubleValue();
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();

		Graphics g = painter.getGraphics();
		if (painter.shouldDrawColor()) {
			g.setColor(BACKGROUND_COLOR);
			g.fillRect(x + bds.getX(), y + bds.getY(), bds.getWidth(),
					bds.getHeight());
		}
		g.setColor(Color.BLACK);
		GraphicsUtil.drawCenteredText(g, valueToText(floatValue), x + bds.getX()
				+ bds.getWidth() / 2, y + bds.getY() + bds.getHeight() / 2
				- 2);
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		float value = (float)state.getAttributeValue(ATTR_VALUE).doubleValue();
		state.setPort(0, Value.createFloat(value), 1);
	}

	private void updatePorts(Instance instance) {
		Port[] ps = { new Port(0, 0, Port.OUTPUT, BitWidth.FLOAT) };
		instance.setPorts(ps);
	}

	// TODO: Allow editing of value via text tool/attribute table
}
