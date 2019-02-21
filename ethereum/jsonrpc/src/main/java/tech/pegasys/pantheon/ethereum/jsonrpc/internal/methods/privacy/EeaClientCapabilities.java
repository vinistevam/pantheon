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

import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;

import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EeaClientCapabilities implements JsonRpcMethod {
  private final PrivacyParameters privacyParameters;

  public EeaClientCapabilities(PrivacyParameters privacyParameters) {
    this.privacyParameters = privacyParameters;
  }

  @Override
  public String getName() {
    return "eea_clientCapabilities";
  }

  @Override
  public JsonRpcResponse response(JsonRpcRequest request) {

    String restriction = "unrestricted";
    ClientCapabilities clientCapabilities =
        new ClientCapabilities(
            privacyParameters.getConsensusSupported(), Arrays.asList(restriction));

    final ObjectMapper mapper = new ObjectMapper();
    String capabilities;
    try {
      capabilities = mapper.writeValueAsString(clientCapabilities);
    } catch (JsonProcessingException e) {
      return null;
    }

    return new JsonRpcSuccessResponse(request.getId(), capabilities);
  }
}
