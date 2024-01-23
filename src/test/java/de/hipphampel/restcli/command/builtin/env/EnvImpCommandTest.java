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
package de.hipphampel.restcli.command.builtin.env;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentConfig;
import de.hipphampel.restcli.env.EnvironmentRepository;
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
class EnvImpCommandTest extends CommandTestBase {

  @Inject
  EnvImpCommand command;
  @Inject
  EnvironmentRepository environmentRepository;
  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    Environment parent = environmentRepository.createTransientEnvironment("parent", null);
    parent.setLocalVariables(Map.of(
        "abc", "parent_1",
        "def", "parent_2"
    ));
    parent.setLocalHeaders(Map.of(
        "ghi", List.of("parent_3", "parent_4"),
        "jkl", List.of("parent_5", "parent_6")
    ));
    environmentRepository.storeEnvironment(context.configPath(), parent, false);
    Environment child = environmentRepository.createTransientEnvironment("child", parent);
    child.setLocalVariables(Map.of(
        "def", "child_1",
        "ghi", "child_2"
    ));
    child.setLocalHeaders(Map.of(
        "jkl", List.of("child_3", "child_4"),
        "mno", List.of("child_5", "child_6")
    ));
    environmentRepository.storeEnvironment(context.configPath(), child, false);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, String stdIn, boolean expectedResult, String expectedOut, String expectedErr, String expectedName,
      String expectedConfig) throws IOException {
    context.in(new ByteArrayInputStream(stdIn.getBytes(StandardCharsets.UTF_8)));
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedName != null) {
      assertThat(environmentRepository.getConfig(context.configPath(), expectedName)).contains(
          objectMapper.readValue(expectedConfig, EnvironmentConfig.class));
    }
  }

  static Stream<Arguments> execute_data() throws IOException {
    String fileName = "target/%s.json".formatted(UUID.randomUUID());
    Files.writeString(Path.of(fileName), """
        {
          "parent" : "child",
          "variables" : {
            "xyz" : "var_1"
          },
          "headers" : {
            "uvw" : [ "header_1", "header_2" ]
          }
        }""");
    return Stream.of(
        // No args
        Arguments.of(
            List.of(),
            "",
            false,
            "",
            """
                *** error test-app: Missing required argument "<name>".
                usage: test-app env imp [-r|--replace] [(-p|--parent
                                        <parent>) | --ignore-parent |
                                        -n|--no-parent] <name> <source>
                """,
            null,
            null),
        // OK (use parent from import)
        Arguments.of(
            List.of("imported", "@"),
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
            true,
            "",
            "",
            "imported",
            """
                {
                  "parent" : "child",
                  "variables" : {
                    "xyz" : "var_1"
                  },
                  "headers" : {
                    "uvw" : [ "header_1", "header_2" ]
                  }
                }"""),
        // OK (use file)
        Arguments.of(
            List.of("imported", "@" + fileName),
            "",
            true,
            "",
            "",
            "imported",
            """
                {
                  "parent" : "child",
                  "variables" : {
                    "xyz" : "var_1"
                  },
                  "headers" : {
                    "uvw" : [ "header_1", "header_2" ]
                  }
                }"""),
        // OK (no parent)
        Arguments.of(
            List.of("--no-parent", "imported", "@"),
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
            true,
            "",
            "",
            "imported",
            """
                {
                  "parent" : null,
                  "variables" : {
                    "xyz" : "var_1"
                  },
                  "headers" : {
                    "uvw" : [ "header_1", "header_2" ]
                  }
                }"""),
        // OK (ignore parent, not existing)
        Arguments.of(
            List.of("--ignore-parent", "imported", "@"),
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
            true,
            "",
            "",
            "imported",
            """
                {
                  "parent" : null,
                  "variables" : {
                    "xyz" : "var_1"
                  },
                  "headers" : {
                    "uvw" : [ "header_1", "header_2" ]
                  }
                }"""),
        // OK (ignore parent, existing)
        Arguments.of(
            List.of("--replace", "--ignore-parent", "child", "@"),
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
            true,
            "",
            "",
            "child",
            """
                {
                  "parent" : null,
                  "variables" : {
                    "xyz" : "var_1"
                  },
                  "headers" : {
                    "uvw" : [ "header_1", "header_2" ]
                  }
                }"""),
        // FAIL (existing)
        Arguments.of(
            List.of("child", "@"),
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
                *** error test-app: Environment "child" already exists - use
                                    the --replace option if you want to
                                    overwrite it.
                """,
            null,
            null)
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            imp - Imports an environment.

            Usage
              imp [-r|--replace] [(-p|--parent <parent>) |
                  --ignore-parent | -n|--no-parent] <name> <source>

            Description
              Imports an environment based on the data that was
              previously exported via the `exp` command.
              The environment settings are read from the given
              `<source>` input source. If the target environment already
              exists, the `--replace` option must be set in order to
              overwrite the settings.
              Depending on the `--parent`, `--no-parent`, or
              `--ignore-parent` option the parent reference of the
              environment is set to the value found in the exported
              data, taken from the command line, or is unset.

            Arguments and options
              <name>
                  The name of the environment to import to. If the
                  environment not exist yet, it is created, if it
                  already exists, the `--replace` option must be set in
                  order to overwrite the existing settings.
              <source>
                  The source where to read the environment settings
                  from. This is specified as an input source (see
                  section "Further Infos").
              --ignore-parent
                  Ignores the parent information from the imported
                  settings. If the target environment does not exist
                  yet, the target environment is created without a
                  parent. If the target environment already exists, the
                  parent is left unchanged. This option mutual excludes
                  the options `--parent` and `--no-parent`.
              --no-parent
                  Ensures that the target environment has no parent
                  after the import. If the target environment already
                  exists, the reference to the parent is removed. This
                  option mutual excludes the options `--parent` and
                  `--ignore-parent`.
              -p | --parent <parent>
                  Sets the parent reference of the target environment to
                  `<parent>` independent from the settings found in
                  imported data or the maybe already existing target
                  environment. This option mutual excludes the options
                  `--no-parent` and `--ignore-parent`.
              -r | --replace
                  The option allows to overwrite existing environments.
                  If the target environment already exists, this option
                  must be set in order to import the data into it. If
                  the target environment not exists, this option has no
                  effect.

            Further infos
              Input source:
              An input source is basically a string literal, which
              represents either a string, refers to a file, an URL, or a
              builtin resource. Detailed information can be obtained by
              typing `test-app help :input-sources`.
            """,
        "");
  }

}