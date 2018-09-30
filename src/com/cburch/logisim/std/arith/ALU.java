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

package com.cburch.logisim.std.arith;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsRenderer;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;

public class ALU extends InstanceFactory {
	static Value compute(BitWidth width, Value a, Value b, ALUOperation operation, boolean signed) {
		int w = width.getWidth();
		if (a.isFullyDefined() && b.isFullyDefined()) {
		    long left = a.toLongValue();
		    long right = b.toLongValue();

		    if(signed) {
				int extension = (1 << (64 - w)) - 1;
				extension = extension << w;
				int signBit = 1 << (w - 1);
				if ((left & signBit) != 0) {
					left = left | extension;
				}
				if ((right & signBit) != 0) {
					right = right | extension;
				}
			}

			try {
			    long result = operation.run(left, right, w, signed);
				return Value.createKnown(width, (int)result);
			} catch(ArithmeticException e) {
				return Value.createError(width);
			}
		} else {
			return Value.createError(width);
		}
	}

	static final int PER_DELAY = 1;
	private static final int IN_OP = 0;
	private static final int IN_SIGNED = 1;
	private static final int IN_LEFT = 2;
	private static final int IN_RIGHT = 3;
	private static final int OUT = 4;

	public ALU() {
		super("ALU", Strings.getter("aluComponent"));
		setAttributes(new Attribute[] { StdAttr.WIDTH },
				new Object[] { BitWidth.create(8) });
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		setOffsetBounds(Bounds.create(0, 0, 60, 60));
		setIconName("alu.gif");

		Port[] ps = new Port[5];
		ps[IN_OP] = new Port(0, 30, Port.INPUT, 5);
		ps[IN_SIGNED] = new Port(0, 10, Port.INPUT, 1);
		ps[IN_LEFT] = new Port(20, 0, Port.INPUT, StdAttr.WIDTH);
		ps[IN_RIGHT] = new Port(40, 0, Port.INPUT, StdAttr.WIDTH);
		ps[OUT] = new Port(60, 30, Port.OUTPUT, StdAttr.WIDTH);
		ps[IN_OP].setToolTip(Strings.getter("aluOperationTip")); // +, -, *, u*, /, u/, %, u%, -x, <<, >>, >>>, >>> (rot), << (rot), ~, &, |, ^, !, &&, ||, ^, ==, !=, , u>, <, u<, =, u>=, <=, u<=
		ps[IN_SIGNED].setToolTip(Strings.getter("aluSignedTip"));
		ps[IN_LEFT].setToolTip(Strings.getter("aluLeftInputTip"));
		ps[IN_RIGHT].setToolTip(Strings.getter("aluRightInputTip"));
		ps[OUT].setToolTip(Strings.getter("aluOutputTip"));
		setPorts(ps);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics graphics = painter.getGraphics();
		Location loc = painter.getLocation();
		GraphicsRenderer g = new GraphicsRenderer(graphics, loc.getX(), loc.getY());

		painter.drawBounds();

		g.switchToWidth(2);
		g.run("l 60,0 l 0,60 l -60,0 l 0,-60 m 30,30");

		String bitGrid = " m 0,-25 l 0,50 m 5,0 l 0,-50 {l -5,0 m 5,5}*11";
		String bitwise = " r s m -25,0" + bitGrid + " r m 20,0" + bitGrid;
		String not = " m 5,0 l -20,-8 l 0,16 l 20,-8 m 5,0 e 5,5";
		String and = " a 15,-90,180 m 0,-15 l -15,0 l 0,30 l 15,0";
		String or = " m -18,-28 a 44,-85,47 m 0,56 a 44,85,-47 m -24,-28 a 32,-28,56";
		String xor = " m -15,-28 a 44,-85,47 m 0,56 a 44,85,-47 m -24,-28 a 32,-28,56 m -5,0 a 32,-28,56";
		String[] icons = new String[] {
				"m -15,0 l 30,0 m -15,-15 l 0,30",// +
				"m -15,0 l 30,0",// -
				"m -10,10 l 20,-20 m 0,20 l -20,-20",// *
				"m -10,15 l 20,-30",// /
				"s m -10,15 l 20,-30 r m -10,-10 e 5,5 m 20,20 e 5,5",// %
				"l -15,0 m 15,0 m 10,0 m -10,-10 l 20,20 m 0,-20 l -20,20",// -x
				"m 0,-10 l -20,10 l 20,10 m 20,-20 l -20,10 l 20,10",// <<
				"m 0,-10 l 20,10 l -20,10 m -20,-20 l 20,10 l -20,10",// >>
				"s m 0,-10 l -20,10 l 20,10 m 20,-20 l -20,10 l 20,10 r m -20,15 l 0,10 l 40,0 l 0,-10 m -5,5 l 5,-5 l 5,5",// << (rot)
				"s m 0,-10 l 20,10 l -20,10 m -20,-20 l 20,10 l -20,10 r m 20,15 l 0,10 l -40,0 l 0,-10 m -5,5 l 5,-5 l 5,5",// >> (rot)
				"s" + not + bitwise,// ~
				"s" + and + bitwise,// &
				"s" + or + bitwise,// |
				"s" + xor + bitwise,// ^
				not,// !
				and,// &&
				or,// ||
				xor,// ^
				"w 3 m -15,-8 l 30,0 m 0,16 l -30,0 w 2",// ==
				"s w 3 m -15,-8 l 30,0 m 0,16 l -30,0 r m -10,15 l 20,-30 w 2",// !=
				"w 3 m -10,-10 l 20,10 l -20,10 w 2",// >
				"w 3 m 10,-10 l -20,10 l 20,10 w 2",// <
				"w 3 m -10,-10 l 20,10 l -20,10 m 0,7 l 20,-10 w 2",// >=
				"w 3 m 10,-10 l -20,10 l 20,10 m 0,7 l -20,-10 w 2",// <=
		};


		Value op = painter.getPortValue(IN_OP);
		if(op.isFullyDefined()) {
			g.pushPosition();
			int opIndex = op.toIntValue();
			if(opIndex >= 0 && opIndex < icons.length) {
				g.run(icons[opIndex]);
			}
			g.popPosition();
			if(painter.getPortValue(IN_SIGNED) == Value.TRUE && ALUOperation.signed.contains(opIndex)) {
				g.run(" m -20,-20 m -3,0 l 6,0 m -3,-3 l 0,6 m -3,2 l 6,0");
			}
		}

		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		// get attributes
		BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);

		// compute outputs
		Value op = state.getPortValue(IN_OP);
		Value signed = state.getPortValue(IN_SIGNED);
		Value a = state.getPortValue(IN_LEFT);
		Value b = state.getPortValue(IN_RIGHT);
		Value out = Value.createError(dataWidth);
		if(op.isFullyDefined()) {
		    int opIndex = op.toIntValue();
		    if(opIndex >= 0 && opIndex < ALUOperation.operations.length) {
		        out = ALU.compute(dataWidth, a, b, ALUOperation.operations[opIndex], signed == Value.TRUE);
			}
		}

		// propagate them
		int delay = (dataWidth.getWidth() + 2) * PER_DELAY;
		state.setPort(OUT, out, delay);
	}

}
