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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.privacy;

import java.util.List;

public class ClientCapabilitiesResult {
  private List<String> consensus;
  private List<String> restriction;

  public ClientCapabilitiesResult() {}

  public ClientCapabilitiesResult(final List<String> consensus, final List<String> restriction) {
    this.consensus = consensus;
    this.restriction = restriction;
  }

  public List<String> getConsensus() {
    return consensus;
  }

  public void setConsensus(final List<String> consensus) {
    this.consensus = consensus;
  }

  public List<String> getRestriction() {
    return restriction;
  }

  public void setRestriction(final List<String> restriction) {
    this.restriction = restriction;
  }
}
