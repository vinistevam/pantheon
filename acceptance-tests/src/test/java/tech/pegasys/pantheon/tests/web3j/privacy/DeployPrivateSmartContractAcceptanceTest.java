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

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DeployPrivateSmartContractAcceptanceTest extends AcceptanceTestBase {

  @ClassRule public static final TemporaryFolder folder = new TemporaryFolder();

  private PantheonNode minerNode;
  private OrionTestHarness testHarness;

  @Before
  public void setUp() throws Exception {
    testHarness = OrionTestHarness.create(folder.newFolder().toPath());
    final PrivacyParameters privacyParameters = setPrivacyParameters();

    minerNode = pantheon.createPrivateTransactionEnabledMinerNode("miner-node", privacyParameters);
    cluster.start(minerNode);
  }

  @After
  public void tearDownOnce() {
    testHarness.getOrion().stop();
  }

  @Test
  public void deployingMustGiveValidReceipt() {

    // Verify if private contract is deployed
    final String signedRawDeployTransaction = toRlp(DEPLOY_CONTRACT);
    final String transactionHash =
        minerNode.execute(transactions.createPrivateRawTransaction(signedRawDeployTransaction));
    privateContractVerifier
        .validPrivateTransactionReceipt(minerNode, CONTRACT_ADDRESS.toString())
        .verifyContractDeployed(transactionHash, PUBLIC_KEY);

    // Verify the values returned in the events when calling a function of privately deployed
    // contract
    final String signedRawSetFunctionTransaction = toRlp(SET_FUNCTION_CALL);
    final String transactionHashSet =
        minerNode.execute(
            transactions.createPrivateRawTransaction(signedRawSetFunctionTransaction));
    privateContractVerifier
        .validPrivateTransactionReceipt(minerNode, CONTRACT_ADDRESS.toString())
        .verifyEventsReturned(transactionHashSet, PUBLIC_KEY);

    // Verify the values returned in the output when calling a function of privately deployed
    // contract
    final String signedRawGetFunctionTransaction = toRlp(GET_FUNCTION_CALL);
    final String transactionHashGet =
        minerNode.execute(
            transactions.createPrivateRawTransaction(signedRawGetFunctionTransaction));
    privateContractVerifier
        .validPrivateTransactionReceipt(minerNode, CONTRACT_ADDRESS.toString())
        .verifyOutputReturned(transactionHashGet, PUBLIC_KEY);

    // TODO: fire function call from minerNode and from a non-privy node

  }

  private String toRlp(final PrivateTransaction transaction) {
    BytesValueRLPOutput bvrlpo = new BytesValueRLPOutput();
    transaction.writeTo(bvrlpo);
    return bvrlpo.encoded().toString();
  }

  private PrivacyParameters setPrivacyParameters() throws IOException {
    final PrivacyParameters privacyParameters = new PrivacyParameters();
    privacyParameters.setEnabled(true);
    privacyParameters.setUrl(testHarness.clientUrl());
    privacyParameters.setPrivacyAddress(Address.PRIVACY);
    privacyParameters.setPublicKeyUsingFile(testHarness.getConfig().publicKeys().get(0).toFile());
    privacyParameters.enablePrivateDB(folder.newFolder("private").toPath());
    return privacyParameters;
  }
}
