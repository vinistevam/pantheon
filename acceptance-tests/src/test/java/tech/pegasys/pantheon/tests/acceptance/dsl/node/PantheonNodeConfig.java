package tech.pegasys.pantheon.tests.acceptance.dsl.node;

import tech.pegasys.pantheon.ethereum.blockcreation.MiningParameters;
import tech.pegasys.pantheon.ethereum.core.MiningParametersTestBuilder;
import tech.pegasys.pantheon.ethereum.jsonrpc.JsonRpcConfiguration;
import tech.pegasys.pantheon.ethereum.jsonrpc.JsonRpcConfiguration.RpcApis;
import tech.pegasys.pantheon.ethereum.jsonrpc.websocket.WebSocketConfiguration;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

public class PantheonNodeConfig {

  private final String name;
  private final MiningParameters miningParameters;
  private final JsonRpcConfiguration jsonRpcConfiguration;
  private final WebSocketConfiguration webSocketConfiguration;
  private ServerSocket serverSocket;

  private PantheonNodeConfig(
      final String name,
      final MiningParameters miningParameters,
      final JsonRpcConfiguration jsonRpcConfiguration,
      final WebSocketConfiguration webSocketConfiguration) {
    this.name = name;
    this.miningParameters = miningParameters;
    this.jsonRpcConfiguration = jsonRpcConfiguration;
    this.webSocketConfiguration = webSocketConfiguration;
  }

  private PantheonNodeConfig(final String name, final MiningParameters miningParameters) {
    this.name = name;
    this.miningParameters = miningParameters;
    this.jsonRpcConfiguration = createJsonRpcConfig();
    this.webSocketConfiguration = createWebSocketConfig();
  }

  private static MiningParameters createMiningParameters(final boolean miner) {
    return new MiningParametersTestBuilder().enabled(miner).build();
  }

  public static PantheonNodeConfig pantheonMinerNode(final String name) {
    return new PantheonNodeConfig(name, createMiningParameters(true));
  }

  public static PantheonNodeConfig pantheonNode(final String name) {
    return new PantheonNodeConfig(name, createMiningParameters(false));
  }

  public static PantheonNodeConfig pantheonRpcDisabledNode(final String name) {
    return new PantheonNodeConfig(
        name,
        createMiningParameters(false),
        JsonRpcConfiguration.createDefault(),
        WebSocketConfiguration.createDefault());
  }

  public static PantheonNodeConfig patheonNodeWithRpcApis(
      final String name, final RpcApis... enabledRpcApis) {
    final JsonRpcConfiguration jsonRpcConfig = createJsonRpcConfig();
    jsonRpcConfig.setRpcApis(Arrays.asList(enabledRpcApis));
    final WebSocketConfiguration webSocketConfig = createWebSocketConfig();
    webSocketConfig.setRpcApis(Arrays.asList(enabledRpcApis));

    return new PantheonNodeConfig(
        name, createMiningParameters(false), jsonRpcConfig, webSocketConfig);
  }

  private static JsonRpcConfiguration createJsonRpcConfig() {
    final JsonRpcConfiguration config = JsonRpcConfiguration.createDefault();
    config.setEnabled(true);
    config.setPort(0);
    return config;
  }

  private static WebSocketConfiguration createWebSocketConfig() {
    final WebSocketConfiguration config = WebSocketConfiguration.createDefault();
    config.setEnabled(true);
    config.setPort(0);
    return config;
  }

  public void initSocket() throws IOException {
    serverSocket = new ServerSocket(0);
  }

  public void closeSocket() throws IOException {
    serverSocket.close();
  }

  public int getSocketPort() {
    return serverSocket.getLocalPort();
  }

  public String getName() {
    return name;
  }

  public MiningParameters getMiningParameters() {
    return miningParameters;
  }

  public JsonRpcConfiguration getJsonRpcConfiguration() {
    return jsonRpcConfiguration;
  }

  public WebSocketConfiguration getWebSocketConfiguration() {
    return webSocketConfiguration;
  }
}