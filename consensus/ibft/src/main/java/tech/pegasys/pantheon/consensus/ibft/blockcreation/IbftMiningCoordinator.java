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
package tech.pegasys.pantheon.consensus.ibft.blockcreation;

import static org.apache.logging.log4j.LogManager.getLogger;

import tech.pegasys.pantheon.consensus.ibft.IbftEventQueue;
import tech.pegasys.pantheon.consensus.ibft.IbftProcessor;
import tech.pegasys.pantheon.consensus.ibft.ibftevent.NewChainHead;
import tech.pegasys.pantheon.ethereum.blockcreation.MiningCoordinator;
import tech.pegasys.pantheon.ethereum.chain.BlockAddedEvent;
import tech.pegasys.pantheon.ethereum.chain.BlockAddedObserver;
import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import org.apache.logging.log4j.Logger;

public class IbftMiningCoordinator implements MiningCoordinator, BlockAddedObserver {

  private final IbftBlockCreatorFactory blockCreatorFactory;
  private static final Logger LOG = getLogger();
  protected final Blockchain blockchain;
  private final IbftEventQueue eventQueue;
  private final IbftProcessor ibftProcessor;

  public IbftMiningCoordinator(
      final IbftProcessor ibftProcessor,
      final IbftBlockCreatorFactory blockCreatorFactory,
      final Blockchain blockchain,
      final IbftEventQueue eventQueue) {
    this.ibftProcessor = ibftProcessor;
    this.blockCreatorFactory = blockCreatorFactory;
    this.eventQueue = eventQueue;

    this.blockchain = blockchain;
    this.blockchain.observeBlockAdded(this);
  }

  @Override
  public void enable() {}

  @Override
  public void disable() {
    ibftProcessor.stop();
  }

  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public void setMinTransactionGasPrice(final Wei minGasPrice) {
    blockCreatorFactory.setMinTransactionGasPrice(minGasPrice);
  }

  @Override
  public Wei getMinTransactionGasPrice() {
    return blockCreatorFactory.getMinTransactionGasPrice();
  }

  @Override
  public void setExtraData(final BytesValue extraData) {
    blockCreatorFactory.setExtraData(extraData);
  }

  @Override
  public void onBlockAdded(final BlockAddedEvent event, final Blockchain blockchain) {
    if (event.isNewCanonicalHead()) {
      LOG.info("New canonical head detected");
      eventQueue.add(new NewChainHead(event.getBlock().getHeader()));
    }
  }
}
