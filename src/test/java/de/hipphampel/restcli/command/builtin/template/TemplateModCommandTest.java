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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.config.CommandConfig;
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
class TemplateModCommandTest extends CommandTestBase {

  @Inject
  TemplateModCommand command;
  @Inject
  ObjectMapper objectMapper;
  @Inject
  TemplateRepository templateRepository;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    this.rootDir = rootDir;
    createCommand(CommandAddress.fromString("abc"));
    createTemplate(TemplateAddress.fromString("abc@"));
    createTemplate(TemplateAddress.fromString("def@abc"));
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedAddress,
      String expectedTemplate) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedAddress != null) {
      assertThat(templateRepository.getTemplate(context.configPath(), TemplateAddress.fromString(expectedAddress)).orElseThrow().config())
          .isEqualTo(objectMapper.readValue(expectedTemplate, TemplateConfig.class));
    }
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // No args
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<address>".
                usage: test-app template mod [-d|--description
                                             <description>] [-v|--variable
                                             key=value]...
                                             [-V|--remove-variable
                                             <variable>]... <address>
                                             [<template>]
                """,
            null,
            null),
        // Not found
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
        // Some changes
        Arguments.of(
            List.of("-Vparam1", "-vparam2=value", "-dNew Description", "def@abc", "New Content"),
            true,
            "",
            "",
            "def@abc",
            """
                {
                  "description" : "New Description",
                  "parameters" : {
                    "param2" : {
                      "defaultValue": "value"
                    }
                  },
                  "content" : "New Content"
                }
                """)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            mod - Modifies an output template.

            Usage
              mod [-d|--description <description>] [-v|--variable
                  key=value]... [-V|--remove-variable <variable>]...
                  <address> [<template>]

            Description
              Allows to modify the existing output template `<address>`.
              It is possible to modify it content, parameters and
              description; in order to rename it, use the `mv` command
              instead,

            Arguments and options
              <address>
                  The output template to change, see section "Further
                  Infos" for more information about the syntax.
              <template>
                  The output template content. It can be omitted in case
                  you do not want to change the content itself. This is
                  specified as an input source, see section "Further
                  Infos" for more information about the syntax.
              -d | --description <description>
                  The new description of the output template content.
                  This is specified as an input source, see section
                  "Further Infos" for more information about the syntax.
              -v | --variable <key>=<value>
                  Defines the variable `<key>` having given the default
                  value `<value>`.
              -V | --remove-variable <variable>
                  Remove the given variable.

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

  private void createCommand(CommandAddress address) {
    commandConfigRepository.store(context.configPath(), address, new CommandConfig());
  }

  private void createTemplate(TemplateAddress address) {
    Template template = new Template(
        address,
        new TemplateConfig("foo", Map.of("param1", new Parameter(address + " value")), "content of " + address),
        false);

    templateRepository.storeTemplate(context.configPath(), template, true);
  }

}