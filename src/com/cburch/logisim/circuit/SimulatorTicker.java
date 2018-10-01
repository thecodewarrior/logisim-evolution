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
import com.cburch.logisim.util.UniquelyNamedThread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class SimulatorTicker extends UniquelyNamedThread {
	private Simulator.PropagationManager manager;
	private final AtomicLong tickInterval = new AtomicLong();
	private final AtomicInteger forcedTicks = new AtomicInteger();

	private final AtomicBoolean shouldTick = new AtomicBoolean();
	private final AtomicBoolean complete = new AtomicBoolean();

	public SimulatorTicker(Simulator.PropagationManager manager) {
		super("SimulationTicker");
		this.manager = manager;
		tickInterval.set(1000000000);
		shouldTick.set(false);
		complete.set(false);
	}

	@Override
	public void run() {
		long lastTick = System.nanoTime();
		boolean willTick = false;
		while (!complete.get()) {
			while(forcedTicks.getAndDecrement() > 0)
				manager.requestTick();
			while (forcedTicks.get() < 0)
				forcedTicks.incrementAndGet();

			if(shouldTick.get() && willTick) {
				manager.requestTick();
				willTick = false;
			}

			try {
				long wait = tickInterval.get() - (System.nanoTime() - lastTick);
				if(wait < 0) {
					lastTick = System.nanoTime();
				} else if(wait > 50_000_000L) {
				    Thread.sleep(50);
				} else {
                    lastTick = System.nanoTime();
                    willTick = true;
					Thread.sleep((long)Math.floor(wait / 1_000_000.0), (int)(wait % 1_000_000));
                }
			} catch (InterruptedException e) {
			}
		}
	}

	synchronized void setAwake(boolean value) {
		shouldTick.set(value);
		if (value)
			notifyAll();
	}

	public synchronized void setTickFrequency(long nanos) {
	    tickInterval.set(nanos);
	}

	public synchronized void shutDown() {
		complete.set(true);
		notifyAll();
	}

	public synchronized void tickOnce() {
		forcedTicks.incrementAndGet();
		notifyAll();
	}
}
