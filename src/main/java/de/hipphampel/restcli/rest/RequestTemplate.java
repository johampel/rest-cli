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

import de.hipphampel.restcli.io.InputStreamProvider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RequestTemplate {

  private boolean expectContinue = false;
  private String method;
  private Duration timeout;
  private String baseUri;
  private Map<String, String> queryParameters = Map.of();
  private Map<String, List<String>> headers = Map.of();
  private InputStreamProvider requestBody;
  public boolean expectContinue() {
    return this.expectContinue;
  }

  public RequestTemplate expectContinue(boolean expectContinue) {
    this.expectContinue = expectContinue;
    return this;
  }

  public String method() {
    return this.method;
  }

  public RequestTemplate method(String method) {
    this.method = Objects.requireNonNull(method);
    return this;
  }

  public Duration timeout() {
    return this.timeout;
  }

  public RequestTemplate timeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public String baseUri() {
    return this.baseUri;
  }

  public RequestTemplate baseUri(String baseUri) {
    this.baseUri = Objects.requireNonNull(baseUri);
    return this;
  }

  public Map<String, String> queryParameters() {
    return this.queryParameters;
  }

  public RequestTemplate queryParameters(Map<String, String> queryParameters) {
    this.queryParameters = Objects.requireNonNull(queryParameters);
    return this;
  }

  public Map<String, List<String>> headers() {
    return this.headers;
  }

  public RequestTemplate headers(Map<String, List<String>> headers) {
    this.headers = Objects.requireNonNull(headers);
    return this;
  }

  public boolean hasRequestBody() {
    return this.requestBody != null;
  }

  public InputStreamProvider requestBody() {
    return this.requestBody;
  }

  public RequestTemplate requestBody(InputStreamProvider requestBody) {
    this.requestBody = requestBody;
    return this;
  }
}
