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

import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.template.TemplateModel;
import de.hipphampel.restcli.template.TemplateRenderer;
import de.hipphampel.restcli.utils.Categorizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ResponseActionFactory {

  @Inject
  TemplateRenderer templateRenderer;

  public <T> ResponseAction<T> doAndReturn(ResponseAction<T> returnAction, List<ResponseAction<?>> actions) {
    return (context, request, response) -> {
      actions.forEach(action -> action.handleResponse(context, request, response));
      return returnAction.handleResponse(context, request, response);
    };
  }

  public <T> ResponseAction<T> doAndReturn(ResponseAction<T> returnAction, ResponseAction<?>... actions) {
    return doAndReturn(returnAction, List.of(actions));
  }

  public ResponseAction<Boolean> returnTrueIfSuccess() {
    return ((context, request, response) -> response.getStatusCode() >= 200 && response.getStatusCode() < 300);
  }

  public ResponseAction<Integer> returnStatusCode() {
    return (context, request, response) -> response.getStatusCode();
  }

  public ResponseAction<String> returnStringOf(InputStreamProvider templateStreamProvider) {
    return (context, request, response) ->
        templateRenderer.render(templateStreamProvider, createTemplateModel(context, request, response));
  }

  public <T> ResponseAction<T> switchByStatusCode(Categorizer<Integer, ResponseAction<T>> switches) {
    return (context, request, response) -> switches.get(response.getStatusCode())
        .map(action -> action.handleResponse(context, request, response))
        .orElse(null);
  }

  public <T> ResponseAction<T> switchBySuccess(ResponseAction<T> success, ResponseAction<T> error) {
    return switchByStatusCode(new Categorizer<Integer, ResponseAction<T>>()
        .defaultValue(error)
        .add(200, 300, success));
  }

  public ResponseAction<Void> print(Output output) {
    return (context, request, response) -> {
      templateRenderer.render(context.format().format(), createTemplateModel(context, request, response), output.asWriter());
      return null;
    };
  }

  private TemplateModel createTemplateModel(RequestContext context, Request request, Response response) {
    Map<String, Object> model = new HashMap<>(context.format().parameters());
    model.put("_request", request);
    model.put("_response", response);
    model.put("_env", context.templateModel());
    return new TemplateModel(model, context.templateModel().get_());
  }
}
