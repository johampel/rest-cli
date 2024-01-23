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
import de.hipphampel.restcli.command.CommandRepository;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.builtin.BuiltinCommand;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfigRepository;
import de.hipphampel.restcli.command.config.CommandConfigTree;
import de.hipphampel.restcli.exception.ExecutionException;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class CmdCommandBase extends BuiltinCommand {

  @Inject
  protected CommandConfigRepository commandConfigRepository;

  @Inject
  protected CommandRepository commandRepository;

  protected CmdCommandBase(String name, String synopsis, CommandLineSpec commandLineSpec,
      Map<HelpSection, Function<CommandContext, Block>> helpSections) {
    super(
        CommandAddress.fromString(CmdCommandParent.NAME).child(name),
        synopsis,
        commandLineSpec,
        helpSections);
  }

  protected CommandConfigTree getCommandConfigTree(CommandContext context, CommandAddress address, boolean excludeChildren) {
    CommandInfo info = commandRepository.getCommandInfo(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Command \"%s\" does not exist.".formatted(address)));
    if (info.builtin()) {
      throw new ExecutionException("Command \"%s\" is a builtin and cannot be exported or copied.".formatted(address));
    }

    CommandConfigTree tree = new CommandConfigTree();
    CommandConfig config = commandConfigRepository.load(context.configPath(), address);
    Map<String, CommandConfigTree> children = new HashMap<>();
    tree.setConfig(config);
    tree.setSubCommands(children);

    if (!excludeChildren) {
      info.children().forEach(child -> children.put(child.name(), getCommandConfigTree(context, child, excludeChildren)));
    }

    return tree;
  }

}
