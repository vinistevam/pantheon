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

import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class EeaclientCapabilitiesTest {
  @Test
  public void requestClientCapabilities() throws IOException {
    PrivacyParameters privacyParameters = PrivacyParameters.noPrivacy();
    privacyParameters.appendConsensus("PoW");
    privacyParameters.appendConsensus("IBFT");

    final EeaClientCapabilities eeaClientCapabilities =
        new EeaClientCapabilities(privacyParameters);

    final JsonRpcRequest request = new JsonRpcRequest("1", "eea_clientCapabilities", null);

    final JsonRpcSuccessResponse actualResponse =
        (JsonRpcSuccessResponse) eeaClientCapabilities.response(request);

    final ObjectMapper mapper = new ObjectMapper();
    ClientCapabilities capabilities;
    capabilities =
        mapper.readValue(actualResponse.getResult().toString(), ClientCapabilities.class);

    assertThat(capabilities.getConsensus()).isEqualTo(privacyParameters.getConsensusSupported());
    assertThat(capabilities.getRestriction()).isEqualTo(privacyParameters.getRestrictions());
  }
}
