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
import static org.apache.logging.log4j.LogManager.getLogger;

import tech.pegasys.pantheon.enclave.Enclave;
import tech.pegasys.pantheon.enclave.types.ReceiveRequest;
import tech.pegasys.pantheon.enclave.types.ReceiveResponse;
import tech.pegasys.pantheon.ethereum.chain.TransactionLocation;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockBody;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.Log;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

public class EeaGetTransactionReceipt implements JsonRpcMethod {

  private static final Logger LOG = getLogger();

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
    LOG.trace("Executing eea_getTransactionReceipt");
    final Hash transactionHash = parameters.required(request.getParams(), 0, Hash.class);
    final String publicKey = parameters.required(request.getParams(), 1, String.class);
    final Optional<TransactionLocation> maybeLocation =
            blockchain.getBlockchain().getTransactionLocation(transactionHash);
    if (!maybeLocation.isPresent()) {
      return new JsonRpcSuccessResponse(request.getId(), null);
    }
    final TransactionLocation location = maybeLocation.get();
    final BlockBody blockBody = blockchain.getBlockchain().getBlockBody(location.getBlockHash()).get();
    final Transaction transaction = blockBody.getTransactions().get(location.getTransactionIndex());

    final Hash blockhash = location.getBlockHash();
    final long blockNumber = blockchain.getBlockchain().getBlockHeader(blockhash).get().getNumber();

    PrivateTransaction privateTransaction;
    try {
      privateTransaction = getTransactionFromEnclave(transaction, publicKey);
    } catch (Exception e) {
      LOG.error("Failed to fetch transaction from Enclave with error " + e.getMessage());
      return new JsonRpcErrorResponse(
          request.getId(), JsonRpcError.PRIVATE_TRANSACTION_RECEIPT_ERROR);
    }

    final String contractAddress =
        !privateTransaction.getTo().isPresent()
            ? Address.privateContractAddress(
                    privateTransaction.getSender(), privateTransaction.getNonce(), BytesValue.EMPTY)
                .toString()
            : null;

    LOG.trace("Calculated contractAddress: {}", contractAddress);

    BytesValue rlpEncoded = RLP.encode(privateTransaction::writeTo);
    Bytes32 txHash = tech.pegasys.pantheon.crypto.Hash.keccak256(rlpEncoded);

    LOG.trace("Calculated private transaction hash: {}", txHash);

    List<Log> events =
        privacyParameters
            .getPrivateTransactionStorage()
            .getEvents(txHash)
            .orElse(Collections.emptyList());

    LOG.trace("Processed private transaction events");

    BytesValue output =
        privacyParameters
            .getPrivateTransactionStorage()
            .getOutput(txHash)
            .orElse(BytesValue.wrap(new byte[0]));

    LOG.trace("Processed private transaction output");

    LOG.trace("Creating new PrivateTransactionReceiptResult(\n{},\n{},\n{},\n{},\n{},\n{},\n{},\n{},\n{})",
            contractAddress, privateTransaction.getSender().toString(),
            privateTransaction.getTo().map(Address::toString).orElse(null),
            events, output, blockhash, transactionHash, blockNumber, location.getTransactionIndex());

    PrivateTransactionReceiptResult result =
        new PrivateTransactionReceiptResult(
            contractAddress,
            privateTransaction.getSender().toString(),
            privateTransaction.getTo().map(Address::toString).orElse(null),
            events,
            output,
                blockhash,
                transactionHash,
            blockNumber,
                location.getTransactionIndex());

    LOG.trace("Created Private Transaction from given Transaction Hash");

    return new JsonRpcSuccessResponse(request.getId(), result);
  }

  private int getTransactionIndex(
      final BlockchainQueries blockchain, final Transaction transaction, final long blockNumber) {
    return blockchain
        .getBlockchain()
        .getBlockBody(blockchain.getBlockchain().getBlockHeader(blockNumber).get().getHash())
        .get()
        .getTransactions()
        .indexOf(transaction);
  }

  private PrivateTransaction getTransactionFromEnclave(
      final Transaction transaction, final String publicKey) throws IOException {
    LOG.trace("Fetching transaction information from Enclave");
    final ReceiveRequest enclaveRequest =
        new ReceiveRequest(new String(transaction.getPayload().extractArray(), UTF_8), publicKey);
    ReceiveResponse enclaveResponse = enclave.receive(enclaveRequest);
    final BytesValueRLPInput bytesValueRLPInput =
        new BytesValueRLPInput(
            BytesValue.wrap(Base64.getDecoder().decode(enclaveResponse.getPayload())), false);
    LOG.trace("Received transaction information from Enclave");
    return PrivateTransaction.readFrom(bytesValueRLPInput);
  }
}
