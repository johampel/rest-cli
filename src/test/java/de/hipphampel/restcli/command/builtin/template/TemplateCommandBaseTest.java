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

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import de.hipphampel.restcli.template.TemplateConfig.Parameter;
import de.hipphampel.restcli.template.TemplateRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class TemplateCommandBaseTest extends CommandTestBase {

  TemplateCommandBase command;
  @Inject
  TemplateRepository templateRepository;
  final CommandAddress commandAddress = CommandAddress.fromString("test/command");
  final Template existingTemplate = new Template(
      TemplateAddress.fromString("existing@" + commandAddress),
      new TemplateConfig("existing description", Map.of("foo", new Parameter("bar")), "content sample"), false);

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    this.rootDir = rootDir;
    templateRepository.storeTemplate(context.configPath(), existingTemplate, false);
    CommandConfig config = new CommandConfig();
    config.setType(Type.Parent);
    commandConfigRepository.store(context.configPath(), CommandAddress.fromString("test/command"), config);
    this.command = new TemplateCommandBase("test", "", new CommandLineSpec(false), Map.of()) {
      @Override
      public boolean execute(CommandContext context, CommandLine commandLine) {
        return false;
      }
    };
    this.command.templateRepository = templateRepository;
    this.command.commandRepository = commandRepository;
  }

  @ParameterizedTest
  @CsvSource({
      "notexisting@test/command, false,",
      "existing@test/command,    false, 'Output template \"existing\" for command \"test/command\" already exists. Use `--replace` option if you wish to overwrite.'",
      "existing@test/command,    true,  ",
      "notexisting@not/found,    true,  'Command \"not/found\" does not exist.'",
      "notexisting@env,          true,  'Command \"env\" is not suitable for output templates.'",
      "existing@,                false,  ",
  })
  void checkWriteableTemplateAddress(String address, boolean replace, String expectedError) {
    try {
      command.checkWriteableTemplateAddress(context, TemplateAddress.fromString(address), replace);
      assertThat(expectedError).isNull();
    }
    catch(ExecutionException e) {
      assertThat(e.getMessage()).isEqualTo(expectedError);
    }
  }
}
