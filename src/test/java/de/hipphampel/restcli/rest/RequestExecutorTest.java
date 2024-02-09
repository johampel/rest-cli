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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.rest.RequestContext.OutputFormat;
import de.hipphampel.restcli.template.TemplateModel;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class RequestExecutorTest {

  @Inject
  RequestExecutorFactory executorFactory;

  RequestExecutor executor;

  @Inject
  RequestBuilder requestBuilder;

  @Inject
  ResponseActionFactory responseActionFactory;

  HttpClient client;
  Map<String, Object> variables;

  @BeforeEach
  void beforeEach() {
    this.client = mock(HttpClient.class);
    this.variables = new HashMap<>();
    this.executor = executorFactory.newExecutor(new RequestContext(
        client,
        new TemplateModel(variables),
        new OutputFormat(
            InputStreamProvider.ofString(""),
            Map.of()),
        mock(Output.class),
        mock(Output.class),
        false
    ));
  }

  @Test
  void execute_Request() throws IOException, InterruptedException {
    RequestTemplate template = new RequestTemplate()
        .method("GET")
        .baseUri("http://www.example.com");
    ArgumentCaptor<HttpRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    HttpResponse<Object> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(234);
    when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of("a", List.of("b", "c")), (a, b) -> true));
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8)));
    when(client.send(httpRequestCaptor.capture(), any())).thenReturn(httpResponse);
    RequestContext context = new RequestContext(
        mock(HttpClient.class),
        new TemplateModel(Map.of()),
        new OutputFormat(InputStreamProvider.ofString(""), Map.of()),
        mock(Output.class),
        mock(Output.class),
        false
    );
    Request request = requestBuilder.buildRequest(template, context);

    try (Response response = executor.execute(request)) {
      // Assert request
      assertThat(httpRequestCaptor.getAllValues()).hasSize(1);
      HttpRequest httpRequest = httpRequestCaptor.getValue();
      assertThat(httpRequest.method()).isEqualTo("GET");
      assertThat(httpRequest.uri()).hasToString("http://www.example.com");
      // assert Response
      assertThat(response.getStatusCode()).isEqualTo(234);
      assertThat(response.getHeaders()).isEqualTo(Map.of("a", List.of("b", "c")));
      assertThat(response.getStringBody()).isEqualTo("Hello");
    }
  }

  @Test
  void execute_RequestTemplate() throws IOException, InterruptedException {
    RequestTemplate template = new RequestTemplate()
        .method("GET")
        .baseUri("http://www.example.com");
    ArgumentCaptor<HttpRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    HttpResponse<Object> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(234);
    when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of("a", List.of("b", "c")), (a, b) -> true));
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8)));
    when(client.send(httpRequestCaptor.capture(), any())).thenReturn(httpResponse);
    ResponseAction<Integer> action = responseActionFactory.returnStatusCode();

    assertThat(executor.execute(template, action)).isEqualTo(234);
    assertThat(httpRequestCaptor.getAllValues()).hasSize(1);
    HttpRequest httpRequest = httpRequestCaptor.getValue();
    assertThat(httpRequest.method()).isEqualTo("GET");
    assertThat(httpRequest.uri()).hasToString("http://www.example.com");
  }
}
