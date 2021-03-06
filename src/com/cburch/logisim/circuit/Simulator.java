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

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.UniquelyNamedThread;

public class Simulator {

	class PropagationManager {
		private ThreadFactory factory = new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new UniquelyNamedThread(() -> {
					try {
						r.run();
					} catch (Exception e) {
						e.printStackTrace();
						exceptionEncountered = true;
						setIsRunning(false);
						javax.swing.SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								javax.swing.JOptionPane.showMessageDialog(null, "The simulator has crashed. Save your work and restart Logisim.");
							}
						});
					}
				}, "Propagation Manager");
				try {
					thread.setPriority(thread.getPriority()-1);
				} catch (SecurityException e) {
				} catch (IllegalArgumentException e) {
				}
				return thread;
			}
		};
		private ExecutorService executor;

		private Propagator propagator = null;
		private PropagationPoints stepPoints = new PropagationPoints();
		private final AtomicInteger ticksRequested = new AtomicInteger();
		private volatile int stepsRequested = 0;
		private volatile boolean propagateRequested = false;

		private final AtomicInteger queuedJobs = new AtomicInteger();

		private ExecutorService executor() {
			if(executor == null)
				executor = Executors.newSingleThreadExecutor(factory);
			return executor;
        }

		private void doTick() {
			ticksRequested.getAndDecrement();
			propagator.tick();
		}

		public Propagator getPropagator() {
			return propagator;
		}

		public void requestPropagate() {
			if (!propagateRequested) {
				propagateRequested = true;
				queuedJobs.getAndIncrement();
				executor().execute(this::propagateOrTickOrStep);
			}
		}

		public void requestReset() {
			if(executor != null) {
			    shutDown();
				queuedJobs.set(1);
				executor().execute(this::reset);
				if(isRunning) {
				    if(!propagateRequested) {
						propagateRequested = true;
						queuedJobs.getAndIncrement();
						executor().execute(this::propagateOrTickOrStep);
					}
				}
			}
		}

		public void requestTick() {
			if (ticksRequested.get() < 16) {
				ticksRequested.getAndIncrement();
				queuedJobs.getAndIncrement();
				executor().execute(this::propagateOrTickOrStep);
			}
		}

		private void reset() {
			queuedJobs.getAndDecrement();
			if (propagator != null) {
				propagator.reset();
			}
			firePropagationCompleted();
		}

		private void propagateOrTickOrStep() {
			queuedJobs.getAndDecrement();
			if (propagateRequested || ticksRequested.get() > 0
					|| stepsRequested > 0) {
				boolean ticked = false;
				propagateRequested = false;
				if (isRunning) {
					stepPoints.clear();
					stepsRequested = 0;
					if (propagator == null) {
						ticksRequested.set(0);
					} else {
						ticked = ticksRequested.get() > 0;
						if (ticked) {
							doTick();
						}
						do {
							propagateRequested = false;
							try {
								exceptionEncountered = false;
								propagator.propagate();
							} catch (UnsupportedOperationException thr) {
								exceptionEncountered = true;
								setIsRunning(false);
							} catch (Exception thr) {
								thr.printStackTrace();
								exceptionEncountered = true;
								setIsRunning(false);
							}
						} while (propagateRequested);
						if (isOscillating()) {
							setIsRunning(false);
							propagateRequested = false;
						}
					}
				} else {
					if (stepsRequested > 0) {
						if (ticksRequested.get() > 0) {
							doTick();
						}

						synchronized (this) {
							stepsRequested--;
						}
						exceptionEncountered = false;
						try {
							stepPoints.clear();
							propagator.step(stepPoints);
						} catch (Exception thr) {
							thr.printStackTrace();
							exceptionEncountered = true;
						}
					}
				}
				if (ticked) {
					fireTickCompleted();
				}
				firePropagationCompleted();
			}
		}

		public void setPropagator(Propagator value) {
			propagator = value;
		}

		public synchronized void shutDown() {
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
	}

	private boolean isRunning = true;
	private boolean isTicking = false;
	private boolean exceptionEncountered = false;
	private double tickFrequency = 1.0;
	private PropagationManager manager;
	private SimulatorTicker ticker;
	private ArrayList<SimulatorListener> listeners = new ArrayList<SimulatorListener>();

	public Simulator() {
		manager = new PropagationManager();
		ticker = new SimulatorTicker(manager);

		tickFrequency = 0.0;
		setTickFrequency(AppPreferences.TICK_FREQUENCY.get().doubleValue());
	}

	public void addSimulatorListener(SimulatorListener l) {
		listeners.add(l);
	}

	public void drawStepPoints(ComponentDrawContext context) {
		manager.stepPoints.draw(context);
	}

	void firePropagationCompleted() {
		SimulatorEvent e = new SimulatorEvent(this);
		for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
			l.propagationCompleted(e);
		}
	}

	void fireSimulatorStateChanged() {
		SimulatorEvent e = new SimulatorEvent(this);
		for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
			l.simulatorStateChanged(e);
		}
	}

	void fireTickCompleted() {
		SimulatorEvent e = new SimulatorEvent(this);
		for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
			l.tickCompleted(e);
		}
	}

	public CircuitState getCircuitState() {
		Propagator prop = manager.getPropagator();
		return prop == null ? null : prop.getRootState();
	}

	public double getTickFrequency() {
		return tickFrequency;
	}

	public boolean isExceptionEncountered() {
		return exceptionEncountered;
	}

	public boolean isOscillating() {
		Propagator prop = manager.getPropagator();
		return prop != null && prop.isOscillating();
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isTicking() {
		return isTicking;
	}

	public void removeSimulatorListener(SimulatorListener l) {
		listeners.remove(l);
	}

	private void renewTickerAwake() {
		ticker.setAwake(isRunning && isTicking && tickFrequency > 0);
	}

	public void requestPropagate() {
		manager.requestPropagate();
	}

	public void requestReset() {
		manager.requestReset();
	}

	public void setCircuitState(CircuitState state) {
		manager.setPropagator(state.getPropagator());
		renewTickerAwake();
	}

	public void setIsRunning(boolean value) {
		if (isRunning != value) {
			isRunning = value;
			renewTickerAwake();
			/*
			 * DEBUGGING - comment out: if (!value) flushLog(); //
			 */
			fireSimulatorStateChanged();
		}
	}

	public void setIsTicking(boolean value) {
		if (isTicking != value) {
			isTicking = value;
			renewTickerAwake();
			fireSimulatorStateChanged();
		}
	}

	public void setTickFrequency(double freq) {
		if (tickFrequency != freq) {
			long nanos = Math.round(1_000_000_000L / freq);
			tickFrequency = freq;
			ticker.setTickFrequency(nanos);
			renewTickerAwake();
			fireSimulatorStateChanged();
		}
	}

	public void shutDown() {
		ticker.shutDown();
		manager.shutDown();
	}

	public void step() {
		synchronized (manager) {
			manager.stepsRequested++;
			manager.notifyAll();
		}
	}

	public void tick() {
		ticker.tickOnce();
	}

	public void tickMain(int count) {
		while (count > 0) {
			ticker.tickOnce();
			count--;
			try {
				Thread.sleep(50);
			} catch (InterruptedException ex) {
				Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		}

	}

}
