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
package tech.pegasys.pantheon.tests.acceptance.dsl.jsonrpc;

import tech.pegasys.pantheon.tests.acceptance.dsl.condition.Condition;
import tech.pegasys.pantheon.tests.acceptance.dsl.condition.eea.ExpectSuccessfulEeaGetTransactionReceipt;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.eea.EeaTransactions;

public class Eea {

  EeaTransactions transactions;

  public Eea(final EeaTransactions transactions) {
    this.transactions = transactions;
  }

  public Condition expectSuccessfulTransactionReceipt(
      final String transactionHash, final String publicKey) {
    return new ExpectSuccessfulEeaGetTransactionReceipt(
        transactions.getTransactionReceipt(transactionHash, publicKey));
  }
}
