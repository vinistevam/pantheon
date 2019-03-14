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

import com.google.common.collect.Lists;
import org.web3j.crypto.Credentials;
import tech.pegasys.orion.testutil.OrionTestHarness;
import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.privacy.PrivateTransaction;
import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.PantheonNode;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import static java.nio.charset.StandardCharsets.UTF_8;
import static tech.pegasys.pantheon.tests.acceptance.dsl.WaitUtils.waitFor;

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
  private static PrivacyParameters privacyParameters1;
  private static PrivacyParameters privacyParameters2;
//  private static PrivacyParameters privacyParameters3;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    enclave1 = createEnclave("orion_key_0.pub", "orion_key_0.key");
    enclave2 = createEnclave("orion_key_1.pub", "orion_key_1.key", enclave1.nodeUrl());
//    enclave3 =
//        createEnclave("orion_key_2.pub", "orion_key_2.key", enclave1.nodeUrl(), enclave2.nodeUrl());
    privacyParameters1 = getPrivacyParams(enclave1, "p1");
    privacyParameters2 = getPrivacyParams(enclave2, "p2");
//    privacyParameters3 = getPrivacyParams(enclave3, "p3");
  }

  @Before
  public void setUp() throws Exception {
    node1 = pantheon.createPrivateTransactionEnabledNode("node1", privacyParameters1);
    node2 = pantheon.createPrivateTransactionEnabledMinerNode("node2", privacyParameters2);
//    node3 = pantheon.createPrivateTransactionEnabledMinerNode("node3", privacyParameters3);
    cluster.start(node1, node2);
  }

  @Test
  public void node2CanSeeContract() throws IOException {
    final String transactionHash =
            node1.execute(transactions.deployPrivateSmartContract(getDeploySimpleStorageCluster()));

    privateTransactionVerifier
            .validPrivateContractDeployed(CONTRACT_ADDRESS.toString())
            .verify(node2, transactionHash, PUBLIC_KEY_2);
  }

  @Test
  public void node2CanExecuteContract() throws IOException {

    node1.execute(transactions.deployPrivateSmartContract(getDeploySimpleStorageCluster()));

    final String transactionHash =
            node2.execute(transactions.createPrivateRawTransaction(getExecuteStoreFuncCluster()));

    privateTransactionVerifier
            .validEventReturned("1000")
            .verify(node2, transactionHash, PUBLIC_KEY_2);
  }

  @Test
  public void node2CanSeePrivateTransactionReceipt() throws IOException {

    node1.execute(transactions.deployPrivateSmartContract(getDeploySimpleStorageCluster()));

    node2.execute(transactions.createPrivateRawTransaction(getExecuteStoreFuncCluster()));

    final String transactionHash =
            node2.execute(transactions.createPrivateRawTransaction(getExecuteGetFuncCluster()));

    privateTransactionVerifier
            .validOutputReturned("1000")
            .verify(node1, transactionHash, PUBLIC_KEY_1);
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

  @AfterClass
  public static void tearDownOnce() {
    enclave1.getOrion().stop();
    enclave2.getOrion().stop();
//    enclave3.getOrion().stop();
  }
}
