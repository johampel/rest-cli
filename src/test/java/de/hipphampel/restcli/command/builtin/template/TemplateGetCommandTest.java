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

import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import de.hipphampel.restcli.template.TemplateConfig.Parameter;
import de.hipphampel.restcli.template.TemplateRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class TemplateGetCommandTest extends CommandTestBase {

  @Inject
  TemplateGetCommand command;
  @Inject
  TemplateRepository templateRepository;


  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    Template template = new Template(
        TemplateAddress.fromString("some_template@"),
        new TemplateConfig(
            "The description",
            Map.of("parameter1", new Parameter("The default value1"),
                "parameter2", new Parameter("The default value2")),
            """
                The content:
                - line 1
                - line 2
                """),
        false);
    templateRepository.storeTemplate(context.configPath(), template, false);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedFileName,
      String expectedEnv) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedFileName != null) {
      assertThat(Files.readString(Path.of(expectedFileName))).isEqualTo(expectedEnv);
    }
  }

  static Stream<Arguments> execute_data() {
    String fileName = "target/%s.json".formatted(UUID.randomUUID());
    return Stream.of(
        // No args
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument
                                    "<template-address>".
                usage: test-app template get [-b|--beautify] [-c|--content]
                                             [-v|--variables]
                                             <template-address>
                """,
            null,
            null),
        // Template not found
        Arguments.of(
            List.of("unknown@"),
            false,
            "",
            """
                *** error test-app: Output template "unknown@" does not
                                    exist.
                """,
            null,
            null),
        // Template all
        Arguments.of(
            List.of("-b", "some_template@"),
            true,
            """
                Variables:
                ┌──────────┬──────────────────┐
                │Name      │Default value     │
                ├──────────┼──────────────────┤
                │parameter1│The default value1│
                │parameter2│The default value2│
                └──────────┴──────────────────┘
                Content:
                The content:
                - line 1
                - line 2
                                
                """,
            "",
            null,
            null),
        // Template (variables)
        Arguments.of(
            List.of("-v", "some_template@"),
            true,
            """
                parameter1 = The default value1
                parameter2 = The default value2
                """,
            "",
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            get - Gets the content and variables of an output template.

            Usage
              get [-b|--beautify] [-c|--content] [-v|--variables]
                  <template-address>

            Description
              Gets the template definition and its variables.
              Depending on the options provided, both content of the
              template and variables are shown only one of both.

            Arguments and options
              <template-address>
                  The address of the template you want to get. See
                  section "Further Infos" for more information about the
                  syntax.
              -b | --beautify
                  Add some decorations to the output to let look it more
                  nicely.
              -c | --content
                  If this option is set (and no `--variables` option is
                  given), only the content of the template is shown. If
                  both options, `--content` and `--variables`, are
                  omitted, then variables and content are shown.
              -v | --variables
                  If this option is set (and no `--content` option is
                  given), only the variables of the template is shown.
                  If both options, `--content` and `--variables`, are
                  omitted, then variables and content are shown.

            Further infos
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