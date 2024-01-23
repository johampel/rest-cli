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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.rest.RequestContext.OutputFormat;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import de.hipphampel.restcli.template.TemplateConfig.Parameter;
import de.hipphampel.restcli.template.TemplateModel;
import de.hipphampel.restcli.utils.Categorizer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.StringWriter;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
public class ResponseActionFactoryTest {

  @Inject
  ResponseActionFactory factory;

  private StringWriter out;
  private StringWriter err;
  private RequestContext context;
  private Response response;
  private Request request;

  @BeforeEach
  void beforeEach() {
    this.out = new StringWriter();
    this.err = new StringWriter();
    this.context = new RequestContext(
        mock(HttpClient.class),
        new TemplateModel(Map.of("key", "value")),
        Set.of("key"),
        new OutputFormat(
            InputStreamProvider.ofString("Hello ${_response.statusCode}"),
            Map.of("param", "xyz")),
        new Output(out),
        new Output(err));
    this.response = mock(Response.class);
    this.request = mock(Request.class);
  }

  @Test
  void returnStatusCode() {
    ResponseAction<Integer> action = factory.returnStatusCode();
    when(response.getStatusCode()).thenReturn(new Random().nextInt());

    assertThat(action.handleResponse(context, request, response)).isEqualTo(response.getStatusCode());
    assertThat(out).hasToString("");
    assertThat(err).hasToString("");
  }

  @ParameterizedTest
  @CsvSource({
      "199, false",
      "200, true",
      "201, true",
      "298, true",
      "299, true",
      "301, false",
  })
  void returnTrueIfSuccess(int statusCode, boolean expected) {
    ResponseAction<Boolean> action = factory.returnTrueIfSuccess();
    when(response.getStatusCode()).thenReturn(statusCode);

    assertThat(action.handleResponse(context, request, response)).isEqualTo(expected);
    assertThat(out).hasToString("");
    assertThat(err).hasToString("");
  }

  @Test
  void returnStringOf() {
    ResponseAction<String> action = factory.returnStringOf(
        InputStreamProvider.ofString("${_response.statusCode} Param: ${param} Env ${_env.key} ${_request.uri}"));
    when(response.getStatusCode()).thenReturn(4711);
    when(request.getUri()).thenReturn("http://example.com");

    assertThat(action.handleResponse(context, request, response)).isEqualTo("4711 Param: xyz Env value http://example.com");
    assertThat(out).hasToString("");
    assertThat(err).hasToString("");
  }

  @ParameterizedTest
  @CsvSource({
      "  0, 'default'",
      " 99, 'default'",
      "100, '100-199: 100'",
      "199, '100-199: 199'",
      "200, '200-299: 200'",
      "299, '200-299: 299'",
      "300, 'default'",
  })
  void switchByStatusCode(int statusCode, String expected) {
    ResponseAction<String> action = factory.switchByStatusCode(new Categorizer<Integer, ResponseAction<String>>()
        .defaultValue(factory.returnStringOf(InputStreamProvider.ofString("default")))
        .add(100, 200, factory.returnStringOf(InputStreamProvider.ofString("100-199: ${_response.statusCode}")))
        .add(200, 300, factory.returnStringOf(InputStreamProvider.ofString("200-299: ${_response.statusCode}"))));
    when(response.getStatusCode()).thenReturn(statusCode);

    assertThat(action.handleResponse(context, request, response)).isEqualTo(expected);
    assertThat(out).hasToString("");
    assertThat(err).hasToString("");
  }

  @ParameterizedTest
  @CsvSource({
      "199, 'fail: 199'",
      "200, 'ok: 200'",
      "299, 'ok: 299'",
      "300, 'fail: 300'",
  })
  void switchBySuccess(int statusCode, String expected) {
    ResponseAction<String> action = factory.switchBySuccess(
        factory.returnStringOf(InputStreamProvider.ofString("ok: ${_response.statusCode}")),
        factory.returnStringOf(InputStreamProvider.ofString("fail: ${_response.statusCode}")));
    when(response.getStatusCode()).thenReturn(statusCode);

    assertThat(action.handleResponse(context, request, response)).isEqualTo(expected);
    assertThat(out).hasToString("");
    assertThat(err).hasToString("");
  }

  @Test
  void print() {
    Template template = new Template(
        TemplateAddress.fromString("ignoreMe@"),
        new TemplateConfig(
            "ignore me",
            Map.of("param", new Parameter("the params default value")),
            "Hello ${_response.statusCode}"
        ),
        false);

    ResponseAction<Void> action = factory.print(context.out());
    when(response.getStatusCode()).thenReturn(4711);

    assertThat(action.handleResponse(context, request, response)).isNull();
    assertThat(out).hasToString("Hello 4711");
    assertThat(err).hasToString("");
  }

  @Test
  void doAndReturn() {
    ResponseAction<Boolean> action = factory.doAndReturn(
        factory.returnTrueIfSuccess(),
        factory.print(context.out()),
        factory.print(context.err()));
    when(response.getStatusCode()).thenReturn(200);

    assertThat(action.handleResponse(context, request, response)).isTrue();
    assertThat(out).hasToString("Hello 200");
    assertThat(err).hasToString("Hello 200");
  }
}
