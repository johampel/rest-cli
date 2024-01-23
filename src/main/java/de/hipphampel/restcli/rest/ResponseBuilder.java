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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.net.http.HttpResponse;
import javax.xml.parsers.DocumentBuilderFactory;

@ApplicationScoped
public class ResponseBuilder {

  @Inject
  ObjectMapper objectMapper;

  @Inject
  DocumentBuilderFactory documentBuilderFactory;

  private int maxResponseBodyBytesKeptInMemory = 1_000_000;

  public int getMaxResponseBodyBytesKeptInMemory() {
    return maxResponseBodyBytesKeptInMemory;
  }

  public void setMaxResponseBodyBytesKeptInMemory(int maxResponseBodyBytesKeptInMemory) {
    this.maxResponseBodyBytesKeptInMemory = maxResponseBodyBytesKeptInMemory;
  }

  public Response toResponse(HttpResponse<InputStream> httpResponse) {
    return new Response(documentBuilderFactory, objectMapper, httpResponse, maxResponseBodyBytesKeptInMemory);
  }
}
