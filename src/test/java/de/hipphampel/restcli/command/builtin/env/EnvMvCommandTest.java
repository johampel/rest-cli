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
import de.hipphampel.restcli.TestUtils;
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
class EnvMvCommandTest extends CommandTestBase {

  @Inject
  EnvMvCommand command;
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
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedEnvs,
      String expectedEnvName,
      String expectedEnv) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    assertThat(getEnvironments()).isEqualTo(TestUtils.stringToList(expectedEnvs));
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
                usage: test-app env mv [-r|--replace] <name> <target>
                """,
            "child=parent;grandchild=child;other=null;parent=null",
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
            "child=parent;grandchild=child;other=null;parent=null",
            null,
            null),
        // Target already exists - without --replace
        Arguments.of(
            List.of("other", "parent"),
            false,
            "",
            """
                *** error test-app: Environment "parent" already exists -
                                    use option `--replace` to replace it.
                """,
            "child=parent;grandchild=child;other=null;parent=null",
            null,
            null),
        // Rename new name
        Arguments.of(
            List.of("other", "new-name"),
            true,
            "",
            "",
            "child=parent;grandchild=child;new-name=null;parent=null",
            "new-name",
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
        // Rename existing
        Arguments.of(
            List.of("other", "parent", "-r"),
            true,
            "",
            "",
            "child=parent;grandchild=child;parent=null",
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
        // Invalid renaming
        Arguments.of(
            List.of("child", "parent", "-r"),
            false,
            "",
            """
                *** error test-app: Environment "parent" has an inheritance
                                    loop, path is [parent].
                """,
            "child=parent;grandchild=child;other=null;parent=null",
            null,
            null),
        // Renaming in tree
        Arguments.of(
            List.of("parent", "grandchild", "-r"),
            true,
            "",
            "",
            "child=grandchild;grandchild=null;other=null",
            "grandchild",
            """
                {
                  "parent" : null,
                  "variables" : {
                    "abc" : "parent_1",
                    "def" : "parent_2"
                  },
                  "headers" : {
                    "ghi" : [ "parent_3", "parent_4" ],
                    "jkl" : [ "parent_5", "parent_6" ]
                  },
                  "requestTimeout": 1000
                }
                """)

    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            mv - Renames an existing environment.

            Usage
              mv [-r|--replace] <name> <target>

            Description
              Renames environment `<name>` to `<target>`. If the target
              environment already exists, the `--replace` option must be
              specified in order to overwrite it.

            Arguments and options
              <name>
                  Name environment to be moved.
              <target>
                  New name of the environment.
              -r | --replace
                  This option must be specified in case that the
                  `<target>` environment already exists. If specified,
                  it overwrites the existing environment.
            """,
        "");
  }

  List<String> getEnvironments() {
    return environmentRepository.listEnvironments(context.configPath()).stream()
        .map(env -> environmentRepository.getEnvironment(context.configPath(), env).orElseThrow())
        .map(env -> env.getName() + "=" + env.getParent())
        .sorted()
        .toList();
  }
}