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
class CmdAliasCommandTest extends CommandTestBase {

  @Inject
  CmdAliasCommand command;
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
                usage: test-app cmd alias [-r|--replace [-f|--force]]
                                          [-d|--description [--arguments |
                                          --main | --infos]
                                          <description>]... [-s|--synopsis
                                          <synopsis>] <address>
                                          <alias-definition>...
                """,
            null,
            null),
        // Simple new creation (all fields)
        Arguments.of(
            List.of("-s", "The synopsis", "-d", "The description", "-d", "--arguments", "The Arguments", "abc", "the", "alias"),
            true,
            "",
            "",
            "abc",
            """
                {
                  "type": "Alias",
                  "synopsis": "The synopsis",
                  "descriptions": {
                    "DESCRIPTION": "The description",
                    "ARGS_AND_OPTIONS": "The Arguments"
                  },
                  "aliasConfig": [ "the", "alias" ]
                }
                """),
        // Simple new creation (no fields)
        Arguments.of(
            List.of("abc", "the", "alias"),
            true,
            "",
            "",
            "abc",
            """
                {
                  "type": "Alias",
                  "synopsis": "The abc command.",
                  "aliasConfig": [ "the", "alias" ]
                }
                """),
        // Parent not found
        Arguments.of(
            List.of("not-found/abc", "the", "alias"),
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
            List.of("cmd/abc", "the", "alias"),
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
            List.of("an-alias/abc", "the", "alias"),
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
            List.of("a-parent", "the", "alias"),
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
            List.of("-r", "a-parent", "the", "alias"),
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
            List.of("-rf", "a-parent", "the", "alias"),
            true,
            "",
            "",
            "a-parent",
            """
                {
                  "type": "Alias",
                  "synopsis": "The a-parent command.",
                  "aliasConfig": [ "the", "alias" ]
                }
                """)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            alias - Creates a new alias command.

            Usage
              alias [-r|--replace [-f|--force]] [-d|--description
                    [--arguments | --main | --infos] <description>]...
                    [-s|--synopsis <synopsis>] <address>
                    <alias-definition>...

            Description
              Creates a new alias command. An alias command is a
              short-cut for other commands, whereas the alias contains
              the first portion of a command line. When calling the
              alias command, the parameters passed to the alias command
              are appended to it to form the final command to execute.
              For example:
             \s
              test-app cmd alias myAlias http post http://example.com
             \s
              Creates an alias named `myAlias` for the the `http`
              command with some of the parameters preset, so calling
              `myAlias` will execute the `http` with the given
              parameters plus the parameters you may pass in addition,
              such as `myAlias @/file/with/body`.
              The alias definition might also contain some of the global
              options that influence the output of the command, see the
              following section below.
              In order to update an existing command use the `mod`
              command.

            Arguments and options
              <address>
                  The address of the command to create, see also section
                  "Further Infos:" for details concerning the syntax.
              <alias-definition>
                  The `<alias-definition>` contains the first portion of
                  or the complete command line to be executed. It might
                  contain also some of the global options available for
                  `test-app` in general. For example
                  `-orc=true -obody=false http get http://example.com`
                  will be an alias definition for the `http` command
                  that passes the `rc` and `body` output parameters as
                  global output parameters (so that the command outputs
                  the status code only). The available global options
                  are `-o | --output-parameter`, `-f | --format`, and
                  `-t | --template`; type `test-app help` to get more
                  information about them.
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
