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

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.privacy.ClientCapabilitiesResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Implements eea spec v2. 6.3.3.3 eea_clientCapabilities */
public class EeaClientCapabilities implements JsonRpcMethod {

  public EeaClientCapabilities() {}

  @Override
  public String getName() {
    return "eea_clientCapabilities";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {

    // There is no way to programmatically get this
    List<String> restriction = Collections.singletonList("restricted");
    List<String> consensus = Arrays.asList("PoW", "IBFT", "PoA/Clique");

    ClientCapabilitiesResult clientCapabilitiesResult =
        new ClientCapabilitiesResult(consensus, restriction);

    return new JsonRpcSuccessResponse(request.getId(), clientCapabilitiesResult);
  }
}
