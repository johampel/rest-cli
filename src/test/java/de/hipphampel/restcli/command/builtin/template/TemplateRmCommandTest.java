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

import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
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
class TemplateRmCommandTest extends CommandTestBase {

  @Inject
  TemplateRmCommand command;
  @Inject
  TemplateRepository templateRepository;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    this.rootDir = rootDir;
    createCommand(CommandAddress.fromString("abc"));
    createCommand(CommandAddress.fromString("abc/ghi"));
    createTemplate(TemplateAddress.fromString("abc@"));
    createTemplate(TemplateAddress.fromString("def@"));
    createTemplate(TemplateAddress.fromString("def@abc"));
    createTemplate(TemplateAddress.fromString("ghi@abc"));
    createTemplate(TemplateAddress.fromString("ghi@abc/ghi"));
    createTemplate(TemplateAddress.fromString("jkl@abc/ghi"));
    createTemplate(TemplateAddress.fromString("mno@"));
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedTemplates) {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);

    assertThat(templateRepository.getAllTemplates(context.configPath())).isEqualTo(TestUtils.stringToList(expectedTemplates).stream()
        .map(TemplateAddress::fromString)
        .toList());
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
                usage: test-app template rm [-f|--force] [-r|--recursive]
                                            <address>
                """,
            "abc@;def@;def@abc;ghi@abc;ghi@abc/ghi;jkl@abc/ghi;mno@"),
        // OK existing template
        Arguments.of(
            List.of("ghi@abc"),
            true,
            "",
            "",
            "abc@;def@;def@abc;ghi@abc/ghi;jkl@abc/ghi;mno@"),
        // Fail: Template not found
        Arguments.of(
            List.of("not_found@abc"),
            false,
            "",
            """
                *** error test-app: Output template "not_found@abc" does not
                                    exists.
                """,
            "abc@;def@;def@abc;ghi@abc;ghi@abc/ghi;jkl@abc/ghi;mno@"),
        // FAIL: command without force
        Arguments.of(
            List.of("abc"),
            false,
            "",
            """
                *** error test-app: Deleting output templates of command
                                    "abc" requires `--force` option.
                """,
            "abc@;def@;def@abc;ghi@abc;ghi@abc/ghi;jkl@abc/ghi;mno@"),
        // FAIL: command not found
        Arguments.of(
            List.of("not_found"),
            false,
            "",
            """
                *** error test-app: Command "not_found" not found.
                """,
            "abc@;def@;def@abc;ghi@abc;ghi@abc/ghi;jkl@abc/ghi;mno@"),
        // OK: command with force
        Arguments.of(
            List.of("-f", "abc"),
            true,
            "",
            "",
            "abc@;def@;ghi@abc/ghi;jkl@abc/ghi;mno@"),
        // OK: command with force and recursive
        Arguments.of(
            List.of("-rf", "abc"),
            true,
            "",
            "",
            "abc@;def@;mno@")

    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            rm - Removes one or more output templates.

            Usage
              rm [-f|--force] [-r|--recursive] <address>

            Description
              `<address>` is either an output template address or a
              command address. If it is an output template address, this
              command deletes exactly this template. If it is a command
              address, it deletes all output template address associated
              with this command. Note that in case you delete all output
              templates for a command, you have to use the `--force`
              option.

            Arguments and options
              <address>
                  The `<address>` of the command or output template (see
                  section "Further Infos" for information about the
                  syntax). Note if it is a command address, it deletes
                  all output templates directly associated with this
                  command; if you also want to delete the output
                  templates of the sub commands, use the `--recursive`
                  option. In any case, if specifying a command address,
                  you have to use the `--force` option.
              -f | --force
                  Force deletion of the selected output templates. This
                  is mandatory if `<address>` is a command address.
              -r | --recursive
                  If `<address>` is a command address, this option also
                  deletes all templates of the sub-commands. If omitted,
                  only the output templates of the command itself are
                  deleted.

            Further infos
              Command addresses:
              A command address uniquely identifies a command in
              test-app. It is basically a path the command, so `foo/bar`
              refers to the command `bar` that is a child command of
              `foo`. The special empty path (``) refers to the root
              command that represents the application itself. For
              details, please type `test-app help :addresses`.
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

  private void createCommand(CommandAddress address) {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Parent);
    commandConfigRepository.store(context.configPath(), address, config);
  }

  private void createTemplate(TemplateAddress address) {
    Template template = new Template(
        address,
        new TemplateConfig("foo", Map.of(), ""),
        false);

    templateRepository.storeTemplate(context.configPath(), template, true);
  }
}
