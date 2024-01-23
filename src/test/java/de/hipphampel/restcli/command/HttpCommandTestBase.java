/*
 * The MIT License
 * Copyright © ${year} Johannes Hampel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.hipphampel.restcli.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import okhttp3.Headers;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
public class HttpCommandTestBase extends CommandTestBase {

  protected MockWebServer server;

  protected Function<RecordedRequest, MockResponse> dispatchFunction;
  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    server = new MockWebServer();
    server.setDispatcher(new Dispatcher() {
      @Override
      public MockResponse dispatch(RecordedRequest recordedRequest) {
        if (dispatchFunction == null) {
          return new MockResponse().setResponseCode(404);
        }
        return dispatchFunction.apply(recordedRequest);
      }
    });
    server.start();
    dispatchFunction = defaultDispatchFunction(any -> any.startsWith("test-"));
    context.environment().setLocalVariable("baseUrl", server.getHostName() + ":" + server.getPort());
    context.environment().setRequestTimeout(1000L);
  }

  @AfterEach
  protected void afterEach() throws IOException {
    server.shutdown();
  }

  Function<RecordedRequest, MockResponse> defaultDispatchFunction(Predicate<String> headerFilter) {
    return request -> {
      MockResponse response = new MockResponse();
      try {
        Map<String, List<String>> requestHeaders = request.getHeaders().toMultimap().entrySet().stream()
            .filter(entry -> headerFilter.test(entry.getKey()))
            .collect(Collectors.toMap(
                Entry::getKey,
                Entry::getValue,
                (a, b) -> b,
                TreeMap::new
            ));
        RequestData data = new RequestData(
            request.getMethod(),
            request.getPath(),
            requestHeaders,
            request.getBody().readString(StandardCharsets.UTF_8));
        String body = objectMapper.writeValueAsString(data);
        Headers headers = new Headers.Builder()
            .set("Content-Length", body.length() + "")
            .set("Content-Type", "application/json")
            .build();
        response.setResponseCode(200);
        response.setBody(body);
        response.setHeaders(headers);
      } catch (Exception e) {
        response.setResponseCode(500);
        response.setBody(e.getMessage());
      }
      return response;
    };
  }


  @RegisterForReflection
  protected record RequestData(
      String method,
      String path,
      Map<String, List<String>> headers,
      String body
  ) {

  }
}
