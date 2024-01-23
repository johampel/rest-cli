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
package de.hipphampel.restcli.command.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.command.CommandInvoker;
import de.hipphampel.restcli.command.HttpCommandTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class HttpCommandTest extends HttpCommandTestBase {

  @Inject
  HttpCommand command;

  @Inject
  CommandInvoker invoker;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr) {
    assertThat(invoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<method>".
                usage: test-app http [-h|--header <header>=<value>]...
                                     [-v|--value <key>=<value>]...
                                     [-j|--json <key>=<json>]... <method>
                                     <uri> [<body>]
                """),
        // Basic get
        Arguments.of(
            List.of("get", "http://${baseUrl}/foo/bar"),
            true,
            """
                {
                  "method" : "GET",
                  "path" : "/foo/bar",
                  "headers" : { },
                  "body" : ""
                }
                """,
            ""),
        // Basic post
        Arguments.of(
            List.of("post", "http://${baseUrl}/foo/bar", "My body is ${foo}"),
            true,
            """
                {
                  "method" : "POST",
                  "path" : "/foo/bar",
                  "headers" : { },
                  "body" : "My body is ${foo}"
                }
                """,
            ""),
        // Post with replacement
        Arguments.of(
            List.of("-vfoo=small", "post", "http://${baseUrl}/foo/bar", "%My body is ${foo}"),
            true,
            """
                {
                  "method" : "POST",
                  "path" : "/foo/bar",
                  "headers" : { },
                  "body" : "My body is small"
                }
                """,
            ""),
        // with headers
        Arguments.of(
            List.of("-htest-abc=${foo}", "-vfoo=bar", "get", "http://${baseUrl}/foo/bar"),
            true,
            """
                {
                  "method" : "GET",
                  "path" : "/foo/bar",
                  "headers" : {
                    "test-abc" : [ "bar" ]
                  },
                  "body" : ""
                }
                """,
            ""),
        // with variables
        Arguments.of(
            List.of("-jbar=[1,2]", "-vfoo=bar", "post", "http://${baseUrl}/foo/bar", "%${foo} ${bar[1]}"),
            true,
            """
                {
                  "method" : "POST",
                  "path" : "/foo/bar",
                  "headers" : { },
                  "body" : "bar 2"
                }
                """,
            "")
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput(
        """
            http - Executes an ad hoc HTTP request.

            Usage
              http [-h|--header <header>=<value>]... [-v|--value
                   <key>=<value>]... [-j|--json <key>=<json>]...
                   <method> <uri> [<body>]

            Description
              `http` allows to execute arbitrary HTTP requests from
              scratch. The exact output format is specified via the
              global `--format` or `--template` option, so for example
              `test-app --template abc http ...` uses a different output
              template than the default one.

            Arguments and options
              <method>
                  The HTTP method to execute (so `get`, `post`, `put`,
                  `delete` and so on). The `<method>` parameter is
                  implicitly converted to an upper case string.
              <uri>
                  The complete request uri. The uri might contain
                  references to placeholders which might be provided by
                  the environment and/or in the command line. For
                  details please type `test-app help :templates`.
              <body>
                  This is optional and contains the request body,
                  specified as an input source, see section "Further
                  Infos" for more information about the syntax. This
                  might be omitted in case the request does not expect a
                  body.
              -h | --header <header>=<value>
                  Passes in addition to the headers inherited from the
                  current environment the specified header with the name
                  `<header>` and the specified `<value>`. Since it is
                  possible to specify more than one value for a specific
                  header, it is valid to use this option more than once
                  per `<header>`. If a header is already defined by the
                  environment, the headers passed via the command line
                  are prepended to the headers of the environment.
              -j | --json <key>=<json>
                  Defines the variable `<key>` having given the object
                  `<json>`; the `<json>` is interpreted as a JSON
                  string, so it might contain a complex data structure
                  like a map or list. If a same named value is already
                  defined in the current environment, the value from the
                  this option overwrites it for the request execution.
              -v|--value <key>=<value>
                  Defines the variable `<key>` having given the string
                  `<value>`. In case that you want to assign a more
                  complex structure to a variable, such as a list or
                  map, you should use the `--json` option instead. If a
                  same named value is already defined in the current
                  environment, the value from the this option overwrites
                  it for the request execution.

            Further infos
              Input source:
              An input source is basically a string literal, which
              represents either a string, refers to a file, an URL, or a
              builtin resource. Detailed information can be obtained by
              typing `test-app help :input-sources`.
            """,
        "");
  }
}
