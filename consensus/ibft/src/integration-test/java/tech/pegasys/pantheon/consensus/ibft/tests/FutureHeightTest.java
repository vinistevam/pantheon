/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.pantheon.consensus.ibft.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;
import static tech.pegasys.pantheon.consensus.ibft.support.TestHelpers.createSignedCommitPayload;

import tech.pegasys.pantheon.consensus.ibft.ConsensusRoundIdentifier;
import tech.pegasys.pantheon.consensus.ibft.IbftHelpers;
import tech.pegasys.pantheon.consensus.ibft.ibftevent.NewChainHead;
import tech.pegasys.pantheon.consensus.ibft.payload.CommitPayload;
import tech.pegasys.pantheon.consensus.ibft.payload.MessageFactory;
import tech.pegasys.pantheon.consensus.ibft.payload.PreparePayload;
import tech.pegasys.pantheon.consensus.ibft.payload.SignedData;
import tech.pegasys.pantheon.consensus.ibft.support.RoundSpecificPeers;
import tech.pegasys.pantheon.consensus.ibft.support.TestContext;
import tech.pegasys.pantheon.consensus.ibft.support.TestContextBuilder;
import tech.pegasys.pantheon.ethereum.core.Block;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.Before;
import org.junit.Test;

public class FutureHeightTest {

  private final long blockTimeStamp = 100;
  private final Clock fixedClock =
      Clock.fixed(Instant.ofEpochSecond(blockTimeStamp), ZoneId.systemDefault());

  private final int NETWORK_SIZE = 5;

  // Configuration ensures remote peer will provide proposal for first block
  private final TestContext context =
      new TestContextBuilder()
          .validatorCount(NETWORK_SIZE)
          .indexOfFirstLocallyProposedBlock(0)
          .clock(fixedClock)
          .build();

  private final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(1, 0);
  private final RoundSpecificPeers peers = context.roundSpecificPeers(roundId);

  private final ConsensusRoundIdentifier futureHeightRoundId = new ConsensusRoundIdentifier(2, 0);

  private final MessageFactory localNodeMessageFactory = context.getLocalNodeMessageFactory();

  @Before
  public void setup() {
    context.getController().start();
  }

  @Test
  public void messagesForFutureHeightAreBufferedUntilChainHeightCatchesUp() {
    final Block currentHeightBlock = context.createBlockForProposalFromChainHead(0, 30);
    final Block signedCurrentHeightBlock =
        IbftHelpers.createSealedBlock(currentHeightBlock, peers.sign(currentHeightBlock.getHash()));

    final Block futureHeightBlock =
        context.createBlockForProposal(signedCurrentHeightBlock.getHeader(), 0, 60);

    peers.getProposer().injectProposal(futureHeightRoundId, futureHeightBlock);
    peers.verifyNoMessagesReceived();

    // Inject prepares and commits from all peers
    peers.prepareForNonProposing(futureHeightRoundId, futureHeightBlock.getHash());
    peers.commitForNonProposing(futureHeightRoundId, futureHeightBlock.getHash());

    peers.verifyNoMessagesReceived();
    assertThat(context.getCurrentChainHeight()).isEqualTo(0);

    // Add block to chain, and notify system of its arrival.
    context.getBlockchain().appendBlock(signedCurrentHeightBlock, emptyList());
    assertThat(context.getCurrentChainHeight()).isEqualTo(1);
    context
        .getController()
        .handleNewBlockEvent(new NewChainHead(signedCurrentHeightBlock.getHeader()));

    final SignedData<PreparePayload> expectedPrepareMessage =
        localNodeMessageFactory.createSignedPreparePayload(
            futureHeightRoundId, futureHeightBlock.getHash());

    final SignedData<CommitPayload> expectedCommitMessage =
        createSignedCommitPayload(
            futureHeightRoundId, futureHeightBlock, context.getLocalNodeParams().getNodeKeyPair());

    peers.verifyMessagesReceived(expectedPrepareMessage, expectedCommitMessage);
    assertThat(context.getCurrentChainHeight()).isEqualTo(2);
  }

  @Test
  public void messagesFromPreviousHeightAreDiscarded() {
    final Block currentHeightBlock = context.createBlockForProposalFromChainHead(0, 30);
    final Block signedCurrentHeightBlock =
        IbftHelpers.createSealedBlock(currentHeightBlock, peers.sign(currentHeightBlock.getHash()));

    peers.getProposer().injectProposal(roundId, currentHeightBlock);
    peers.getNonProposing(0).injectPrepare(roundId, currentHeightBlock.getHash());

    final SignedData<PreparePayload> expectedPrepareMessage =
        localNodeMessageFactory.createSignedPreparePayload(roundId, currentHeightBlock.getHash());

    peers.verifyMessagesReceived(expectedPrepareMessage);

    // Add block to chain, and notify system of its arrival.
    context.getBlockchain().appendBlock(signedCurrentHeightBlock, emptyList());
    assertThat(context.getCurrentChainHeight()).isEqualTo(1);
    context
        .getController()
        .handleNewBlockEvent(new NewChainHead(signedCurrentHeightBlock.getHeader()));

    // Inject prepares and commits from all peers for the 'previous' round (i.e. the height
    // from before the block arrived).
    peers.prepareForNonProposing(roundId, currentHeightBlock.getHash());
    peers.commitForNonProposing(roundId, currentHeightBlock.getHash());

    peers.verifyNoMessagesReceived();
    assertThat(context.getCurrentChainHeight()).isEqualTo(1);
  }

  @Test
  public void multipleNewChainHeadEventsDoesNotRestartCurrentHeightManager() {
    final Block currentHeightBlock = context.createBlockForProposalFromChainHead(0, 30);

    peers.getProposer().injectProposal(roundId, currentHeightBlock);
    peers.getNonProposing(0).injectPrepare(roundId, currentHeightBlock.getHash());

    peers.clearReceivedMessages();

    // inject a NewHeight FOR THE CURRENT HEIGHT
    context
        .getController()
        .handleNewBlockEvent(new NewChainHead(context.getBlockchain().getChainHeadHeader()));

    // Should only require 1 more prepare to close it out
    peers.getNonProposing(1).injectPrepare(roundId, currentHeightBlock.getHash());

    final SignedData<CommitPayload> expectedCommitMessage =
        createSignedCommitPayload(
            roundId, currentHeightBlock, context.getLocalNodeParams().getNodeKeyPair());
    peers.verifyMessagesReceived(expectedCommitMessage);
  }

  @Test
  public void correctMessagesAreExtractedFromFutureHeightBuffer() {
    final Block currentHeightBlock = context.createBlockForProposalFromChainHead(0, 30);
    final Block signedCurrentHeightBlock =
        IbftHelpers.createSealedBlock(currentHeightBlock, peers.sign(currentHeightBlock.getHash()));

    final Block nextHeightBlock =
        context.createBlockForProposal(signedCurrentHeightBlock.getHeader(), 0, 60);
    final Block signedNextHeightBlock =
        IbftHelpers.createSealedBlock(nextHeightBlock, peers.sign(nextHeightBlock.getHash()));

    final Block futureHeightBlock =
        context.createBlockForProposal(signedNextHeightBlock.getHeader(), 0, 90);

    final ConsensusRoundIdentifier nextHeightRoundId = new ConsensusRoundIdentifier(2, 0);
    final ConsensusRoundIdentifier futureHeightRoundId = new ConsensusRoundIdentifier(3, 0);

    // Inject prepares and commits from all peers into FutureHeight (2 height time)
    peers.prepareForNonProposing(futureHeightRoundId, futureHeightBlock.getHash());
    peers.commitForNonProposing(futureHeightRoundId, futureHeightBlock.getHash());

    // Add the "interim" block to chain, and notify system of its arrival.
    context.getBlockchain().appendBlock(signedCurrentHeightBlock, emptyList());
    assertThat(context.getCurrentChainHeight()).isEqualTo(1);
    context
        .getController()
        .handleNewBlockEvent(new NewChainHead(signedCurrentHeightBlock.getHeader()));

    peers.verifyNoMessagesReceived();
    peers.getProposer().injectProposal(nextHeightRoundId, nextHeightBlock);

    final SignedData<PreparePayload> expectedPrepareMessage =
        localNodeMessageFactory.createSignedPreparePayload(
            nextHeightRoundId, nextHeightBlock.getHash());

    // Assert ONLY a prepare message was received, not any commits (i.e. futureHeightRoundId
    // messages have not been used.
    peers.verifyMessagesReceived(expectedPrepareMessage);

    peers.getProposer().injectProposal(futureHeightRoundId, futureHeightBlock);

    // Change to the FutureRound, and confirm prepare and commit msgs are sent
    context.getBlockchain().appendBlock(signedNextHeightBlock, emptyList());
    assertThat(context.getCurrentChainHeight()).isEqualTo(2);
    context
        .getController()
        .handleNewBlockEvent(new NewChainHead(signedNextHeightBlock.getHeader()));

    final SignedData<PreparePayload> expectedFuturePrepareMessage =
        localNodeMessageFactory.createSignedPreparePayload(
            futureHeightRoundId, futureHeightBlock.getHash());

    final SignedData<CommitPayload> expectedCommitMessage =
        createSignedCommitPayload(
            futureHeightRoundId, futureHeightBlock, context.getLocalNodeParams().getNodeKeyPair());

    // Assert ONLY a prepare message was received, not any commits (i.e. futureHeightRoundId
    // messages have not been used.
    peers.verifyMessagesReceived(expectedCommitMessage, expectedFuturePrepareMessage);
  }
}
