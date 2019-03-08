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
package tech.pegasys.pantheon.tests.acceptance.dsl.privacy;

import static org.junit.Assert.assertEquals;
import static tech.pegasys.pantheon.tests.acceptance.dsl.WaitUtils.waitFor;

import tech.pegasys.pantheon.tests.acceptance.dsl.jsonrpc.Eea;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.PantheonNode;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.ResponseTypes;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.Transactions;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.math.BigInteger;

import org.web3j.utils.Numeric;

public class ExpectValidPrivateContractDeployedReceipt {

  private final PantheonNode minerNode;
  private final String contractAddress;
  private final Transactions transactions;
  private final Eea eea;

  public ExpectValidPrivateContractDeployedReceipt(
      final PantheonNode minerNode,
      final String contractAddress,
      final Eea eea,
      final Transactions transactions) {
    this.minerNode = minerNode;
    this.contractAddress = contractAddress;
    this.eea = eea;
    this.transactions = transactions;
  }

  public void verifyContractDeployed(final String transactionHash, final String PUBLIC_KEY) {
    ResponseTypes.PrivateTransactionReceipt privateTxReceipt =
        getPrivateTransactionReceipt(transactionHash, PUBLIC_KEY);

    assertEquals(contractAddress, privateTxReceipt.getContractAddress());
  }

  public void verifyEventsReturned(final String transactionHash, final String PUBLIC_KEY) {
    ResponseTypes.PrivateTransactionReceipt privateTxReceipt =
        getPrivateTransactionReceipt(transactionHash, PUBLIC_KEY);

    String event = privateTxReceipt.getLogs().get(0).getData().substring(66, 130);
    assertEquals(Numeric.decodeQuantity(Numeric.prependHexPrefix(event)), BigInteger.valueOf(1000));
  }

  public void verifyOutputReturned(final String transactionHash, final String PUBLIC_KEY) {
    ResponseTypes.PrivateTransactionReceipt privateTxReceipt =
        getPrivateTransactionReceipt(transactionHash, PUBLIC_KEY);

    BytesValue output = BytesValue.fromHexString(privateTxReceipt.getOutput());
    assertEquals(Numeric.decodeQuantity(output.toString()), BigInteger.valueOf(1000));
  }

  private ResponseTypes.PrivateTransactionReceipt getPrivateTransactionReceipt(
      final String transactionHash, final String PUBLIC_KEY) {
    waitFor(
        90,
        () ->
            minerNode.verify(eea.expectSuccessfulTransactionReceipt(transactionHash, PUBLIC_KEY)));
    ResponseTypes.PrivateTransactionReceipt privateTxReceipt =
        minerNode.execute(transactions.getPrivateTransactionReceipt(transactionHash, PUBLIC_KEY));
    return privateTxReceipt;
  }
}
