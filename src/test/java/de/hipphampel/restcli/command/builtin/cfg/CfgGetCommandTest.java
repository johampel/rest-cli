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
package de.hipphampel.restcli.command.builtin.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.command.CommandTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class CfgGetCommandTest extends CommandTestBase {

  @Inject
  CfgGetCommand command;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr) {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        Arguments.of(
            List.of(),
            true,
            """
                environment        _empty
                request-timeout    30000
                output-template    default
                output-width       80
                output-with-styles true
                """,
            ""
        ),
        Arguments.of(
            List.of("-b"),
            true,
            """
                ┌──────────────────┬───────┐
                │Key               │Value  │
                ├──────────────────┼───────┤
                │environment       │_empty │
                │request-timeout   │30000  │
                │output-template   │default│
                │output-width      │80     │
                │output-with-styles│true   │
                └──────────────────┴───────┘
                """,
            ""
        ),
        Arguments.of(
            List.of("-b", "output-width", "request-timeout", "unknown"),
            true,
            """
                ┌───────────────┬─────┐
                │Key            │Value│
                ├───────────────┼─────┤
                │output-width   │80   │
                │request-timeout│30000│
                └───────────────┴─────┘
                """,
            """
                warning test-app: Unknown configuration key "unknown".
                """
        ),
        Arguments.of(
            List.of("-b", "unknown1", "unknown2"),
            false,
            "",
            """
                warning test-app: Unknown configuration key "unknown1".
                warning test-app: Unknown configuration key "unknown2".
                *** error test-app: None of the given keys is known.
                """
        )
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            get - Gets one or more settings from the application
            configuration.

            Usage
              get [-b|--beautify] [<key>...]

            Description
              Prints the values of one or more application configuration
              settings. When omitting any `<key>` all configuration
              settings are printed, otherwise only the specified ones.

            Arguments and options
              <key>
                  The key of the application configurations setting to
                  print. This argument can be specified more than once
                  in order to print multiple configuration settings. If
                  no `<key>` is specified, all configuration settings
                  are printed.
              -b|--beautify
                  If specified, the output is decorated with a table
                  header describing the meaning of the columns and table
                  borders. Without this option, no table header or
                  borders are printed.

            Further infos
              Known settings
                  The following table gives an overview about the known
                  configuration keys and their meaning:
                  ┌──────────────────┬───────┬─────────────────────────┐
                  │Key               │Type   │Description              │
                  ├──────────────────┼───────┼─────────────────────────┤
                  │environment       │String │The name of the          │
                  │                  │       │environment to use by    │
                  │                  │       │default unless the       │
                  │                  │       │application was started  │
                  │                  │       │with a different one.    │
                  │                  │       │Default value is         │
                  │                  │       │`_empty`, an internal    │
                  │                  │       │empty environment.       │
                  │request-timeout   │Integer│Specifies the request    │
                  │                  │       │timeout for HTTP         │
                  │                  │       │requests, measured in    │
                  │                  │       │milli seconds. The value │
                  │                  │       │from this application    │
                  │                  │       │configuration is used,   │
                  │                  │       │unless a timeout is      │
                  │                  │       │specified at request or  │
                  │                  │       │environment level.       │
                  │default-template  │String │Specifies how to output  │
                  │                  │       │HTTP responses unless    │
                  │                  │       │something more specific  │
                  │                  │       │was given by the request │
                  │                  │       │or the environment. Type │
                  │                  │       │`test-app help :template`│
                  │                  │       │for more information     │
                  │                  │       │about templates.         │
                  │output-width      │Integer│Defines the preferred    │
                  │                  │       │output width. At least   │
                  │                  │       │the output generated by  │
                  │                  │       │the application itself is│
                  │                  │       │restricted to the given  │
                  │                  │       │width as far as possible │
                  │                  │       │- it tries to wrap the   │
                  │                  │       │lines if possible to not │
                  │                  │       │exceed the output width. │
                  │                  │       │Some output may still be │
                  │                  │       │longer. Negative values  │
                  │                  │       │do not limit the output  │
                  │                  │       │in width, so no line     │
                  │                  │       │wrapping will take place.│
                  │                  │       │Default value is `80`.   │
                  │output-with-styles│Boolean│If set to `true`, ANSI   │
                  │                  │       │escape codes are added to│
                  │                  │       │the applications output  │
                  │                  │       │to highlight sections or │
                  │                  │       │key values. If `false`,  │
                  │                  │       │no escape codes are      │
                  │                  │       │emitted. Default is      │
                  │                  │       │`true`.                  │
                  └──────────────────┴───────┴─────────────────────────┘
            """,
        "");
  }
}
