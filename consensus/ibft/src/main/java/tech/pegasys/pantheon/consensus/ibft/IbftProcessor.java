/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.consensus.ibft;

import tech.pegasys.pantheon.consensus.ibft.ibftevent.IbftEvent;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Execution context for draining queued ibft events and applying them to a maintained state */
public class IbftProcessor implements Runnable {

  private static final Logger LOG = LogManager.getLogger();

  private final IbftEventQueue incomingQueue;
  private final ScheduledExecutorService roundTimerExecutor;
  private volatile boolean shutdown = false;
  private final EventMultiplexer eventMultiplexer;

  /**
   * Construct a new IbftProcessor
   *
   * @param incomingQueue The event queue from which to drain new events
   * @param eventMultiplexer an object capable of handling any/all IBFT events
   */
  public IbftProcessor(
      final IbftEventQueue incomingQueue, final EventMultiplexer eventMultiplexer) {
    // Spawning the round timer with a single thread as we should never have more than 1 timer in
    // flight at a time
    this(incomingQueue, eventMultiplexer, Executors.newSingleThreadScheduledExecutor());
  }

  @VisibleForTesting
  IbftProcessor(
      final IbftEventQueue incomingQueue,
      final EventMultiplexer eventMultiplexer,
      final ScheduledExecutorService roundTimerExecutor) {
    this.incomingQueue = incomingQueue;
    this.eventMultiplexer = eventMultiplexer;
    this.roundTimerExecutor = roundTimerExecutor;
  }

  /** Indicate to the processor that it should gracefully stop at its next opportunity */
  public void stop() {
    shutdown = true;
  }

  @Override
  public void run() {
    while (!shutdown) {
      nextIbftEvent().ifPresent(event -> eventMultiplexer.handleIbftEvent(event));
    }
    // Clean up the executor service the round timer has been utilising
    LOG.info("Shutting down IBFT event processor");
    roundTimerExecutor.shutdownNow();
  }

  private Optional<IbftEvent> nextIbftEvent() {
    try {
      return Optional.ofNullable(incomingQueue.poll(500, TimeUnit.MILLISECONDS));
    } catch (final InterruptedException interrupt) {
      // If the queue was interrupted propagate it and spin to check our shutdown status
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }
}
