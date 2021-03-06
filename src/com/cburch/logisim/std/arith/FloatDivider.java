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

import java.awt.*;

public class FloatDivider extends FloatArithmetic {
	static final int PER_DELAY = 1;
	private static final int IN0 = 0;
	private static final int IN1 = 1;
	private static final int OUT = 2;

	public FloatDivider() {
		super("FloatDivider", Strings.getter("floatDividerComponent"));
		setIconName("divider.gif");

		getPorts().get(IN0).setToolTip(Strings.getter("dividerInputTip"));
		getPorts().get(IN1).setToolTip(Strings.getter("dividerInputTip"));
		getPorts().get(OUT).setToolTip(Strings.getter("dividerOutputTip"));
	}

	@Override
	public float perform(float inputA, float inputB) {
		return inputA / inputB;
	}

	@Override
	public void drawIcon(Graphics g, int x, int y) {
		g.fillOval(x - 12, y - 7, 4, 4);
		g.drawLine(x - 15, y, x - 5, y);
		g.fillOval(x - 12, y + 3, 4, 4);
	}
}
