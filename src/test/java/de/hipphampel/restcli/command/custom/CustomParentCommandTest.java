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
package de.hipphampel.restcli.command.custom;

import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND;
import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CustomParentCommandTest extends CommandTestBase {

  CustomParentCommand command;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    CommandAddress address = CommandAddress.fromString("test");

    CommandConfig config = new CommandConfig();
    config.setType(Type.Parent);
    config.setSynopsis("The synopsis of the test command.");
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, "The description of the test command."));
    commandConfigRepository.store(context.configPath(), address, config);

    CommandConfig childConfig1 = new CommandConfig();
    childConfig1.setSynopsis("Child 1 synopsis.");
    childConfig1.setType(Type.Parent);
    commandConfigRepository.store(context.configPath(), address.child("abc"), childConfig1);

    CommandConfig childConfig2 = new CommandConfig();
    childConfig2.setSynopsis("Child 2 synopsis.");
    childConfig2.setType(Type.Parent);
    commandConfigRepository.store(context.configPath(), address.child("def"), childConfig2);

    command = new CustomParentCommand(address, config);
  }

  @Test
  void commandLineSpec() {
    assertThat(command.commandLineSpec()).hasToString(
        new CommandLineSpec(false, CMD_ARG_SUB_COMMAND).toString()
    );
  }

  @ParameterizedTest
  @CsvSource({
      "USAGE,            'test [<sub-command> [<sub-command-args>...]]\n'",
      "DESCRIPTION,      'The description of the test command.\n'",
      "ARGS_AND_OPTIONS, '<sub-command>\n    The name of the sub command to execute. See the list\n    below for the available sub-commands.\n[<sub-command-args>...]\n    Arguments passed to the sub-command.\n'",
      "SUB_COMMANDS,     'abc - Child 1 synopsis.\ndef - Child 2 synopsis.\n'",
  })
  void helpSection(HelpSection section, String expected) {
    Block block = command.helpSection(context, section).orElse(null);
    if (block != null) {
      context.out().block(block);
      assertStdOut(expected);
    } else {
      assertThat(expected).isNull();
    }
  }
}
