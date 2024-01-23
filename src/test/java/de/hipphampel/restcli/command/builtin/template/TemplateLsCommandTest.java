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
class TemplateLsCommandTest extends CommandTestBase {

  @Inject
  TemplateLsCommand command;
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
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr) {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            true,
            """
                abc
                def
                default
                mno
                """,
            ""),
        // FAIL: Command not found
        Arguments.of(
            List.of("not/found"),
            false,
            "",
            """
                *** error test-app: No such command "not/found\"
                """),
        // OK: Beautified with details
        Arguments.of(
            List.of("-db", "abc"),
            true,
            """
                ┌───────┬──────────────┬───────┐
                │Name   │Owning Command│Type   │
                ├───────┼──────────────┼───────┤
                │abc    │<global>      │custom │
                │def    │abc           │custom │
                │default│<global>      │builtin│
                │ghi    │abc           │custom │
                │mno    │<global>      │custom │
                └───────┴──────────────┴───────┘
                """,
            ""),
        // OK: Beautified with details, local
        Arguments.of(
            List.of("-dbl", "abc"),
            true,
            """
                ┌────┬──────────────┬──────┐
                │Name│Owning Command│Type  │
                ├────┼──────────────┼──────┤
                │def │abc           │custom│
                │ghi │abc           │custom│
                └────┴──────────────┴──────┘
                """,
            "")

    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            ls - Lists available output templates.

            Usage
              ls [-l|--local] [-d|--details [-b|--beautify]]
                 [<command-address>]

            Description
              Lists the output templates that are available for the
              given command.

            Arguments and options
              <command-address>
                  The command address of the command the output
                  templates should be listed for. See section "Further
                  Infos" for more information about the syntax.
                  If omitted, the global output templates are listed.
              -d | --details
                  Show details for the output templates being listed,
                  such as the type of the command (builtin or custom)
                  and for which command the output template was actually
                  defined.
                  -b | --beautify
                      The `--beautify` option is only available in
                      combination with the `--details` option. If set,
                      the listing is enriched with additional
                      decorations.
              -l | --local
                  If set, it shows only those output templates that are
                  defined exactly for the given `<command-address>`. If
                  omitted, also the output templates inherited from the
                  parent commands are shown.

            Further infos
              Command addresses:
              A command address uniquely identifies a command in
              test-app. It is basically a path the command, so `foo/bar`
              refers to the command `bar` that is a child command of
              `foo`. The special empty path (``) refers to the root
              command that represents the application itself. For
              details, please type `test-app help :addresses`.
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
