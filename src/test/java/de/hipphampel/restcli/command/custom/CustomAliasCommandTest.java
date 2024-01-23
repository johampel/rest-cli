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
package de.hipphampel.restcli.command.custom;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.cli.commandline.CommandLineParser;
import de.hipphampel.restcli.command.Command;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandInvoker;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HttpCommandTestBase;
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
public class CustomAliasCommandTest extends HttpCommandTestBase {

  @Inject
  CustomCommandFactory factory;

  @Inject
  CommandInvoker invoker;

  @Inject
  CommandLineParser parser;

  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(String config, List<String> args, boolean expectedResult, String expectedOut, String expectedErr)
      throws JsonProcessingException {
    CommandConfig commandConfig = objectMapper.readValue(config, CommandConfig.class);
    Command command = factory.createCommand(CommandAddress.fromString("test"), commandConfig);
    assertThat(invoker.runCommand(context, command, args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
  }

  static Stream<Arguments> execute_data() throws JsonProcessingException {
    return Stream.of(
        // No parameters
        Arguments.of(
            commandConfig2String(new CommandConfig()
                .setType(Type.Alias)
                .setAliasConfig(List.of("http", "get", "http://${baseUrl}"))),
            List.of(),
            true,
            """
                {
                  "method" : "GET",
                  "path" : "/",
                  "headers" : { },
                  "body" : ""
                }
                """,
            ""),
        // With global objects and args
        Arguments.of(
            commandConfig2String(new CommandConfig()
                .setType(Type.Alias)
                .setAliasConfig(List.of("-orc=true","http", "get", "http://${baseUrl}"))),
            List.of("-htest-foo=bar"),
            true,
            """
                200
                {
                  "method" : "GET",
                  "path" : "/",
                  "headers" : {
                    "test-foo" : [ "bar" ]
                  },
                  "body" : ""
                }
                """,
            ""),
        // With bad definition
        Arguments.of(
            commandConfig2String(new CommandConfig()
                .setType(Type.Alias)
                .setAliasConfig(List.of("bad"))),
            List.of(),
            false,
            "",
            """
                *** error test-app: No such command "bad".
                """)
    );
  }

  @Test
  void help() {
    CommandConfig config = initEmptyConfig();
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "foo", HelpSection.ARGS_AND_OPTIONS, "bar", HelpSection.FURTHER_INFOS, "baz"));
    config.setSynopsis("foo.");
    Command command = factory.createCommand(CommandAddress.fromString("test"), config);

    command.showHelp(context, context.out());
    assertStdOut("""
        test - foo.

        Usage
          test [<alias-args>...]

        Description
          foo

        Arguments and options
          bar

        Further infos
          baz
        """);
  }

  static String commandConfig2String(CommandConfig config) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(config);
  }

  static CommandConfig initEmptyConfig() {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Alias);
    return config;
  }


}
