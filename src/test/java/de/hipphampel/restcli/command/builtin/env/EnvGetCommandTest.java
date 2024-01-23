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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class EnvGetCommandTest extends CommandTestBase {

  @Inject
  EnvGetCommand command;
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
    parent.setRequestTimeout(100L);
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
    child.setRequestTimeout(1000L);
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
    environmentRepository.storeEnvironment(context.configPath(), grandchild, false);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedFileName,
      String expectedEnv) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedFileName != null) {
      assertThat(Files.readString(Path.of(expectedFileName))).isEqualTo(expectedEnv);
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
                usage: test-app env get [-o|--origin] [-l|--local]
                                        [-b|--beautify] [-c|--common]
                                        [-v|--variables] [-h|--headers]
                                        <name>
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
        // (full output, beautified)
        Arguments.of(
            List.of("-ob", "grandchild"),
            true,
            """
                Common:    ┌───────────────┬─────┐
                           │Setting        │Value│
                           ├───────────────┼─────┤
                           │Parent         │child│
                           │Request timeout│1000 │
                           └───────────────┴─────┘
                Headers:   ┌──────┬──────────┬────────────┐
                           │Header│Origin    │Value       │
                           ├──────┼──────────┼────────────┤
                           │ghi   │parent    │parent_3    │
                           │      │parent    │parent_4    │
                           │jkl   │child     │child_3     │
                           │      │child     │child_4     │
                           │      │parent    │parent_5    │
                           │      │parent    │parent_6    │
                           │mno   │grandchild│grandchild_3│
                           │      │grandchild│grandchild_4│
                           │      │child     │child_5     │
                           │      │child     │child_6     │
                           │pqr   │grandchild│grandchild_5│
                           │      │grandchild│grandchild_6│
                           └──────┴──────────┴────────────┘
                Variables: ┌────────┬──────────┬────────────┐
                           │Variable│Origin    │Value       │
                           ├────────┼──────────┼────────────┤
                           │abc     │parent    │parent_1    │
                           │def     │child     │child_1     │
                           │ghi     │grandchild│grandchild_1│
                           │jkl     │grandchild│grandchild_2│
                           └────────┴──────────┴────────────┘
                """,
            "",
            null,
            null),
        // (full output, simple)
        Arguments.of(
            List.of("-o", "grandchild"),
            true,
            """
                Common:    Parent:          child
                           Request timeout: 1000
                Headers:   ghi: (from parent)     parent_3
                                (from parent)     parent_4
                           jkl: (from child)      child_3
                                (from child)      child_4
                                (from parent)     parent_5
                                (from parent)     parent_6
                           mno: (from grandchild) grandchild_3
                                (from grandchild) grandchild_4
                                (from child)      child_5
                                (from child)      child_6
                           pqr: (from grandchild) grandchild_5
                                (from grandchild) grandchild_6
                Variables: abc: (from parent)     parent_1
                           def: (from child)      child_1
                           ghi: (from grandchild) grandchild_1
                           jkl: (from grandchild) grandchild_2
                """,
            "",
            null,
            null),
        // (no origin)
        Arguments.of(
            List.of("grandchild"),
            true,
            """
                Common:    Parent:          child
                           Request timeout: 1000
                Headers:   ghi: parent_3
                                parent_4
                           jkl: child_3
                                child_4
                                parent_5
                                parent_6
                           mno: grandchild_3
                                grandchild_4
                                child_5
                                child_6
                           pqr: grandchild_5
                                grandchild_6
                Variables: abc: parent_1
                           def: child_1
                           ghi: grandchild_1
                           jkl: grandchild_2
                """,
            "",
            null,
            null),
        // (no origin, parent and variable only)
        Arguments.of(
            List.of("-cv", "grandchild"),
            true,
            """
                Common:    Parent:          child
                           Request timeout: 1000
                Variables: abc: parent_1
                           def: child_1
                           ghi: grandchild_1
                           jkl: grandchild_2
                """,
            "",
            null,
            null)

    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            get - Gets the settings of an environment.

            Usage
              get [-o|--origin] [-l|--local] [-b|--beautify]
                  [-c|--common] [-v|--variables] [-h|--headers] <name>

            Description
              Gets information about a single environment. In order to
              obtain the information in a more machine readable way, use
              the `exp` command instead. This command is intended for
              human usage to investigate which settings are present and
              where they come from (esp. when it comes to inheritance of
              environments).

            Arguments and options
              <name>
                  The name of the environment to show.
              -b | --beautify
                  If set, produce beatified output with tabular print
                  out of all settings. If omitted, the output is a
                  little bit less verbose.
              -c | --common
                  Show the common settings of the environment, such as
                  the parent reference or the the request timeout. If
                  none of the options `--headers`, `--common`, or
                  `--variables` is set, this information is shown as
                  well; otherwise, if only one of this group but no
                  `--common` is set, this information is not shown.
              -h | --headers
                  Show the headers defined by this environment. If none
                  of the options `--headers`, `--common`, or
                  `--variables` is set, the headers are shown as well;
                  otherwise, if only one of this group but no
                  `--headers` is set, the headers are not shown.
              -l | --local
                  If set, the command output only the settings made in
                  the environment itself, but it does not output those
                  defined in the parent environment (if any). If this
                  option is omitted, also the inherited values are
                  shown.
              -o | --origin
                  If present, print for each variable and header value,
                  in which environment the corresponding value is
                  defined. This option is useful in case that the
                  environment to show is inherited from an other one and
                  therefore the values might be defined not only in the
                  environment directly.
              -v | --variables
                  Show the variables defined by this environment. If
                  none of the options `--headers`, `--common`, or
                  `--variables` is set, the variables are shown as well;
                  otherwise, if only one of this group but no
                  `--variables` is set, the variables are not shown.
            """,
        "");
  }

}