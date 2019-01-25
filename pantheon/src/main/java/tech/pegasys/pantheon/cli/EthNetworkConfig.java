/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.pantheon.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static tech.pegasys.pantheon.ethereum.p2p.config.DiscoveryConfiguration.GOERLI_BOOTSTRAP_NODES;
import static tech.pegasys.pantheon.ethereum.p2p.config.DiscoveryConfiguration.MAINNET_BOOTSTRAP_NODES;
import static tech.pegasys.pantheon.ethereum.p2p.config.DiscoveryConfiguration.RINKEBY_BOOTSTRAP_NODES;
import static tech.pegasys.pantheon.ethereum.p2p.config.DiscoveryConfiguration.ROPSTEN_BOOTSTRAP_NODES;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

public class EthNetworkConfig {
  private static final int MAINNET_NETWORK_ID = 1;
  private static final int RINKEBY_NETWORK_ID = 4;
  private static final int ROPSTEN_NETWORK_ID = 3;
  private static final int GOERLI_NETWORK_ID = 6284;
  private static final int DEV_NETWORK_ID = 2018;
  private static final String MAINNET_GENESIS = "mainnet.json";
  private static final String RINKEBY_GENESIS = "rinkeby.json";
  private static final String ROPSTEN_GENESIS = "ropsten.json";
  private static final String GOERLI_GENESIS = "goerli.json";
  private static final String DEV_GENESIS = "dev.json";
  private final String genesisConfig;
  private final int networkId;
  private final Collection<?> bootNodes;

  public EthNetworkConfig(
      final String genesisConfig, final int networkId, final Collection<?> bootNodes) {
    Preconditions.checkNotNull(genesisConfig);
    Preconditions.checkNotNull(bootNodes);
    this.genesisConfig = genesisConfig;
    this.networkId = networkId;
    this.bootNodes = bootNodes;
  }

  public String getGenesisConfig() {
    return genesisConfig;
  }

  public int getNetworkId() {
    return networkId;
  }

  public Collection<?> getBootNodes() {
    return bootNodes;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final EthNetworkConfig that = (EthNetworkConfig) o;
    return networkId == that.networkId
        && Objects.equals(genesisConfig, that.genesisConfig)
        && Objects.equals(Lists.newArrayList(bootNodes), Lists.newArrayList(that.bootNodes));
  }

  @Override
  public int hashCode() {
    return Objects.hash(genesisConfig, networkId, bootNodes);
  }

  @Override
  public String toString() {
    return "EthNetworkConfig{"
        + "genesisConfig="
        + genesisConfig
        + ", networkId="
        + networkId
        + ", bootNodes="
        + bootNodes
        + '}';
  }

  public static EthNetworkConfig getNetworkConfig(final NetworkName networkName) {
    switch (networkName) {
      case ROPSTEN:
        return new EthNetworkConfig(
            jsonConfig(ROPSTEN_GENESIS), ROPSTEN_NETWORK_ID, ROPSTEN_BOOTSTRAP_NODES);
      case RINKEBY:
        return new EthNetworkConfig(
            jsonConfig(RINKEBY_GENESIS), RINKEBY_NETWORK_ID, RINKEBY_BOOTSTRAP_NODES);
      case GOERLI:
        return new EthNetworkConfig(
            jsonConfig(GOERLI_GENESIS), GOERLI_NETWORK_ID, GOERLI_BOOTSTRAP_NODES);
      case DEV:
        return new EthNetworkConfig(jsonConfig(DEV_GENESIS), DEV_NETWORK_ID, new ArrayList<>());
      case MAINNET:
      default:
        return new EthNetworkConfig(
            jsonConfig(MAINNET_GENESIS), MAINNET_NETWORK_ID, MAINNET_BOOTSTRAP_NODES);
    }
  }

  private static String jsonConfig(final String resourceName) {
    try {
      URI uri = Resources.getResource(resourceName).toURI();
      return Resources.toString(uri.toURL(), UTF_8);
    } catch (final URISyntaxException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static class Builder {

    private String genesisConfig;
    private int networkId;
    private Collection<?> bootNodes;

    public Builder(final EthNetworkConfig ethNetworkConfig) {
      this.genesisConfig = ethNetworkConfig.genesisConfig;
      this.networkId = ethNetworkConfig.networkId;
      this.bootNodes = ethNetworkConfig.bootNodes;
    }

    public Builder setGenesisConfig(final String genesisConfig) {
      this.genesisConfig = genesisConfig;
      return this;
    }

    public Builder setNetworkId(final int networkId) {
      this.networkId = networkId;
      return this;
    }

    public Builder setBootNodes(final Collection<?> bootNodes) {
      this.bootNodes = bootNodes;
      return this;
    }

    public EthNetworkConfig build() {
      return new EthNetworkConfig(genesisConfig, networkId, bootNodes);
    }
  }
}
