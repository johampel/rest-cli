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

import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandInfo;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfigTree;
import de.hipphampel.restcli.exception.ExecutionException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public abstract class CmdWriteCommandBase extends CmdCommandBase {

  protected CmdWriteCommandBase(String name, String synopsis, CommandLineSpec commandLineSpec,
      Map<HelpSection, Function<CommandContext, Block>> helpSections) {
    super(name, synopsis, commandLineSpec, helpSections);
  }

  protected void checkIfValidCustomCommandAddress(CommandContext context, CommandAddress address) {
    if (address.isRoot()) {
      throw new ExecutionException("The root command is not a valid address for a custom command.");
    }
    if (commandRepository.getCommandInfo(context.configPath(), address).map(CommandInfo::builtin).orElse(false)) {
      throw new ExecutionException("\"%s\" is a builtin command.".formatted(address));
    }
    CommandAddress parent = address.parent();
    if (parent.isRoot()) {
      return;
    }

    CommandInfo parentInfo = commandRepository.getCommandInfo(context.configPath(), parent)
        .orElseThrow(
            () -> new ExecutionException("Command address \"%s\" refers to not existing parent \"%s\".".formatted(address, parent)));

    if (parentInfo.builtin() || !parentInfo.parent()) {
      throw new ExecutionException("The builtin command \"%s\" cannot have a custom child command.".formatted(parent));
    }
  }

  protected Optional<CommandInfo> checkIfReplacementValid(CommandContext context, CommandAddress address, boolean replace, boolean force) {
    Optional<CommandInfo> existing = commandRepository.getCommandInfo(context.configPath(), address);
    if (existing.isPresent()) {
      if (!replace) {
        throw new ExecutionException("Command \"%s\" already exists - use --replace option to enforce replacement.".formatted(address));
      }
      if (!existing.get().children().isEmpty() && !force) {
        throw new ExecutionException("Command \"%s\" has sub-commands - use --force option to enforce removal.".formatted(address));
      }
    }
    return existing;
  }

  protected void store(CommandContext context, CommandAddress address, CommandConfig config) {
    CommandConfigTree tree = new CommandConfigTree();
    tree.setConfig(config);
    store(context, address, tree);
  }

  protected void store(CommandContext context, CommandAddress address, CommandConfigTree tree) {
    CommandAddress tmpAddress = address.parent().child("_%s".formatted(UUID.randomUUID()));
    commandConfigRepository.store(context.configPath(), tmpAddress, tree);
    commandConfigRepository.delete(context.configPath(), address, true);
    commandConfigRepository.move(context.configPath(), tmpAddress, address);
  }
}
