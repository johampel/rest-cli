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

import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class EnvExpCommandTest extends CommandTestBase {

  @Inject
  EnvExpCommand command;
  @Inject
  EnvironmentRepository environmentRepository;

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
    parent.setRequestTimeout(4711L);
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
    child.setRequestTimeout(4712L);
    environmentRepository.storeEnvironment(context.configPath(), child, false);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedFileName,
      String expectedEnv) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedFileName != null) {
      assertThat(Files.readString(Path.of(expectedFileName))).isEqualTo(expectedEnv);
      Files.deleteIfExists(Path.of(expectedFileName));
    }
  }

  static Stream<Arguments> execute_data() {
    String fileName = "target/%s.json".formatted(UUID.randomUUID());
    return Stream.of(
        // No args
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<name>".
                usage: test-app env exp [-l|--local] <name> [<target>]
                """,
            null,
            null),
        // Environment not found
        Arguments.of(
            List.of("unknown"),
            false,
            "",
            """
                *** error test-app: Environment "unknown" does not exist.
                """,
            null,
            null),
        // (local)
        Arguments.of(
            List.of("--local", "child"),
            true,
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
                  "requestTimeout" : 4712
                }
                """,
            "",
            null,
            null),
        // (not local)
        Arguments.of(
            List.of("child"),
            true,
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
                  "requestTimeout" : 4712
                }
                """,
            "",
            null,
            null),
        // (output error)
        Arguments.of(
            List.of("child", fileName),
            true,
            "",
            "",
            fileName,
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
                  "requestTimeout" : 4712
                }"""),
        Arguments.of(
            List.of("-l", "child", "."),
            false,
            "",
            """
                *** error test-app: Failed to export environment "child": .
                                    (Is a directory)
                """,
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            exp - Exports an environment.

            Usage
              exp [-l|--local] <name> [<target>]

            Description
              Exports the specified environment with the intention to
              import it (in a different installation) using the `imp`
              command.
              When no options are given, the environment is exported as
              a standalone environment, containing all settings - even
              those inherited from the parent, but without a reference
              to the parent. If the `--local` option is given, it
              exports only the settings that made locally for the
              environment itself.

            Arguments and options
              <name>
                  The name of the environment to export.
              <target>
                  The file where to export to. If omitted, the export is
                  written to standard output.
              -l | --local
                  If set, only the settings locally defined for this
                  environment are exported. If omitted, all settings,
                  including those inherited from the parent are
                  exported.
            """,
        "");
  }

}
