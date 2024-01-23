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
class EnvCpCommandTest extends CommandTestBase {

  @Inject
  EnvCpCommand command;
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
    Environment other = environmentRepository.createTransientEnvironment("other", null);
    other.setLocalVariables(Map.of(
        "ghi", "other_1",
        "jkl", "other_2"
    ));
    other.setLocalHeaders(Map.of(
        "mno", List.of("other_3", "other_4"),
        "pqr", List.of("other_5", "other_6")
    ));
    other.setRequestTimeout(4000L);
    environmentRepository.storeEnvironment(context.configPath(), other, false);
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
                *** error test-app: Missing required argument "<source>".
                usage: test-app env cp [-d|--deep] [-r|--replace]
                                       [(-p|--parent <parent>) |
                                       -n|--no-parent] <source> <name>
                """,
            null,
            null),
        // Source not found
        Arguments.of(
            List.of("not-found", "new-name"),
            false,
            "",
            """
                *** error test-app: Environment "not-found" does not exist.
                """,
            null,
            null),
        // Target already exists - without --replace
        Arguments.of(
            List.of("child", "other"),
            false,
            "",
            """
                *** error test-app: Environment "other" already exists - use
                                    option `--replace` to replace it.
                """,
            null,
            null),
        // Copy (local)
        Arguments.of(
            List.of("child", "new-name"),
            true,
            "",
            "",
            "new-name",
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
                  },
                  "requestTimeout": 2000
                }
                """),
        // Copy (deep, reparent)
        Arguments.of(
            List.of("child", "new-name", "-d", "-pother"),
            true,
            "",
            "",
            "new-name",
            """
                {
                  "parent" : "other",
                  "variables" : {
                    "abc" : "parent_1",
                    "def" : "child_1",
                    "ghi" : "child_2"
                  },
                  "headers" : {
                    "ghi" : [ "parent_3", "parent_4" ],
                    "jkl" : [ "child_3", "child_4", "parent_5", "parent_6" ],
                    "mno" : [ "child_5", "child_6" ]
                  },
                  "requestTimeout": 2000
                }
                """),
        // Copy (deep, no parent)
        Arguments.of(
            List.of("child", "new-name", "-dn"),
            true,
            "",
            "",
            "new-name",
            """
                {
                  "parent" : null,
                  "variables" : {
                    "abc" : "parent_1",
                    "def" : "child_1",
                    "ghi" : "child_2"
                  },
                  "headers" : {
                    "ghi" : [ "parent_3", "parent_4" ],
                    "jkl" : [ "child_3", "child_4", "parent_5", "parent_6" ],
                    "mno" : [ "child_5", "child_6" ]
                  },
                  "requestTimeout": 2000
                }
                """),
        // Replace existing
        Arguments.of(
            List.of("other", "parent", "-r"),
            true,
            "",
            "",
            "parent",
            """
                {
                  "parent" : null,
                  "variables" : {
                    "ghi" : "other_1",
                    "jkl" : "other_2"
                  },
                  "headers" : {
                    "mno" : [ "other_3", "other_4" ],
                    "pqr" : [ "other_5", "other_6" ]
                  },
                  "requestTimeout": 4000
                }
                """),
        // Invalid reparent
        Arguments.of(
            List.of("child", "parent", "-r"),
            false,
            "",
            """
                *** error test-app: Environment "parent" has an inheritance
                                    loop, path is [parent].
                """,
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            cp - Creates a copy of an environment.

            Usage
              cp [-d|--deep] [-r|--replace] [(-p|--parent <parent>) |
                 -n|--no-parent] <source> <name>

            Description
              Creates a copy of environment `<source>` and stores it as
              environment `<name>`. Depending on the options passed to
              this command, the copy might have a different parent than
              the original one and the settings of the copy might
              include the settings inherited from the parent of the
              `<source>` or just the settings directly set on the
              `<source>` environment.
              If the `<name>` environment already exists, the
              `--replace` option must be specified in order to overwrite
              it.

            Arguments and options
              <source>
                  Name environment to copy from.
              <name>
                  Name of the copy to create.
              -d | --deep
                  If specified, create a deep copy of the `<source>`
                  environment, so that the environment `<name>` contains
                  also the settings that the `<source>` environment
                  inherits from its parent. If omitted, the copy
                  contains only the settings directly set for the
                  `<source>` environment.
              -n | --no parent
                  If specified, the link to parent of the `<source>`
                  environment is not copied to environment `<name>`.
                  This mutual excludes the option `--parent`. If neither
                  option `--no-parent` nor `--parent` is used, the link
                  to the parent will be copied to `<name>`.
              -p | --parent <parent>
                  If specified, the parent of the `<name>` environment
                  will be `<parent>`. This mutual excludes the option
                  `--no-parent`. If neither option `--no-parent` nor
                  `--parent` is used, the link to the parent will be
                  copied to `<name>`.
              -r | --replace
                  This option must be specified in case that the
                  `<name>` environment already exists. If specified, it
                  overwrites the existing environment.
            """,
        "");
  }
}