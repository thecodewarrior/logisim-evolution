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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class SimulatorTicker {
	private Simulator.PropagationManager manager;
    private ScheduledExecutorService executor;
    private ThreadFactory factory = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new UniquelyNamedThread(r, "Simulator Ticker");
			try {
				thread.setPriority(thread.getPriority()-1);
			} catch (SecurityException e) {
			} catch (IllegalArgumentException e) {
			}
			return thread;
		}
	};
	private long tickInterval = TimeUnit.NANOSECONDS.toSeconds(1);
	private boolean isTicking = false;

	public SimulatorTicker(Simulator.PropagationManager manager) {
		this.manager = manager;
	}

	private ScheduledExecutorService executor() {
		if(executor == null)
			executor = Executors.newSingleThreadScheduledExecutor(factory);
		return executor;
	}

	private void forceTick() {
		executor().schedule(runTick, 1, TimeUnit.MILLISECONDS);
	}

	private void startTicking() {
		isTicking = true;
		executor().scheduleAtFixedRate(runTick, 0, tickInterval, TimeUnit.NANOSECONDS);
	}

	private void stopTicking() {
		if(!isTicking || executor == null) return;
		isTicking = false;
		executor.shutdownNow();
		try {
			executor.awaitTermination(10, TimeUnit.SECONDS);
			if(!executor.isTerminated()) {
				executor = null;
				throw new RuntimeException("Failed to shutdown ticker within ten seconds");
			}
			executor = null;
		} catch(InterruptedException e) {
			executor = null;
			throw new RuntimeException("Interrupt while awaiting termination of ticker", e);
		}
	}

	private void restartTicking() {
		if(isTicking) {
			stopTicking();
			startTicking();
		}
	}

	Runnable runTick = new Runnable() {
		public void run() {
			manager.requestTick();
		}
	};

	synchronized void setAwake(boolean value) {
		if(isTicking != value) {
			if(value) {
				startTicking();
			} else {
				stopTicking();
			}
		}
	}

	public synchronized void setTickFrequency(long nanos) {
		tickInterval = nanos;
		restartTicking();
	}

	public synchronized void shutDown() {
		stopTicking();
	}

	public synchronized void tickOnce() {
		forceTick();
	}
}
