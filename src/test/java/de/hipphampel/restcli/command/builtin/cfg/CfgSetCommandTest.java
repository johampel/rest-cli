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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.config.ApplicationConfig;
import de.hipphampel.restcli.config.ApplicationConfigRepository;
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
class CfgSetCommandTest extends CommandTestBase {

  @Inject
  CfgSetCommand command;

  @Inject
  ObjectMapper objectMapper;

  @Inject
  ApplicationConfigRepository applicationConfigRepository;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(String existingConfig, List<String> args, boolean expectedResult, String expectedOut, String expectedErr,
      String expectedConfig)
      throws JsonProcessingException {
    applicationConfigRepository.store(context.configPath(), objectMapper.readValue(existingConfig, ApplicationConfig.class));
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    assertThat(objectMapper.writeValueAsString(applicationConfigRepository.getOrCreate(context.configPath()))).isEqualTo(expectedConfig);
  }

  static Stream<Arguments> execute_data() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return Stream.of(
        // No arguments
        Arguments.of(
            objectMapper.writeValueAsString(new ApplicationConfig()),
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument
                                    "<key>[=<value>]".
                usage: test-app cfg set <key>[=<value>]...
                """,
            objectMapper.writeValueAsString(new ApplicationConfig())),
        // Ok
        Arguments.of(
            objectMapper.writeValueAsString(new ApplicationConfig()
                .setOutputWidth(1)
                .setOutputTemplate("2")
                .setOutputWithStyles(false)),
            List.of("output-width=10", "output-with-styles", "output-template=foo"),
            true,
            "",
            "",
            objectMapper.writeValueAsString(new ApplicationConfig()
                .setOutputWidth(10)
                .setOutputTemplate("foo")
                .setOutputWithStyles(true))),
        // Bad key
        Arguments.of(
            objectMapper.writeValueAsString(new ApplicationConfig()
                .setOutputWidth(1)
                .setOutputTemplate("2")
                .setOutputWithStyles(false)),
            List.of("output-width=10", "unknown"),
            false,
            "",
            """
                *** error test-app: No such configuration key "unknown".
                """,
            objectMapper.writeValueAsString(new ApplicationConfig()
                .setOutputWidth(1)
                .setOutputTemplate("2")
                .setOutputWithStyles(false)))
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            set - Sets or resets one or more application configuration
            settings.

            Usage
              set <key>[=<value>]...

            Description
              Sets or resets the values of one or more application
              configuration settings.

            Arguments and options
              <key>[=<value>]
                  Either sets or resets a application configuration
                  setting to its default value. The argument comes in
                  two forms. If the argument has the format
                  `<key>=<value>` it sets the application configuration
                  `<key>` to `<value>`. If the value is omitted, so only
                  `<key>` without an equal sign is specified, the
                  application configuration setting will be reset the to
                  application default value.

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
