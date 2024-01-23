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
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineParser;
import de.hipphampel.restcli.command.Command;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandInvoker;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HttpCommandTestBase;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterConfig.Style;
import de.hipphampel.restcli.command.config.ParameterListConfig;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.rest.RequestContext;
import de.hipphampel.restcli.rest.RequestContext.OutputFormat;
import de.hipphampel.restcli.template.TemplateModel;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class CustomHttpCommandTest extends HttpCommandTestBase {

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
                .setType(Type.Http)
                .setRestConfig(
                    new RestCommandConfig()
                        .setMethod("get")
                        .setBaseUri("http://${baseUrl}")
                )),
            List.of(),
            true,
            """
                {
                  "method" : "get",
                  "path" : "/",
                  "headers" : { },
                  "body" : ""
                }
                """,
            ""),
        // Bad command line
        Arguments.of(
            commandConfig2String(new CommandConfig()
                .setType(Type.Http)
                .setRestConfig(
                    new RestCommandConfig()
                        .setMethod("post")
                        .setBaseUri("http://${baseUrl}/${foo}")
                        .setParameters(new ParameterListConfig(List.of(
                            new ParameterConfig(Style.SingleOption, "<arg>", "foo", List.of("--id")),
                            new ParameterConfig(Style.RequiredPositional, "<data>", "_body", null)
                        )))
                )),
            List.of("--id"),
            false,
            "",
            """
                *** error test-app: Missing required argument "<data>".
                usage: test-app test [--id <arg>] <data>
                """),
        // Some parameters
        Arguments.of(
            commandConfig2String(new CommandConfig()
                .setType(Type.Http)
                .setRestConfig(
                    new RestCommandConfig()
                        .setMethod("post")
                        .setBaseUri("http://${baseUrl}/${foo}")
                        .setQueryParameters(Map.of("bar", "${bar}"))
                        .setHeaders(Map.of("test-header", List.of("${bar}/${foo}")))
                        .setBody(new BodyConfig(InputStreamProviderConfig.fromString("%{\"id\": \"${foo}\"}"), null))
                        .setParameters(new ParameterListConfig(List.of(
                            new ParameterConfig(Style.SingleOption, "<arg>", "foo", List.of("--id")),
                            new ParameterConfig(Style.RequiredPositional, "<bar>", "bar", null),
                            new ParameterConfig(Style.RequiredPositional, "<data>", "_body", null)
                        )))
                )),
            List.of("--id", "THE_ID", "THE_BAR", """
                %{
                  "id": "${foo}
                }"""),
            true,
            """
                {
                  "method" : "post",
                  "path" : "/THE_ID?bar=THE_BAR",
                  "headers" : {
                    "test-header" : [ "THE_BAR/THE_ID" ]
                  },
                  "body" : "{\\"id\\": \\"THE_ID\\"}"
                }
                """,
            "")
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
          test\s
                
        Description
          foo
                
        Arguments and options
          bar
                
        Further infos
          baz
        """);
  }

  @Test
  void commandLineSpec() {
    CommandConfig config = initEmptyConfig();
    RestCommandConfig restConfig = config.getRestConfig();
    restConfig.setParameters(new ParameterListConfig(List.of(
        new ParameterConfig(Style.MultiOption, "eins", "var1", List.of("-e", "--eins")),
        new ParameterConfig(Style.SingleOption, "zwei", "var2", List.of("-z", "--zwei")),
        new ParameterConfig(Style.RequiredPositional, "drei", "var3", null),
        new ParameterConfig(Style.RequiredMultiPositional, "vier", "var4", null)
    )));
    Command command = factory.createCommand(CommandAddress.fromString("test"), config);

    assertThat(command.commandLineSpec().usageString()).isEqualTo("[-e|--eins eins]... [-z|--zwei zwei] drei vier...");
  }

  @Test
  void collectVariables() {
    CommandConfig config = initEmptyConfig();
    RestCommandConfig restConfig = config.getRestConfig();
    restConfig.setParameters(new ParameterListConfig(List.of(
        new ParameterConfig(Style.MultiOption, "eins", "var1", List.of("-e", "--eins")),
        new ParameterConfig(Style.SingleOption, "zwei", "var2", List.of("-z", "--zwei")),
        new ParameterConfig(Style.RequiredPositional, "drei", "var3", null),
        new ParameterConfig(Style.RequiredMultiPositional, "vier", "var4", null)
    )));
    CustomHttpCommand command = (CustomHttpCommand) factory.createCommand(CommandAddress.fromString("test"), config);
    CommandLine commandLine = parser.parseCommandLine(command.commandLineSpec(), List.of("1", "-e2", "-z3", "-e4", "5", "6", "7"));

    Map<String, Object> variables = command.collectVariables(commandLine);
    assertThat(variables).isEqualTo(Map.of(
        "var1", List.of("2", "4"),
        "var2", "3",
        "var3", "1",
        "var4", List.of("5", "6", "7")
    ));
  }

  @ParameterizedTest
  @CsvSource({
      "'one',      ,           'one'",
      ",           'one',      'zwei'",
      "'three',    ,           'three'",
      ",           'three',    'drei'",
      "'notfound', ,           'notfound'",
      ",           'notfound', ",
      "'${one}',   ,           '${one}'",
      ",           '${one}',   ",
  })
  void createBody(String content, String variable, String expected) throws IOException {
    RequestContext requestContext = new RequestContext(
        context.httpClient(),
        new TemplateModel(Map.of("one", "%${two}", "two", "zwei", "three", "string:drei")),
        Set.of("one", "two", "three"),
        new OutputFormat(InputStreamProvider.ofString("output"), Map.of()),
        context.out(),
        context.err()
    );
    CommandConfig config = initEmptyConfig();
    RestCommandConfig restConfig = config.getRestConfig();
    restConfig.setBody(new BodyConfig(
        InputStreamProviderConfig.fromString(content),
        variable
    ));
    CustomHttpCommand command = (CustomHttpCommand) factory.createCommand(CommandAddress.fromString("test"), config);

    InputStreamProvider body = command.createBody(context, requestContext);
    if (expected != null) {
      try (InputStream in = body.open(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        in.transferTo(out);
        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo(expected);
      }
    } else {
      assertThat(body).isNull();
    }
  }

  static String commandConfig2String(CommandConfig config) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(config);
  }

  static CommandConfig initEmptyConfig() {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Http);
    config.setRestConfig(new RestCommandConfig());
    return config;
  }
}
