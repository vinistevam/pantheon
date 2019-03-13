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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.privacy;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.pantheon.enclave.Enclave;
import tech.pegasys.pantheon.enclave.types.ReceiveRequest;
import tech.pegasys.pantheon.enclave.types.ReceiveResponse;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.privacy.PrivateTransactionReceiptResult;
import tech.pegasys.pantheon.ethereum.privacy.PrivateTransaction;
import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPInput;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

public class EeaGetTransactionReceipt implements JsonRpcMethod {

  private final BlockchainQueries blockchain;
  private final Enclave enclave;
  private final JsonRpcParameter parameters;
  private final PrivacyParameters privacyParameters;

  public EeaGetTransactionReceipt(
      final BlockchainQueries blockchain,
      final Enclave enclave,
      final JsonRpcParameter parameters,
      final PrivacyParameters privacyParameters) {
    this.blockchain = blockchain;
    this.enclave = enclave;
    this.parameters = parameters;
    this.privacyParameters = privacyParameters;
  }

  @Override
  public String getName() {
    return "eea_getTransactionReceipt";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final Hash hash = parameters.required(request.getParams(), 0, Hash.class);
    final String publicKey = parameters.required(request.getParams(), 1, String.class);
    final Optional<Transaction> transactionCompleteResult =
        blockchain.getBlockchain().getTransactionByHash(hash);
    Hash blockHash = blockchain.getBlockchain().getChainHeadBlock().getHash();
    long blockNumber = blockchain.getBlockchain().getChainHeadBlockNumber();
    int txIndex =
        blockchain
            .getBlockchain()
            .getBlockBody(blockchain.getBlockchain().getBlockHeader(blockNumber).get().getHash())
            .get()
            .getTransactions()
            .indexOf(transactionCompleteResult.get());
    final PrivateTransactionReceiptResult result =
        transactionCompleteResult
            .map(
                t -> {
                  final ReceiveRequest enclaveRequest =
                      new ReceiveRequest(
                          new String(t.getPayload().extractArray(), UTF_8), publicKey);
                  try {
                    ReceiveResponse enclaveResponse = enclave.receive(enclaveRequest);
                    final BytesValueRLPInput bytesValueRLPInput =
                        new BytesValueRLPInput(
                            BytesValue.wrap(
                                Base64.getDecoder().decode(enclaveResponse.getPayload())),
                            false);
                    PrivateTransaction privateTransaction =
                        PrivateTransaction.readFrom(bytesValueRLPInput);
                    final String contractAddress =
                        Address.contractAddress(
                                privateTransaction.getSender(), privateTransaction.getNonce())
                            .toString();

                    BytesValue rlpEncoded = RLP.encode(privateTransaction::writeTo);
                    Bytes32 txHash = tech.pegasys.pantheon.crypto.Hash.keccak256(rlpEncoded);

                    return new PrivateTransactionReceiptResult(
                        contractAddress,
                        privateTransaction.getSender().toString(),
                        privateTransaction.getTo().map(Address::toString).orElse(null),
                        privacyParameters.getPrivateStateStorage().getEvents(txHash).get(),
                        privacyParameters.getPrivateStateStorage().getOutput(txHash).get(),
                        blockHash,
                        hash,
                        blockNumber,
                        txIndex);
                  } catch (IOException e) {
                    return null;
                  }
                })
            .orElse(null);
    return new JsonRpcSuccessResponse(request.getId(), result);
  }
}
