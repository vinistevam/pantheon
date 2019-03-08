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

import static tech.pegasys.pantheon.tests.web3j.privacy.ContractCallConstants.CONTRACT_ADDRESS;
import static tech.pegasys.pantheon.tests.web3j.privacy.ContractCallConstants.DEPLOY_CONTRACT;
import static tech.pegasys.pantheon.tests.web3j.privacy.ContractCallConstants.GET_FUNCTION_CALL;
import static tech.pegasys.pantheon.tests.web3j.privacy.ContractCallConstants.PUBLIC_KEY;
import static tech.pegasys.pantheon.tests.web3j.privacy.ContractCallConstants.SET_FUNCTION_CALL;

import tech.pegasys.orion.testutil.OrionTestHarness;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.privacy.PrivateTransaction;
import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import tech.pegasys.pantheon.tests.acceptance.dsl.AcceptanceTestBase;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.PantheonNode;
import tech.pegasys.pantheon.tests.acceptance.dsl.privacy.ExpectValidPrivateContractDeployedReceipt;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DeployPrivateSmartContractAcceptanceTest extends AcceptanceTestBase {

  @ClassRule public static final TemporaryFolder folder = new TemporaryFolder();

  private PantheonNode minerNode;
  private static OrionTestHarness testHarness;
  private static PrivacyParameters privacyParameters;
  private ExpectValidPrivateContractDeployedReceipt verifier;
  private final String deployPrivateSmartContract = toRlp(DEPLOY_CONTRACT);
  private final String executeStoreFunc = toRlp(SET_FUNCTION_CALL);
  private final String executeGetValueFunc = toRlp(GET_FUNCTION_CALL);

  @BeforeClass
  public static void setUpOnce() throws Exception {
    testHarness = OrionTestHarness.create(folder.newFolder().toPath());
    privacyParameters = setPrivacyParameters();
  }

  @AfterClass
  public static void tearDownOnce() {
    testHarness.getOrion().stop();
  }

  @Before
  public void setUp() throws Exception {
    minerNode = pantheon.createPrivateTransactionEnabledMinerNode("miner-node", privacyParameters);
    cluster.start(minerNode);
    verifier =
        privateContractVerifier.validPrivateTransactionReceipt(
            minerNode, CONTRACT_ADDRESS.toString());
  }

  @Test
  public void deployingMustGiveValidReceipt() {
    final String transactionHash =
        minerNode.execute(transactions.deployPrivateSmartContract(deployPrivateSmartContract));

    verifier.verifyContractDeployed(transactionHash, PUBLIC_KEY);
  }

  @Test
  public void privateSmartContractMustEmitValues() {

    minerNode.execute(transactions.deployPrivateSmartContract(deployPrivateSmartContract));

    final String transactionHash =
        minerNode.execute(transactions.createPrivateRawTransaction(executeStoreFunc));

    verifier.verifyEventsReturned(transactionHash, PUBLIC_KEY);
  }

  @Test
  public void privateSmartContractMustReturnValues() {

    minerNode.execute(transactions.deployPrivateSmartContract(deployPrivateSmartContract));

    minerNode.execute(transactions.createPrivateRawTransaction(executeStoreFunc));

    final String transactionHash =
        minerNode.execute(transactions.createPrivateRawTransaction(executeGetValueFunc));

    verifier.verifyOutputReturned(transactionHash, PUBLIC_KEY);
  }

  private String toRlp(final PrivateTransaction transaction) {
    BytesValueRLPOutput bvrlpo = new BytesValueRLPOutput();
    transaction.writeTo(bvrlpo);
    return bvrlpo.encoded().toString();
  }

  private static PrivacyParameters setPrivacyParameters() throws IOException {
    final PrivacyParameters privacyParameters = new PrivacyParameters();
    privacyParameters.setEnabled(true);
    privacyParameters.setUrl(testHarness.clientUrl());
    privacyParameters.setPrivacyAddress(Address.PRIVACY);
    privacyParameters.setPublicKeyUsingFile(testHarness.getConfig().publicKeys().get(0).toFile());
    privacyParameters.enablePrivateDB(folder.newFolder("private").toPath());
    return privacyParameters;
  }
}
