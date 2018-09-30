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

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsRenderer;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BigRom extends InstanceFactory {
	public static void draw(InstancePainter painter, int x, int y) {
		BigRomPorts ports = new BigRomPorts(painter.getAttributeValue(ATTR_ADDRESS_WIDTH), painter.getAttributeValue(ATTR_VALUE_WIDTH));
		BigRomContents contents = painter.getAttributeValue(BigRomContents.ATTR_CONTENTS);

		GraphicsRenderer g = new GraphicsRenderer(painter.getGraphics(), x, y);
		g.switchToWidth(2);
		g.run("l " + ports.chipWidth() + ",0 l 0,50 l -" + ports.chipWidth() + ",0 l 0,-50");

		g.move(5,5);
		int addressLen = (ports.addressWidth + 3) / 4;
		g.presetTextOnPlate(
				String.format("%1$" + addressLen + "s", contents.address.toString(16)).replace(' ', '0'),
				GraphicsRenderer.H_LEFT,
				GraphicsRenderer.V_TOP
		);
		g.move(0, 40);
		int valueLen = (ports.valueWidth + 3) / 4;
		g.presetTextOnPlate(
				String.format("%1$" + valueLen + "s", contents.getCurrent().toString(16)).replace(' ', '0'),
				GraphicsRenderer.H_LEFT,
				GraphicsRenderer.V_BOTTOM
		);
	}

	static final int DELAY = 10;

	public static final Attribute<Integer> ATTR_ADDRESS_WIDTH = Attributes
			.forInteger("addressWidth", Strings.getter("bigRomAddressWidth"));
	public static final Attribute<Integer> ATTR_VALUE_WIDTH = Attributes
			.forInteger("valueWidth", Strings.getter("bigRomValueWidth"));

	public BigRom() {
		super("BigRom", Strings.getter("bigRomComponent"));
		setAttributes(
				new Attribute[] {
						StdAttr.LABEL, StdAttr.LABEL_FONT,
						ATTR_ADDRESS_WIDTH, ATTR_VALUE_WIDTH, BigRomContents.ATTR_CONTENTS
				},
				new Object[] {
						"", StdAttr.DEFAULT_LABEL_FONT,
                        32, 32, null
				}
		);
		setIconName("bigRom.gif");
	}

	void computePorts(Instance instance) {
		BigRomPorts ports = new BigRomPorts(
				instance.getAttributeValue(ATTR_ADDRESS_WIDTH), instance.getAttributeValue(ATTR_VALUE_WIDTH)
		);
		instance.setPorts(ports.computePorts());
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.getAttributeSet().setValue(BigRomContents.ATTR_CONTENTS, new BigRomContents());
		Bounds bds = instance.getBounds();
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bds.getX()
				+ bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);
		computePorts(instance);
        instance.addAttributeListener();
	}

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
		BigRomPorts ports = new BigRomPorts(attrs.getValue(ATTR_ADDRESS_WIDTH), attrs.getValue(ATTR_VALUE_WIDTH));
        return Bounds.create(0, 0, ports.chipWidth(), 50);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == ATTR_ADDRESS_WIDTH || attr == ATTR_VALUE_WIDTH) {
            instance.recomputeBounds();
            computePorts(instance);
        }
    }

	@Override
	public void paintInstance(InstancePainter painter) {
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();

		draw(painter, x, y);

		painter.drawLabel();
        painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		BigRomPorts ports = new BigRomPorts(state.getAttributeValue(ATTR_ADDRESS_WIDTH), state.getAttributeValue(ATTR_VALUE_WIDTH));
		BigRomContents contents = state.getAttributeValue(BigRomContents.ATTR_CONTENTS);

		boolean isFullyDefined = true;
		long[] values = new long[ports.addressPortWidths.length];
		for (int i = 0; i < values.length && isFullyDefined; i++) {
		    Value value = state.getPortValue(i);
		    isFullyDefined &= value.isFullyDefined();
			values[i] = value.toLongValue();
		}

		if(isFullyDefined) {
			contents.address = ports.getInput(values);
			values = ports.getOutput(contents.getCurrent());
			for (int i = 0; i < ports.valuePortWidths.length; i++) {
				int value = 0;
				if(i < values.length) value = (int) (values[i] >> (32-ports.valuePortWidths[i]));
				state.setPort(ports.addressPortWidths.length + i,
						Value.createKnown(ports.valuePortWidths[i], value), DELAY
				);
			}
		} else {
			for (int i = 0; i < ports.valuePortWidths.length; i++) {
				state.setPort(ports.addressPortWidths.length + i,
						Value.createError(BitWidth.create(ports.valuePortWidths[i])), DELAY);
			}
		}
	}

    private static class BigRomPorts {
	    int addressWidth, valueWidth;
	    int[] addressPortWidths, valuePortWidths;

	    public BigRomPorts(int addressWidth, int valueWidth) {
	        this.addressWidth = addressWidth;
	        this.valueWidth = valueWidth;

			addressPortWidths = new int[(int)Math.ceil(addressWidth/32.0)];
			for (int width = addressWidth, i = 0; width > 0; width -= 32, i++) {
				addressPortWidths[i] = Math.min(32, width);
			}
			valuePortWidths = new int[(int)Math.ceil(valueWidth/32.0)];
			for (int width = valueWidth, i = 0; width > 0; width -= 32, i++) {
				valuePortWidths[i] = Math.min(32, width);
			}
		}

		BigInteger getInput(long[] values) {
			byte[] numberAsArray = new byte[values.length*4];
			for(int word = 0; word < values.length; word++) {
				long value = values[values.length-1-word];
				for(int b = 0; b < 4; b++) {
					int shift = (3 - b) * 8;
					numberAsArray[word*4 + b] = (byte)((value >> shift) & 0xFF);
				}
			}
			return new BigInteger(1, numberAsArray);
		}

		public long[] getOutput(BigInteger value) {
	    	int start = value.bitLength() % 8 == 0 ? 1 : 0; // if the end falls on a
			byte[] bytes = value.toByteArray();
			long[] words = new long[(int)Math.ceil(bytes.length/4.0)];
			for(int b = start; b < bytes.length; b++) {
				int word = (int)Math.floor(b/4.0);
				int shift = (3 - (b-start) % 4) * 8;
				words[word] |= (bytes[b] & 0xFF) << shift;
			}
			return words;
		}

		Port[] computePorts() {
			Port[] ports = new Port[addressPortWidths.length + valuePortWidths.length];

			for (int i = 0; i < addressPortWidths.length; i++) {
				ports[i] = setTooltip(new Port(70*i + 10, 0, Port.INPUT, Math.min(32, addressPortWidths[i])),
						Strings.getter("bigRomAddress"));
			}
			for (int i = 0; i < valuePortWidths.length; i++) {
				ports[addressPortWidths.length + i] = setTooltip(new Port(70*i + 10, 50, Port.OUTPUT, Math.min(32, valuePortWidths[i])),
						Strings.getter("bigRomValue"));
			}

			return ports;
		}

		private Port setTooltip(Port port, StringGetter tooltip) {
	    	port.setToolTip(tooltip);
	    	return port;
		}

		private int chipWidth() {
			int addressLen = (addressWidth + 3) / 4;
			int valueLen = (valueWidth + 3) / 4;
			int labelWidth = Math.max(addressLen, valueLen) * 9;
			labelWidth = (labelWidth + 9)/10; // divide by 10 and round up
			labelWidth *= 10; // multiply by 10 again. Now it's rounded up to a multiple of 10
			return labelWidth + 10;
		}
	}
}
