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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static tech.pegasys.pantheon.tests.acceptance.dsl.WaitUtils.waitFor;

import tech.pegasys.orion.testutil.OrionTestHarness;
import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.privacy.PrivateTransaction;
import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import tech.pegasys.pantheon.tests.acceptance.dsl.AcceptanceTestBase;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.PantheonNode;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.ResponseTypes.PrivateTransactionReceipt;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.math.BigInteger;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

public class DeployPrivateSmartContractAcceptanceTest extends AcceptanceTestBase {

  @ClassRule public static final TemporaryFolder folder = new TemporaryFolder();

  // Contract address is generated from sender address and transaction nonce
  private static final Address CONTRACT_ADDRESS =
      Address.fromHexString("0x42699a7612a82f1d9c36148af9c77354759b210b");
  private static final String PUBLIC_KEY = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";

  private static final Address SENDER =
      Address.fromHexString(
          Credentials.create("8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63")
              .getAddress());

  private static final PrivateTransaction DEPLOY_CONTRACT =
      PrivateTransaction.builder()
          .nonce(0)
          .gasPrice(Wei.of(1000))
          .gasLimit(3000000)
          .to(null)
          .value(Wei.ZERO)
          .payload(
              BytesValue.fromHexString(
                  "0x608060405234801561001057600080fd5b5060008054600160a06"
                      + "0020a03191633179055610199806100326000396000f3fe6080"
                      + "604052600436106100565763ffffffff7c01000000000000000"
                      + "000000000000000000000000000000000000000006000350416"
                      + "633fa4f245811461005b5780636057361d1461008257806367e"
                      + "404ce146100ae575b600080fd5b34801561006757600080fd5b"
                      + "506100706100ec565b60408051918252519081900360200190f"
                      + "35b34801561008e57600080fd5b506100ac6004803603602081"
                      + "10156100a557600080fd5b50356100f2565b005b3480156100b"
                      + "a57600080fd5b506100c3610151565b6040805173ffffffffff"
                      + "ffffffffffffffffffffffffffffff909216825251908190036"
                      + "0200190f35b60025490565b6040805133815260208101839052"
                      + "81517fc9db20adedc6cf2b5d25252b101ab03e124902a73fcb1"
                      + "2b753f3d1aaa2d8f9f5929181900390910190a1600255600180"
                      + "5473ffffffffffffffffffffffffffffffffffffffff1916331"
                      + "79055565b60015473ffffffffffffffffffffffffffffffffff"
                      + "ffffff169056fea165627a7a72305820c7f729cb24e05c221f5"
                      + "aa913700793994656f233fe2ce3b9fd9a505ea17e8d8a0029"))
          .sender(SENDER)
          .chainId(2018)
          .privateFrom(BytesValue.wrap(PUBLIC_KEY.getBytes(UTF_8)))
          .privateFor(
              Lists.newArrayList(
                  BytesValue.wrap("Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=".getBytes(UTF_8))))
          .restriction(BytesValue.wrap("unrestricted".getBytes(UTF_8)))
          .signAndBuild(
              SECP256K1.KeyPair.create(
                  SECP256K1.PrivateKey.create(
                      new BigInteger(
                          "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
                          16))));

  private static final PrivateTransaction SET_FUNCTION_CALL =
      PrivateTransaction.builder()
          .nonce(1)
          .gasPrice(Wei.of(1000))
          .gasLimit(3000000)
          .to(CONTRACT_ADDRESS)
          .value(Wei.ZERO)
          .payload(
              BytesValue.fromHexString(
                  "0x6057361d00000000000000000000000000000000000000000000000000000000000003e8"))
          .sender(SENDER)
          .chainId(2018)
          .privateFrom(BytesValue.wrap(PUBLIC_KEY.getBytes(UTF_8)))
          .privateFor(
              Lists.newArrayList(
                  BytesValue.wrap("Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=".getBytes(UTF_8))))
          .restriction(BytesValue.wrap("unrestricted".getBytes(UTF_8)))
          .signAndBuild(
              SECP256K1.KeyPair.create(
                  SECP256K1.PrivateKey.create(
                      new BigInteger(
                          "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
                          16))));

  private static final PrivateTransaction GET_FUNCTION_CALL =
      PrivateTransaction.builder()
          .nonce(2)
          .gasPrice(Wei.of(1000))
          .gasLimit(3000000)
          .to(CONTRACT_ADDRESS)
          .value(Wei.ZERO)
          .payload(BytesValue.fromHexString("0x3fa4f245"))
          .sender(SENDER)
          .chainId(2018)
          .privateFrom(BytesValue.wrap(PUBLIC_KEY.getBytes(UTF_8)))
          .privateFor(
              Lists.newArrayList(
                  BytesValue.wrap("Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=".getBytes(UTF_8))))
          .restriction(BytesValue.wrap("unrestricted".getBytes(UTF_8)))
          .signAndBuild(
              SECP256K1.KeyPair.create(
                  SECP256K1.PrivateKey.create(
                      new BigInteger(
                          "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
                          16))));

  private PantheonNode minerNode;

  private OrionTestHarness testHarness;

  @Before
  public void setUp() throws Exception {
    testHarness = OrionTestHarness.create(folder.newFolder().toPath());

    final PrivacyParameters privacyParameters = new PrivacyParameters();
    privacyParameters.setEnabled(true);
    privacyParameters.setUrl(testHarness.clientUrl());
    privacyParameters.setPrivacyAddress(Address.PRIVACY);
    privacyParameters.setPublicKeyUsingFile(testHarness.getConfig().publicKeys().get(0).toFile());
    privacyParameters.enablePrivateDB(folder.newFolder("private").toPath());

    minerNode = pantheon.createPrivateTransactionEnabledMinerNode("miner-node", privacyParameters);
    cluster.start(minerNode);
  }

  @After
  public void tearDownOnce() {
    testHarness.getOrion().stop();
  }

  @Test
  public void deployingMustGiveValidReceipt() {

    final String signedRawDeployTransaction = toRlp(DEPLOY_CONTRACT);
    final String transactionHash =
        minerNode.execute(transactions.createPrivateRawTransaction(signedRawDeployTransaction));
    waitFor(
        90,
        () ->
            minerNode.verify(eea.expectSuccessfulTransactionReceipt(transactionHash, PUBLIC_KEY)));
    TransactionReceipt txReceipt =
        minerNode.execute(transactions.getTransactionReceipt(transactionHash)).get();
    assertEquals(Address.DEFAULT_PRIVACY.toString(), txReceipt.getTo());
    PrivateTransactionReceipt privateTxReceipt =
        minerNode.execute(transactions.getPrivateTransactionReceipt(transactionHash, PUBLIC_KEY));
    assertEquals(CONTRACT_ADDRESS.toString(), privateTxReceipt.getContractAddress());

    final String signedRawSetFunctionTransaction = toRlp(SET_FUNCTION_CALL);
    final String transactionHashSet =
        minerNode.execute(
            transactions.createPrivateRawTransaction(signedRawSetFunctionTransaction));
    waitFor(
        90,
        () ->
            minerNode.verify(
                eea.expectSuccessfulTransactionReceipt(transactionHashSet, PUBLIC_KEY)));
    PrivateTransactionReceipt privateTxReceiptSet =
        minerNode.execute(
            transactions.getPrivateTransactionReceipt(transactionHashSet, PUBLIC_KEY));
    String event = privateTxReceiptSet.getLogs().get(0).getData().substring(66, 130);
    assertEquals(Numeric.decodeQuantity(Numeric.prependHexPrefix(event)), BigInteger.valueOf(1000));

    final String signedRawGetFunctionTransaction = toRlp(GET_FUNCTION_CALL);
    final String transactionHashGet =
        minerNode.execute(
            transactions.createPrivateRawTransaction(signedRawGetFunctionTransaction));
    waitFor(
        90,
        () ->
            minerNode.verify(
                eea.expectSuccessfulTransactionReceipt(transactionHashGet, PUBLIC_KEY)));
    PrivateTransactionReceipt privateTxReceiptGet =
        minerNode.execute(
            transactions.getPrivateTransactionReceipt(transactionHashGet, PUBLIC_KEY));
    BytesValue outputGet = BytesValue.fromHexString(privateTxReceiptGet.getOutput());
    assertEquals(CONTRACT_ADDRESS.toString(), privateTxReceipt.getContractAddress());
    assertEquals(Numeric.decodeQuantity(outputGet.toString()), BigInteger.valueOf(1000));

    // TODO: fire function call from minerNode and from a non-privy node

  }

  private String toRlp(final PrivateTransaction transaction) {
    BytesValueRLPOutput bvrlpo = new BytesValueRLPOutput();
    transaction.writeTo(bvrlpo);
    return bvrlpo.encoded().toString();
  }
}
