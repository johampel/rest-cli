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
package de.hipphampel.restcli.command.builtin.cmd;

import de.hipphampel.restcli.command.CommandTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class CmdCommandParentTest extends CommandTestBase {

  @Inject
  CmdCommandParent command;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            cmd - Collection of commands to manage custom commands.

            Usage
              cmd [<sub-command> [<sub-command-args>...]]

            Description
              This is a collection of commands to manage custom
              commands. A custom command is a command to execute an HTTP
              request, an alias or a group of other custom commands.
              `cmd` is a collection of sub commands and has no
              functionality apart from grouping the commands. See the
              following list for the available sub commands.

            Arguments and options
              <sub-command>
                  The name of the sub command to execute. See the list
                  below for the available sub-commands.
              [<sub-command-args>...]
                  Arguments passed to the sub-command.

            Available sub-commands
              alias   - Creates a new alias command.
              cp      - Copies a command (tree).
              exp     - Exports a command (tree).
              group   - Creates a new group command.
              http    - Creates a new HTTP command.
              imp     - Imports a command (tree).
              mod     - Modifies an existing custom command
              mv      - Moves a command (tree).
              openapi - Imports commands from an OpenAPI spec.
              rm      - Deletes commands.
              tree    - Shows a tree of the available commands.
            """,
        "");
  }

}
