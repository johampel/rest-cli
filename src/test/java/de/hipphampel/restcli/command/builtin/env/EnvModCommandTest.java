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
class EnvModCommandTest extends CommandTestBase {

  @Inject
  EnvModCommand command;
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
    parent.setRequestTimeout(1000L);
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
    child.setRequestTimeout(2000L);
    environmentRepository.storeEnvironment(context.configPath(), child, false);
    Environment grandchild = environmentRepository.createTransientEnvironment("grandchild", child);
    grandchild.setLocalVariables(Map.of(
        "ghi", "grandchild_1",
        "jkl", "grandchild_2"
    ));
    grandchild.setLocalHeaders(Map.of(
        "mno", List.of("grandchild_3", "grandchild_4"),
        "pqr", List.of("grandchild_5", "grandchild_6")
    ));
    grandchild.setRequestTimeout(3000L);
    environmentRepository.storeEnvironment(context.configPath(), grandchild, false);
  }


  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedEnvName,
      String expectedEnv) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedEnvName != null) {
      assertThat(environmentRepository.getEnvironment(context.configPath(), expectedEnvName).orElseThrow().getLocalConfig())
          .isEqualTo(objectMapper.readValue(expectedEnv, EnvironmentConfig.class));
    }
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // No args
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<name>".
                usage: test-app env mod [-v|--value <key>=<value>]...
                                        [-V|--remove-variable <variable>]...
                                        [-j|--json <key>=<json>]...
                                        [-h|--header <header>=<value>]...
                                        [-H|--remove-header <key>]...
                                        [(--request-timeout <timeout>) |
                                        --no-request-timeout] [(-p|--parent
                                        <parent>) | -n|--no-parent] <name>
                """,
            null,
            null),
        // Not found
        Arguments.of(
            List.of("unknown"),
            false,
            "",
            """
                *** error test-app: Environment "unknown" does not exist.
                """,
            null,
            null),
        // Reset request timeout
        Arguments.of(
            List.of("--no-request-timeout", "child"),
            true,
            "",
            "",
            "child",
            """
                {
                  "parent" : "parent",
                  "variables" : {
                    "def" : "child_1",
                    "ghi" : "child_2"
                  },
                  "headers" : {
                    "jkl" : [ "child_3", "child_4" ],
                    "mno" : [ "child_5", "child_6" ]
                  }
                }
                """),
        // Some changes
        Arguments.of(
            List.of("--request-timeout", "4711", "-Vdef", "-vnew=value", "-hx=y", "-Hmno", "child"),
            true,
            "",
            "",
            "child",
            """
                {
                  "parent" : "parent",
                  "variables" : {
                    "ghi" : "child_2",
                    "new" : "value"
                  },
                  "headers" : {
                    "jkl" : [ "child_3", "child_4" ],
                    "x": [ "y" ]
                  },
                  "requestTimeout" : 4711
                }
                """)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            mod - Modifies an existing environment.

            Usage
              mod [-v|--value <key>=<value>]... [-V|--remove-variable
                  <variable>]... [-j|--json <key>=<json>]...
                  [-h|--header <header>=<value>]... [-H|--remove-header
                  <key>]... [(--request-timeout <timeout>) |
                  --no-request-timeout] [(-p|--parent <parent>) |
                  -n|--no-parent] <name>

            Description
              Allows to modify the settings of an existing environment.
              Note that in order to change the name of the environment,
              you should use the `mv` command instead.

            Arguments and options
              <name>
                  Name environment to be changed
              -h | --header <header>=<value>
                  Defines a new header `<header>` with the header value
                  `<value>`. Note that header have a list of values. So
                  it is legal to specific this option more than once
                  with the same `<header>` value. Also, if the
                  environment already has values for the given header,
                  the new values are prepended to the list of values. In
                  order to remove a header, use the `--remove-header`
                  option. This option can be used more than once.
              -H | --remove-header <key>
                  Removes the header `<key>` from the environment. This
                  option can be used more than once.
              -j | --json <key>=<json>
                  Sets the variable `<key>` having given the object
                  `<json>`. The `<json>` is interpreted as a JSON
                  string, so it might contain a complex data structure
                  like a map or list. Any previous value is overwritten.
                  In order to remove a variable, use the
                  `--remove-variable` option. This option can be used
                  more than once.
              --request-timeout <timeout> | --no-request-timeout
                  `--request-timeout` sets the request timout of the
                  environment. `timeout` is measured in milliseconds. If
                  this environment does not define a timeout, the
                  timeout is inherited from the parent environment, or -
                  if there is no parent - from the application
                  configuration. In order to un-define the timeout use
                  the `--no-request-timeout` option instead.
              --no-parent
                  Unsets the parent of the environment. Mutual excludes
                  the `--parent` option.
              -p | --parent <new-parent>
                  Specifies a new parent of the environment. In order to
                  unset the parent for this environment use the
                  `--no-parent` option instead.
              -v | --value <key>=<value>
                  Defines the variable `<key>` having given the string
                  `<value>`. Any previous value is overwritten. In order
                  to remove a variable, use the `--remove-variable`
                  option. This option can be used more than once.
              -V | --remove-variable <variable>
                  Removes the variable `<variable>`from the environment.
                  This option can be used more than once.
            """,
        "");
  }

}