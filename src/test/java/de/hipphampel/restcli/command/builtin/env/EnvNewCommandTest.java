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
package de.hipphampel.restcli.command.builtin.env;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentConfig;
import de.hipphampel.restcli.env.EnvironmentRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class EnvNewCommandTest extends CommandTestBase {

  @Inject
  EnvNewCommand command;
  @Inject
  EnvironmentRepository environmentRepository;
  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    Environment parent = environmentRepository.createTransientEnvironment("parent", null);
    parent.setLocalVariables(Map.of(
        "abc", "parent_1",
        "def", "parent_2"
    ));
    parent.setLocalHeaders(Map.of(
        "ghi", List.of("parent_3", "parent_4"),
        "jkl", List.of("parent_5", "parent_6")
    ));
    environmentRepository.storeEnvironment(context.configPath(), parent, false);
    Environment child = environmentRepository.createTransientEnvironment("child", parent);
    child.setLocalVariables(Map.of(
        "def", "child_1",
        "ghi", "child_2"
    ));
    child.setLocalHeaders(Map.of(
        "jkl", List.of("child_3", "child_4"),
        "mno", List.of("child_5", "child_6")
    ));
    environmentRepository.storeEnvironment(context.configPath(), child, false);
  }


  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedEnvName,
      String expectedEnv)
      throws JsonProcessingException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedEnv != null && expectedEnvName != null) {
      assertThat(environmentRepository.getConfig(context.configPath(), expectedEnvName))
          .contains(objectMapper.readValue(expectedEnv, EnvironmentConfig.class));
    }
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<name>".
                usage: test-app env new [-r|--replace] [--request-timeout
                                        <timeout>] [-v|--value
                                        <key>=<value>]... [-j|--json
                                        <key>=<json>]... [-h|--header
                                        <header>=<value>]... [(-p|--parent
                                        <parent>)] <name>
                """,
            null,
            null),
        // Environment already exists (failed)
        Arguments.of(
            List.of("parent"),
            false,
            "",
            """
                *** error test-app: Environment "parent" already exists -
                                    use option `--replace` to replace it.
                """,
            null,
            null),
        // Environment already exists (replace mode)
        Arguments.of(
            List.of("--replace", "-vnew=env", "parent"),
            true,
            "",
            "",
            "parent",
            """
                {
                  "parent": null,
                  "variables": {
                    "new": "env"
                  },
                  "headers": {}
                }"""),
        // Parent not found
        Arguments.of(
            List.of("--parent", "not-found", "env"),
            false,
            "",
            """
                *** error test-app: Environment "not-found" does not exist.
                """,
            null,
            null),
        // Ok from scratch
        Arguments.of(
            List.of("--parent", "parent", "-j", "foo=[1,2]", "-vbar=baz", "-hx=y", "--request-timeout", "4711", "env"),
            true,
            "",
            "",
            "env",
            """
                {
                  "parent": "parent",
                  "variables": {
                    "bar": "baz",
                    "foo": [1, 2]
                  },
                  "headers": {
                    "x": ["y"]
                  },
                  "requestTimeout": 4711
                }"""),
        // JSON Error
        Arguments.of(
            List.of("-j", "foo=[1,2", "env"),
            false,
            "",
            """
                *** error test-app: Failed to convert value "[1,2" to a JSON
                                    object.
                """,
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            new - Creates a new environment.

            Usage
              new [-r|--replace] [--request-timeout <timeout>]
                  [-v|--value <key>=<value>]... [-j|--json
                  <key>=<json>]... [-h|--header <header>=<value>]...
                  [(-p|--parent <parent>)] <name>

            Description
              Creates a new environment.
              Upon creation, you may define its parent and the settings
              it should know.
              An alternative to create an environment is to use the
              `imp` command that imports an environment definition or
              the `cp` to create a copy from an existing one.
              Modifications on the environments can be done via the `mv`
              and `mod` commands.

            Arguments and options
              <name>
                  The name of the environment to create.
              -h | --header <header>=<value>
                  Defines a new header `<header>` with the header value
                  `<value>`. In opposite to variables, it is possible to
                  define more than one string value for a header by
                  using the `--header` option for the same `<header>`
                  multiple times.
              -j | --json <key>=<json>
                  Defines the variable `<key>` having given the object
                  `<json>`. `<json>` is interpreted as a JSON string, so
                  it might contain a complex data structure like a map
                  or list.
              -p | --parent <parent>
                  If specified, the created environment has the
                  specified parent. An environment having a parent
                  implicitly inherits the variables and settings of the
                  parent, but it is allowed to overwrite them. The
                  inheritance is dynamic, meaning if you change a
                  setting in the parent, the change is also visible in
                  this newly created environment.
              -r | --replace
                  This has only an effect, if the environment already
                  exists. If set, then it allows to replace the already
                  existing environment, of omitted, it is not allowed to
                  overwrite already existing environments.
              --request-timeout <timeout>
                  Specifies the request timout of the environment.
                  `timeout` is measured in milliseconds. If this
                  environment does not define a timeout, the timeout is
                  inherited from the parent environment, or - if there
                  is no parent - from the application configuration.
              -v | --value <key>=<value>
                  Defines the variable `<key>` having given the string
                  `<value>`. In case that you want to assign a more
                  complex structure to a variable, such as a list or
                  map, you should use the `--json` option instead.
            """,
        "");
  }

}