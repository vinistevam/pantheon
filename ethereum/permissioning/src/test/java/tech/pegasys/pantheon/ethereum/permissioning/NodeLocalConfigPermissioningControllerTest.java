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
package tech.pegasys.pantheon.ethereum.permissioning;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static tech.pegasys.pantheon.ethereum.permissioning.NodeLocalConfigPermissioningController.NodesWhitelistResult;

import tech.pegasys.pantheon.ethereum.permissioning.node.NodeWhitelistUpdatedEvent;
import tech.pegasys.pantheon.util.enode.EnodeURL;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NodeLocalConfigPermissioningControllerTest {

  @Mock private WhitelistPersistor whitelistPersistor;
  private final List<EnodeURL> bootnodesList = new ArrayList<>();
  private NodeLocalConfigPermissioningController controller;

  private final String enode1 =
      "enode://6f8a80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@192.168.0.10:4567";
  private final String enode2 =
      "enode://5f8a80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@192.168.0.10:4567";
  private final String selfEnode =
      "enode://5f8a80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@192.168.0.10:1111";

  @Before
  public void setUp() {
    bootnodesList.clear();
    controller =
        new NodeLocalConfigPermissioningController(
            LocalPermissioningConfiguration.createDefault(),
            bootnodesList,
            new EnodeURL(selfEnode),
            whitelistPersistor);
  }

  @Test
  public void whenAddNodesWithValidInputShouldReturnSuccess() {
    NodesWhitelistResult expected = new NodesWhitelistResult(WhitelistOperationResult.SUCCESS);
    NodesWhitelistResult actualResult = controller.addNodes(Lists.newArrayList(enode1));

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
    assertThat(controller.getNodesWhitelist()).containsExactly(enode1);
  }

  @Test
  public void whenAddNodesInputHasExistingNodeShouldReturnAddErrorExistingEntry() {
    controller.addNodes(Arrays.asList(enode1));

    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_EXISTING_ENTRY);
    NodesWhitelistResult actualResult = controller.addNodes(Lists.newArrayList(enode1, enode2));

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
  }

  @Test
  public void whenAddNodesInputHasDuplicatedNodesShouldReturnDuplicatedEntryError() {
    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_DUPLICATED_ENTRY);

    NodesWhitelistResult actualResult = controller.addNodes(Arrays.asList(enode1, enode1));

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
  }

  @Test
  public void whenAddNodesInputHasEmptyListOfNodesShouldReturnErrorEmptyEntry() {
    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_EMPTY_ENTRY);
    NodesWhitelistResult actualResult = controller.removeNodes(new ArrayList<>());

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
  }

  @Test
  public void whenAddNodesInputHasNullListOfNodesShouldReturnErrorEmptyEntry() {
    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_EMPTY_ENTRY);
    NodesWhitelistResult actualResult = controller.removeNodes(null);

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
  }

  @Test
  public void whenRemoveNodesInputHasAbsentNodeShouldReturnRemoveErrorAbsentEntry() {
    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_ABSENT_ENTRY);
    NodesWhitelistResult actualResult = controller.removeNodes(Lists.newArrayList(enode1, enode2));

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
  }

  @Test
  public void whenRemoveNodesInputHasDuplicateNodesShouldReturnErrorDuplicatedEntry() {
    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_DUPLICATED_ENTRY);
    NodesWhitelistResult actualResult = controller.removeNodes(Lists.newArrayList(enode1, enode1));

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
  }

  @Test
  public void whenRemoveNodesInputHasEmptyListOfNodesShouldReturnErrorEmptyEntry() {
    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_EMPTY_ENTRY);
    NodesWhitelistResult actualResult = controller.removeNodes(new ArrayList<>());

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
  }

  @Test
  public void whenRemoveNodesInputHasNullListOfNodesShouldReturnErrorEmptyEntry() {
    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_EMPTY_ENTRY);
    NodesWhitelistResult actualResult = controller.removeNodes(null);

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
  }

  @Test
  public void whenNodeIdsAreDifferentItShouldNotBePermitted() {
    String peer1 =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30303";
    String peer2 =
        "enode://bbbb80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30303";

    controller.addNodes(Arrays.asList(peer1));

    assertThat(controller.isPermitted(peer2)).isFalse();
  }

  @Test
  public void whenNodesHostsAreDifferentItShouldNotBePermitted() {
    String peer1 =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30303";
    String peer2 =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.2:30303";

    controller.addNodes(Arrays.asList(peer1));

    assertThat(controller.isPermitted(peer2)).isFalse();
  }

  @Test
  public void whenNodesListeningPortsAreDifferentItShouldNotBePermitted() {
    String peer1 =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30301";
    String peer2 =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30302";

    controller.addNodes(Arrays.asList(peer1));

    assertThat(controller.isPermitted(peer2)).isFalse();
  }

  @Test
  public void whenCheckingIfNodeIsPermittedDiscoveryPortShouldNotBeConsideredIfAbsent() {
    String peerWithDiscoveryPortSet =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30303?discport=10001";
    String peerWithoutDiscoveryPortSet =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30303";

    controller.addNodes(Arrays.asList(peerWithDiscoveryPortSet));

    assertThat(controller.isPermitted(peerWithoutDiscoveryPortSet)).isTrue();
  }

  @Test
  public void whenCheckingIfNodeIsPermittedDiscoveryPortShouldBeConsideredIfPresent() {
    String peerWithDiscoveryPortSet =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30303?discport=10001";
    String peerWithDifferentDiscoveryPortSet =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@127.0.0.1:30303?discport=10002";

    controller.addNodes(Arrays.asList(peerWithDifferentDiscoveryPortSet));

    assertThat(controller.isPermitted(peerWithDiscoveryPortSet)).isFalse();
  }

  @Test
  public void whenCheckingIfNodeIsPermittedOrderDoesNotMatter() {
    controller.addNodes(Arrays.asList(enode1));
    assertThat(controller.isPermitted(new EnodeURL(enode1), new EnodeURL(selfEnode))).isTrue();
    assertThat(controller.isPermitted(new EnodeURL(selfEnode), new EnodeURL(enode1))).isTrue();
  }

  @Test
  public void stateShouldRevertIfWhitelistPersistFails()
      throws IOException, WhitelistFileSyncException {
    List<String> newNode1 = singletonList(new EnodeURL(enode1).toString());
    List<String> newNode2 = singletonList(new EnodeURL(enode2).toString());

    assertThat(controller.getNodesWhitelist().size()).isEqualTo(0);

    controller.addNodes(newNode1);
    assertThat(controller.getNodesWhitelist().size()).isEqualTo(1);

    doThrow(new IOException()).when(whitelistPersistor).updateConfig(any(), any());
    controller.addNodes(newNode2);

    assertThat(controller.getNodesWhitelist().size()).isEqualTo(1);
    assertThat(controller.getNodesWhitelist()).isEqualTo(newNode1);

    verify(whitelistPersistor, times(3)).verifyConfigFileMatchesState(any(), any());
    verify(whitelistPersistor, times(2)).updateConfig(any(), any());
    verifyNoMoreInteractions(whitelistPersistor);
  }

  @Test
  public void reloadNodeWhitelistWithValidConfigFileShouldUpdateWhitelist() throws Exception {
    final String expectedEnodeURL =
        "enode://6f8a80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@192.168.0.9:4567";
    final Path permissionsFile = createPermissionsFileWithNode(expectedEnodeURL);
    final LocalPermissioningConfiguration permissioningConfig =
        mock(LocalPermissioningConfiguration.class);

    when(permissioningConfig.getNodePermissioningConfigFilePath())
        .thenReturn(permissionsFile.toAbsolutePath().toString());
    when(permissioningConfig.isNodeWhitelistEnabled()).thenReturn(true);
    when(permissioningConfig.getNodeWhitelist())
        .thenReturn(Arrays.asList(URI.create(expectedEnodeURL)));
    controller =
        new NodeLocalConfigPermissioningController(
            permissioningConfig, bootnodesList, new EnodeURL(selfEnode));

    controller.reload();

    assertThat(controller.getNodesWhitelist()).containsExactly(expectedEnodeURL);
  }

  @Test
  public void reloadNodeWhitelistWithErrorReadingConfigFileShouldKeepOldWhitelist() {
    final String expectedEnodeURI =
        "enode://aaaa80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@192.168.0.9:4567";
    final LocalPermissioningConfiguration permissioningConfig =
        mock(LocalPermissioningConfiguration.class);

    when(permissioningConfig.getNodePermissioningConfigFilePath()).thenReturn("foo");
    when(permissioningConfig.isNodeWhitelistEnabled()).thenReturn(true);
    when(permissioningConfig.getNodeWhitelist())
        .thenReturn(Arrays.asList(URI.create(expectedEnodeURI)));
    controller =
        new NodeLocalConfigPermissioningController(
            permissioningConfig, bootnodesList, new EnodeURL(selfEnode));

    final Throwable thrown = catchThrowable(() -> controller.reload());

    assertThat(thrown)
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Unable to read permissioning TOML config file");

    assertThat(controller.getNodesWhitelist()).containsExactly(expectedEnodeURI);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void whenAddingNodeShouldNotifyWhitelistModifiedSubscribers() {
    final Consumer<NodeWhitelistUpdatedEvent> consumer = mock(Consumer.class);
    final NodeWhitelistUpdatedEvent expectedEvent =
        new NodeWhitelistUpdatedEvent(
            Lists.newArrayList(new EnodeURL(enode1)), Collections.emptyList());

    controller.subscribeToListUpdatedEvent(consumer);
    controller.addNodes(Lists.newArrayList(enode1));

    verify(consumer).accept(eq(expectedEvent));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void whenAddingNodeDoesNotAddShouldNotNotifyWhitelistModifiedSubscribers() {
    // adding node before subscribing to whitelist modified events
    controller.addNodes(Lists.newArrayList(enode1));
    final Consumer<NodeWhitelistUpdatedEvent> consumer = mock(Consumer.class);

    controller.subscribeToListUpdatedEvent(consumer);
    // won't add duplicate node
    controller.addNodes(Lists.newArrayList(enode1));

    verifyZeroInteractions(consumer);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void whenRemovingNodeShouldNotifyWhitelistModifiedSubscribers() {
    // adding node before subscribing to whitelist modified events
    controller.addNodes(Lists.newArrayList(enode1));

    final Consumer<NodeWhitelistUpdatedEvent> consumer = mock(Consumer.class);
    final NodeWhitelistUpdatedEvent expectedEvent =
        new NodeWhitelistUpdatedEvent(
            Collections.emptyList(), Lists.newArrayList(new EnodeURL(enode1)));

    controller.subscribeToListUpdatedEvent(consumer);
    controller.removeNodes(Lists.newArrayList(enode1));

    verify(consumer).accept(eq(expectedEvent));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void whenRemovingNodeDoesNotRemoveShouldNotifyWhitelistModifiedSubscribers() {
    final Consumer<NodeWhitelistUpdatedEvent> consumer = mock(Consumer.class);

    controller.subscribeToListUpdatedEvent(consumer);
    // won't remove absent node
    controller.removeNodes(Lists.newArrayList(enode1));

    verifyZeroInteractions(consumer);
  }

  @Test
  public void whenRemovingBootnodeShouldReturnRemoveBootnodeError() {
    NodesWhitelistResult expected =
        new NodesWhitelistResult(WhitelistOperationResult.ERROR_BOOTNODE_CANNOT_BE_REMOVED);
    bootnodesList.add(new EnodeURL(enode1));
    controller.addNodes(Lists.newArrayList(enode1, enode2));

    NodesWhitelistResult actualResult = controller.removeNodes(Lists.newArrayList(enode1));

    assertThat(actualResult).isEqualToComparingOnlyGivenFields(expected, "result");
    assertThat(controller.getNodesWhitelist()).containsExactly(enode1, enode2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void whenReloadingWhitelistShouldNotifyWhitelistModifiedSubscribers() throws Exception {
    final Path permissionsFile = createPermissionsFileWithNode(enode2);
    final LocalPermissioningConfiguration permissioningConfig =
        mock(LocalPermissioningConfiguration.class);
    final Consumer<NodeWhitelistUpdatedEvent> consumer = mock(Consumer.class);
    final NodeWhitelistUpdatedEvent expectedEvent =
        new NodeWhitelistUpdatedEvent(
            Lists.newArrayList(new EnodeURL(enode2)), Lists.newArrayList(new EnodeURL(enode1)));

    when(permissioningConfig.getNodePermissioningConfigFilePath())
        .thenReturn(permissionsFile.toAbsolutePath().toString());
    when(permissioningConfig.isNodeWhitelistEnabled()).thenReturn(true);
    when(permissioningConfig.getNodeWhitelist()).thenReturn(Arrays.asList(URI.create(enode1)));
    controller =
        new NodeLocalConfigPermissioningController(
            permissioningConfig, bootnodesList, new EnodeURL(selfEnode));
    controller.subscribeToListUpdatedEvent(consumer);

    controller.reload();

    verify(consumer).accept(eq(expectedEvent));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void whenReloadingWhitelistAndNothingChangesShouldNotNotifyWhitelistModifiedSubscribers()
      throws Exception {
    final Path permissionsFile = createPermissionsFileWithNode(enode1);
    final LocalPermissioningConfiguration permissioningConfig =
        mock(LocalPermissioningConfiguration.class);
    final Consumer<NodeWhitelistUpdatedEvent> consumer = mock(Consumer.class);

    when(permissioningConfig.getNodePermissioningConfigFilePath())
        .thenReturn(permissionsFile.toAbsolutePath().toString());
    when(permissioningConfig.isNodeWhitelistEnabled()).thenReturn(true);
    when(permissioningConfig.getNodeWhitelist()).thenReturn(Arrays.asList(URI.create(enode1)));
    controller =
        new NodeLocalConfigPermissioningController(
            permissioningConfig, bootnodesList, new EnodeURL(selfEnode));
    controller.subscribeToListUpdatedEvent(consumer);

    controller.reload();

    verifyZeroInteractions(consumer);
  }

  private Path createPermissionsFileWithNode(final String node) throws IOException {
    final String nodePermissionsFileContent = "nodes-whitelist=[\"" + node + "\"]";
    final Path permissionsFile = Files.createTempFile("node_permissions", "");
    permissionsFile.toFile().deleteOnExit();
    Files.write(permissionsFile, nodePermissionsFileContent.getBytes(StandardCharsets.UTF_8));
    return permissionsFile;
  }
}
