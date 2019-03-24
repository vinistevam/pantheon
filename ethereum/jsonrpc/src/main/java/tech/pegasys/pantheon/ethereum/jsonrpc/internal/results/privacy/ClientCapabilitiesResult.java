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

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.JsonRpcResult;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"consensus", "restriction"})
public class ClientCapabilitiesResult implements JsonRpcResult {
  private List<String> consensus;
  private List<String> restriction;

  public ClientCapabilitiesResult(final List<String> consensus, final List<String> restriction) {
    this.consensus = consensus;
    this.restriction = restriction;
  }

  @JsonGetter(value = "consensus")
  public List<String> getConsensus() {
    return consensus;
  }

  @JsonGetter(value = "restriction")
  public List<String> getRestriction() {
    return restriction;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClientCapabilitiesResult that = (ClientCapabilitiesResult) o;
    return consensus.equals(that.consensus) && restriction.equals(that.restriction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consensus, restriction);
  }
}
