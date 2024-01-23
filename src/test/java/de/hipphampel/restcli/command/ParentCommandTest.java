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
package de.hipphampel.restcli.command;

import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.builtin.BuiltinCommand;
import de.hipphampel.restcli.command.builtin.BuiltinParentCommand;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class ParentCommandTest extends CommandTestBase {

  private TestCommand command;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    command = new TestCommand();
    registerCommand(command);
  }

  @Test
  void commandLineSpec() {
    assertThat(command.commandLineSpec()).hasToString(
        new CommandLineSpec(false, CMD_ARG_SUB_COMMAND).toString()
    );
  }

  @Test
  void execute_commandFound() {
    mockChildCommand(CommandAddress.fromString("the/address/child1"));

    assertExecution(command, List.of("child1", "arg"),
        true,
        """
            Output of the/address/child1: Optional[arg]
            """,
        "");
  }

  @Test
  void execute_commandNotFound() {
    mockChildCommand(CommandAddress.fromString("the/address/child1"));

    assertExecution(command, List.of("unknown", "arg"),
        false,
        "",
        """
            *** error test-app: Unknown sub-command "unknown" for
                                "address".
            Type `test-app help the address` to get a list of available
            sub-commands.
             """);
  }

  @ParameterizedTest
  @CsvSource({
      "USAGE,            'address [<sub-command> [<sub-command-args>...]]\n'",
      "DESCRIPTION,      '`address` is a collection of sub commands and has no\nfunctionality apart from grouping the commands. See the\nfollowing list for the available sub commands.\n'",
      "ARGS_AND_OPTIONS, '<sub-command>\n    The name of the sub command to execute. See the list\n    below for the available sub-commands.\n[<sub-command-args>...]\n    Arguments passed to the sub-command.\n'",
      "SUB_COMMANDS,     'child1 - Command 1\nchild2 - Command 2\n'",
  })
  void helpSection(HelpSection section, String expected) {
    when(mockCommand(CommandAddress.fromString("the/address/child1")).synopsis()).thenReturn("Command 1");
    when(mockCommand(CommandAddress.fromString("the/address/child2")).synopsis()).thenReturn("Command 2");

    Block block = command.helpSection(context, section).orElse(null);
    if (block != null) {
      context.out().block(block);
      assertStdOut(expected);
    } else {
      assertThat(expected).isNull();
    }
  }

  private void mockChildCommand(CommandAddress address) {
    BuiltinCommand command = mockCommand(address);
    Positional arg = new Positional("id", "arg", true, false, null, null);
    when(command.synopsis()).thenReturn("Synopsis for " + address);
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(true, arg));
    when(command.execute(any(), any())).thenAnswer(call -> {
      call.getArgument(0, CommandContext.class).out().linef("Output of %s: %s",
          address,
          call.getArgument(1, CommandLine.class).getValue(arg));
      return true;
    });
  }

  static class TestCommand extends BuiltinParentCommand {

    public TestCommand() {
      super(CommandAddress.fromString("the/address"), "Synopsis");
    }
  }
}
