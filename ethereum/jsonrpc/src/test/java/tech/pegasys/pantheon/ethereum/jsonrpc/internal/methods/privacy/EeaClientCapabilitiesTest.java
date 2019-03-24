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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.privacy.ClientCapabilitiesResult;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

public class EeaClientCapabilitiesTest {

  private EeaClientCapabilities method;
  private final String JSON_RPC_VERSION = "2.0";
  private final String ETH_METHOD = "eea_clientCapabilities";

  @Before
  public void setUp() {
    method = new EeaClientCapabilities();
  }

  @Test
  public void requestClientCapabilities() {

    final JsonRpcRequest request = new JsonRpcRequest(JSON_RPC_VERSION, ETH_METHOD, null);

    final JsonRpcSuccessResponse expectedResponse =
        new JsonRpcSuccessResponse(
            request.getId(),
            new ClientCapabilitiesResult(
                Arrays.asList("PoW", "IBFT", "PoA/Clique"),
                Collections.singletonList("restricted")));

    final JsonRpcResponse actualResponse = method.response(request);

    assertThat(actualResponse).isEqualToComparingFieldByFieldRecursively(expectedResponse);
  }
}
