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
package tech.pegasys.pantheon.ethereum.eth.sync.worldstate;

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.services.tasks.Task;

public class StubTask implements Task<NodeDataRequest> {

  private final NodeDataRequest data;
  private boolean completed = false;
  private boolean failed = false;

  public StubTask(final NodeDataRequest data) {
    this.data = data;
  }

  public static StubTask forHash(final Hash hash) {
    return new StubTask(NodeDataRequest.createCodeRequest(hash));
  }

  @Override
  public NodeDataRequest getData() {
    return data;
  }

  @Override
  public void markCompleted() {
    completed = true;
  }

  @Override
  public void markFailed() {
    failed = true;
  }

  public boolean isCompleted() {
    return completed;
  }

  public boolean isFailed() {
    return failed;
  }
}
