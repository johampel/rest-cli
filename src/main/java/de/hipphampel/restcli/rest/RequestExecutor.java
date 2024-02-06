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

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;


public class RequestExecutor {

  private final RequestContext context;
  private final RequestBuilder requestBuilder;
  private final ResponseBuilder responseBuilder;

  public RequestExecutor(RequestContext context, RequestBuilder requestBuilder, ResponseBuilder responseBuilder) {
    this.context = Objects.requireNonNull(context);
    this.requestBuilder = Objects.requireNonNull(requestBuilder);
    this.responseBuilder = Objects.requireNonNull(responseBuilder);
  }

  public Response execute(Request request) throws IOException {
    try {
      HttpRequest httpRequest = request.toHttpRequest();
      HttpResponse<InputStream> response = context.client().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
      return responseBuilder.toResponse(response);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new IOException(ie);
    }
  }

  public <T> T execute(RequestTemplate requestTemplate, ResponseAction<T> responseAction) throws IOException {
    Request request = requestBuilder.buildRequest(requestTemplate, context);
    try (Response response = execute(request)) {
      return responseAction.handleResponse(context, request, response);
    }
  }

}
