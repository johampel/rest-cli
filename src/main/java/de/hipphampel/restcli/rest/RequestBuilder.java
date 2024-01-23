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
import de.hipphampel.restcli.template.TemplateModel;
import de.hipphampel.restcli.template.TemplateRenderer;
import de.hipphampel.restcli.utils.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;

@ApplicationScoped
public class RequestBuilder {

  @Inject
  ObjectMapper objectMapper;

  @Inject
  DocumentBuilderFactory documentBuilderFactory;

  @Inject
  TemplateRenderer renderer;

  public Request buildRequest(RequestTemplate template, RequestContext context) {
    return new Request(
        objectMapper,
        documentBuilderFactory,
        template.method(),
        createRenderedURIString(template, context),
        createRenderedHeaders(template, context),
        template.requestBody(),
        template.timeout(),
        template.expectContinue());
  }

  String createRenderedURIString(RequestTemplate template, RequestContext context) {
    String uri = render(template.baseUri(), context.templateModel());
    String queryString = createRenderedQueryString(template, context);
    if (queryString.isEmpty()) {
      return uri;
    }

    if (uri.endsWith("?") || uri.endsWith("&")) {
      return uri + queryString;
    }

    if (uri.contains("?")) {
      return uri + "&" + queryString;
    }

    return uri + "?" + queryString;
  }

  String createRenderedQueryString(RequestTemplate template, RequestContext context) {
    return template.queryParameters().entrySet().stream()
        .filter(e -> isTextOrFilledVariableReference(context, e.getKey()) && isTextOrFilledVariableReference(context, e.getValue()))
        .map(p -> urlEncode(render(p.getKey(), context.templateModel())) + "=" + urlEncode(render(p.getValue(), context.templateModel())))
        .collect(Collectors.joining("&"));
  }

  Map<String, List<String>> createRenderedHeaders(RequestTemplate template, RequestContext context) {
    return template.headers().entrySet().stream()
        .filter(e -> isTextOrFilledVariableReference(context, e.getKey()))
        .map(e ->new Pair<>(
            render(e.getKey(), context.templateModel()),
            e.getValue().stream()
                .filter(v -> isTextOrFilledVariableReference(context, v))
                .map(v -> render(v, context.templateModel()))
                .toList()))
        .filter(e -> !e.second().isEmpty())
        .collect(Collectors.toMap(Pair::first, Pair::second));
  }

  String render(String template, TemplateModel model) {
    return renderer.render(template, model);
  }

  static boolean isTextOrFilledVariableReference(RequestContext context, String value) {
    if (!value.startsWith("${") || !value.endsWith("}")) {
      return true;
    }
    String variable = value.substring(2, value.length() - 1);
    if (context.templateModel().get(variable) != null) {
      return true;
    }
    return !context.declaredVariables().contains(variable);
  }

  static String urlEncode(String str) {
    return URLEncoder.encode(str, StandardCharsets.UTF_8);
  }
}
