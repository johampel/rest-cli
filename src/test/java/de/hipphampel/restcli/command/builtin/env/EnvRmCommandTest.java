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

import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentRepository;
import de.hipphampel.restcli.utils.Pair;
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
class EnvRmCommandTest extends CommandTestBase {

  @Inject
  EnvRmCommand command;
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
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedEnvs) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    assertThat(environmentRepository.streamConfigs(context.configPath()).map(Pair::first).sorted().toList()).containsExactlyElementsOf(
        TestUtils.stringToList(expectedEnvs));
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // (No Args)
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<name>".
                usage: test-app env rm [-f|--force] <name>...
                """,
            "child111;child112;child121;child122;grandchild1221;grandchild1222;parent11;parent12;parent21;root1;root2"),
        // (Environment not found)
        Arguments.of(
            List.of("not_found"),
            false,
            "",
            """
                *** error test-app: Environment "not_found" does not exist.
                """,
            "child111;child112;child121;child122;grandchild1221;grandchild1222;parent11;parent12;parent21;root1;root2"),
        // (Environment in use)
        Arguments.of(
            List.of("parent11"),
            false,
            "",
            """
                *** error test-app: Environment "parent11" has child
                                    environments - use --force option, if
                                    you really want to delete.
                """,
            "child111;child112;child121;child122;grandchild1221;grandchild1222;parent11;parent12;parent21;root1;root2"),
        // (leaf environment)
        Arguments.of(
            List.of("grandchild1221"),
            true,
            "",
            "",
            "child111;child112;child121;child122;grandchild1222;parent11;parent12;parent21;root1;root2"),
        // (force delete environment)
        Arguments.of(
            List.of("-f", "parent11"),
            true,
            "",
            "",
            "child121;child122;grandchild1221;grandchild1222;parent12;parent21;root1;root2")

    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            rm - Removes environments.

            Usage
              rm [-f|--force] <name>...

            Description
              Delete the environments specified by the `<name>`
              parameter. When an environment has a child environment,
              the `--force` option has to be specified in order to
              delete the child environments as well.

            Arguments and options
              <name>
                  Name of the environment to delete. Can be specified
                  more than one.
              -f | --force
                  Must be used in case the environment to delete has at
                  least one child environment. If so, the child
                  environments are deleted as well.
            """,
        "");
  }

}