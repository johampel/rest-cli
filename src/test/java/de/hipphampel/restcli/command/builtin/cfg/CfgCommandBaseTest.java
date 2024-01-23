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

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.builtin.cfg.CfgCommandBase.ConfigKey;
import de.hipphampel.restcli.config.ApplicationConfig;
import de.hipphampel.restcli.env.EnvironmentRepository;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CfgCommandBaseTest extends CommandTestBase {

  private CfgCommandBase command;

  @Inject
  EnvironmentRepository environmentRepository;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    command = new CfgCommandBase("test", "test", new CommandLineSpec(true), Map.of()) {
      @Override
      public boolean execute(CommandContext context, CommandLine commandLine) {
        return false;
      }
    };
    command.environmentRepository = environmentRepository;
  }


  @ParameterizedTest
  @CsvSource({
      "foo,",
      "output-width, output-width"
  })
  void getKey(String key, String expected) {
    assertThat(CfgCommandBase.getKey(key).map(ConfigKey::getName).orElse(null)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'unknown', false, 'Environment \"unknown\" does not exist.'",
      "'_empty',  true, "
  })
  void setEnvironment(String value, boolean expectException, String expectedMessage) {
    ApplicationConfig config = new ApplicationConfig();
    try {
      command.setEnvironment(context, config, value);
      assertThat(config.getEnvironment()).hasToString(value);
      assertThat(expectException).isTrue();
    } catch (ExecutionException ee) {
      assertThat(ee.getMessage()).isEqualTo(expectedMessage);
      assertThat(expectException).isFalse();
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'bad',  false, '\"bad\" is not an integer.'",
      "'123',  true, "
  })
  void setOutputWidth(String value, boolean expectException, String expectedMessage) {
    ApplicationConfig config = new ApplicationConfig();
    try {
      command.setOutputWidth(context, config, value);
      assertThat(config.getOutputWidth()).hasToString(value);
      assertThat(expectException).isTrue();
    } catch (ExecutionException ee) {
      assertThat(ee.getMessage()).isEqualTo(expectedMessage);
      assertThat(expectException).isFalse();
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'bad',  false, '\"bad\" is not a long.'",
      "'123',  true, "
  })
  void setRequestTimeout(String value, boolean expectException, String expectedMessage) {
    ApplicationConfig config = new ApplicationConfig();
    try {
      command.setRequestTimeout(context, config, value);
      assertThat(config.getRequestTimeout()).hasToString(value);
      assertThat(expectException).isTrue();
    } catch (ExecutionException ee) {
      assertThat(ee.getMessage()).isEqualTo(expectedMessage);
      assertThat(expectException).isFalse();
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'string:123', true, "
  })
  void setOutputTemplate(String value, boolean expectException, String expectedMessage) {
    ApplicationConfig config = new ApplicationConfig();
    try {
      command.setOutputTemplate(context, config, value);
      assertThat(config.getOutputTemplate()).hasToString(value);
      assertThat(expectException).isTrue();
    } catch (ExecutionException ee) {
      assertThat(ee.getMessage()).isEqualTo(expectedMessage);
      assertThat(expectException).isFalse();
    }
  }
}
