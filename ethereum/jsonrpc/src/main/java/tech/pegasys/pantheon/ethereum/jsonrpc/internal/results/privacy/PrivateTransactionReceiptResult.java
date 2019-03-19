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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.privacy;

import org.apache.logging.log4j.Logger;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.Log;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.TransactionReceiptLogResult;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.apache.logging.log4j.LogManager.getLogger;

@JsonPropertyOrder({
  "contractAddress",
  "from",
  "to",
  "output",
  "logs",
})
public class PrivateTransactionReceiptResult {

  private static final Logger LOG = getLogger();


  private final String contractAddress;
  private final String from;
  private final String to;
  private final String output;
  private final List<TransactionReceiptLogResult> logs;

  public PrivateTransactionReceiptResult(
      final String contractAddress,
      final String from,
      final String to,
      final List<Log> logs,
      final BytesValue output,
      final Hash blockHash,
      final Hash hash,
      final long blockNumber,
      final int txIndex) {
    this.contractAddress = contractAddress;
    this.from = from;
    this.to = to;
    this.output = output.toString();
    this.logs = logReceipts(logs, blockNumber, hash, blockHash, txIndex);
  }

  @JsonGetter(value = "contractAddress")
  public String getContractAddress() {
    return contractAddress;
  }

  @JsonGetter(value = "from")
  public String getFrom() {
    return from;
  }

  @JsonGetter(value = "to")
  public String getTo() {
    return to;
  }

  @JsonGetter(value = "output")
  public String getOutput() {
    return output;
  }

  @JsonGetter(value = "logs")
  public List<TransactionReceiptLogResult> getLogs() {
    return logs;
  }

  private List<TransactionReceiptLogResult> logReceipts(
      final List<Log> logs,
      final long blockNumber,
      final Hash transactionHash,
      final Hash blockHash,
      final int transactionIndex) {
    LOG.trace("Building transaction receipt log results");
    final List<TransactionReceiptLogResult> logResults = new ArrayList<>(logs.size());

    for (int i = 0; i < logs.size(); i++) {
      final Log log = logs.get(i);
      LOG.trace("Creating new TransactionReceiptLogResult(\n{},\n{},\n{},\n{},\n{},\n{})",
              log, blockNumber, transactionHash, blockHash, transactionIndex, i);
      logResults.add(
          new TransactionReceiptLogResult(
              log, blockNumber, transactionHash, blockHash, transactionIndex, i));
    }
    LOG.trace("Transaction receipt log results built");
    return logResults;
  }
}
