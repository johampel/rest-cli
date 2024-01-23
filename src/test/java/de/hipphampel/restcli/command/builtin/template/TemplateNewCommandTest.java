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
package de.hipphampel.restcli.command.builtin.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import de.hipphampel.restcli.template.TemplateConfig.Parameter;
import de.hipphampel.restcli.template.TemplateRepository;
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
class TemplateNewCommandTest extends CommandTestBase {

  @Inject
  TemplateNewCommand command;
  @Inject
  TemplateRepository templateRepository;
  @Inject
  ObjectMapper objectMapper;

  final CommandAddress commandAddress = CommandAddress.fromString("test/command");
  final Template existingTemplate = new Template(
      TemplateAddress.fromString("existing@" + commandAddress),
      new TemplateConfig("existing description", Map.of("foo", new Parameter("bar")), "content sample"), false);

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    this.rootDir = rootDir;
    templateRepository.storeTemplate(context.configPath(), existingTemplate, false);
    CommandConfig config = new CommandConfig();
    config.setType(Type.Parent);
    commandConfigRepository.store(context.configPath(), CommandAddress.fromString("test/command"), config);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedAddress,
      String expectedTemplate)
      throws JsonProcessingException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedTemplate != null && expectedAddress != null) {
      assertThat(templateRepository.getTemplate(context.configPath(), TemplateAddress.fromString(expectedAddress)))
          .contains(
              new Template(TemplateAddress.fromString(expectedAddress), objectMapper.readValue(expectedTemplate, TemplateConfig.class),
                  false));
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
                usage: test-app template new [-r|--replace] [-v|--variable
                                             key=value]... [-d|--description
                                             <description>] <address>
                                             <template>
                """,
            null,
            null),
        // OK: Simple template for root
        Arguments.of(
            List.of("-dfoo", "-vparam1=value1", "-vparam2=value2", "test@", "some content"),
            true,
            "",
            "",
            "test@",
            """
                {
                  "description": "foo",
                  "parameters": {
                    "param1": { "defaultValue": "value1"},
                    "param2": { "defaultValue": "value2"}
                  },
                  "content": "some content"
                }
                """),
        // FAIL: Replace without flag
        Arguments.of(
            List.of("-vparam1=value1", "-vparam2=value2", "existing@test/command", "some content"),
            false,
            "",
            """
                *** error test-app: Output template "existing" for command
                                    "test/command" already exists. Use
                                    `--replace` option if you wish to
                                    overwrite.
                 """,
            "existing@test/command",
            """
                {
                  "description": "existing description",
                  "parameters": {
                    "foo": { "defaultValue": "bar"}
                  },
                  "content": "content sample"
                }
                """),
        // FAIL: Unsuitable command
        Arguments.of(
            List.of("-vparam1=value1", "-vparam2=value2", "existing@env", "some content"),
            false,
            "",
            """
                *** error test-app: Command "env" is not suitable for output
                                    templates.
                """,
            null,
            null),
        // OK: Replace with flag
        Arguments.of(
            List.of("--replace", "-d", "string:The description", "-vparam1=value1", "-vparam2=value2", "existing@test/command",
                "string:some content"),
            true,
            "",
            "",
            "existing@test/command",
            """
                {
                  "description": "The description",
                  "parameters": {
                    "param1": { "defaultValue": "value1"},
                    "param2": { "defaultValue": "value2"}
                  },
                  "content": "some content"
                }
                """),
        // FAIL: Parent command not found
        Arguments.of(
            List.of("-vparam=value", "test@not/found", "some content"),
            false,
            "",
            """
                *** error test-app: Command "not/found" does not exist.
                """,
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            new - Creates a new output template.

            Usage
              new [-r|--replace] [-v|--variable key=value]...
                  [-d|--description <description>] <address> <template>

            Description
              Creates a new output template, either based on an existing
              one or from scratch. The output template can be adapted
              via the `mod` command after creation.
              Details about output templates can be found in the
              `templates` help topic, type `test-app help :templates` to
              learn more about it.
              An alternative way to create an output template is the
              usage of the `imp` command which imports previously
              exported templates, or `cp`, which copies templates.

            Arguments and options
              <address>
                  The address of the output template to create, see
                  section "Further Infos" for more information about the
                  syntax.
              <template>
                  The content of the template, specified as an input
                  source, see section "Further Infos" for more
                  information about the syntax.
              -d | --description <description>
                  The description of the template, specified as an input
                  source, see section "Further Infos" for more
                  information about the syntax.
              -r | --replace
                  This option is required in case you like to replace an
                  already existing output template. Without this option
                  the command will fail in case the output template
                  already exists.
              -v | --variable <key>=<value>
                  Defines the output parameter `<key>` having the
                  default value `<value>`. Each output template
                  parameter should have such a default value.

            Further infos
              Input source:
              An input source is basically a string literal, which
              represents either a string, refers to a file, an URL, or a
              builtin resource. Detailed information can be obtained by
              typing `test-app help :input-sources`.
                 \s
              Template addresses:
              A template address is intended to uniquely identify a
              template. A template address has the general format
              `<name>@<command>`, whereas the `<name>` is the local name
              of the template and `<command>` the address of the owning
              command. For details, please type
              `test-app help :addresses`.
            """,
        "");
  }

}
