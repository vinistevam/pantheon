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

import static org.junit.Assert.assertEquals;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.privacy.ClientCapabilitiesResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class EeaClientCapabilitiesTest {
  @Test
  public void requestClientCapabilities() throws IOException {
    final EeaClientCapabilities eeaClientCapabilities = new EeaClientCapabilities();

    final JsonRpcRequest request = new JsonRpcRequest("1", "eea_clientCapabilities", null);

    final JsonRpcSuccessResponse actualResponse =
        (JsonRpcSuccessResponse) eeaClientCapabilities.response(request);

    final ObjectMapper mapper = new ObjectMapper();
    ClientCapabilitiesResult capabilities;
    capabilities =
        mapper.readValue(actualResponse.getResult().toString(), ClientCapabilitiesResult.class);

    assertEquals(capabilities.getRestriction(), Collections.singletonList("restricted"));
    assertEquals(capabilities.getConsensus(), Arrays.asList("PoW", "IBFT", "PoS/Clique"));
  }
}
