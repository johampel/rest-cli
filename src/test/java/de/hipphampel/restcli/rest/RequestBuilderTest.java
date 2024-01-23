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

import static de.hipphampel.restcli.TestUtils.assertInputStreamProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.rest.RequestContext.OutputFormat;
import de.hipphampel.restcli.template.TemplateModel;
import de.hipphampel.restcli.template.TemplateRenderer;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class RequestBuilderTest {

  @Inject
  TemplateRenderer templateRenderer;
  @Inject
  RequestBuilder builder;
  RequestContext context;
  Map<String, Object> variables;
  Set<String> declaredVariables;
  Function<String, Object> undefinedVariableCallback;

  @BeforeEach
  void beforeEach() {
    declaredVariables = new HashSet<>();
    variables = new HashMap<>();
    undefinedVariableCallback = variable -> null;
    context = new RequestContext(
        mock(HttpClient.class),
        new TemplateModel(variables, null, name -> undefinedVariableCallback.apply(name)),
        declaredVariables,
        new OutputFormat(InputStreamProvider.ofString(""), Map.of()),
        mock(Output.class),
        mock(Output.class)
    );
  }

  @Test
  void build() {
    variables.putAll(Map.of(
        "abc", "def",
        "ghi", "jkl"
    ));
    RequestTemplate template = new RequestTemplate()
        .requestBody(InputStreamProvider.interpolated(InputStreamProvider.ofString("123${abc}"), templateRenderer, context.templateModel()))
        .method("GET")
        .baseUri("http://foo/${ghi}")
        .headers(Map.of("Header1", List.of("${abc}")))
        .queryParameters(Map.of("param1", "${ghi}"));

    Request request = builder.buildRequest(template, context);
    assertThat(request).isNotNull();
    assertThat(request.getUri()).isEqualTo("http://foo/jkl?param1=jkl");
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getHeaders()).isEqualTo(Map.of(
        "Header1", List.of("def")
    ));
    assertInputStreamProvider(request.getBody(), "123def");
  }

  @ParameterizedTest
  @CsvSource({
      "'http://base',             false, 'http://base'",
      "'http://${abc}',           true,  'http://def?def=value1&jkl=value2'",
      "'http://${abc}?',          false, 'http://def?'",
      "'http://base?',            true,  'http://base?def=value1&jkl=value2'",
      "'http://base?foo=bar',     false, 'http://base?foo=bar'",
      "'http://base?foo=bar',     true,  'http://base?foo=bar&def=value1&jkl=value2'",
      "'http://base?${abc}=bar&', false, 'http://base?def=bar&'",
      "'http://base?${abc}=bar&', true,  'http://base?def=bar&def=value1&jkl=value2'",
  })
  void createRenderedURIString(String baseUri, boolean hasQuery, String expected) {
    RequestTemplate template = new RequestTemplate()
        .baseUri(baseUri)
        .queryParameters(hasQuery ? new TreeMap<>(Map.of("${abc}", "value1", "${ghi}", "value2")) : Map.of());
    variables.putAll(Map.of(
        "abc", "def",
        "ghi", "jkl"
    ));
    assertThat(builder.createRenderedURIString(template, context)).isEqualTo(expected);
  }

  @Test
  void createRenderedQueryString_noQuery() {
    initContextVariables("abc=def;ghi=jkl", "abc;def;ghi");
    RequestTemplate template = new RequestTemplate();
    assertThat(builder.createRenderedQueryString(template, context)).isEqualTo("");
  }

  @Test
  void createRenderedQueryString_withQuery() {
    initContextVariables("abc=def;ghi=jkl", "abc;def;ghi");
    RequestTemplate template = new RequestTemplate()
        .queryParameters(new TreeMap<>(Map.of(
            "?=", "?&",
            "${abc}", "?=",
            "?&", "${ghi}",
            "foo", "${def}",
            "${def}", "bar"
        )));
    assertThat(builder.createRenderedQueryString(template, context))
        .isEqualTo("def=%3F%3D&%3F%26=jkl&%3F%3D=%3F%26");
  }

  @Test
  void createRenderedHeaders() {
    RequestTemplate template = new RequestTemplate().headers(Map.of(
        "key1", List.of("value1"),
        "${abc}", List.of("value2", "${def}", "${ghi}"),
        "key2", List.of("${def}"),
        "key3", List.of("${ghi}")));
    initContextVariables("abc=def;ghi=jkl", "abc;def;ghi");
    assertThat(builder.createRenderedHeaders(template, context)).isEqualTo(Map.of(
        "key1", List.of("value1"),
        "def", List.of("value2", "jkl"),
        "key3", List.of("jkl")
    ));
  }

  @Test
  void render_filledVariables() {
    initContextVariables("abc=def;ghi=jkl", "abc;ghi");
    assertThat(builder.render("10 ${abc}", context.templateModel())).isEqualTo("10 def");
  }

  @Test
  void render_fail() {
    assertThatThrownBy(() -> builder.render("10 ${foo}", context.templateModel()))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Failed to render template.");
  }

  @ParameterizedTest
  @CsvSource({
      "${abc},    true",
      "${def},    false",
      "xyz${abc}, true"
  })
  void isTextOrFilledVariableReference(String value, boolean expected) {
    initContextVariables("abc=def;ghi=jkl", "abc;def;ghi");

  }

  @Test
  void urlEncode() {
    assertThat(RequestBuilder.urlEncode("abc?=")).isEqualTo("abc%3F%3D");
  }

  void initContextVariables(String variables, String declaredVariables) {
    this.declaredVariables.addAll(TestUtils.stringToList(declaredVariables));
    this.variables.putAll(TestUtils.stringToList(variables).stream()
        .map(Pair::fromString)
        .collect(Collectors.toMap(Pair::first, Pair::second)));
  }
}
