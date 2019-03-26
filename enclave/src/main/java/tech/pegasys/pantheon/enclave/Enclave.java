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
package tech.pegasys.pantheon.enclave;

import tech.pegasys.pantheon.enclave.types.ReceiveRequest;
import tech.pegasys.pantheon.enclave.types.ReceiveResponse;
import tech.pegasys.pantheon.enclave.types.SendRequest;
import tech.pegasys.pantheon.enclave.types.SendResponse;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Enclave {
  private static final MediaType JSON = MediaType.parse("application/json");
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger LOG = LogManager.getLogger();

  private final String url;
  private final OkHttpClient client;

  public Enclave(final String enclaveUrl) {
    this.url = enclaveUrl;
    this.client = new OkHttpClient();
  }

  public Boolean upCheck() throws IOException {
    Request request = new Request.Builder().url(url + "/upcheck").get().build();

    try (Response response = client.newCall(request).execute()) {
      return response.isSuccessful();
    } catch (IOException e) {
      LOG.error("Enclave failed to execute upcheck");
      throw new IOException("Failed to perform upcheck", e);
    }
  }

  public SendResponse send(final SendRequest content) throws IOException {
    RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(content));
    Request request = new Request.Builder()
            .url(url + "/send").post(body).build();
    return executePost(request, SendResponse.class);
  }

  public ReceiveResponse receive(final ReceiveRequest content) throws IOException {
    RequestBody body = RequestBody.create(MediaType.get("application/vnd.orion.v1+json"), objectMapper.writeValueAsString(content));
    Request request = new Request.Builder()
            .url(url + "/receive").post(body).build();
    return executePost(request, ReceiveResponse.class);
  }

  private <T> T executePost(final Request request, final Class<T> responseType)
      throws IOException {
    OkHttpClient client = new OkHttpClient();



    try (Response response = client.newCall(request).execute()) {
      return objectMapper.readValue(response.body().string(), responseType);
    } catch (IOException e) {
      LOG.error("Enclave failed to execute ", request);
      throw new IOException("Failed to execute post", e);
    }
  }
}
