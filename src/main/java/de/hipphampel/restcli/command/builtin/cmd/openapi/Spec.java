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
package de.hipphampel.restcli.command.builtin.cmd.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class Spec {

  public static final String NO_TAG = " no tag ";

  private static final Map<HttpMethod, Function<PathItem, Operation>> OPERATIONS = Map.of(
      HttpMethod.DELETE, PathItem::getDelete,
      HttpMethod.GET, PathItem::getGet,
      HttpMethod.HEAD, PathItem::getHead,
      HttpMethod.OPTIONS, PathItem::getOptions,
      HttpMethod.PATCH, PathItem::getPatch,
      HttpMethod.POST, PathItem::getPost,
      HttpMethod.PUT, PathItem::getPut,
      HttpMethod.TRACE, PathItem::getTrace);

  private final OpenAPI spec;

  public Spec(OpenAPI spec) {
    this.spec = Objects.requireNonNull(spec);
  }

  public List<OperationCoordinate> allOperationCoordinates() {
    return Optional.ofNullable(spec.getPaths())
        .map(paths -> paths.keySet().stream()
            .flatMap(path -> OPERATIONS.keySet().stream().map(method -> new OperationCoordinate(path, method)))
            .filter(coordinate -> operationOf(coordinate).isPresent()))
        .orElse(Stream.empty())
        .toList();
  }

  public Optional<PathItem> pathItemOf(OperationCoordinate coordinate) {
    return Optional.ofNullable(spec.getPaths())
        .flatMap(paths -> Optional.ofNullable(paths.get(coordinate.path)));
  }

  public Optional<Operation> operationOf(OperationCoordinate coordinate) {
    return pathItemOf(coordinate)
        .flatMap(pathInfo -> Optional.ofNullable(OPERATIONS.get(coordinate.method).apply(pathInfo)));
  }

  public List<String> responseMediaTypesOf(OperationCoordinate coordinate) {
    return operationOf(coordinate).stream()
        .map(Operation::getResponses)
        .map(LinkedHashMap::values)
        .flatMap(Collection::stream)
        .map(ApiResponse::getContent)
        .filter(Objects::nonNull)
        .map(Content::keySet)
        .flatMap(Collection::stream)
        .distinct()
        .toList();
  }

  public List<String> requestBodyMediaTypesOf(OperationCoordinate coordinate) {
    return operationOf(coordinate)
        .map(Operation::getRequestBody)
        .map(RequestBody::getContent)
        .filter(content -> !content.isEmpty())
        .map(content -> (List<String>) new ArrayList<>(content.keySet()))
        .orElse(List.of());
  }

  public List<Parameter> parametersOf(OperationCoordinate coordinate) {
    return Stream.concat(
            pathItemOf(coordinate).stream()
                .flatMap(pi -> pi.getParameters() == null ? Stream.empty() : pi.getParameters().stream()),
            operationOf(coordinate).stream()
                .flatMap(op -> op.getParameters() == null ? Stream.empty() : op.getParameters().stream()))
        .toList();
  }

  public Set<String> tagsOf(OperationCoordinate coordinate) {
    return operationOf(coordinate)
        .map(op -> op.getTags() == null || op.getTags().isEmpty() ? Set.of(NO_TAG) : new HashSet<>(op.getTags()))
        .orElse(Set.of());
  }

  public String descriptionOfApi(String defaultDescription) {
    return Optional.ofNullable(spec.getInfo())
        .map(Info::getDescription)
        .filter(d -> !d.isBlank())
        .orElse(defaultDescription);
  }

  public String synopsisOfApi(String defaultSynopsis) {
    return Optional.ofNullable(spec.getInfo())
        .map(Info::getTitle)
        .filter(d -> !d.isBlank())
        .orElse(defaultSynopsis);
  }

  public String descriptionOfTag(String tag, String defaultDescription) {
    return Optional.ofNullable(spec.getTags()).stream()
        .flatMap(Collection::stream)
        .filter(t -> Objects.equals(tag, t.getName()))
        .map(Tag::getDescription)
        .filter(Objects::nonNull)
        .filter(d -> !d.isBlank())
        .findFirst()
        .orElse(defaultDescription);
  }

  public String descriptionOfOperation(OperationCoordinate coordinate, String defaultDescription) {
    return operationOf(coordinate)
        .map(Operation::getDescription)
        .filter(d -> !d.isBlank())
        .orElse(defaultDescription);
  }

  public String descriptionOfParameter(Parameter parameter, String defaultDescription) {
    if (parameter.getDescription() != null && !parameter.getDescription().isBlank()) {
      return parameter.getDescription();
    }
    if (parameter.getSchema() != null && parameter.getSchema().getDescription() != null && !parameter.getSchema().getDescription()
        .isBlank()) {
      return parameter.getSchema().getDescription();
    }

    return defaultDescription;
  }

  public record OperationCoordinate(String path, HttpMethod method) {

  }
}
