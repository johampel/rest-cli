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
package de.hipphampel.restcli.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProvider;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;

@RegisterForReflection
public class Request extends BodyAndHeaders<InputStreamProvider> {

  private final String method;
  private final String uri;
  public final Map<String, List<String>> headers;
  public final InputStreamProvider body;
  private final Duration timeout;
  private final boolean expectContinue;

  public Request(ObjectMapper objectMapper, DocumentBuilderFactory documentBuilderFactory, String method, String uri,
      Map<String, List<String>> headers, InputStreamProvider body, Duration timeout, boolean expectContinue) {
    super(objectMapper, documentBuilderFactory);
    this.method = method;
    this.uri = uri;
    this.headers = headers;
    this.body = body;
    this.timeout = timeout;
    this.expectContinue = expectContinue;
  }

  public HttpRequest toHttpRequest() {
    BodyPublisher publisher = getBody() == null ? BodyPublishers.noBody() : BodyPublishers.ofInputStream(getBody()::open);
    try {
      Builder builder = HttpRequest.newBuilder();
      if (timeout != null) {
        builder.timeout(timeout);
      }
      if (!getHeaders().isEmpty()) {
        builder.headers(getHeaders().entrySet().stream()
            .flatMap(entry -> entry.getValue().stream().flatMap(value -> Stream.of(entry.getKey(), value)))
            .toArray(String[]::new));
      }
      builder.expectContinue(expectContinue)
          .uri(new URI(uri))
          .method(method, publisher);

      return builder.build();
    } catch (URISyntaxException e) {
      throw new ExecutionException("Invalid URI \"%s\".".formatted(uri));
    }
  }

  public String getMethod() {
    return method;
  }

  public String getUri() {
    return uri;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public boolean isExpectContinue() {
    return expectContinue;
  }

  @Override
  public Map<String, List<String>> getHeaders() {
    return this.headers;
  }

  @Override
  public InputStreamProvider getBody() {
    return body;
  }
}
