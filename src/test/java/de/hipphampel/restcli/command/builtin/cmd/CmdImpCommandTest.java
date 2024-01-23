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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
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
class CmdImpCommandTest extends CommandTestBase {

  @Inject
  CmdImpCommand command;

  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    super.beforeEach(rootDir);
    CommandConfig config = new CommandConfig();

    config.setType(Type.Parent);
    config.setSynopsis("doo");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "doo doo"));
    storeCommand(CommandAddress.fromString("foo"), config);

  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedConfigs,
      String unexpectedConfigs)
      throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedConfigs != null) {
      Map<String, CommandConfig> configMap = objectMapper.readValue(expectedConfigs, new TypeReference<Map<String, CommandConfig>>() {
      });
      for (Map.Entry<String, CommandConfig> entry : configMap.entrySet()) {
        assertThat(commandConfigRepository.load(context.configPath(), CommandAddress.fromString(entry.getKey()))).isEqualTo(
            entry.getValue());
      }
    }
    if (unexpectedConfigs != null) {
      List<String> addresses = objectMapper.readValue(unexpectedConfigs, new TypeReference<List<String>>() {
      });
      for (String address : addresses) {
        assertThat(commandConfigRepository.getValidatedPath(context.configPath(), CommandAddress.fromString(address))).isEmpty();
      }
    }
  }

  static Stream<Arguments> execute_data() {
    String aParentConfig = """
        {
          "type" : "Parent",
          "synopsis" : "Original synopsis1",
          "descriptions" : {
            "DESCRIPTION" : "Original description1"
          },
          "restConfig" : null,
          "aliasConfig" : null
        }""";
    String aHttpConfig = """
        {
           "type" : "Http",
           "synopsis" : "Original synopsis4",
           "descriptions" : {
             "DESCRIPTION" : "Original description4"
           },
           "restConfig" : {
             "method" : "GET",
             "baseUri" : "https://example.com",
             "queryParameters" : {
               "id" : "4711"
             },
             "headers" : {
               "foo" : [ "bar" ]
             },
             "parameters" : {
               "parameters" : [ {
                 "style" : "RequiredPositional",
                 "name" : "name",
                 "variable" : "id",
                 "optionNames" : null
               } ]
             },
             "body" : {
                "body" : {
                  "type" : "string",
                  "value" : "foo",
                  "interpolate" : false
                },
                "interpretAsInputSource" : false
             }
           },
           "aliasConfig" : null
         }""";
    String anAliasConfig = """
        {
          "type" : "Alias",
          "synopsis" : "Original synopsis3",
          "descriptions" : {
            "DESCRIPTION" : "Original description3"
          },
          "restConfig" : null,
          "aliasConfig" : [ "the", "alias" ]
        }""";
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<address>".
                usage: test-app cmd imp [-e|--exclude-children]
                                        [-r|--replace [-f|--force]]
                                        <address> [<source>]
                """,
            null,
            null),
        // FAIL: Command not found
        Arguments.of(
            List.of(
                "not/found",
                """
                    {
                      "config" : %s,
                      "subCommands" : { }
                    }""".formatted(aParentConfig)),
            false,
            "",
            """
                *** error test-app: Command address "not/found" refers to
                                    not existing parent "not".
                """,
            null,
            """
                [
                  "not/found"
                ]
                """),
        // FAIL: Command is builtin
        Arguments.of(
            List.of(
                "cmd",
                """
                    {
                      "config" : %s,
                      "subCommands" : { }
                    }""".formatted(aParentConfig)),
            false,
            "",
            """
                *** error test-app: "cmd" is a builtin command.
                """,
            null,
            """
                [
                  "cmd"
                ]
                """),
        // FAIL: Command exists
        Arguments.of(
            List.of(
                "foo",
                """
                    {
                      "config" : %s,
                      "subCommands" : { }
                    }""".formatted(aParentConfig)),
            false,
            "",
            """
                *** error test-app: Command "foo" already exists - use
                                    --replace option to enforce replacement.
                """,
            """
                {
                  "foo": {
                    "type" : "Parent",
                    "synopsis" : "doo",
                    "descriptions" : {
                      "DESCRIPTION" : "doo doo"
                    },
                    "restConfig" : null,
                    "aliasConfig" : null
                  }
                }""",
            null),
        // OK: no children
        Arguments.of(
            List.of("a-parent", "-e",  """
                    {
                      "config" : %s,
                      "subCommands" : {
                          "alias": {
                            "config": %s
                          },
                          "http": {
                            "config": %s
                          }
                       }
                    }""".formatted(aParentConfig, anAliasConfig, aHttpConfig)),
            true,
            "",
            "",
            """
                {
                  "a-parent": %s
                }
                """.formatted(aParentConfig),
            """
                [ "a-parent/alias", "a-parent/http" ]
                """),
        // OK: with children
        Arguments.of(
            List.of("a-parent",  """
                    {
                      "config" : %s,
                      "subCommands" : {
                          "alias": {
                            "config": %s
                          },
                          "http": {
                            "config": %s
                          }
                       }
                    }""".formatted(aParentConfig, anAliasConfig, aHttpConfig)),
            true,
            "",
            "",
            """
                {
                  "a-parent": %s,
                  "a-parent/http": %s,
                  "a-parent/alias": %s
                }
                """.formatted(aParentConfig, aHttpConfig, anAliasConfig),
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            imp - Imports a command (tree).

            Usage
              imp [-e|--exclude-children] [-r|--replace [-f|--force]]
                  <address> [<source>]

            Description
              Imports the specified commands and sub commands based on a
              file that was previously created by the `exp` command.
              In order to import an OpenAPI specification, use the
              `openapi` command instead.

            Arguments and options
              <address>
                  The name of the command to import.
              <source>
                  The source where to read the command tree from. This
                  is specified as an input source (see section "Further
                  Infos"). If this is omitted, the data is read from
                  stdin.
              -e | --exclude-children
                  If set, only the top level command is imported and the
                  children - if any - are skipped.
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
