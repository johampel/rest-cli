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

import de.hipphampel.restcli.command.CommandAddress;
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
class BuiltinParentCommandTest extends CommandTestBase {

  private final BuiltinParentCommand command = new BuiltinParentCommand(
      CommandAddress.fromString("parent"),
      "THE SYNOPSIS",
      Map.of(HelpSection.FURTHER_INFOS, CommandUtils.helpSection("FURTHER"))
  );

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    registerCommand(command);
    mockCommand(CommandAddress.fromString("parent/child"));
  }

  @Test
  void address() {
    assertThat(command.address()).isEqualTo(CommandAddress.fromString("parent"));
  }

  @Test
  void synopsis() {
    assertThat(command.synopsis()).isEqualTo("THE SYNOPSIS");
  }

  @Test
  void showHelp() {

    command.showHelp(context, context.out());
    assertOutput("""
            parent - THE SYNOPSIS

            Usage
              parent [<sub-command> [<sub-command-args>...]]

            Description
              `parent` is a collection of sub commands and has no
              functionality apart from grouping the commands. See the
              following list for the available sub commands.

            Arguments and options
              <sub-command>
                  The name of the sub command to execute. See the list
                  below for the available sub-commands.
              [<sub-command-args>...]
                  Arguments passed to the sub-command.

            Available sub-commands
              child - null

            Further infos
              FURTHER
            """,
        "");
  }
}
