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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.cli.format.ParagraphBlock;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CommandTest extends CommandTestBase {

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @ParameterizedTest
  @CsvSource({
      "USAGE,            'address [-l|--long foo]... arg\n'",
      "DESCRIPTION,      'No help available.\n'",
      "ARGS_AND_OPTIONS, ",
      "SUB_COMMANDS,     ",
      "FURTHER_INFOS,    ",
  })
  void helpSection(HelpSection section, String expected) {
    Command command = mock(Command.class);
    when(command.helpSection(any(), any())).thenCallRealMethod();
    when(command.address()).thenReturn(CommandAddress.fromString("the/address"));
    when(command.name()).thenReturn("address");
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(
        true,
        option("-l", "--long").repeatable().parameter(positional("foo").build()).build(),
        positional("arg").build()
    ));

    Block block = command.helpSection(context, section).orElse(null);
    if (block != null) {
      context.out().block(block);
      assertStdOut(expected);
    } else {
      assertThat(expected).isNull();
    }
  }

  @Test
  void showHelp_theoretically() {
    context.out(context.out().withStyles(true));
    Command command = mock(Command.class);
    doCallRealMethod().when(command).showHelp(any(), any());
    when(command.address()).thenReturn(CommandAddress.fromString("the/address"));
    when(command.name()).thenReturn("address");
    when(command.synopsis()).thenReturn("synopsis");
    when(command.helpSection(any(), any())).thenAnswer(
        call -> Optional.of(new ParagraphBlock("The " + call.getArgument(1, HelpSection.class).getName())));

    command.showHelp(context, context.out());
    assertOutput(
        """
            \033[0;1maddress\033[0m - synopsis
                        
            \033[0;1mUsage\033[0m
              The Usage
                        
            \033[0;1mDescription\033[0m
              The Description
                        
            \033[0;1mArguments and options\033[0m
              The Arguments and options
                        
            \033[0;1mAvailable sub-commands\033[0m
              The Available sub-commands
                        
            \033[0;1mFurther infos\033[0m
              The Further infos
            """,
        "");
  }

  @Test
  void showHelp_reallyFilled() {
    Command command = mock(Command.class);
    doCallRealMethod().when(command).showHelp(any(), any());
    when(command.helpSection(any(), any())).thenCallRealMethod();
    when(command.address()).thenReturn(CommandAddress.fromString("the/address"));
    when(command.name()).thenReturn("address");
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(
        true,
        option("-l", "--long").repeatable().parameter(positional("foo").build()).build(),
        positional("arg").build()
    ));
    when(command.synopsis()).thenReturn("synopsis");

    command.showHelp(context, context.out());
    assertOutput(
        """
            address - synopsis
                                        
            Usage
              address [-l|--long foo]... arg

            Description
              No help available.
            """,
        "");
  }
}
