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
class TemplateCpCommandTest extends CommandTestBase {

  @Inject
  TemplateCpCommand command;
  @Inject
  TemplateRepository templateRepository;
  @Inject
  ObjectMapper objectMapper;

  final CommandAddress commandAddress = CommandAddress.fromString("test/command");
  final Template existingTemplate1 = new Template(
      TemplateAddress.fromString("existing1@" + commandAddress),
      new TemplateConfig("existing description1", Map.of("foo", new Parameter("bar1")), "content sample1"), false);
  final Template existingTemplate2 = new Template(
      TemplateAddress.fromString("existing2@" + commandAddress),
      new TemplateConfig("existing description2", Map.of("foo", new Parameter("bar2")), "content sample2"), false);

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    this.rootDir = rootDir;
    templateRepository.storeTemplate(context.configPath(), existingTemplate1, false);
    templateRepository.storeTemplate(context.configPath(), existingTemplate2, false);
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
                *** error test-app: Missing required argument "<source>".
                usage: test-app template cp [-r|--replace] <source>
                                            <address>
                """,
            null,
            null),
        // OK: Copy without replacement
        Arguments.of(
            List.of("existing1@test/command", "test@"),
            true,
            "",
            "",
            "test@",
            """
                {
                  "description": "existing description1",
                  "parameters": {
                    "foo": { "defaultValue": "bar1"}
                  },
                  "content": "content sample1"
                }
                """),
        // OK: Copy without replacement
        Arguments.of(
            List.of("existing1@test/command", "existing2@test/command", "-r"),
            true,
            "",
            "",
            "existing2@test/command",
            """
                {
                  "description": "existing description1",
                  "parameters": {
                    "foo": { "defaultValue": "bar1"}
                  },
                  "content": "content sample1"
                }
                """),
        // FAIL: Copy without replacement
        Arguments.of(
            List.of("existing1@test/command", "existing2@test/command"),
            false,
            "",
            """
                *** error test-app: Output template "existing2" for command
                                    "test/command" already exists. Use
                                    `--replace` option if you wish to
                                    overwrite.
                """,
            null,
            null),
        // FAIL: Source unknown
        Arguments.of(
            List.of("existing1@not/found", "existing3@test/command"),
            false,
            "",
            """
                *** error test-app: Output template "existing1" for command
                                    "not/found" does not exist.
                """,
            null,
            null),
        // FAIL: Copy to unknown command
        Arguments.of(
            List.of("existing1@test/command", "not@found"),
            false,
            "",
            """
                *** error test-app: Command "found" does not exist.
                """,
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            cp - Copies an output template.

            Usage
              cp [-r|--replace] <source> <address>

            Description
              Creates a copy of template `<source>` and stores it as a
              new template `<address>`. If the `<address>` template
              already exists, the `--replace` option must be specified
              to overwrite it.
              Details about output templates can be found in the
              `templates` help topic, type `test-app help :templates` to
              learn more about it.

            Arguments and options
              <address>
                  The address of the output template to create, see
                  section "Further Infos" for more information about the
                  syntax.
              <source>
                  The address of the template to copy from, see section
                  "Further Infos" for more information about the syntax.
              -r|--replace
                  This option is required in case you like to replace an
                  already existing output template. Without this option
                  the command will fail in case the output template
                  already exists.

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
