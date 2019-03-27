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
package tech.pegasys.pantheon.ethereum.p2p.api;

import tech.pegasys.pantheon.ethereum.p2p.peers.Peer;
import tech.pegasys.pantheon.ethereum.p2p.wire.Capability;
import tech.pegasys.pantheon.ethereum.p2p.wire.PeerInfo;
import tech.pegasys.pantheon.util.enode.EnodeURL;

import java.io.Closeable;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** P2P Network Interface. */
public interface P2PNetwork extends Closeable {

  void start();

  /**
   * Returns a snapshot of the currently connected peer connections.
   *
   * @return Peers currently connected.
   */
  Collection<PeerConnection> getPeers();

  /**
   * Connects to a {@link Peer}.
   *
   * @param peer Peer to connect to.
   * @return Future of the established {@link PeerConnection}
   */
  CompletableFuture<PeerConnection> connect(Peer peer);

  /**
   * Subscribe a {@link Consumer} to all incoming {@link Message} of a given sub-protocol. Calling
   * {@link #start()} on an implementation without at least having one subscribed {@link Consumer}
   * per supported sub-protocol should throw a {@link RuntimeException}.
   *
   * @param capability Capability (sub-protocol) to subscribe to.
   * @param consumer Consumer to subscribe
   */
  void subscribe(Capability capability, Consumer<Message> consumer);

  /**
   * Subscribe a {@link Consumer} to all incoming new Peer connection events.
   *
   * @param consumer Consumer to subscribe
   */
  void subscribeConnect(Consumer<PeerConnection> consumer);

  /**
   * Subscribe a {@link Consumer} to all incoming new Peer disconnect events.
   *
   * @param consumer Consumer to subscribe
   */
  void subscribeDisconnect(DisconnectCallback consumer);

  /**
   * Adds a {@link Peer} to a list indicating efforts should be made to always stay connected to it
   *
   * @param peer The peer that should be connected to
   * @return boolean representing whether or not the peer has been added to the list or was already
   *     on it
   */
  boolean addMaintainConnectionPeer(final Peer peer);

  /**
   * Removes a {@link Peer} from a list indicating any existing efforts to connect to a given peer
   * should be removed, and if connected, the peer should be disconnected
   *
   * @param peer The peer to which connections are not longer required
   * @return boolean representing whether or not the peer has been disconnected, or if it was not
   *     currently connected.
   */
  boolean removeMaintainedConnectionPeer(final Peer peer);

  /** Stops the P2P network layer. */
  void stop();

  /** Blocks until the P2P network layer has stopped. */
  void awaitStop();

  Optional<? extends Peer> getAdvertisedPeer();

  /**
   * Returns {@link PeerInfo} object for this node
   *
   * @return the PeerInfo for this node.
   */
  PeerInfo getLocalPeerInfo();

  /**
   * Checks if the node is listening for network connections
   *
   * @return true if the node is listening for network connections, false, otherwise.
   */
  boolean isListening();

  /**
   * Returns whether the P2P network is enabled
   *
   * @return true if the P2P network is enabled, false, otherwise.
   */
  boolean isP2pEnabled();

  /**
   * Returns the EnodeURL used to identify this peer in the network.
   *
   * @return the enodeURL associated with this node if P2P has been enabled. Returns empty
   *     otherwise.
   */
  Optional<EnodeURL> getSelfEnodeURL();
}
