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

import java.awt.*;

import static com.cburch.logisim.std.memory.RegisterBlock.PortType.*;

public class RegisterBlock extends InstanceFactory {
	public static void draw(InstancePainter painter, int x, int y) {
        RegisterBlockData data = fixData(painter);
		RegisterBlockPorts ports = new RegisterBlockPorts(painter.getAttributeValue(ATTR_REGISTER_COUNT), painter.getAttributeValue(ATTR_PORT_COUNT));

		GraphicsRenderer g = new GraphicsRenderer(painter.getGraphics(), x, y);
		g.switchToWidth(2);
		g.run("M 0,0 l 100,0 l 0,30 l -100,0 l 0,-30 m 0,20");
		g.presetClockInput(painter.getAttributeValue(StdAttr.TRIGGER));
		g.run("m 0,-10 l -10,0 m 15,0");
		g.drawText("R", GraphicsRenderer.H_LEFT, GraphicsRenderer.V_CENTER);
		g.run("m -5,-10");

        for (int i = 0; i < ports.registerCount; i++) {
            g.moveAbsolute(10, 30+40*i);
            drawRegister(g, data.getValues()[i], true);
        }

		for (int i = 0; i < ports.portCount; i++) {
			Value address = painter.getPortValue(ports.portIndex(i, ADDRESS));
			g.moveAbsolute(10, -40 -60*i);
			if(address.isFullyDefined()) {
				if(address.toIntValue() >= 0 && address.toIntValue() < data.getValues().length) {
					drawRegister(g, data.getValues()[address.toIntValue()], false);
				} else {
					drawRegister(g, Value.createError(painter.getAttributeValue(StdAttr.WIDTH)), false);
				}
			} else if(address.isErrorValue()) {
				drawRegister(g, Value.createError(painter.getAttributeValue(StdAttr.WIDTH)), false);
			} else {
                drawRegister(g, Value.createUnknown(painter.getAttributeValue(StdAttr.WIDTH)), false);
			}
			g.moveAbsolute(10, -40 -60*i);
			drawPort(g, address);
		}
	}

	private static void drawRegister(GraphicsRenderer g, Value value, boolean drawBottom) {
		if(drawBottom)
			g.run("l 0,40   l 80,0   l 0,-40 m -80,0 ");
		else
			g.run("l 0,40   m 80,0   l 0,-40 m -80,0 ");
		g.run("m -10,10 l 10,0 m -10,20 ");
		g.run("l 10,0 m -5,-5 l 5,5 l -5,5 m 5,-5");
		g.run("m 80,0");
		g.run("l 10,0 m -5,-5 l 5,5 l -5,5 m 5,-5");
		g.run("m 0,-20 l -10,0");

		g.run("m -40,10");
		g.presetValue(value, GraphicsRenderer.H_CENTER, GraphicsRenderer.V_CENTER);
	}

    private static void drawPort(GraphicsRenderer g, Value addressValue) {
		g.pushPosition();
	    g.run("l -10,0 l 0,-20 l 90,0 l 0,20 l -90,0 l 0,-20");

		g.popPosition();
        g.run("m -10,-10 l -10,0 m 10,-10 m 1,2");
        g.presetValue(addressValue, GraphicsRenderer.H_LEFT, GraphicsRenderer.V_TOP);
    }

	static final int DELAY = 8;

	public static final Attribute<Integer> ATTR_REGISTER_COUNT = Attributes
			.forIntegerRange("registerCount", Strings.getter("registerBlockRegisterCount"), 1, 16);
	public static final Attribute<Integer> ATTR_PORT_COUNT = Attributes
			.forIntegerRange("portCount", Strings.getter("registerBlockPortCount"), 0, 8);

	public RegisterBlock() {
		super("RegisterBlock", Strings.getter("registerBlockComponent"));
		setAttributes(
				new Attribute[] {
						StdAttr.WIDTH, StdAttr.TRIGGER,
						StdAttr.LABEL, StdAttr.LABEL_FONT,
						ATTR_REGISTER_COUNT, ATTR_PORT_COUNT,
				},
				new Object[] {
						BitWidth.create(8), StdAttr.TRIG_RISING,
						"", StdAttr.DEFAULT_LABEL_FONT,
						1, 0,
				}
		);
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		setIconName("register.gif");
	}

	void computePorts(Instance instance) {
		RegisterBlockPorts ports = new RegisterBlockPorts(
				instance.getAttributeValue(ATTR_REGISTER_COUNT), instance.getAttributeValue(ATTR_PORT_COUNT)
		);
		instance.setPorts(ports.computePorts());
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		Bounds bds = instance.getBounds();
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bds.getX()
				+ bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);
		computePorts(instance);
        instance.addAttributeListener();
	}

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
        int registers = attrs.getValue(ATTR_REGISTER_COUNT);
		int ports = attrs.getValue(ATTR_PORT_COUNT);
		int registerHeight = registers * 40;
		int portHeight = ports * 60;
        return Bounds.create(-10, -portHeight, 110, 30 + portHeight + registerHeight);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == ATTR_REGISTER_COUNT || attr == ATTR_PORT_COUNT) {
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
		BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
		int registerCount = state.getAttributeValue(ATTR_REGISTER_COUNT);
		int portCount = state.getAttributeValue(ATTR_PORT_COUNT);
		RegisterBlockPorts ports = new RegisterBlockPorts(registerCount, portCount);
		Object triggerType = state.getAttributeValue(StdAttr.TRIGGER);
		RegisterBlockData data = (RegisterBlockData) state.getData();

		fixData(state);

		boolean triggered = data.updateClock(state.getPortValue(RegisterBlockPorts.clock), triggerType);
        boolean isReset = state.getPortValue(RegisterBlockPorts.globalReset) == Value.TRUE;

        // the direct controls take precedence, so compute the addressable ports first.
		for (int i = 0; i < portCount; i++) {
			Value addressValue = state.getPortValue(ports.portIndex(i, ADDRESS));
			if(addressValue.isFullyDefined()) {
				int address = addressValue.toIntValue();
				if(address < 0 || address >= registerCount) {
				    continue;
				}

				Value newValue = data.getValues()[address];
				if (state.getPortValue(ports.portIndex(i, RESET)) == Value.TRUE) {
					newValue = Value.createKnown(dataWidth, 0);
				} else if (triggered && state.getPortValue(ports.portIndex(i, WRITE_ENABLE)) == Value.TRUE) {
					newValue = state.getPortValue(ports.portIndex(i, INPUT));
				}
				data.getValues()[address] = newValue;
			}
		}

        for (int i = 0; i < registerCount; i++) {
            Value newValue = data.getValues()[i];
            if (isReset || state.getPortValue(ports.registerIndex(i, RESET)) == Value.TRUE) {
                newValue = Value.createKnown(dataWidth, 0);
            } else if (triggered && state.getPortValue(ports.registerIndex(i, WRITE_ENABLE)) == Value.TRUE) {
                newValue = state.getPortValue(ports.registerIndex(i, INPUT));
            }
			data.getValues()[i] = newValue;
        }

        // now that everything has been updated, push those values to the outputs
		for (int i = 0; i < portCount; i++) {
			Value addressValue = state.getPortValue(ports.portIndex(i, ADDRESS));
			if (!addressValue.isUnknown()) {
				int address = addressValue.isErrorValue() ? -1 : addressValue.toIntValue();

				Value storedValue;

				if(address < 0 || address >= registerCount)
					storedValue = Value.createError(dataWidth);
				else if(addressValue.isErrorValue())
					storedValue = Value.createError(dataWidth);
				else
					storedValue = data.getValues()[address];

				if (state.getPortValue(ports.portIndex(i, READ_ENABLE)) != Value.FALSE) {
					state.setPort(ports.portIndex(i, OUTPUT), storedValue, DELAY);
				} else {
					state.setPort(ports.portIndex(i, OUTPUT), Value.createUnknown(dataWidth), DELAY);
				}
				state.setPort(ports.portIndex(i, VALUE), storedValue, DELAY);
			} else {
				state.setPort(ports.portIndex(i, OUTPUT), Value.createUnknown(dataWidth), DELAY);
				state.setPort(ports.portIndex(i, VALUE), Value.createUnknown(dataWidth), DELAY);
			}
		}

		for (int i = 0; i < registerCount; i++) {
			Value storedValue = data.getValues()[i];
			if (state.getPortValue(ports.registerIndex(i, READ_ENABLE)) != Value.FALSE) {
				state.setPort(ports.registerIndex(i, OUTPUT), storedValue, DELAY);
			} else {
				state.setPort(ports.registerIndex(i, OUTPUT), Value.createUnknown(dataWidth), DELAY);
			}
			state.setPort(ports.registerIndex(i, VALUE), storedValue, DELAY);
		}

	}

	private static RegisterBlockData fixData(InstancePainter painter) {
	    BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
	    int registerCount = painter.getAttributeValue(ATTR_REGISTER_COUNT);
	    RegisterBlockData data = (RegisterBlockData)painter.getData();
        if (data == null) {
            data = new RegisterBlockData(width, registerCount);
            painter.setData(data);
        }
        data.ensureSize(width, registerCount);
        return data;
    }

    private static RegisterBlockData fixData(InstanceState state) {
        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        int registerCount = state.getAttributeValue(ATTR_REGISTER_COUNT);
        RegisterBlockData data = (RegisterBlockData)state.getData();
        if (data == null) {
            data = new RegisterBlockData(width, registerCount);
            state.setData(data);
        }
        data.ensureSize(width, registerCount);
        return data;
    }

    private static class RegisterBlockPorts {
		static final int clock = 0;
		static final int globalReset = 1;

	    int registerCount, portCount;

	    public RegisterBlockPorts(int registerCount, int portCount) {
	    	this.registerCount = registerCount;
	    	this.portCount = portCount;
		}

		int size() {
	    	return 2 +
					registerCount * PortType.registerTypes.length +
					portCount * PortType.portTypes.length;
		}

		int registerIndex(int i, PortType type) {
	        return 2 +
                    PortType.registerTypes.length * i + type.ordinal();
		}

		int portIndex(int i, PortType type) {
			return 2 +
					PortType.registerTypes.length * registerCount +
					PortType.portTypes.length * i + type.ordinal();
		}

		Port[] computePorts() {
			Port[] ports = new Port[size()];
			ports[0] = setTooltip(new Port(-10, 20, Port.INPUT, 1),
					Strings.getter("registerBlockClockTooltip"));
			ports[1] = setTooltip(new Port(-10, 10, Port.INPUT, 1),
					Strings.getter("registerBlockGlobalResetTooltip"));

			for (int i = 0; i < registerCount; i++) {
				int y = 30+40*i;
				ports[registerIndex(i, WRITE_ENABLE)] = setTooltip(new Port(  0, y+10, Port.INPUT, 1),
						Strings.getter("registerBlockWriteEnableTooltip"));
				ports[registerIndex(i, READ_ENABLE )] = setTooltip(new Port(100, y+10, Port.INPUT, 1),
						Strings.getter("registerBlockReadEnableTooltip"));
				ports[registerIndex(i, RESET       )] = setTooltip(new Port( 10, y+20, Port.INPUT, 1),
						Strings.getter("registerBlockResetTooltip"));
				ports[registerIndex(i, INPUT       )] = setTooltip(new Port(  0, y+30, Port.INPUT, StdAttr.WIDTH),
						Strings.getter("registerBlockInputTooltip"));
				ports[registerIndex(i, OUTPUT      )] = setTooltip(new Port(100, y+30, Port.OUTPUT, StdAttr.WIDTH),
						Strings.getter("registerBlockOutputTooltip"));
				ports[registerIndex(i, VALUE       )] = setTooltip(new Port( 90, y+20, Port.OUTPUT, StdAttr.WIDTH),
						Strings.getter("registerBlockValueTooltip"));
			}

			for (int i = 0; i < portCount; i++) {
				int y = -40 -60*i;
				ports[portIndex(i, WRITE_ENABLE)] = setTooltip(new Port(  0, y+10, Port.INPUT, 1),
						Strings.getter("registerBlockWriteEnableTooltip"));
				ports[portIndex(i, READ_ENABLE )] = setTooltip(new Port(100, y+10, Port.INPUT, 1),
						Strings.getter("registerBlockReadEnableTooltip"));
				ports[portIndex(i, RESET       )] = setTooltip(new Port( 10, y+20, Port.INPUT, 1),
						Strings.getter("registerBlockResetTooltip"));
				ports[portIndex(i, INPUT       )] = setTooltip(new Port(  0, y+30, Port.INPUT, StdAttr.WIDTH),
						Strings.getter("registerBlockInputTooltip"));
				ports[portIndex(i, OUTPUT      )] = setTooltip(new Port(100, y+30, Port.OUTPUT, StdAttr.WIDTH),
						Strings.getter("registerBlockOutputTooltip"));
				ports[portIndex(i, VALUE       )] = setTooltip(new Port( 90, y+20, Port.OUTPUT, StdAttr.WIDTH),
						Strings.getter("registerBlockValueTooltip"));
				ports[portIndex(i, ADDRESS     )] = setTooltip(new Port(-10, y-10, Port.INPUT, addressBits()),
						Strings.getter("registerBlockAddressTooltip"));
			}

			return ports;
		}

		private Port setTooltip(Port port, StringGetter tooltip) {
	    	port.setToolTip(tooltip);
	    	return port;
		}

		int addressBits() {
	        return 32-Integer.numberOfLeadingZeros(registerCount-1);
		}
	}

	enum PortType {
	    WRITE_ENABLE, READ_ENABLE, RESET,
		INPUT, OUTPUT, VALUE,
		ADDRESS;

	    static PortType[] registerTypes = new PortType[] { WRITE_ENABLE, READ_ENABLE, RESET, INPUT, OUTPUT, VALUE };
		static PortType[] portTypes = new PortType[] { WRITE_ENABLE, READ_ENABLE, RESET, INPUT, OUTPUT, VALUE, ADDRESS };
	}
}
