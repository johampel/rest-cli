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
package de.hipphampel.restcli.command.config;

import de.hipphampel.restcli.io.InputStreamProviderConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RegisterForReflection
public class RestCommandConfig {

  private String method;
  private String baseUri;
  private Map<String, String> queryParameters = new HashMap<>();
  private Map<String, List<String>> headers = new HashMap<>();
  private ParameterListConfig parameters = new ParameterListConfig();
  private BodyConfig body;

  public String getMethod() {
    return method;
  }

  public RestCommandConfig setMethod(String method) {
    this.method = method;
    return this;
  }

  public String getBaseUri() {
    return baseUri;
  }

  public RestCommandConfig setBaseUri(String baseUri) {
    this.baseUri = baseUri;
    return this;
  }

  public Map<String, String> getQueryParameters() {
    return queryParameters;
  }

  public RestCommandConfig setQueryParameters(Map<String, String> queryParameters) {
    this.queryParameters = Objects.requireNonNull(queryParameters);
    return this;
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  public RestCommandConfig setHeaders(Map<String, List<String>> headers) {
    this.headers = Objects.requireNonNull(headers);
    return this;
  }

  public ParameterListConfig getParameters() {
    return parameters;
  }

  public RestCommandConfig setParameters(ParameterListConfig parameters) {
    this.parameters = Objects.requireNonNull(parameters);
    return this;
  }

  public BodyConfig getBody() {
    return body;
  }

  public RestCommandConfig setBody(BodyConfig body) {
    this.body = body;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RestCommandConfig that = (RestCommandConfig) o;
    return Objects.equals(method, that.method) && Objects.equals(baseUri, that.baseUri) && Objects.equals(queryParameters,
        that.queryParameters) && Objects.equals(headers, that.headers) && Objects.equals(parameters, that.parameters) && Objects.equals(
        body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, baseUri, queryParameters, headers, parameters, body);
  }

  @Override
  public String toString() {
    return "RestCommandConfig{" +
        "method='" + method + '\'' +
        ", baseUri='" + baseUri + '\'' +
        ", queryParameters=" + queryParameters +
        ", headers=" + headers +
        ", parameters=" + parameters +
        ", body=" + body +
        '}';
  }
}
