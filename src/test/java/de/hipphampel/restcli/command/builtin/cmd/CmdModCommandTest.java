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
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class CmdModCommandTest extends CommandTestBase {

  @Inject
  CmdModCommand command;

  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    CommandConfig config = new CommandConfig();

    config.setType(Type.Parent);
    config.setSynopsis("Original synopsis");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description"));
    storeCommand(CommandAddress.fromString("a-parent"), config);
    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("a-parent/child"), config);

    config.setType(Type.Alias);
    config.setSynopsis("Original synopsis");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description"));
    config.setAliasConfig(List.of("the", "alias"));
    storeCommand(CommandAddress.fromString("an-alias"), config);

    config.setType(Type.Http);
    config.setAliasConfig(null);
    config.setSynopsis("Original synopsis");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description"));
    config.setRestConfig(new RestCommandConfig()
        .setMethod("GET")
        .setBaseUri("https://example.com")
        .setHeaders(Map.of("foo", List.of("bar")))
        .setQueryParameters(Map.of("id", "4711"))
        .setParameters(new ParameterListConfig(List.of(
            new ParameterConfig(Style.RequiredPositional, "name", "id", null)
        )))
        .setBody(new BodyConfig(InputStreamProviderConfig.fromString("foo"), null)));

    storeCommand(CommandAddress.fromString("a-http"), config);
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
        // No arguments
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<address>".
                usage: test-app cmd mod [-r|--reset
                                        cli|docu|headers|query]...
                                        [-d|--description [--arguments |
                                        --main | --infos] <description>]...
                                        [-s|--synopsis <synopsis>]
                                        [-n|--new-address <new-address]
                                        [-m|--method <method>] [-h|--header
                                        <header>=<value>]... [-u|--uri
                                        <base-uri>] [-q|--query
                                        <key>=<value>]... [-o|--option
                                        <name>=<variable>]...
                                        [-p|--positional
                                        <name>=<variable>]... [--no-body |
                                        (-b|--body [-l|--load-body] <body>)]
                                        <address>
                """,
            null,
            null),
        // Change description and synopsis (no-reset)
        Arguments.of(
            List.of("a-parent", "-d", "--arguments", "THE ARGUMENTS", "-sTHE SYNOPSIS"),
            true,
            "",
            "",
            "a-parent",
            """
                {
                  "type": "Parent",
                  "synopsis": "THE SYNOPSIS",
                  "descriptions": {
                    "DESCRIPTION": "Original description",
                    "ARGS_AND_OPTIONS": "THE ARGUMENTS"
                  }
                }
                """),
        // Change description and synopsis (with reset)
        Arguments.of(
            List.of("a-parent", "-d", "--arguments", "THE ARGUMENTS", "-sTHE SYNOPSIS", "-rdocu"),
            true,
            "",
            "",
            "a-parent",
            """
                {
                  "type": "Parent",
                  "synopsis": "THE SYNOPSIS",
                  "descriptions": {
                    "ARGS_AND_OPTIONS": "THE ARGUMENTS"
                  }
                }
                """),
        // Change address (valid)
        Arguments.of(
            List.of("a-parent", "-n", "b-parent"),
            true,
            "",
            "",
            "b-parent",
            """
                {
                  "type": "Parent",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  }
                }
                """),
        // Change address (no valid target 1)
        Arguments.of(
            List.of("a-parent", "-n", "a-parent/child-2"),
            false,
            "",
            """
                *** error test-app: Cannot move command "a-parent" to
                                    "a-parent/child-2".
                """,
            null,
            null),
        // Change address (no valid target 2)
        Arguments.of(
            List.of("a-parent", "-n", "cmd"),
            false,
            "",
            """
                *** error test-app: "cmd" is a builtin command.
                """,
            null,
            null),
        // Change address (no valid target 3)
        Arguments.of(
            List.of("a-parent", "-n", "an-alias"),
            false,
            "",
            """
                *** error test-app: Command "an-alias" already exists.
                """,
            null,
            null),
        // Change description and synopsis (not allowed)
        Arguments.of(
            List.of("a-parent", "--no-body"),
            false,
            "",
            """
                *** error test-app: Command "a-parent" is not a HTTP
                                    command.
                """,
            null,
            null),
        // Change body
        Arguments.of(
            List.of("a-http", "--no-body"),
            true,
            "",
            "",
            "a-http",
            """
                {
                  "type": "Http",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  },
                  "restConfig": {
                    "method": "GET",
                    "baseUri": "https://example.com",
                    "queryParameters": {
                      "id": "4711"
                    },
                    "headers": {
                      "foo": ["bar"]
                    },
                    "parameters": {
                      "parameters": [
                        { "style": "RequiredPositional", "name": "name", "variable": "id" }
                      ]
                    }
                  }
                }
                """),
        // Change body
        Arguments.of(
            List.of("a-http", "--body", "-l", "%builtin:/templates/test.ftl"),
            true,
            "",
            "",
            "a-http",
            """
                {
                  "type": "Http",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  },
                  "restConfig": {
                    "method": "GET",
                    "baseUri": "https://example.com",
                    "queryParameters": {
                      "id": "4711"
                    },
                    "headers": {
                      "foo": ["bar"]
                    },
                    "parameters": {
                      "parameters": [
                        { "style": "RequiredPositional", "name": "name", "variable": "id" }
                      ]
                    },
                    "body": {
                      "content": {
                        "type": "string",
                        "interpolate": true,
                        "value": "${a} Foo bar"
                      }
                    }
                  }
                }
                """),
        // Change headers
        Arguments.of(
            List.of("a-http", "-hkey=value"),
            true,
            "",
            "",
            "a-http",
            """
                {
                  "type": "Http",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  },
                  "restConfig": {
                    "method": "GET",
                    "baseUri": "https://example.com",
                    "queryParameters": {
                      "id": "4711"
                    },
                    "headers": {
                      "foo": ["bar"],
                      "key": ["value"]
                    },
                    "parameters": {
                      "parameters": [
                        { "style": "RequiredPositional", "name": "name", "variable": "id" }
                      ]
                    },
                    "body": {
                      "content": {
                        "type": "string",
                        "interpolate": false,
                        "value": "foo"
                      }
                    }
                  }
                }
                """),
        // Change headers with reset
        Arguments.of(
            List.of("-rheaders", "a-http", "-hkey=value"),
            true,
            "",
            "",
            "a-http",
            """
                {
                  "type": "Http",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  },
                  "restConfig": {
                    "method": "GET",
                    "baseUri": "https://example.com",
                    "queryParameters": {
                      "id": "4711"
                    },
                    "headers": {
                      "key": ["value"]
                    },
                    "parameters": {
                      "parameters": [
                        { "style": "RequiredPositional", "name": "name", "variable": "id" }
                      ]
                    },
                    "body": {
                      "content": {
                        "type": "string",
                        "interpolate": false,
                        "value": "foo"
                      }
                    }
                  }
                }
                """),
        // Change query parameter
        Arguments.of(
            List.of("a-http", "-qkey=value"),
            true,
            "",
            "",
            "a-http",
            """
                {
                  "type": "Http",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  },
                  "restConfig": {
                    "method": "GET",
                    "baseUri": "https://example.com",
                    "queryParameters": {
                      "id": "4711",
                      "key": "value"
                    },
                    "headers": {
                      "foo": ["bar"]
                    },
                    "parameters": {
                      "parameters": [
                        { "style": "RequiredPositional", "name": "name", "variable": "id" }
                      ]
                    },
                    "body": {
                      "content": {
                        "type": "string",
                        "interpolate": false,
                        "value": "foo"
                      }
                    }
                  }
                }
                """),
        // Change query parameter with reset
        Arguments.of(
            List.of("a-http", "-rquery", "-qkey=value"),
            true,
            "",
            "",
            "a-http",
            """
                {
                  "type": "Http",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  },
                  "restConfig": {
                    "method": "GET",
                    "baseUri": "https://example.com",
                    "queryParameters": {
                      "key": "value"
                    },
                    "headers": {
                      "foo": ["bar"]
                    },
                    "parameters": {
                      "parameters": [
                        { "style": "RequiredPositional", "name": "name", "variable": "id" }
                      ]
                    },
                    "body": {
                      "content": {
                        "type": "string",
                        "interpolate": false,
                        "value": "foo"
                      }
                    }
                  }
                }
                """),
        // Change cli
        Arguments.of(
            List.of("a-http", "-okey=value"),
            true,
            "",
            "",
            "a-http",
            """
                {
                  "type": "Http",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  },
                  "restConfig": {
                    "method": "GET",
                    "baseUri": "https://example.com",
                    "queryParameters": {
                      "id": "4711"
                    },
                    "headers": {
                      "foo": ["bar"]
                    },
                    "parameters": {
                      "parameters": [
                        { "style": "RequiredPositional", "name": "name", "variable": "id" },
                        { "style": "SingleOption", "name": "key", "variable": "value", "optionNames": ["--key"] }
                      ]
                    },
                    "body": {
                      "content": {
                        "type": "string",
                        "interpolate": false,
                        "value": "foo"
                      }
                    }
                  }
                }
                """),
        // Change cli with reset
        Arguments.of(
            List.of("a-http", "-rcli", "-okey=value"),
            true,
            "",
            "",
            "a-http",
            """
                {
                  "type": "Http",
                  "synopsis": "Original synopsis",
                  "descriptions": {
                    "DESCRIPTION": "Original description"
                  },
                  "restConfig": {
                    "method": "GET",
                    "baseUri": "https://example.com",
                    "queryParameters": {
                      "id": "4711"
                    },
                    "headers": {
                      "foo": ["bar"]
                    },
                    "parameters": {
                      "parameters": [
                        { "style": "SingleOption", "name": "key", "variable": "value", "optionNames": ["--key"] }
                      ]
                    },
                    "body": {
                      "content": {
                        "type": "string",
                        "interpolate": false,
                        "value": "foo"
                      }
                    }
                  }
                }
                """)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            mod - Modifies an existing custom command

            Usage
              mod [-r|--reset cli|docu|headers|query]...
                  [-d|--description [--arguments | --main | --infos]
                  <description>]... [-s|--synopsis <synopsis>]
                  [-n|--new-address <new-address] [-m|--method <method>]
                  [-h|--header <header>=<value>]... [-u|--uri
                  <base-uri>] [-q|--query <key>=<value>]... [-o|--option
                  <name>=<variable>]... [-p|--positional
                  <name>=<variable>]... [--no-body | (-b|--body
                  [-l|--load-body] <body>)] <address>

            Description
              Modifies an existing command. In case of parent and alias
              commands, you can modify the synopsis, description, or its
              address only. For custom HTTP commands, you may modify all
              its properties.

            Arguments and options
              <address>
                  The address of the command to modify, see also section
                  "Further Infos:" for details concerning the syntax.
              -b | --body <body>
                  If specified, it sets the request body. This mutual
                  excludes option `--no-body` to unset the body. The
                  body is specified as an input source, see section
                  "Further Infos" for more information about the syntax.
                  Note that if the input source is interpolated (so it
                  starts with a `%` sign), the interpolation is done
                  when the HTTP request is executed. If a `<body>` is
                  given, you may use the following option as well:
                  -l | --load-body
                      In case input source of the body refers to a file,
                      the standard input, or an URL, the `--load-body`
                      option can be used to instruct test-app to load
                      the content of the input source and store this
                      content along with the command definition instead
                      of the reference to this file/URL.
              -d | --description [--main|--arguments|--infos]
              <description>
                  Adds or overwrites a section of the command help text.
                  The `<description>` itself is specified as an input
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
              -m | --method <method>
                  Only available for custom HTTP requests: changes the
                  HTTP method to use for the request. This is typically
                  something like `get`, `put`, `post`, or `delete`. The
                  `<method>` is implicitly converted to upper case. If
                  the `--method` parameter is omitted, the method of the
                  HTTP requests remains unchanged.
              -n | --new-address <new-address>
                  The new address of the command. If given, the command
                  is moved to the given address, if omitted, no change
                  is done regard its name and location in the command
                  tree. See also section "Further Infos:" for details
                  concerning the syntax of command addresses.
              --no-body
                  Indicates that the request has no body. This mutually
                  excludes the `--body` option.
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
              -r | --reset cli|docu|headers|query
                  Resets given group of settings (for example
                  `--reset docu` will remove all settings regarding the
                  documentation of the command). Typically, this is
                  normally used in combination with other options (for
                  example with the `--description` option to add new
                  documentation later on again). The `--reset` option
                  can be used once per group, so it is ok to use
                  `--reset docu --reset cli` in the command line. The
                  following groups are known:
                  cli
                      Only available for custom HTTP commands: removes
                      all parameter settings from the command, they can
                      be added with `--positional` and/or `--option` to
                      the command again.
                  docu
                      Removes the documentation from the command.
                      Documentation can be added via the `--description`
                      option.
                  headers
                      Only available for custom HTTP commands: removes
                      all headers settings from teh command. Headers can
                      be added with the `--header` option.
                  query
                      Only available for custom HTTP commands: removes
                      all query parameter settings from teh command.
                      Query parameters can be added with the `--query`
                      option.
              -s | --synopsis <synopsis>
                  The synopsis of the command. This should be a one
                  liner describing the command's purpose.
              -u | --uri <base-uri>
                  Sets the base URI of the HTTP request. This URI might
                  contain references to placeholders, which are replaced
                  before execution. The final URI might be extended by
                  the query parameters (see option `--query`).

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
