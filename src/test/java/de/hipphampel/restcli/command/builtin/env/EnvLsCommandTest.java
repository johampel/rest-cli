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
class EnvLsCommandTest extends CommandTestBase {

  @Inject
  EnvLsCommand command;
  @Inject
  EnvironmentRepository environmentRepository;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    Environment root1 = environmentRepository.createTransientEnvironment("root1", null);
    environmentRepository.storeEnvironment(context.configPath(), root1, false);
    Environment parent11 = environmentRepository.createTransientEnvironment("parent11", root1);
    environmentRepository.storeEnvironment(context.configPath(), parent11, false);
    Environment child111 = environmentRepository.createTransientEnvironment("child111", parent11);
    environmentRepository.storeEnvironment(context.configPath(), child111, false);
    Environment child112 = environmentRepository.createTransientEnvironment("child112", parent11);
    environmentRepository.storeEnvironment(context.configPath(), child112, false);
    Environment parent12 = environmentRepository.createTransientEnvironment("parent12", root1);
    environmentRepository.storeEnvironment(context.configPath(), parent12, false);
    Environment child121 = environmentRepository.createTransientEnvironment("child121", parent12);
    environmentRepository.storeEnvironment(context.configPath(), child121, false);
    Environment child122 = environmentRepository.createTransientEnvironment("child122", parent12);
    environmentRepository.storeEnvironment(context.configPath(), child122, false);
    Environment grandchild1221 = environmentRepository.createTransientEnvironment("grandchild1221", child122);
    environmentRepository.storeEnvironment(context.configPath(), grandchild1221, false);
    Environment grandchild1222 = environmentRepository.createTransientEnvironment("grandchild1222", child122);
    environmentRepository.storeEnvironment(context.configPath(), grandchild1222, false);
    Environment root2 = environmentRepository.createTransientEnvironment("root2", null);
    environmentRepository.storeEnvironment(context.configPath(), root2, false);
    Environment parent21 = environmentRepository.createTransientEnvironment("parent21", root2);
    environmentRepository.storeEnvironment(context.configPath(), parent21, false);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // (List without root)
        Arguments.of(
            List.of(),
            true,
            """
                child111
                child112
                child121
                child122
                grandchild1221
                grandchild1222
                parent11
                parent12
                parent21
                root1
                root2
                """,
            ""),
        // (List, unknown root)
        Arguments.of(
            List.of("unknown"),
            false,
            "",
            """
                *** error test-app: Environment "unknown" does not exist.
                """),
        // (List, known root)
        Arguments.of(
            List.of("root1"),
            true,
            """
                parent11
                parent12
                """,
            ""),
        // Beautified tree, no root
        Arguments.of(
            List.of("-bt"),
            true,
            """
                root1
                ├── parent11
                │   ├── child111
                │   └── child112
                └── parent12
                    ├── child121
                    └── child122
                        ├── grandchild1221
                        └── grandchild1222
                root2
                └── parent21
                """,
            ""),
        // Beautified tree, root
        Arguments.of(
            List.of("-bt", "parent12"),
            true,
            """
                parent12
                ├── child121
                └── child122
                    ├── grandchild1221
                    └── grandchild1222
                """,
            ""),
        // Simple tree, root
        Arguments.of(
            List.of("-t", "parent12"),
            true,
            """
                parent12
                    child121
                    child122
                        grandchild1221
                        grandchild1222
                """,
            "")

    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            ls - Lists available environments.

            Usage
              ls [-b|--beautify] [-t|--tree] [<root>]

            Description
              Lists the available environments, either in list or in
              tree format:
              - When `--tree` is given, it shows a tree of the
                environments starting with the `<root>` environment and
                all its direct or indirect children or, if no `<root>`
                is given, the trees of really all available
                environments.
              - When `--tree` is omitted it either shows a list of all
                direct children of the `<root>` environment or, if no
                `<root>` is given, a list of really all environments.

            Arguments and options
              <root>
                  The root environment to start with. For details please
                  refer to section "Description" above.
              -b | --beautify
                  This option has only an effect in combination with the
                  `--tree` option and adds some characters to the output
                  that visualizes the tree structure.
              -t | --tree
                  If set, print a tree visualizing the parent - child
                  relationship of the environments. If omitted, print a
                  list.
            """,
        "");
  }

}