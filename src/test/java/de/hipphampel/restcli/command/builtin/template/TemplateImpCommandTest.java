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
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import de.hipphampel.restcli.template.TemplateRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
class TemplateImpCommandTest extends CommandTestBase {

  @Inject
  TemplateImpCommand command;
  @Inject
  TemplateRepository templateRepository;
  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    createCommand(CommandAddress.fromString("abc"));
    createTemplate(TemplateAddress.fromString("aTemplate@abc"));
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, String stdIn, boolean expectedResult, String expectedOut, String expectedErr, String expectedAddress,
      String expectedConfig) throws IOException {
    context.in(new ByteArrayInputStream(stdIn.getBytes(StandardCharsets.UTF_8)));
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedAddress != null) {
      assertThat(templateRepository.getTemplate(context.configPath(), TemplateAddress.fromString(expectedAddress))).map(Template::config)
          .contains(
              objectMapper.readValue(expectedConfig, TemplateConfig.class));
    }
  }

  static Stream<Arguments> execute_data() throws IOException {
    String fileName = "target/%s.json".formatted(UUID.randomUUID());
    Files.writeString(Path.of(fileName), """
        {
          "description" : "The description",
          "parameters" : {
            "parameter" : {
              "defaultValue" : "The default value"
            }
          },
          "content" : "The content"
        }""");
    return Stream.of(
        // No args
        Arguments.of(
            List.of(),
            "",
            false,
            "",
            """
                *** error test-app: Missing required argument
                                    "<template-address>".
                usage: test-app template imp [-r|--replace]
                                             <template-address> <source>
                """,
            null,
            null),
        // OK (use parent from import)
        Arguments.of(
            List.of("imported@abc", "@"),
            """
                {
                  "description" : "The description",
                  "parameters" : {
                    "parameter" : {
                      "defaultValue" : "The default value"
                    }
                  },
                  "content" : "The content"
                }""",
            true,
            "",
            "",
            "imported@abc",
            """
                {
                  "description" : "The description",
                  "parameters" : {
                    "parameter" : {
                      "defaultValue" : "The default value"
                    }
                  },
                  "content" : "The content"
                }"""),
        // OK (use file)
        Arguments.of(
            List.of("imported@abc", "@" + fileName),
            "",
            true,
            "",
            "",
            "imported@abc",
            """
                {
                  "description" : "The description",
                  "parameters" : {
                    "parameter" : {
                      "defaultValue" : "The default value"
                    }
                  },
                  "content" : "The content"
                }"""),
        // OK (force replacement)
        Arguments.of(
            List.of("--replace", "aTemplate@abc", "@" + fileName),
            "",
            true,
            "",
            "",
            "aTemplate@abc",
            """
                {
                  "description" : "The description",
                  "parameters" : {
                    "parameter" : {
                      "defaultValue" : "The default value"
                    }
                  },
                  "content" : "The content"
                }"""),
        // FAIL (existing)
        Arguments.of(
            List.of("aTemplate@abc", "@"),
            """
                {
                  "parent" : "child",
                  "variables" : {
                    "xyz" : "var_1"
                  },
                  "headers" : {
                    "uvw" : [ "header_1", "header_2" ]
                  }
                }""",
            false,
            "",
            """
                *** error test-app: Output template "aTemplate@abc" already
                                    exists. Use `--replace` option to allow
                                    replacement.
                """,
            null,
            null),
        // FAIL (command not found)
        Arguments.of(
            List.of("aTemplate@not_found", "@"),
            """
                {
                  "parent" : "child",
                  "variables" : {
                    "xyz" : "var_1"
                  },
                  "headers" : {
                    "uvw" : [ "header_1", "header_2" ]
                  }
                }""",
            false,
            "",
            """
                *** error test-app: Command "not_found" does not exist.
                """,
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            imp - Imports an output template.

            Usage
              imp [-r|--replace] <template-address> <source>

            Description
              Imports an output template from `<source>` and stores it
              at address `<template-address>`. The content of `<source>`
              is typically produced by the `exp` command.

            Arguments and options
              <template-address>
                  The address of the template you want to create by this
                  import. See section "Further Infos" for more
                  information about the syntax.
              <source>
                  The source file where to read the output template
                  settings from. This is specified as an input source,
                  see section "Further Infos" for more information about
                  the syntax.
              -r | --replace
                  The option allows to overwrite existing output
                  templates. If the target output template already
                  exists, this option must be set in order to import the
                  data into it. If the target output template not
                  exists, this option has no effect.

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