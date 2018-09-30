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

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.prefs.AppPreferences;

class RegisterBlockData extends ClockState implements InstanceData {
	Value[] value;
	BitWidth width;

	public RegisterBlockData(BitWidth width, int count) {
        this.width = width;
		value = new Value[count];
		for (int i = 0; i < count; i++) {
			value[i] = createInitial(width);
		}
	}

	private Value createInitial(BitWidth width) {
		if(AppPreferences.Memory_Startup_Unknown.get()) {
			return Value.createUnknown(width);
		} else {
			return Value.createKnown(width, 0);
		}
	}

	public Value[] getValues() {
		return value;
	}

	public void ensureSize(BitWidth width, int size) {
	    if(value.length != size || width != this.width) {
	        Value[] newValue = new Value[size];
            for (int i = 0; i < size; i++) {
                if(i < value.length) {
                    newValue[i] = Value.createKnown(width, value[i].toIntValue());
                } else {
                    newValue[i] = createInitial(width);
                }
            }
            value = newValue;
        }
    }
}