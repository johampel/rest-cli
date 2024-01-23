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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
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
class CmdGroupCommandTest extends CommandTestBase {

  @Inject
  CmdGroupCommand command;
  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    CommandConfig config = new CommandConfig();

    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("a-parent"), config);
    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("a-parent/child"), config);
    config.setType(Type.Alias);
    storeCommand(CommandAddress.fromString("an-alias"), config);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedAddress,
      String expectedConfig) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedAddress != null) {
      assertThat(commandConfigRepository.load(context.configPath(), CommandAddress.fromString(expectedAddress)))
          .isEqualTo(objectMapper.readValue(expectedConfig, CommandConfig.class));
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
                *** error test-app: Missing required argument "<address>".
                usage: test-app cmd group [-r|--replace [-f|--force]]
                                          [-d|--description [--arguments |
                                          --main | --infos]
                                          <description>]... [-s|--synopsis
                                          <synopsis>] <address>
                """,
            null,
            null),
        // Simple new creation (all fields)
        Arguments.of(
            List.of("-s", "The synopsis", "-d", "The description", "-d", "--arguments", "The Arguments", "abc"),
            true,
            "",
            "",
            "abc",
            """
                {
                  "type": "Parent",
                  "synopsis": "The synopsis",
                  "descriptions": {
                    "DESCRIPTION": "The description",
                    "ARGS_AND_OPTIONS": "The Arguments"
                  }
                }
                """),
        // Simple new creation (no fields)
        Arguments.of(
            List.of("abc"),
            true,
            "",
            "",
            "abc",
            """
                {
                  "type": "Parent",
                  "synopsis": "The abc command."
                }
                """),
        // Parent not found
        Arguments.of(
            List.of("not-found/abc"),
            false,
            "",
            """
                *** error test-app: Command address "not-found/abc" refers
                                    to not existing parent "not-found".
                """,
            null,
            null),
        // Parent not suitable (builtin)
        Arguments.of(
            List.of("cmd/abc"),
            false,
            "",
            """
                *** error test-app: The builtin command "cmd" cannot have a
                                    custom child command.
                                """,
            null,
            null),
        // Parent not suitable (custom, not parent)
        Arguments.of(
            List.of("an-alias/abc"),
            false,
            "",
            """
                *** error test-app: The builtin command "an-alias" cannot
                                    have a custom child command.
                """,
            null,
            null),
        // Command exists
        Arguments.of(
            List.of("a-parent"),
            false,
            "",
            """
                *** error test-app: Command "a-parent" already exists - use
                                    --replace option to enforce replacement.
                """,
            null,
            null),
        // Command exists
        Arguments.of(
            List.of("-r", "a-parent"),
            false,
            "",
            """
                *** error test-app: Command "a-parent" has sub-commands -
                                    use --force option to enforce removal.
                """,
            null,
            null),
        // Command exists
        Arguments.of(
            List.of("-rf", "a-parent"),
            true,
            "",
            "",
            "a-parent",
            """
                {
                  "type": "Parent",
                  "synopsis": "The a-parent command."
                }
                """)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            group - Creates a new group command.

            Usage
              group [-r|--replace [-f|--force]] [-d|--description
                    [--arguments | --main | --infos] <description>]...
                    [-s|--synopsis <synopsis>] <address>

            Description
              Creates a new group command that can act as a container
              for other custom commands. Compared with other custom
              command types, a group command just has a synopsis and a
              description; the commands it may contain can be added via
              the `group`, `alias`, or `http` commands.
              In order to update an existing command use the `mod`
              command.

            Arguments and options
              <address>
                  The address of the command to create, see also section
                  "Further Infos:" for details concerning the syntax.
              -d | --description [--main|--arguments|--infos]
              <description>
                  Sets a section of the command help text. The
                  `<description>` itself is specified as an input
                  source, see also section "Further Infos:" for details.
                  The sub options `--main`, `--arguments`, or `--infos`
                  specify the section being set (see below). The
                  `--description`option can be used multiple times, but
                  once per section:
                  --main
                      Sets the "Description" section of the command
                      help. This is the same as leaving out any sub
                      option.
                  --arguments
                      Sets the "Arguments and options" section of the
                      command help.
                  --infos
                      Sets the "Further infos" section of the command
                      help.
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
              -s | --synopsis <synopsis>
                  The synopsis of the command. This should be a one
                  liner describing the command's purpose.

            Further infos
              Command addresses:
              A command address uniquely identifies a command in
              test-app. It is basically a path the command, so `foo/bar`
              refers to the command `bar` that is a child command of
              `foo`. The special empty path (``) refers to the root
              command that represents the application itself. For
              details, please type `test-app help :addresses`.
                 \s
              Input source:
              An input source is basically a string literal, which
              represents either a string, refers to a file, an URL, or a
              builtin resource. Detailed information can be obtained by
              typing `test-app help :input-sources`.
            """,
        "");
  }

}
