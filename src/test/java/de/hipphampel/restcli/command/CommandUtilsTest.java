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
package de.hipphampel.restcli.command;


import static de.hipphampel.restcli.TestUtils.assertInputStreamProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.FormatBuilder;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentConfig;
import de.hipphampel.restcli.env.EnvironmentRepository;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.rest.RequestContext.OutputFormat;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import de.hipphampel.restcli.template.TemplateConfig.Parameter;
import de.hipphampel.restcli.template.TemplateRepository;
import de.hipphampel.restcli.utils.KeyValue;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CommandUtilsTest extends CommandTestBase {

  @Inject
  TemplateRepository templateRepository;
  @Inject
  EnvironmentRepository environmentRepository;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    environmentRepository.storeEnvironment(
        context.configPath(),
        new Environment(
            null,
            "test",
            EnvironmentConfig.EMPTY,
            new EnvironmentConfig(
                null,
                Map.of("foo1", "bar1", "foo2", "bar"),
                Map.of(),
                null)),
        true);
    context.environment(environmentRepository.getEnvironment(context.configPath(), "test").orElseThrow());
    templateRepository.storeTemplate(
        context.configPath(),
        new Template(
            TemplateAddress.fromString("existing@"),
            new TemplateConfig("ignore", Map.of("param", new Parameter("defaultValue")), "the content"),
            false),
        true);
  }

  @Test
  void showError_string() {
    CommandUtils.showError(context, "This is an error message, with parameter %s and parameter %s.",
        "parameter1", "parameter2");
    assertOutput(
        "",
        """
            *** error test-app: This is an error message, with parameter
                                parameter1 and parameter parameter2.
            """
    );
  }

  @Test
  void showError_block() {
    CommandUtils.showError(context, FormatBuilder.buildFormat(
        """
            |a  |b  |
            |---|---|
            |c  |d  |
            """
    ));
    assertOutput(
        "",
        """
            *** error test-app: ┌─┬─┐
                                │a│b│
                                ├─┼─┤
                                │c│d│
                                └─┴─┘
            """);
  }

  @Test
  void qualifiedName() {
    CommandAddress address = CommandAddress.fromString("abc/def");

    assertThat(CommandUtils.getQualifiedCommandName(context, address)).isEqualTo("test-app abc def");
  }

  @ParameterizedTest
  @CsvSource({
      "'%string:${a}+${b}', '1+2'",
      "'string:${a}+${b}',  '${a}+${b}'",
  })
  void toString_fromInputStreamProviderConfig(String input, String expected) {
    Map<String, Object> model = Map.of("a", 1, "b", 2);
    InputStreamProviderConfig config = InputStreamProviderConfig.fromString(input);
    assertThat(CommandUtils.toString(context, config, model)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'${a} string',  '${a} string'",
      "'%${a} string', 'ein string'",
  })
  void createInputStreamProvider_string(String input, String expected) {
    Map<String, Object> model = Map.of("a", "ein");
    InputStreamProviderConfig config = InputStreamProviderConfig.fromString(input);
    assertInputStreamProvider(CommandUtils.createInputStreamProvider(context, config, model), expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'@',      '${a} file'",
      "'%@',     'ein file'",
      "'path:',  '${a} file'",
      "'%path:', 'ein file'",
  })
  void createInputStreamProvider_path(String prefix, String expected) throws IOException {
    Map<String, Object> model = Map.of("a", "ein");
    Path path = rootDir.resolve("test.file");
    Files.writeString(path, "${a} file");
    InputStreamProviderConfig config = InputStreamProviderConfig.fromString(prefix + path);
    assertInputStreamProvider(CommandUtils.createInputStreamProvider(context, config, model), expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'url:',  '${a} url'",
      "'%url:', 'ein url'",
  })
  void createInputStreamProvider_url(String prefix, String expected) throws IOException {
    Map<String, Object> model = Map.of("a", "ein");
    Path path = rootDir.resolve("test.file");
    Files.writeString(path, "${a} url");
    InputStreamProviderConfig config = InputStreamProviderConfig.fromString(prefix + path.toUri().toURL().toString());
    assertInputStreamProvider(CommandUtils.createInputStreamProvider(context, config, model), expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'builtin:/templates/test.ftl',  '${a} Foo bar'",
      "'%builtin:/templates/test.ftl', 'ein Foo bar'",
  })
  void createInputStreamProvider_builtin(String input, String expected) {
    Map<String, Object> model = Map.of("a", "ein");
    InputStreamProviderConfig config = InputStreamProviderConfig.fromString(input);
    assertInputStreamProvider(CommandUtils.createInputStreamProvider(context, config, model), expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'@',  '${a} Hallo'",
      "'%@', 'ein Hallo'",
  })
  void createInputStreamProvider_stdin(String input, String expected) {
    Map<String, Object> model = Map.of("a", "ein");
    context.in(new ByteArrayInputStream("${a} Hallo".getBytes(StandardCharsets.UTF_8)));
    InputStreamProviderConfig config = InputStreamProviderConfig.fromString(input);
    assertInputStreamProvider(CommandUtils.createInputStreamProvider(context, config, model), expected);
  }

  @Test
  void fillVariables_ok() {
    Map<String, Object> variables = Map.of("a", "b", "c", "d", "e", "f");

    Map<String, Object> result = CommandUtils.fillVariables(
        context,
        variables,
        List.of(
            KeyValue.fromString("c"),
            KeyValue.fromString("e=g"),
            KeyValue.fromString("h=i")
        ),
        true);

    assertThat(result).isEqualTo(Map.of(
        "a", "b",
        "e", "g",
        "h", "i"
    ));
    assertOutput("", "");
  }

  @Test
  void fillVariables_fail_remove() {
    Map<String, Object> variables = Map.of("a", "b", "c", "d", "e", "f");

    assertThatThrownBy(() -> CommandUtils.fillVariables(
        context,
        variables,
        List.of(KeyValue.fromString("c")),
        false))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Removing variables (\"c\") not allowed.");
    assertOutput("", "");
  }

  @Test
  void fillVariables_warnings() {
    Map<String, Object> variables = Map.of("a", "b", "c", "d", "e", "f");

    Map<String, Object> result = CommandUtils.fillVariables(
        context,
        variables,
        List.of(
            KeyValue.fromString("not_exists"),
            KeyValue.fromString("foo=bar"),
            KeyValue.fromString("foo=baz")),
        true);

    assertThat(result).isEqualTo(Map.of(
        "a", "b",
        "c", "d",
        "e", "f",
        "foo", "baz"
    ));
    assertOutput("",
        """
            warning test-app: Removing not existing variable
                              "not_exists" has no effect.
            warning test-app: Modifying variable "foo" more than once -
                              using last modification.
            """);
  }

  @Test
  void fillHeaders_ok() {
    Map<String, List<String>> existing = Map.of("a", List.of("b1", "b2"), "c", List.of("d1", "d2"));
    List<KeyValue<String>> keyAndValues = List.of(
        KeyValue.fromString("a"),
        KeyValue.fromString("c=a1"),
        KeyValue.fromString("d=b1"),
        KeyValue.fromString("d=b2")
    );

    assertThat(CommandUtils.fillHeaders(context, existing, keyAndValues, true)).isEqualTo(Map.of(
        "c", List.of("a1", "d1", "d2"),
        "d", List.of("b1", "b2")
    ));
    assertOutput("", "");
  }

  @Test
  void fillHeaders_fail_remove() {
    Map<String, List<String>> existing = Map.of("a", List.of("b1", "b2"), "c", List.of("d1", "d2"));
    List<KeyValue<String>> keyAndValues = List.of(
        KeyValue.fromString("a")
    );

    assertThatThrownBy(() -> CommandUtils.fillHeaders(context, existing, keyAndValues, false))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Removing headers (\"a\") not allowed.");
    assertOutput("", "");
  }

  @Test
  void fillHeaders_warn_remove() {
    Map<String, List<String>> existing = Map.of("a", List.of("b1", "b2"), "c", List.of("d1", "d2"));
    List<KeyValue<String>> keyAndValues = List.of(
        KeyValue.fromString("not_found")
    );

    assertThat(CommandUtils.fillHeaders(context, existing, keyAndValues, true)).isEqualTo(Map.of(
        "a", List.of("b1", "b2"),
        "c", List.of("d1", "d2")
    ));
    assertOutput("", """
        warning test-app: Removing not existing header "not_found"
                          has no effect.
        """);
  }

  @Test
  void createOutputFormat_withTemplate() {
    Map<String, Object> variables = Map.of("param", "value");
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER, CommandContext.CMD_OPT_TEMPLATE, CommandContext.CMD_OPT_FORMAT),
        List.of("-texisting", "-oparam=value")));

    OutputFormat format = CommandUtils.createOutputFormat(context, templateRepository, variables);

    assertOutputFormat(format,
        Map.of("param", "value"),
        """
            the content""");
    assertOutput("", "");
  }

  @Test
  void createOutputFormat_noParameters() {
    Map<String, Object> variables = Map.of("param", "value");
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER, CommandContext.CMD_OPT_TEMPLATE, CommandContext.CMD_OPT_FORMAT),
        List.of()));

    OutputFormat format = CommandUtils.createOutputFormat(context, templateRepository, variables);

    assertOutputFormat(format,
        Map.of("body", "true", "headers", "false", "rc", "false", "beautify", "true"),
        """
            <#if rc == "true">${_response.statusCode}
            </#if>
            <#if headers == "true"><#list _response.headers.entrySet() as header><#list header.value as value>${header.key}: ${value}
            </#list></#list></#if>
            <#if body == "true"><#if beautify == "true">${_.beautify(_response)}<#else>${_response.stringBody}</#if>
            </#if>""");
    assertOutput("", "");
  }

  @Test
  void createOutputFormatForFormatOption_ok() {
    Map<String, Object> variables = Map.of("param", "value");
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER, CommandContext.CMD_OPT_TEMPLATE, CommandContext.CMD_OPT_FORMAT),
        List.of("-f%${param} format content", "-oparam=value")));

    OutputFormat format = CommandUtils.createOutputFormatForFormatOption(context, variables);

    assertOutputFormat(format, Map.of("param", "value"), "value format content");
    assertOutput("", "");

  }

  @Test
  void createOutputFormatForTemplateOption_ok() {
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER, CommandContext.CMD_OPT_TEMPLATE, CommandContext.CMD_OPT_FORMAT),
        List.of("-texisting", "-oparam=value")));

    OutputFormat format = CommandUtils.createOutputFormatForTemplateOption(context, templateRepository);

    assertOutputFormat(format, Map.of("param", "value"), "the content");
    assertOutput("", "");
  }

  @Test
  void createOutputFormatForTemplateOption_ok_noParameters() {
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER, CommandContext.CMD_OPT_TEMPLATE, CommandContext.CMD_OPT_FORMAT),
        List.of()));

    OutputFormat format = CommandUtils.createOutputFormatForTemplateOption(context, templateRepository);

    assertOutputFormat(format,
        Map.of("body", "true", "headers", "false", "rc", "false", "beautify", "true"),
        """
            <#if rc == "true">${_response.statusCode}
            </#if>
            <#if headers == "true"><#list _response.headers.entrySet() as header><#list header.value as value>${header.key}: ${value}
            </#list></#list></#if>
            <#if body == "true"><#if beautify == "true">${_.beautify(_response)}<#else>${_response.stringBody}</#if>
            </#if>""");
    assertOutput("", "");
  }

  @Test
  void createOutputFormatForTemplateOption_unknownParameter() {
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER, CommandContext.CMD_OPT_TEMPLATE, CommandContext.CMD_OPT_FORMAT),
        List.of("-texisting", "-ounknown=value")));

    OutputFormat format = CommandUtils.createOutputFormatForTemplateOption(context, templateRepository);

    assertOutputFormat(format, Map.of("unknown", "value", "param", "defaultValue"), "the content");
    assertOutput("", """
        warning test-app: Template "existing@" does not define
                          output parameter "unknown".
        """);
  }

  @Test
  void createOutputFormatForTemplateOption_templateNotFound() {
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER, CommandContext.CMD_OPT_TEMPLATE, CommandContext.CMD_OPT_FORMAT),
        List.of("-tnot_found", "-oparam=value")));

    assertThatThrownBy(() -> CommandUtils.createOutputFormatForTemplateOption(context, templateRepository))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Unable to find matching template for name \"not_found\".");

    assertOutput("", "");
  }

  @Test
  void collectParameters_ok() {
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER),
        List.of("-oa=b", "-oc=d")));

    assertThat(CommandUtils.collectParameters(context)).isEqualTo(Map.of(
        "a", "b",
        "c", "d"
    ));
    assertOutput("", "");
  }

  @Test
  void collectParameters_warning() {
    context.rootCommandLine(commandLineParser.parseCommandLine(
        new CommandLineSpec(true, CommandContext.CMD_OPT_OUTPUT_PARAMETER),
        List.of("-oa=b", "-oc=d", "-oa=e")));

    assertThat(CommandUtils.collectParameters(context)).isEqualTo(Map.of(
        "a", "e",
        "c", "d"
    ));
    assertOutput("", """
        warning test-app: Output parameter "a" defined more than
                          once, taking last definition.
        """);
  }

  static void assertOutputFormat(OutputFormat format, Map<String, String> expectedParameters, String expectedContent) {
    assertThat(format.parameters()).isEqualTo(expectedParameters);
    assertInputStreamProvider(format.format(), expectedContent);
  }
}
