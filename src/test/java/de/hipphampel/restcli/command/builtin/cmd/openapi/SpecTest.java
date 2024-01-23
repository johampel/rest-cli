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

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.command.builtin.cmd.openapi.Spec.OperationCoordinate;
import de.hipphampel.restcli.utils.Pair;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpecTest {

  final String SPEC1 = """
      openapi: 3.0.2
      info:
        title: Test
        version: 1.0
        description: test
      paths:
        /path1/{id}:
          get:
            operationId: getForPath1
            tags:
              - Tag1
            parameters:
              - name: id
                in: path
                required: true
                schema:
                  type: string
            responses:
              default:
                description: foo
                content:
                  application/json:
                    schema:
                      type: string
          post:
            operationId: postForPath1
            tags:
              - Tag1
              - Tag2
            parameters:
              - name: id
                in: path
                required: true
                schema:
                  type: string
            responses:
              default:
                description: foo
                content:
                  application/json:
                    schema:
                      type: string
        /path2:
          get:
            operationId: getForPath2
            responses:
              default:
                description: foo
                content:
                  application/json:
                    schema:
                      type: string
      """;

  final String SPEC2 = """
      openapi: 3.0.2
      info:
        title: Test
        version: 1.0
        description: test
      paths:
        /path1/{id}:
          parameters:
            - name: id
              in: path
              required: true
              schema:
                type: string
          get:
            operationId: getForPath1
            responses:
              default:
                description: foo
                content:
                  application/json:
                    schema:
                      type: string
          post:
            operationId: postForPath1
            parameters:
              - name: query
                in: query
                required: false
                schema:
                  type: string
            requestBody:
              content:
                application/json:
                  schema:
                    type: string
            responses:
              default:
                description: foo
                content:
                  application/json:
                    schema:
                      type: string
        /path2:
          get:
            operationId: getForPath2
            responses:
              default:
                description: foo
                content:
                  application/json:
                    schema:
                      type: string
                  application/xml:
                    schema:
                      type: string
          post:
            operationId: postForPath2
            parameters:
              - name: query
                in: query
                required: false
                schema:
                  type: string
            requestBody:
              content:
                application/json:
                  schema:
                    type: string
                application/xml:
                  schema:
                    type: string
            responses:
              default:
                description: foo
                content:
                  application/json:
                    schema:
                      type: string
      """;

  @Test
  void allOperationCoordinates() {
    Spec spec = readOpenAPISpec(SPEC1);
    assertThat(spec.allOperationCoordinates()).containsExactlyInAnyOrder(
        new OperationCoordinate("/path1/{id}", HttpMethod.GET),
        new OperationCoordinate("/path1/{id}", HttpMethod.POST),
        new OperationCoordinate("/path2", HttpMethod.GET)
    );
  }

  @Test
  void pathInfoOf_found() {
    Spec spec = readOpenAPISpec(SPEC1);
    assertThat(spec.pathItemOf(new OperationCoordinate("/path1/{id}", HttpMethod.GET)))
        .isPresent()
        .isEqualTo(spec.pathItemOf(new OperationCoordinate("/path1/{id}", HttpMethod.POST)))
        .isEqualTo(spec.pathItemOf(new OperationCoordinate("/path1/{id}", HttpMethod.DELETE)));
  }

  @Test
  void pathInfoOf_notFound() {
    Spec spec = readOpenAPISpec(SPEC1);
    assertThat(spec.pathItemOf(new OperationCoordinate("/not/found", HttpMethod.PUT))).isEmpty();
  }

  @Test
  void operationOf_found() {
    Spec spec = readOpenAPISpec(SPEC1);
    assertThat(spec.operationOf(new OperationCoordinate("/path1/{id}", HttpMethod.GET)))
        .isPresent();
    assertThat(spec.operationOf(new OperationCoordinate("/path1/{id}", HttpMethod.POST)))
        .isPresent();
  }

  @Test
  void operationOf_notFound() {
    Spec spec = readOpenAPISpec(SPEC1);
    assertThat(spec.operationOf(new OperationCoordinate("/path1/{id}", HttpMethod.DELETE)))
        .isEmpty();
    assertThat(spec.operationOf(new OperationCoordinate("/not/found", HttpMethod.POST)))
        .isEmpty();
  }

  @Test
  void parametersOf() {
    Spec spec = readOpenAPISpec(SPEC2);
    assertThat(spec.parametersOf(new OperationCoordinate("/path1/{id}", HttpMethod.GET)).stream()
        .map(p -> new Pair<>(p.getName(), p.getRequired())))
        .containsExactly(
            new Pair<>("id", true));
    assertThat(spec.parametersOf(new OperationCoordinate("/path1/{id}", HttpMethod.POST)).stream()
        .map(p -> new Pair<>(p.getName(), p.getRequired())))
        .containsExactly(
            new Pair<>("id", true),
            new Pair<>("query", false));
    assertThat(spec.parametersOf(new OperationCoordinate("/path2", HttpMethod.POST)).stream()
        .map(p -> new Pair<>(p.getName(), p.getRequired())))
        .containsExactly(
            new Pair<>("query", false));
    assertThat(spec.parametersOf(new OperationCoordinate("/path2", HttpMethod.GET)).stream()
        .map(p -> new Pair<>(p.getName(), p.getRequired())))
        .containsExactly();
    assertThat(spec.parametersOf(new OperationCoordinate("/path2", HttpMethod.DELETE)).stream()
        .map(p -> new Pair<>(p.getName(), p.getRequired())))
        .containsExactly();
  }

  @ParameterizedTest
  @CsvSource({
      "'/path2', POST,  'application/json'",
      "'/path2', GET,   'application/json;application/xml'"
  })
  void responseMediaTypesOf(String path, HttpMethod method, String mediaTypes) {
    Spec spec = readOpenAPISpec(SPEC2);
    assertThat(spec.responseMediaTypesOf(new OperationCoordinate(path, method))).isEqualTo(TestUtils.stringToList(mediaTypes));
  }

  @ParameterizedTest
  @CsvSource({
      "'/path1/{id}', GET,  ",
      "'/path1/{id}', POST, 'application/json'",
      "'/path2',      POST, 'application/json;application/xml'"
  })
  void requestBodyMediaTypesOf(String path, HttpMethod method, String mediaTypes) {
    Spec spec = readOpenAPISpec(SPEC2);
    assertThat(spec.requestBodyMediaTypesOf(new OperationCoordinate(path, method))).isEqualTo(TestUtils.stringToList(mediaTypes));
  }

  @Test
  void tagsOf() {
    Spec spec = readOpenAPISpec(SPEC1);
    assertThat(spec.tagsOf(new OperationCoordinate("/path1/{id}", HttpMethod.GET)))
        .containsExactly("Tag1");
    assertThat(spec.tagsOf(new OperationCoordinate("/path1/{id}", HttpMethod.POST)))
        .containsExactly("Tag1", "Tag2");
    assertThat(spec.tagsOf(new OperationCoordinate("/path2", HttpMethod.GET)))
        .containsExactly(" no tag ");
    assertThat(spec.tagsOf(new OperationCoordinate("/path2", HttpMethod.POST)))
        .containsExactly();
  }

  @Test
  void descriptionOfParameter_onParameter() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: Test
                version: 1.0
                description: test
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    tags:
                      - Tag1
                    parameters:
                      - name: id
                        in: path
                        required: true
                        description: My description
                        schema:
                          type: string
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
        """);

    Parameter parameter = spec.parametersOf(new OperationCoordinate("/path1/{id}", HttpMethod.GET)).get(0);
    assertThat(spec.descriptionOfParameter(parameter, "None"))
        .isEqualTo("My description");
  }

  @Test
  void descriptionOfOperation_notFound() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: Test
                version: 1.0
                description: test
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
        """);

    assertThat(spec.descriptionOfOperation(new OperationCoordinate("/path1/{id}", HttpMethod.GET), "None"))
        .isEqualTo("None");
  }

  @Test
  void descriptionOfOperation_found() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: Test
                version: 1.0
                description: test
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    description: Here am I
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
        """);

    assertThat(spec.descriptionOfOperation(new OperationCoordinate("/path1/{id}", HttpMethod.GET), "None"))
        .isEqualTo("Here am I");
  }

  @ParameterizedTest
  @CsvSource({
      "Tag1, 'Found'",
      "Tag2, 'Default'"
  })
  void descriptionOfTag_found(String tag, String expected) {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: Test
                version: 1.0
                description: test
              tags:
                - name: Tag1
                  description: Found
                - name: Tag2
              paths:
                  /foo:
                    get:
                      responses:
                        default:
                          description: foo
                          content:
                            application/json:
                              schema:
                                type: string
        """);

    assertThat(spec.descriptionOfTag(tag, "Default"))
        .isEqualTo(expected);
  }

  @Test
  void descriptionOfParameter_notFound() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: Test
                version: 1.0
                description: test
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    tags:
                      - Tag1
                    parameters:
                      - name: id
                        in: path
                        required: true
                        schema:
                          type: string
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
        """);

    Parameter parameter = spec.parametersOf(new OperationCoordinate("/path1/{id}", HttpMethod.GET)).get(0);
    assertThat(spec.descriptionOfParameter(parameter, "None"))
        .isEqualTo("None");
  }

  @Test
  void descriptionOfParameter_onSchema() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: Test
                version: 1.0
                description: test
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    tags:
                      - Tag1
                    parameters:
                      - name: id
                        in: path
                        required: true
                        schema:
                          $ref: "#/components/schemas/MyType"
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
              components:
                schemas:
                  MyType:
                    description: Schema description
                    type: string
        """);

    Parameter parameter = spec.parametersOf(new OperationCoordinate("/path1/{id}", HttpMethod.GET)).get(0);
    assertThat(spec.descriptionOfParameter(parameter, "None"))
        .isEqualTo("Schema description");
  }

  @Test
  void synopsisOfApi_found() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: The Synopsis
                version: 1.0
                description: test
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    description: Here am I
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
        """);

    assertThat(spec.synopsisOfApi("None"))
        .isEqualTo("The Synopsis");
  }

  @Test
  void synopsisOfApi_notFound() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title:
                version: 1.0
                description: test
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    description: Here am I
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
        """);

    assertThat(spec.synopsisOfApi("None"))
        .isEqualTo("None");
  }

  @Test
  void descriptionOfApi_found() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: The Synopsis
                version: 1.0
                description: The description
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    description: Here am I
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
        """);

    assertThat(spec.descriptionOfApi("None"))
        .isEqualTo("The description");
  }

  @Test
  void descriptionOfApi_notFound() {
    Spec spec = readOpenAPISpec("""
              openapi: 3.0.2
              info:
                title: The Synopsis
                version: 1.0
                description:
              paths:
                /path1/{id}:
                  get:
                    operationId: getForPath1
                    description: Here am I
                    responses:
                      default:
                        description: foo
                        content:
                          application/json:
                            schema:
                              type: string
        """);

    assertThat(spec.descriptionOfApi("None"))
        .isEqualTo("None");
  }

  static Spec readOpenAPISpec(String content) {
    ParseOptions resolve = new ParseOptions();
    resolve.setResolve(true);
    resolve.setFlatten(true);
    resolve.setResolveFully(true);
    SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, List.of(), resolve);
//    System.err.println(result.getMessages());
    return new Spec(result.getOpenAPI());
  }

}
