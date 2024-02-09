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
package de.hipphampel.restcli.command.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.command.Command;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class HelpCommandTest extends CommandTestBase {

  @Inject
  HelpCommand command;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr) {
    Command otherCommand = mockCommand(CommandAddress.fromString("foo/bar"));
    doAnswer(call -> call.getArgument(1, Output.class).line("HELP")).when(otherCommand).showHelp(any(), any());
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            true,
            """
                restcli - Utility to manage and execute REST calls.

                Usage
                  restcli [-c|--config <config-dir>] [-e|--environment
                          <environment>] [-i|--interactive]
                          [-o|--output-parameter <key>=<value>]...
                          [(-f|--format <format>) | (-t|--template
                          <name-or-address>)] [<sub-command>
                          [<sub-command-args>...]]

                Description
                  The main objective of this tool is to manage and execute
                  HTTP requests. In terms of this application, HTTP requests
                  are represented as sub-commands of this tool, which can be
                  dynamically added, e.g. by importing an OpenAPI
                  specification.
                  Beside the definition of the HTTP commands itself,
                  `test-app` also provides builtin commands to define and
                  apply predefined environments for command execution and
                  manage output templates for transforming the responses of
                  the HTTP commands.
                     \s
                  In order to get information how to import an OpenAPI
                  specification type `test-app help cmd openapi`, or just
                  import it via
                  `test-app cmd openapi <command-name> @<file-with-spec>`.

                Arguments and options
                  <sub-command> [<sub-command-args>]
                      The name of the sub-command to execute. If omitted,
                      this help page is shown. For a list of available
                      sub-commands see the list below.
                  -c | --config <config>
                      If specified, it uses the given `<config>` for the
                      application configuration. The configuration contains
                      several settings that influence the behavior of this
                      application (e.g. where to find commands, which
                      environment to use, ...). If not specified, it uses
                      the default configuration.
                  -e | --environment <environment>
                      If specified, it uses the given `<environment>` for
                      executing HTTP commands. If this option is omitted,
                      the default environment is used; you can obtain the
                      default environment setting by calling
                      `test-app cfg get environment`.
                  -f | --format <format>
                      This option is only evaluated for sub-commands that
                      execute HTTP requests. It defines an output format how
                      to render the response of the HTTP request, specified
                      as an input source, see section "Further Infos" for
                      more information about the syntax. This option mutual
                      excludes the option `--template`. Similar to named
                      output templates, the `<format>` may contain
                      placeholders that are replaced with value taken from
                      the response and other locations. This option is an
                      alternative to the `--template` option in case you
                      want to quickly try output formats without having a
                      named template (see `test-app template` commands) in
                      place. Please type `test-app help :templates` to learn
                      more about templates and their syntax. If neither the
                      `--template` nor the `--format` option is set, the
                      default template is used (configured via the
                      application configuration).
                  -i | --interactive
                      Run in interactive mode. If so, test-app might prompt
                      the user to enter values for variables having no
                      value. By default, test-app will fail in case a
                      variable without a specific value is referenced.
                  -o | --output parameter <key>=<value>
                      This option is only evaluated for sub-commands that
                      execute HTTP requests. Sets an output parameter for
                      the output format. Which output parameters are
                      available depends on the format specified via the
                      `--format` option or the template given by the
                      `--template` option.
                  -t | --template <name-or-address>
                      This option is only evaluated for sub-commands that
                      execute HTTP requests. Specifies the template name or
                      address to use for rendering the output. This option
                      mutual excludes the option `--format`. See section
                      "Further Infos" for the syntax of template names and
                      addresses.

                Available sub-commands
                  cfg      - Collection of commands to read or write the
                             application configuration.
                  cmd      - Collection of commands to manage custom
                             commands.
                  env      - Collection of commands to manage environments.
                  help     - Shows help for a command or general topic.
                  http     - Executes an ad hoc HTTP request.
                  template - Collection of commands to manage output
                             templates.

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
            ""),
        // For existing command
        Arguments.of(
            List.of("foo", "bar"),
            true,
            """
                HELP
                """,
            ""),
        // For not existing command
        Arguments.of(
            List.of("foo", "bar", "unknown"),
            true,
            """
                HELP
                """,
            """
                warning test-app: Command "test-app foo bar" has no
                                  sub-command "unknown".
                """),
        // For existing topic
        Arguments.of(
            List.of(":test-topic"),
            true,
            """
                Test topic
                This is a test topic.
                """,
            ""),
        // For existing topic with extra input
        Arguments.of(
            List.of(":test-topic", "extra"),
            true,
            """
                Test topic
                This is a test topic.
                """,
            """
                warning test-app: More than one parameter was given -
                                  ignoring the rest.
                """),
        // For not existing topic
        Arguments.of(
            List.of(":not-found"),
            true,
            """
                General help topics
                   \s
                The following table shows the available general help topics
                for `test-app`. You may display them by typing
                    `test-app help :<topic-name>`
                Possible topics are:
                ┌─────────────┬────────────────────────────────────────────┐
                │Topic        │Description                                 │
                ├─────────────┼────────────────────────────────────────────┤
                │addresses    │Describes how to address a command or       │
                │             │output-template.                            │
                │api          │Describes the internal API the can be used  │
                │             │within templates.                           │
                │input-sources│Describes how to specify different input    │
                │             │sources (string literals, files URLs, etc)  │
                │             │on command line.                            │
                │templates    │Describes how to define output templates.   │
                │topics       │This page, list of available topics.        │
                └─────────────┴────────────────────────────────────────────┘
                """,
            """
                *** error test-app: No such topic "not-found" - showing
                                    available topics instead.
                """)

    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput(
        """
            help - Shows help for a command or general topic.

            Usage
              help [<sub-command-spec>|:<topic>]

            Description
              `help` shows help texts for the `test-app` itself, a
              sub-command, or a general help topic. When no argument is
              specified, the application help is shown, otherwise the
              help of the specified sub-command or topic.

            Arguments and options
              <sub-command-spec>
                  If given, it shows the help of the specified sub
                  command. For example, if you want to get help for the
                  sub-command `get` of the command `cfg`, you have to
                  type:
                      `test-app help cfg get`
                  Alternatively you may use the compact command address
                  notation:
                      `test-app help cfg/get`
              <topic>
                  If you want to get help for a general topic, such as
                  `templates`, you have to prefix the topic with a `:`,
                  like this:
                      `test-app help :templates`
                  To get a list of available help topics, type
                  `test-app help :topics`.
            """,
        "");
  }

  @ParameterizedTest
  @CsvSource({
      ":unknown,       'General help topics'",
      ":addresses,     'Addresses'",
      ":api,           'The internal API'",
      ":input-sources, 'Input sources'",
      ":templates,     'Templates'",
      ":topics,        'General help topics'",
  })
  void topicHelp(String topic, String firstLine) {
    command.showTopicHelp(context, List.of(topic));
    assertThat(out.toString().split("\n")[0]).isEqualTo(firstLine);
  }
}
