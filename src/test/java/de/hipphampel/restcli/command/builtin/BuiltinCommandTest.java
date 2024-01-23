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
package de.hipphampel.restcli.command.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class BuiltinCommandTest extends CommandTestBase {

  private final BuiltinCommand command = new BuiltinCommand(
      CommandAddress.fromString("cmd"),
      "THE SYNOPSIS",
      new CommandLineSpec(true, new Positional("id", "foo", false, false, null, null)),
      Map.of(HelpSection.FURTHER_INFOS, CommandUtils.helpSection("FURTHER"))) {

    @Override
    public boolean execute(CommandContext context, CommandLine commandLine) {
      return false;
    }
  };

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    registerCommand(command);
    mockCommand(CommandAddress.fromString("parent/child"));
  }

  @Test
  void address() {
    assertThat(command.address()).isEqualTo(CommandAddress.fromString("cmd"));
  }

  @Test
  void synopsis() {
    assertThat(command.synopsis()).isEqualTo("THE SYNOPSIS");
  }

  @Test
  void showHelp() {

    command.showHelp(context, context.out());
    assertOutput("""
            cmd - THE SYNOPSIS
                        
            Usage
              cmd foo
                        
            Description
              No help available.
                        
            Further infos
              FURTHER
            """,
        "");
  }
}
