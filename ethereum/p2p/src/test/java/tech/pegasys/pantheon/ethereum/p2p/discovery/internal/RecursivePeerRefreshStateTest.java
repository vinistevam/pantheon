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
package tech.pegasys.pantheon.ethereum.p2p.discovery.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.ethereum.p2p.discovery.DiscoveryPeer;
import tech.pegasys.pantheon.ethereum.p2p.discovery.PeerDiscoveryStatus;
import tech.pegasys.pantheon.ethereum.p2p.discovery.internal.RecursivePeerRefreshState.BondingAgent;
import tech.pegasys.pantheon.ethereum.p2p.discovery.internal.RecursivePeerRefreshState.FindNeighbourDispatcher;
import tech.pegasys.pantheon.ethereum.p2p.peers.PeerBlacklist;
import tech.pegasys.pantheon.ethereum.permissioning.LocalPermissioningConfiguration;
import tech.pegasys.pantheon.ethereum.permissioning.node.NodePermissioningController;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class RecursivePeerRefreshStateTest {
  private static final BytesValue TARGET = createId(0);
  private final PeerBlacklist peerBlacklist = mock(PeerBlacklist.class);
  private final BondingAgent bondingAgent = mock(BondingAgent.class);
  private final FindNeighbourDispatcher neighborFinder = mock(FindNeighbourDispatcher.class);
  private final MockTimerUtil timerUtil = new MockTimerUtil();

  private final DiscoveryPeer localPeer = new DiscoveryPeer(createId(9), "127.0.0.9", 9, 9);
  private final DiscoveryPeer peer1 = new DiscoveryPeer(createId(1), "127.0.0.1", 1, 1);
  private final DiscoveryPeer peer2 = new DiscoveryPeer(createId(2), "127.0.0.2", 2, 2);
  private final DiscoveryPeer peer3 = new DiscoveryPeer(createId(3), "127.0.0.3", 3, 3);
  private final DiscoveryPeer peer4 = new DiscoveryPeer(createId(4), "127.0.0.3", 4, 4);

  private RecursivePeerRefreshState recursivePeerRefreshState =
      new RecursivePeerRefreshState(
          peerBlacklist,
          Optional.empty(),
          bondingAgent,
          neighborFinder,
          timerUtil,
          localPeer,
          new PeerTable(createId(999), 16),
          5,
          100);

  @Test
  public void shouldBondWithInitialNodesWhenStarted() {
    recursivePeerRefreshState.start(asList(peer1, peer2, peer3), TARGET);

    verify(bondingAgent).performBonding(peer1);
    verify(bondingAgent).performBonding(peer2);
    verify(bondingAgent).performBonding(peer3);
    verifyNoMoreInteractions(bondingAgent, neighborFinder);
  }

  @Test
  public void shouldOnlyBondWithUnbondedInitialNodes() {
    peer1.setStatus(PeerDiscoveryStatus.BONDED);
    peer2.setStatus(PeerDiscoveryStatus.KNOWN);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer2);
    verify(bondingAgent, never()).performBonding(peer1);
    verifyNoMoreInteractions(bondingAgent, neighborFinder);
  }

  @Test
  public void shouldSkipStraightToFindNeighboursIfAllInitialNodesAreBonded() {
    peer1.setStatus(PeerDiscoveryStatus.BONDED);
    peer2.setStatus(PeerDiscoveryStatus.BONDED);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);
    verifyNoMoreInteractions(bondingAgent, neighborFinder);
  }

  @Test
  public void shouldBondWithNewlyDiscoveredNodes() {
    peer1.setStatus(PeerDiscoveryStatus.BONDED);

    recursivePeerRefreshState.start(singletonList(peer1), TARGET);

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer1, NeighborsPacketData.create(asList(peer2, peer3)));

    verify(bondingAgent).performBonding(peer2);
    verify(bondingAgent).performBonding(peer3);
    verifyNoMoreInteractions(bondingAgent, neighborFinder);
  }

  @Test
  public void shouldMoveToNeighboursRoundWhenBondingTimesOut() {
    peer1.setStatus(PeerDiscoveryStatus.BONDED);
    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer2);
    timerUtil.runTimerHandlers();
    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verifyNoMoreInteractions(bondingAgent, neighborFinder);
  }

  @Test
  public void shouldMoveToNeighboursRoundWhenBondingTimesOutVariant() {
    peer1.setStatus(PeerDiscoveryStatus.KNOWN);
    peer2.setStatus(PeerDiscoveryStatus.KNOWN);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer1);
    verify(bondingAgent).performBonding(peer2);

    completeBonding(peer1);

    timerUtil.runTimerHandlers();

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder, never()).findNeighbours(peer2, TARGET);
  }

  @Test
  public void shouldStopWhenAllNodesHaveBeenQueried() {
    peer1.setStatus(PeerDiscoveryStatus.KNOWN);
    peer2.setStatus(PeerDiscoveryStatus.KNOWN);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer1);
    verify(bondingAgent).performBonding(peer2);

    completeBonding(peer1);
    completeBonding(peer2);

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer1, NeighborsPacketData.create(emptyList()));
    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer2, NeighborsPacketData.create(emptyList()));

    verify(bondingAgent, times(2)).performBonding(any());
    verifyNoMoreInteractions(neighborFinder);
  }

  @Test
  public void shouldStopWhenMaximumNumberOfRoundsReached() {
    recursivePeerRefreshState =
        new RecursivePeerRefreshState(
            peerBlacklist,
            Optional.empty(),
            bondingAgent,
            neighborFinder,
            timerUtil,
            localPeer,
            new PeerTable(createId(999), 16),
            5,
            1);

    peer1.setStatus(PeerDiscoveryStatus.KNOWN);
    peer2.setStatus(PeerDiscoveryStatus.KNOWN);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer2);
    verify(bondingAgent).performBonding(peer2);

    completeBonding(peer1);
    completeBonding(peer2);

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer1, NeighborsPacketData.create(singletonList(peer3)));
    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer2, NeighborsPacketData.create(singletonList(peer4)));

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);
    verify(neighborFinder, never()).findNeighbours(peer3, TARGET);
    verify(neighborFinder, never()).findNeighbours(peer4, TARGET);
    verifyNoMoreInteractions(neighborFinder);
  }

  @Test
  public void shouldOnlyQueryClosestThreeNeighbours() {
    final BytesValue id0 =
        BytesValue.fromHexString(
            "0x11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111");
    final DiscoveryPeer peer0 = new DiscoveryPeer(id0, "0.0.0.0", 1, 1);
    final BytesValue id1 =
        BytesValue.fromHexString(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000");
    final DiscoveryPeer peer1 = new DiscoveryPeer(id1, "0.0.0.0", 1, 1);
    final BytesValue id2 =
        BytesValue.fromHexString(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010");
    final DiscoveryPeer peer2 = new DiscoveryPeer(id2, "0.0.0.0", 1, 1);
    final BytesValue id3 =
        BytesValue.fromHexString(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100");
    final DiscoveryPeer peer3 = new DiscoveryPeer(id3, "0.0.0.0", 1, 1);
    final BytesValue id4 =
        BytesValue.fromHexString(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000");
    final DiscoveryPeer peer4 = new DiscoveryPeer(id4, "0.0.0.0", 1, 1);

    recursivePeerRefreshState.start(singletonList(peer0), TARGET);

    // Initial bonding round
    verify(bondingAgent).performBonding(peer0);
    completeBonding(peer0);

    // Initial neighbours round
    verify(neighborFinder).findNeighbours(peer0, TARGET);
    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer0, NeighborsPacketData.create(asList(peer1, peer2, peer3, peer4)));

    // Bonding round 2
    verify(bondingAgent).performBonding(peer1);
    verify(bondingAgent).performBonding(peer2);
    verify(bondingAgent).performBonding(peer3);
    verify(bondingAgent).performBonding(peer4);

    completeBonding(peer1);
    completeBonding(peer2);
    completeBonding(peer3);
    completeBonding(peer4);

    verify(neighborFinder, never()).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);
    verify(neighborFinder).findNeighbours(peer3, TARGET);
    verify(neighborFinder).findNeighbours(peer4, TARGET);
  }

  @Test
  public void shouldNotQueryNodeThatIsAlreadyQueried() {
    peer1.setStatus(PeerDiscoveryStatus.KNOWN);
    peer2.setStatus(PeerDiscoveryStatus.KNOWN);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer2);
    verify(bondingAgent).performBonding(peer2);

    completeBonding(peer1);
    completeBonding(peer2);

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer1, NeighborsPacketData.create(singletonList(peer2)));
    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer2, NeighborsPacketData.create(emptyList()));

    verify(bondingAgent, times(1)).performBonding(peer1);
    verify(bondingAgent, times(1)).performBonding(peer2);
    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);
    verifyNoMoreInteractions(bondingAgent, neighborFinder);
  }

  @Test
  public void shouldBondWithNewNeighboursWhenSomeRequestsTimeOut() {
    final BytesValue id0 =
        BytesValue.fromHexString(
            "0x11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111");
    final DiscoveryPeer peer0 = new DiscoveryPeer(id0, "0.0.0.0", 1, 1);
    final BytesValue id1 =
        BytesValue.fromHexString(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000");
    final DiscoveryPeer peer1 = new DiscoveryPeer(id1, "0.0.0.0", 1, 1);
    final BytesValue id2 =
        BytesValue.fromHexString(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010");
    final DiscoveryPeer peer2 = new DiscoveryPeer(id2, "0.0.0.0", 1, 1);
    final BytesValue id3 =
        BytesValue.fromHexString(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100");
    final DiscoveryPeer peer3 = new DiscoveryPeer(id3, "0.0.0.0", 1, 1);
    final BytesValue id4 =
        BytesValue.fromHexString(
            "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000");
    final DiscoveryPeer peer4 = new DiscoveryPeer(id4, "0.0.0.0", 1, 1);
    final List<DiscoveryPeer> peerTable = asList(peer1, peer2, peer3, peer4);

    final NeighborsPacketData neighborsPacketData = NeighborsPacketData.create(peerTable);

    recursivePeerRefreshState.start(singletonList(peer0), TARGET);

    verify(bondingAgent).performBonding(peer0);

    completeBonding(peer0);

    verify(neighborFinder).findNeighbours(peer0, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(peer0, neighborsPacketData);

    verify(bondingAgent).performBonding(peer1);
    verify(bondingAgent).performBonding(peer2);
    verify(bondingAgent).performBonding(peer3);
    verify(bondingAgent).performBonding(peer4);

    completeBonding(peer1);
    completeBonding(peer2);
    completeBonding(peer3);
    completeBonding(peer4);

    verify(neighborFinder, never()).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);
    verify(neighborFinder).findNeighbours(peer3, TARGET);
    verify(neighborFinder).findNeighbours(peer4, TARGET);

    timerUtil.runTimerHandlers();

    verify(neighborFinder).findNeighbours(peer1, TARGET);
  }

  @Test
  public void shouldNotBondWithDiscoveredNodesThatAreAlreadyBonded() {
    peer1.setStatus(PeerDiscoveryStatus.KNOWN);
    peer2.setStatus(PeerDiscoveryStatus.KNOWN);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer1);
    verify(bondingAgent).performBonding(peer2);

    verify(bondingAgent, times(1)).performBonding(peer1);
    verify(bondingAgent, times(1)).performBonding(peer2);

    completeBonding(peer1);
    completeBonding(peer2);

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer1, NeighborsPacketData.create(singletonList(peer2)));
    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer2, NeighborsPacketData.create(emptyList()));

    verify(bondingAgent, times(1)).performBonding(peer2);
  }

  @Test
  public void shouldQueryNodeThatTimedOutWithBondingButLaterCompletedBonding() {
    peer1.setStatus(PeerDiscoveryStatus.KNOWN);
    peer2.setStatus(PeerDiscoveryStatus.KNOWN);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer1);
    verify(bondingAgent).performBonding(peer2);

    verify(bondingAgent, times(1)).performBonding(peer1);
    verify(bondingAgent, times(1)).performBonding(peer2);

    completeBonding(peer1);
    peer2.setStatus(PeerDiscoveryStatus.BONDING);

    timerUtil.runTimerHandlers();

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder, never()).findNeighbours(peer2, TARGET);

    // Already timed out but finally completes. DOES NOT trigger a new neighbour round.
    completeBonding(peer2);
    verify(neighborFinder, never()).findNeighbours(peer2, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer1, NeighborsPacketData.create(singletonList(peer3)));

    verify(bondingAgent).performBonding(peer3);
    verify(bondingAgent, times(1)).performBonding(peer2);

    completeBonding(peer3);

    verify(neighborFinder).findNeighbours(peer2, TARGET);
    verify(neighborFinder).findNeighbours(peer3, TARGET);
  }

  @Test
  public void shouldBondWithPeersInNeighboursResponseReceivedAfterTimeout() {
    peer1.setStatus(PeerDiscoveryStatus.KNOWN);
    peer2.setStatus(PeerDiscoveryStatus.KNOWN);

    recursivePeerRefreshState.start(asList(peer1, peer2), TARGET);

    verify(bondingAgent).performBonding(peer1);
    verify(bondingAgent).performBonding(peer2);

    completeBonding(peer1);
    completeBonding(peer2);

    verify(neighborFinder).findNeighbours(peer1, TARGET);
    verify(neighborFinder).findNeighbours(peer2, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer1, NeighborsPacketData.create(singletonList(peer3)));

    timerUtil.runTimerHandlers();

    verify(bondingAgent).performBonding(peer3);
    completeBonding(peer3);

    verify(neighborFinder).findNeighbours(peer3, TARGET);

    // Receive late response from peer 2. May as well process it in this round.
    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer2, NeighborsPacketData.create(singletonList(peer4)));

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peer3, NeighborsPacketData.create(emptyList()));

    verify(bondingAgent).performBonding(peer4);
    verifyNoMoreInteractions(bondingAgent, neighborFinder);
  }

  @Test
  public void shouldNotBondWithNodesOnBlacklist() {
    final DiscoveryPeer peerA = new DiscoveryPeer(createId(1), "127.0.0.1", 1, 1);
    final DiscoveryPeer peerB = new DiscoveryPeer(createId(2), "127.0.0.2", 2, 2);

    final PeerBlacklist blacklist = new PeerBlacklist();
    blacklist.add(peerB);

    recursivePeerRefreshState =
        new RecursivePeerRefreshState(
            blacklist,
            Optional.empty(),
            bondingAgent,
            neighborFinder,
            timerUtil,
            localPeer,
            new PeerTable(createId(999), 16),
            5,
            100);
    recursivePeerRefreshState.start(singletonList(peerA), TARGET);

    verify(bondingAgent).performBonding(peerA);

    completeBonding(peerA);

    verify(neighborFinder).findNeighbours(peerA, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peerA, NeighborsPacketData.create(Collections.singletonList(peerB)));

    verify(bondingAgent, never()).performBonding(peerB);
  }

  @Test
  public void shouldNotBondWithSelf() {
    final DiscoveryPeer peerA = new DiscoveryPeer(createId(1), "127.0.0.1", 1, 1);
    final DiscoveryPeer peerB = new DiscoveryPeer(createId(2), "127.0.0.2", 2, 2);

    recursivePeerRefreshState.start(singletonList(peerA), TARGET);

    verify(bondingAgent).performBonding(peerA);

    completeBonding(peerA);

    verify(neighborFinder).findNeighbours(peerA, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peerA, NeighborsPacketData.create(asList(peerB, localPeer)));

    verify(bondingAgent).performBonding(peerB);
    verify(bondingAgent, never()).performBonding(localPeer);
  }

  @Test
  public void shouldNotBondWithNodesNotPermitted() throws Exception {
    final DiscoveryPeer localPeer = new DiscoveryPeer(createId(999), "127.0.0.9", 9, 9);
    final DiscoveryPeer peerA = new DiscoveryPeer(createId(1), "127.0.0.1", 1, 1);
    final DiscoveryPeer peerB = new DiscoveryPeer(createId(2), "127.0.0.2", 2, 2);

    final Path tempFile = Files.createTempFile("test", "test");
    tempFile.toFile().deleteOnExit();
    final LocalPermissioningConfiguration permissioningConfiguration =
        LocalPermissioningConfiguration.createDefault();
    permissioningConfiguration.setNodePermissioningConfigFilePath(
        tempFile.toAbsolutePath().toString());

    final NodePermissioningController nodeWhitelistController =
        mock(NodePermissioningController.class);
    when(nodeWhitelistController.isPermitted(any(), eq(peerA.getEnodeURL()))).thenReturn(true);
    when(nodeWhitelistController.isPermitted(any(), eq(peerB.getEnodeURL()))).thenReturn(false);

    recursivePeerRefreshState =
        new RecursivePeerRefreshState(
            peerBlacklist,
            Optional.of(nodeWhitelistController),
            bondingAgent,
            neighborFinder,
            timerUtil,
            localPeer,
            new PeerTable(createId(999), 16),
            5,
            100);
    recursivePeerRefreshState.start(singletonList(peerA), TARGET);

    verify(bondingAgent).performBonding(peerA);

    completeBonding(peerA);

    verify(neighborFinder).findNeighbours(peerA, TARGET);

    recursivePeerRefreshState.onNeighboursPacketReceived(
        peerA, NeighborsPacketData.create(Collections.singletonList(peerB)));

    verify(bondingAgent, never()).performBonding(peerB);
  }

  private static BytesValue createId(final int id) {
    return BytesValue.fromHexString(String.format("%0128x", id));
  }

  private void completeBonding(final DiscoveryPeer peer1) {
    peer1.setStatus(PeerDiscoveryStatus.BONDED);
    recursivePeerRefreshState.onBondingComplete(peer1);
  }
}
