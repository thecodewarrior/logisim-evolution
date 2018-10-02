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

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;

import java.util.Arrays;

class TickCounter implements SimulatorListener {
	private double tickFrequency;
	private long lastTick;
	private long sampleTime;
	private int sampleCount;
	private int durationHead;
	private long[] durations = new long[] { 0 };

	public void clear() {
	    if(sampleCount != 0) {
			Arrays.fill(durations, 0);
		}
		durationHead = 0;
		sampleCount = 0;
		sampleTime = 0;
		lastTick = 0;
	}

	public String getTickRate() {
		if (sampleCount == 0 || sampleTime == 0) {
			return "";
		} else {
			double average = sampleCount / (sampleTime / 1_000_000_000.0);
			if (average >= 1000.0) {
				return Strings.get("tickRateKHz", String.format("%.3f", average/1000.0));
			} else {
				return Strings.get("tickRateHz", String.format("%.3f", average));
			}
		}
	}

	public void propagationCompleted(SimulatorEvent e) {
		Simulator sim = e.getSource();
		if (!sim.isTicking()) {
		    clear();
		}
	}

	public void simulatorStateChanged(SimulatorEvent e) {
		propagationCompleted(e);
	}

	public void tickCompleted(SimulatorEvent e) {
		Simulator sim = e.getSource();
		if (!sim.isTicking()) {
		    clear();
		} else {
			double freq = sim.getTickFrequency();
			if (freq != tickFrequency) {
				tickFrequency = freq;
			    clear();
				durations = new long[(int)Math.ceil(freq)];
			}
			if(lastTick == 0) {
			    lastTick = System.nanoTime();
			    return;
			}

			long duration = System.nanoTime() - lastTick;
			lastTick = System.nanoTime();
			sampleTime -= durations[durationHead];
			sampleTime += duration;
			durations[durationHead] = duration;
			durationHead = (durationHead+1) % durations.length;
			sampleCount = Math.min(sampleCount + 1, durations.length);
		}
	}
}
