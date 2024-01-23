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
class CmdHttpCommandTest extends CommandTestBase {

  @Inject
  CmdHttpCommand command;
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
                usage: test-app cmd http [-r|--replace [-f|--force]]
                                         [-d|--description [--arguments |
                                         --main | --infos] <description>]...
                                         [-s|--synopsis <synopsis>]
                                         [-h|--header <header>=<value>]...
                                         [-q|--query <key>=<value>]...
                                         [-o|--option <name>=<variable>]...
                                         [-p|--positional
                                         <name>=<variable>]...
                                         [-l|--load-body] <address> <method>
                                         <base-uri> [<body>]
                """,
            null,
            null),
        // Minimal parameters
        Arguments.of(
            List.of("address", "get", "https://www.example.com"),
            true,
            "",
            "",
            "address",
            """
                {
                  "type": "Http",
                  "synopsis": "The address command.",
                  "descriptions": {},
                  "restConfig": {
                    "method": "get",
                    "baseUri": "https://www.example.com",
                    "queryParameters": {},
                    "headers": {},
                    "parameters": {
                      "parameters": []
                    },
                    "body": null
                  }
                }
                """),
        // More complex
        Arguments.of(
            List.of("-sThe synopsis", "-d", "The main description", "-d", "--infos", "The further infos",
                "abc", "post", "https://${url}/${path}", "-l", "%builtin:/templates/test.ftl", "-purl=url", "-opath=path",
                "-qabc=def", "-hghi=jkl"),
            true,
            "",
            "",
            "abc",
            """
                {
                  "type": "Http",
                  "synopsis": "The synopsis",
                  "descriptions": {
                    "FURTHER_INFOS": "The further infos",
                    "DESCRIPTION": "The main description"
                  },
                  "restConfig": {
                    "method": "post",
                    "baseUri": "https://${url}/${path}",
                    "queryParameters": {
                      "abc": "def"
                    },
                    "headers": {
                      "ghi": ["jkl"]
                    },
                    "parameters": {
                      "parameters": [
                        {
                          "style": "RequiredPositional",
                          "name": "url",
                          "variable": "url",
                          "optionNames": []
                        },
                        {
                          "style": "SingleOption",
                          "name": "path",
                          "variable": "path",
                          "optionNames": ["--path"]
                        }
                      ]
                    },
                    "body": {
                      "content": {
                        "interpolate": true,
                        "type": "string",
                        "value": "${a} Foo bar"
                      }
                    }
                  }
                }
                """),
        // Already exists
        Arguments.of(
            List.of("a-parent", "get", "https://www.example.com"),
            false,
            "",
            """
                *** error test-app: Command "a-parent" already exists - use
                                    --replace option to enforce replacement.
                """,
            null,
            null),
        // Already exists with children
        Arguments.of(
            List.of("--replace", "a-parent", "get", "https://www.example.com"),
            false,
            "",
            """
                *** error test-app: Command "a-parent" has sub-commands -
                                    use --force option to enforce removal.
                """,
            null,
            null),
        // Replace
        Arguments.of(
            List.of("-rf", "a-parent", "get", "https://www.example.com"),
            true,
            "",
            "",
            "a-parent",
            """
                {
                  "type": "Http",
                  "synopsis": "The a-parent command.",
                  "descriptions": {},
                  "restConfig": {
                    "method": "get",
                    "baseUri": "https://www.example.com",
                    "queryParameters": {},
                    "headers": {},
                    "parameters": {
                      "parameters": []
                    },
                    "body": null
                  }
                }
                """),
        // Variable bound twice
        Arguments.of(
            List.of("address", "get", "https://www.example.com", "-pfoo=foo", "-ofoo=foo"),
            false,
            "",
            """
                *** error test-app: Variable "foo" bound twice in parameter
                                    list.
                """,
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            http - Creates a new HTTP command.

            Usage
              http [-r|--replace [-f|--force]] [-d|--description
                   [--arguments | --main | --infos] <description>]...
                   [-s|--synopsis <synopsis>] [-h|--header
                   <header>=<value>]... [-q|--query <key>=<value>]...
                   [-o|--option <name>=<variable>]... [-p|--positional
                   <name>=<variable>]... [-l|--load-body] <address>
                   <method> <base-uri> [<body>]

            Description
              Creates a new HTTP command. When creating a HTTP command
              you have to specify the HTTP request you want to execute
              and the parameters you want to have in place in order to
              customize the request.
              The HTTP is specified by its method, request URL, and
              optionally body, headers, and query parameters. All these
              may contain references to placeholders that are replaced
              with actual values, either taken from the current
              environment or variables. The variables can be also bound
              to command line parameters, which can be passed along with
              the command. For example, the following defines a command
              named `getEntity` that has a parameter named `id` which is
              bound to the variable `var1`. It executes a simple HTTP
              GET request, whereas parts of the request URL are replaced
              with the value for variable `var1`:
             \s
                 test-app cmd http getEntity -p id=var1 get 'http://localhost:8080/entity/${var1}'
             \s
              Calling `test-app getEntity foo` will execute a HTTP GET
              request for http://localhost:8080/entity/foo then.
              In order to update an existing command use the `mod`
              command.
              There are several options to fine-tune the command, see
              the following section.

            Arguments and options
              <address>
                  The address of the command to create, see also section
                  "Further Infos:" for details concerning the syntax.
              <method>
                  The HTTP method to use for the request. This is
                  typically something like `get`, `put`, `post`, or
                  `delete`. The `<method>` is implicitly converted to
                  upper case.
              <base-uri>
                  The base URI of the HTTP request. This URI might
                  contain references to placeholders, which are replaced
                  before execution. The final URI might be extended by
                  the query parameters (see option `--query`).
              <body>
                  This is an optional parameter containing the request
                  body. The body is specified as an input source, see
                  section "Further Infos" for more information about the
                  syntax. This might be omitted in case the request does
                  not expect a body. Note that if the input source is
                  interpolated (so it starts with a `%` sign), the
                  interpolation is done when the HTTP request is
                  executed. If a `<body>` is given, you may use the
                  following option as well:
                  -l | --load-body
                      In case input source of the body refers to a file,
                      the standard input, or an URL, the `--load-body`
                      option can be used to instruct test-app to load
                      the content of the input source and store this
                      content along with the command definition instead
                      of the reference to this file/URL.
              -d | --description [--main|--arguments|--infos]
              <description>
                  Sets a section of the command help text. The
                  `<description>` itself is specified as an input
                  source, see also section "Further Infos:" for details.
                  The sub options `--main`, `--arguments`, or `--infos`
                  specify the section being set (see below). The
                  `--description` option can be used multiple times, but
                  only once per section:
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
              -h | --header <header>=<value>
                  Sets the HTTP request header `<header>` to `<value>`,
                  whereas the `<value>` might contain references to
                  placeholders that are replaced when the HTTP request
                  is executed. You may use this option more than once,
                  even with identical `<header>` keys.
              -o | --option <name>=<variable>
                  Defines an option parameter for the new command. The
                  option `<name>` is specified without the leading
                  dashes. If `<name>` is just one single character, you
                  have to specify the option with a single leading dash
                  (so if `<name>` was `x`, the created command will
                  accept the option `-x`), otherwise with double leading
                  dashes (so if `<name>` was `xyz`, the created command
                  will accept the option `--xyz`). The option is bound
                  to the given `<variable>`. When executing the command,
                  the value passed to the option are set in the
                  corresponding variables. In order to define a
                  positional parameter, use `--positional` instead.
              -p | --positional <name>=<variable>
                  Defines a positional parameter for the new command. It
                  has a `<name>` that uniquely identifies the parameter
                  and a `<variable>`. When executing the command, the
                  values passed to the parameters are set in the
                  corresponding variables. In order to define an option,
                  use option `--option` instead.
              -q | --query <key>=<value>
                  Adds the query parameter `<key>` with the specified
                  `<value>` to the url of the HTTP request. The
                  `<value>` might contain references to placeholders
                  that are replaced when the HTTP request is executed.
                  You may use this option more than once, but the
                  `<key>` part must be different then.
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
