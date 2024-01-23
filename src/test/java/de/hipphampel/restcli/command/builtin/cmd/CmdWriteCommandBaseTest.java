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

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandInfo;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CmdWriteCommandBaseTest extends CommandTestBase {

  CmdWriteCommandBase command;

  @BeforeEach
  public void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    command = new CmdWriteCommandBase("foo", "ignore", new CommandLineSpec(true), Map.of()) {
      @Override
      public boolean execute(CommandContext context, CommandLine commandLine) {
        return false;
      }
    };
    command.commandConfigRepository = commandConfigRepository;
    command.commandRepository = commandRepository;

    CommandConfig config = new CommandConfig();
    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("abc"), config);

  }

  @ParameterizedTest
  @CsvSource({
      "'',             'The root command is not a valid address for a custom command.'",
      "'env',          '\"env\" is a builtin command.'",
      "'env/abc',      'The builtin command \"env\" cannot have a custom child command.'",
      "'notfound/abc', 'Command address \"notfound/abc\" refers to not existing parent \"notfound\".'",
      "'def', ",
      "'abc/def', ",
  })
  void checkIfValidCustomCommandAddress(String address, String message) {
    try {
      command.checkIfValidCustomCommandAddress(context, CommandAddress.fromString(address));
      assertThat(message).isNull();
    } catch (ExecutionException e) {
      assertThat(e).hasMessage(message);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'notfound', false, false, false, ",
      "'abc',      false, false, false, 'Command \"abc\" already exists - use --replace option to enforce replacement.'",
      "'abc',      true,  false, false, 'Command \"abc\" has sub-commands - use --force option to enforce removal.'",
      "'abc',      true,  true,  true, ",
      "'abc/def',  false, false, false, 'Command \"abc/def\" already exists - use --replace option to enforce replacement.'",
      "'abc/def',  true,  false, true, ",
  })
  void checkIfReplacementValid(String address, boolean replace, boolean force, boolean present, String message) {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("abc"), config);
    storeCommand(CommandAddress.fromString("abc/def"), config);
    try {
      Optional<CommandInfo> info = command.checkIfReplacementValid(context,
          CommandAddress.fromString(address), replace, force);
      assertThat(info.isPresent()).isEqualTo(present);
    } catch (ExecutionException e) {
      assertThat(e).hasMessage(message);
    }
  }
}
