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
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;

public class FloatAdder extends FloatArithmetic {
	static final int PER_DELAY = 1;
	private static final int IN0 = 0;
	private static final int IN1 = 1;
	private static final int OUT = 2;

	public FloatAdder() {
		super("FloatAdder", Strings.getter("floatAdderComponent"));
		setIconName("adder.gif");

		getPorts().get(IN0).setToolTip(Strings.getter("adderInputTip"));
		getPorts().get(IN1).setToolTip(Strings.getter("adderInputTip"));
		getPorts().get(OUT).setToolTip(Strings.getter("adderOutputTip"));
	}

	@Override
	public float perform(float inputA, float inputB) {
		return inputA + inputB;
	}

	@Override
	public void drawIcon(Graphics g, int x, int y) {
		g.drawLine(x - 15, y, x - 5, y);
		g.drawLine(x - 10, y - 5, x - 10, y + 5);
	}
}
