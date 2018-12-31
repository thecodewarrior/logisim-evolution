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

package com.cburch.logisim.circuit;

import java.util.concurrent.CopyOnWriteArraySet;

class WireThread {
	private static int lastId = 0;
	private static synchronized int getNextId() {
		return lastId++;
	}
	public final int uniqueId = getNextId();

	private WireThread parent;
	private CopyOnWriteArraySet<CircuitWires.ThreadBundle> bundles = new CopyOnWriteArraySet<CircuitWires.ThreadBundle>();

	WireThread() {
		parent = this;
	}

	WireThread find() {
		WireThread ret = this;
		if (ret.parent != ret) {
			do
				ret = ret.parent;
			while (ret.parent != ret);
			this.parent = ret;
		}
		return ret;
	}

	CopyOnWriteArraySet<CircuitWires.ThreadBundle> getBundles() {
		return bundles;
	}

	void unite(WireThread other) {
		WireThread group = this.find();
		WireThread group2 = other.find();
		if (group != group2)
			group.parent = group2;
	}
}
