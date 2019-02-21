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
package tech.pegasys.pantheon.ethereum.jsonrpc;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import tech.pegasys.pantheon.ethereum.blockcreation.EthHashMiningCoordinator;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Synchronizer;
import tech.pegasys.pantheon.ethereum.core.TransactionPool;
import tech.pegasys.pantheon.ethereum.eth.EthProtocol;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.filter.FilterManager;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetProtocolSchedule;
import tech.pegasys.pantheon.ethereum.p2p.api.P2PNetwork;
import tech.pegasys.pantheon.ethereum.p2p.wire.Capability;
import tech.pegasys.pantheon.ethereum.permissioning.AccountWhitelistController;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Lists;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonRpcHttpServiceRpcApisTest {
  @Rule public final TemporaryFolder folder = new TemporaryFolder();

  private final Vertx vertx = Vertx.vertx();
  private final OkHttpClient client = new OkHttpClient();
  private JsonRpcHttpService service;
  private static String baseUrl;
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final String CLIENT_VERSION = "TestClientVersion/0.1.0";
  private JsonRpcConfiguration configuration;

  @Mock protected static BlockchainQueries blockchainQueries;

  private final JsonRpcTestHelper testHelper = new JsonRpcTestHelper();

  @Before
  public void before() {
    configuration = JsonRpcConfiguration.createDefault();
    configuration.setPort(0);
  }

  @After
  public void after() {
    service.stop().join();
  }

  @Test
  public void requestWithNetMethodShouldSucceedWhenDefaultApisEnabled() throws Exception {
    service = createJsonRpcHttpServiceWithRpcApis(configuration);
    final String id = "123";
    final RequestBody body =
        RequestBody.create(
            JSON,
            "{\"jsonrpc\":\"2.0\",\"id\":" + Json.encode(id) + ",\"method\":\"net_version\"}");

    try (final Response resp = client.newCall(buildRequest(body)).execute()) {
      assertThat(resp.code()).isEqualTo(200);
    }
  }

  @Test
  public void requestWithNetMethodShouldSucceedWhenNetApiIsEnabled() throws Exception {
    service = createJsonRpcHttpServiceWithRpcApis(RpcApis.NET);
    final String id = "123";
    final RequestBody body =
        RequestBody.create(
            JSON,
            "{\"jsonrpc\":\"2.0\",\"id\":" + Json.encode(id) + ",\"method\":\"net_version\"}");

    try (final Response resp = client.newCall(buildRequest(body)).execute()) {
      assertThat(resp.code()).isEqualTo(200);
    }
  }

  @Test
  public void requestWithNetMethodShouldFailWhenNetApiIsNotEnabled() throws Exception {
    service = createJsonRpcHttpServiceWithRpcApis(RpcApis.WEB3);
    final String id = "123";
    final RequestBody body =
        RequestBody.create(
            JSON,
            "{\"jsonrpc\":\"2.0\",\"id\":" + Json.encode(id) + ",\"method\":\"net_version\"}");

    try (final Response resp = client.newCall(buildRequest(body)).execute()) {
      assertThat(resp.code()).isEqualTo(400);
      // Check general format of result
      final JsonObject json = new JsonObject(resp.body().string());
      final JsonRpcError expectedError = JsonRpcError.METHOD_NOT_FOUND;
      testHelper.assertValidJsonRpcError(
          json, id, expectedError.getCode(), expectedError.getMessage());
    }
  }

  @Test
  public void requestWithNetMethodShouldSucceedWhenNetApiAndOtherIsEnabled() throws Exception {
    service = createJsonRpcHttpServiceWithRpcApis(RpcApis.NET, RpcApis.WEB3);
    final String id = "123";
    final RequestBody body =
        RequestBody.create(
            JSON,
            "{\"jsonrpc\":\"2.0\",\"id\":" + Json.encode(id) + ",\"method\":\"net_version\"}");

    try (final Response resp = client.newCall(buildRequest(body)).execute()) {
      assertThat(resp.code()).isEqualTo(200);
    }
  }

  private JsonRpcConfiguration createJsonRpcConfigurationWithRpcApis(final RpcApi... rpcApis) {
    final JsonRpcConfiguration config = JsonRpcConfiguration.createDefault();
    config.setCorsAllowedDomains(singletonList("*"));
    config.setPort(0);
    if (rpcApis != null) {
      config.setRpcApis(Lists.newArrayList(rpcApis));
    }
    return config;
  }

  private JsonRpcHttpService createJsonRpcHttpServiceWithRpcApis(final RpcApi... rpcApis)
      throws Exception {
    return createJsonRpcHttpServiceWithRpcApis(createJsonRpcConfigurationWithRpcApis(rpcApis));
  }

  private JsonRpcHttpService createJsonRpcHttpServiceWithRpcApis(final JsonRpcConfiguration config)
      throws Exception {
    final Set<Capability> supportedCapabilities = new HashSet<>();
    supportedCapabilities.add(EthProtocol.ETH62);
    supportedCapabilities.add(EthProtocol.ETH63);

    final Map<String, JsonRpcMethod> rpcMethods =
        spy(
            new JsonRpcMethodsFactory()
                .methods(
                    CLIENT_VERSION,
                    mock(P2PNetwork.class),
                    blockchainQueries,
                    mock(Synchronizer.class),
                    MainnetProtocolSchedule.create(),
                    mock(FilterManager.class),
                    mock(TransactionPool.class),
                    mock(EthHashMiningCoordinator.class),
                    new NoOpMetricsSystem(),
                    supportedCapabilities,
                    Optional.of(mock(AccountWhitelistController.class)),
                    config.getRpcApis(),
                    mock(PrivacyParameters.class)));
    final JsonRpcHttpService jsonRpcHttpService =
        new JsonRpcHttpService(
            vertx, folder.newFolder().toPath(), config, new NoOpMetricsSystem(), rpcMethods);
    jsonRpcHttpService.start().join();

    baseUrl = jsonRpcHttpService.url();
    return jsonRpcHttpService;
  }

  private Request buildRequest(final RequestBody body) {
    return new Request.Builder().post(body).url(baseUrl).build();
  }
}
