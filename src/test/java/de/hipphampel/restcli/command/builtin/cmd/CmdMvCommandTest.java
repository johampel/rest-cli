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

import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.CommandConfigTree;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterConfig.Style;
import de.hipphampel.restcli.command.config.ParameterListConfig;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
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
class CmdMvCommandTest extends CommandTestBase {

  @Inject
  CmdMvCommand command;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    CommandConfig config = new CommandConfig();

    config.setType(Type.Parent);
    config.setSynopsis("Original synopsis1");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description1"));
    storeCommand(CommandAddress.fromString("a-parent"), config);
    config.setType(Type.Parent);
    config.setSynopsis("Original synopsis2");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description2"));
    storeCommand(CommandAddress.fromString("a-parent/child"), config);

    config.setType(Type.Alias);
    config.setSynopsis("Original synopsis3");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description3"));
    config.setAliasConfig(List.of("the", "alias"));
    storeCommand(CommandAddress.fromString("a-parent/child/an-alias"), config);

    config.setType(Type.Http);
    config.setAliasConfig(null);
    config.setSynopsis("Original synopsis4");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description4"));
    config.setRestConfig(new RestCommandConfig()
        .setMethod("GET")
        .setBaseUri("https://example.com")
        .setHeaders(Map.of("foo", List.of("bar")))
        .setQueryParameters(Map.of("id", "4711"))
        .setParameters(new ParameterListConfig(List.of(
            new ParameterConfig(Style.RequiredPositional, "name", "id", null)
        )))
        .setBody(new BodyConfig(InputStreamProviderConfig.fromString("foo"), null)));

    storeCommand(CommandAddress.fromString("a-parent/a-http"), config);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedSourceAddress,
      String expectedCopyAddress) throws IOException {
    CommandConfigTree tree = null;
    if (expectedSourceAddress != null) {
      tree = command.getCommandConfigTree(context, CommandAddress.fromString(expectedSourceAddress), false);
    }
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedCopyAddress != null) {
      assertThat(tree).isEqualTo(command.getCommandConfigTree(context, CommandAddress.fromString(expectedCopyAddress), false));
    }
  }

  static Stream<Arguments> execute_data() {
    String fileName = "target/%s.json".formatted(UUID.randomUUID());
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<source>".
                usage: test-app cmd mv [-r|--replace [-f|--force]] <source>
                                       <address>
                """,
            null,
            null),
        // FAIL: Not found
        Arguments.of(
            List.of("not/found", "target"),
            false,
            "",
            """
                *** error test-app: Command "not/found" does not exist.
                """,
            null,
            null),
        // FAIL: builtin
        Arguments.of(
            List.of("cfg", "target"),
            false,
            "",
            """
                *** error test-app: Builtin command "cfg" cannot be moved.
                """,
            null,
            null),
        // FAIL: command exists
        Arguments.of(
            List.of("a-parent/a-http", "a-parent/child"),
            false,
            "",
            """
                *** error test-app: Command "a-parent/child" already exists
                                    - use --replace option to enforce
                                    replacement.
                """,
            null,
            null),
        // OK: command exists with replacement
        Arguments.of(
            List.of("a-parent/a-http", "a-parent/child", "-rf"),
            true,
            "",
            "",
            "a-parent/a-http",
            "a-parent/child"),
        // OK:
        Arguments.of(
            List.of("a-parent", "copy"),
            true,
            "",
            "",
            "a-parent",
            "copy")
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            mv - Moves a command (tree).

            Usage
              mv [-r|--replace [-f|--force]] <source> <address>

            Description
              Moves the specified commands and sub commands. It is only
              possible to move custom commands.

            Arguments and options
              <source>
                  The name of the command to move. See section "Further
                  infos" for information about the syntax.
              <address>
                  The address where to move to. See section "Further
                  infos" for information about the syntax.
              -r | --replace
                  If there is already a command with the given
                  `<address>` you have to provide this option in case
                  you really want to replace it. In case of a
                  replacement, as a further cross check, the following
                  option is present:
                  -f | --force
                      This option is required in case that the command
                      to replace has sub-commands. Note that when
                      replacing a group command, all its sub commands
                      are implicitly removed.

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

}
