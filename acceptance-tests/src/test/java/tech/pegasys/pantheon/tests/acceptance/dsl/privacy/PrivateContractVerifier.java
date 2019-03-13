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

import tech.pegasys.pantheon.tests.acceptance.dsl.jsonrpc.Eea;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.PantheonNode;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.Transactions;

public class PrivateContractVerifier {

  private final Transactions transactions;
  private final Eea eea;

  public PrivateContractVerifier(final Eea eea, final Transactions transactions) {
    this.eea = eea;
    this.transactions = transactions;
  }

  public ExpectValidPrivateContractDeployedReceipt validPrivateTransactionReceipt(
      final PantheonNode minerNode, final String contractAddress) {
    return new ExpectValidPrivateContractDeployedReceipt(
        minerNode, contractAddress, eea, transactions);
  }
}
