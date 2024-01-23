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
package de.hipphampel.restcli.command.builtin.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import de.hipphampel.restcli.template.TemplateRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class CmdRmCommandTest extends CommandTestBase {

  @Inject
  CmdRmCommand command;
  @Inject
  TemplateRepository templateRepository;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    CommandConfig config = new CommandConfig();

    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("a-parent"), config);
    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("a-parent/child"), config);

    config.setType(Type.Alias);
    config.setAliasConfig(List.of("the", "alias"));
    storeCommand(CommandAddress.fromString("an-alias"), config);

    config.setType(Type.Http);
    config.setAliasConfig(null);
    config.setRestConfig(new RestCommandConfig().setMethod("GET").setBaseUri("https://example.com"));
    storeCommand(CommandAddress.fromString("a-http"), config);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedDiff) throws IOException {
    Set<CommandAddress> before = allCommands().collect(Collectors.toSet());
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    Set<CommandAddress> after = allCommands().collect(Collectors.toSet());
    before.removeAll(after);
    assertThat(before).isEqualTo(TestUtils.stringToList(expectedDiff).stream()
        .map(CommandAddress::fromString)
        .collect(Collectors.toSet()));
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<address>".
                usage: test-app cmd rm [-f|--force] <address>...
                """,
            null),
        // command not found
        Arguments.of(
            List.of("not/found"),
            false,
            "",
            """
                *** error test-app: Command "not/found" not found.
                """,
            null),
        // command builtin
        Arguments.of(
            List.of("cmd/rm"),
            false,
            "",
            """
                *** error test-app: Command "cmd/rm" is a builtin and cannot
                                    be deleted.
                """,
            null),
        // Has children
        Arguments.of(
            List.of("a-parent"),
            false,
            "",
            """
                *** error test-app: Command "”a-parent" has child commands.
                                    Use --force option to delete.
                """,
            null),
        // Ok
        Arguments.of(
            List.of("-f", "a-parent"),
            true,
            "",
            "",
            "a-parent;a-parent/child"),
        // Ok
        Arguments.of(
            List.of("a-parent/child"),
            true,
            "",
            "",
            "a-parent/child")
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            rm - Deletes commands.

            Usage
              rm [-f|--force] <address>...

            Description
              Deletes the given commands. Only custom commands can be
              deleted.

            Arguments and options
              <address>...
                  The command addresses to delete, see also section
                  "Further Infos:" for details concerning the syntax. If
                  a command is a parent command, it can be deleted only
                  if it has no children or if the `--force` option is
                  used.
              -f | --force
                  Forces the deletion in case the command to delete has
                  child commands. If so, the child commands are deleted
                  as well. Without this option, commands having child
                  commands cannot be deleted.

            Further infos
              Command addresses:
              A command address uniquely identifies a command in
              test-app. It is basically a path the command, so `foo/bar`
              refers to the command `bar` that is a child command of
              `foo`. The special empty path (``) refers to the root
              command that represents the application itself. For
              details, please type `test-app help :addresses`.
            """,
        "");
  }


  private Stream<CommandAddress> allCommands() {
    return allCommands(CommandAddress.ROOT);
  }

  private Stream<CommandAddress> allCommands(CommandAddress address) {
    return Stream.concat(
        commandRepository.getCommandInfo(context.configPath(), address).stream()
            .flatMap(info -> info.children().stream())
            .flatMap(this::allCommands),
        Stream.of(address));
  }
}
