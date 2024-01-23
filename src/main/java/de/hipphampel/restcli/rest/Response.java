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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.io.ClosableInputStreamProvider;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;


@RegisterForReflection
public class Response extends BodyAndHeaders<ClosableInputStreamProvider> implements AutoCloseable {

  private final HttpResponse<InputStream> httpResponse;
  private final int maxResponseBodyBytesKeptInMemory;
  private ClosableInputStreamProvider body;

  public Response(DocumentBuilderFactory documentBuilderFactory, ObjectMapper objectMapper, HttpResponse<InputStream> httpResponse,
      int maxResponseBodyBytesKeptInMemory) {
    super(objectMapper, documentBuilderFactory);
    this.httpResponse = httpResponse;
    this.maxResponseBodyBytesKeptInMemory = maxResponseBodyBytesKeptInMemory;
    this.body = null;
  }

  @Override
  public void close() throws IOException {
    if (body != null) {
      body.close();
    }
  }

  public int getStatusCode() {
    return httpResponse.statusCode();
  }

  @Override
  public Map<String, List<String>> getHeaders() {
    return httpResponse.headers().map();
  }

  @JsonIgnore
  @Override
  public ClosableInputStreamProvider getBody() {
    if (body != null) {
      return body;
    }
    body = ClosableInputStreamProvider.ofStream(httpResponse.body(), maxResponseBodyBytesKeptInMemory);
    return body;
  }

  @Override
  public String toString() {
    return "Response{" +
        "httpResponse=" + httpResponse +
        ", maxResponseBodyBytesKeptInMemory=" + maxResponseBodyBytesKeptInMemory +
        '}';
  }
}
