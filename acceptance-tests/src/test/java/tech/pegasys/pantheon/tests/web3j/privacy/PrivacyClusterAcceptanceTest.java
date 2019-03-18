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
package tech.pegasys.pantheon.tests.web3j.privacy;

import tech.pegasys.orion.testutil.OrionTestHarness;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.PantheonNode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PrivacyClusterAcceptanceTest extends PrivateAcceptanceTestBase {
  // Contract address is generated from sender address and transaction nonce
  protected static final Address CONTRACT_ADDRESS =
      Address.fromHexString("0x0bac79b78b9866ef11c989ad21a7fcf15f7a18d7");

  protected static final String PUBLIC_KEY_1 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  protected static final String PUBLIC_KEY_2 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  protected static final String PUBLIC_KEY_3 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private PantheonNode node1;
  private PantheonNode node2;
  private PantheonNode node3;
  private static OrionTestHarness enclave1;
  private static OrionTestHarness enclave2;
  //  private static OrionTestHarness enclave3;

  @Before
  public void setUp() throws Exception {
    enclave1 = createEnclave("orion_key_0.pub", "orion_key_0.key");
    enclave2 = createEnclave("orion_key_1.pub", "orion_key_1.key", enclave1.nodeUrl());
    node1 =
        pantheon.createPrivateTransactionEnabledMinerNode(
            "node1", getPrivacyParams(enclave1), "key");
    node2 =
        pantheon.createPrivateTransactionEnabledNode("node2", getPrivacyParams(enclave2), "key1");
    //    node3 = pantheon.createPrivateTransactionEnabledMinerNode("node3", privacyParameters3);
    cluster.start(node1, node2);
  }

  @Test
  public void node2CanSeeContract() {
    final String transactionHash =
        node1.execute(transactions.deployPrivateSmartContract(getDeployEventEmitterCluster()));

    privateTransactionVerifier
        .validPrivateContractDeployed(CONTRACT_ADDRESS.toString())
        .verify(node2, transactionHash, PUBLIC_KEY_2);
  }

  @Test
  public void node2CanExecuteContract() {

    node1.execute(transactions.deployPrivateSmartContract(getDeployEventEmitterCluster()));

    final String transactionHash =
        node2.execute(transactions.createPrivateRawTransaction(getExecuteStoreFuncCluster()));

    privateTransactionVerifier
        .validEventReturned("1000")
        .verify(node1, transactionHash, PUBLIC_KEY_1);
  }

  @Test
  public void node2CanSeePrivateTransactionReceipt() {

    String transactionHash =
        node1.execute(transactions.deployPrivateSmartContract(getDeployEventEmitterCluster()));

    privateTransactionVerifier
        .validPrivateContractDeployed(CONTRACT_ADDRESS.toString())
        .verify(node2, transactionHash, PUBLIC_KEY_2);

    transactionHash =
        node2.execute(transactions.createPrivateRawTransaction(getExecuteStoreFuncCluster()));

    privateTransactionVerifier
        .validEventReturned("1000")
        .verify(node1, transactionHash, PUBLIC_KEY_1);

    transactionHash =
        node2.execute(transactions.createPrivateRawTransaction(getExecuteGetFuncCluster()));

    privateTransactionVerifier
        .validOutputReturned("1000")
        .verify(node2, transactionHash, PUBLIC_KEY_2);
  }

  //  @Test
  //  public void node3CannotSeeContract() throws IOException {
  //
  //    final String transactionHash =
  //            node1.execute(transactions.deployPrivateSmartContract(getDeploySimpleStorage()));
  //
  //    privateTransactionVerifier
  //            .validOutputReturned("1000")
  //            .verify(node3, transactionHash, PUBLIC_KEY_3);
  //  }

  //  @Test
  //  public void node3CannotExecuteContract() throws IOException {
  //
  //    node1.execute(transactions.deployPrivateSmartContract(getDeploySimpleStorage()));
  //
  //    final String transactionHash =
  //            node3.execute(transactions.createPrivateRawTransaction(getExecuteStoreFuncNode3()));
  //
  //    privateTransactionVerifier
  //            .validOutputReturned("1000")
  //            .verify(node3, transactionHash, PUBLIC_KEY_3);
  //  }

  @After
  public void tearDown() {
    enclave1.getOrion().stop();
    enclave2.getOrion().stop();
    //    enclave3.getOrion().stop();
  }
}
