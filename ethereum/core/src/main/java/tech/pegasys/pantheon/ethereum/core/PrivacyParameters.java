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
package tech.pegasys.pantheon.ethereum.core;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.pantheon.ethereum.storage.StorageProvider;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.RocksDbStorageProvider;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateArchive;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateStorage;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import com.google.common.io.Files;

public class PrivacyParameters {
  private static final String ENCLAVE_URL = "http://localhost:8888";
  public static final URI DEFAULT_ENCLAVE_URL = URI.create(ENCLAVE_URL);
  String PRIVATE_DATABASE_PATH = "private";

  private Integer privacyAddress;
  private boolean enabled;
  private String url;
  private String publicKey;
  private File publicKeyFile;
  private WorldStateArchive privateWorldStateArchive;

  public String getPublicKey() {
    return publicKey;
  }

  public File getPublicKeyFile() {
    return publicKeyFile;
  }

  public void setPublicKeyUsingFile(final File publicKeyFile) throws IOException {
    this.publicKeyFile = publicKeyFile;
    this.publicKey = Files.asCharSource(publicKeyFile, UTF_8).read();
  }

  public static PrivacyParameters noPrivacy() {
    final PrivacyParameters config = new PrivacyParameters();
    config.setEnabled(false);
    config.setUrl(ENCLAVE_URL);
    config.setPrivacyAddress(Address.PRIVACY);
    return config;
  }

  @Override
  public String toString() {
    return "PrivacyParameters{" + "enabled=" + enabled + ", url='" + url + '\'' + '}';
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getUrl() {
    return this.url;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public Integer getPrivacyAddress() {
    return privacyAddress;
  }

  public void setPrivacyAddress(final Integer privacyAddress) {
    this.privacyAddress = privacyAddress;
  }

  public void enablePrivateDB(final Path path) throws IOException {
    final Path privateDbPath = path.resolve(PRIVATE_DATABASE_PATH);
    final StorageProvider privateStorageProvider =
        RocksDbStorageProvider.create(privateDbPath, new NoOpMetricsSystem());
    final WorldStateStorage privateWorldStateStorage =
        privateStorageProvider.createWorldStateStorage();
    this.privateWorldStateArchive = new WorldStateArchive(privateWorldStateStorage);
  }

  public WorldStateArchive getPrivateWorldStateArchive() {
    return privateWorldStateArchive;
  }
}
