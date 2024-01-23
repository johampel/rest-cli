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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_ADDRESS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_EXCLUDE_CHILDREN;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_FORCE;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_REPLACE;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.command.config.CommandConfigTree;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class CmdCpCommand extends CmdWriteCommandBase {

  static final String NAME = "cp";
  static final Positional CMD_ARG_SOURCE = positional("<source>")
      .validator(Validators.COMMAND_ADDRESS_VALIDATOR)
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Copies the specified command (tree).

      It is only possible to copy custom commands, depending on the options only the command itself, or - if it is a group command - its 
      children as well.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <source>
            
      >The name of the command to copy. See section "Further infos" for information about the syntax.
            
      <address>
            
      >The address where to store the copy. See section "Further infos" for information about the syntax.
            
      -e | --exclude-children
            
      >If set, only the command identified by the `<address>` is copied and any children are skipped.

      -r | --replace
            
      >If there is already a command with the given `<address>` you have to provide this option in case you really want to replace it.
      In case of a replacement, as a further cross check, the following option is present:
            
      >-f | --force
            
      >>This option is required in case that the command to replace has sub-commands. Note that when replacing a group command, all its
      sub commands are implicitly removed.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS);

  public CmdCpCommand() {
    super(NAME,
        "Copies a command (tree).",
        new CommandLineSpec(true, CMD_OPT_EXCLUDE_CHILDREN, CMD_OPT_REPLACE, CMD_ARG_SOURCE, CMD_ARG_ADDRESS),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);
    boolean force = commandLine.hasOption(CMD_OPT_FORCE);
    CommandAddress source = commandLine.getValue(CMD_ARG_SOURCE)
        .map(CommandAddress::fromString)
        .orElseThrow();
    CommandAddress address = commandLine.getValue(CMD_ARG_ADDRESS)
        .map(CommandAddress::fromString)
        .orElseThrow();
    boolean excludeChildren = commandLine.hasOption(CMD_OPT_EXCLUDE_CHILDREN);

    copy(context, source, address, replace, force, excludeChildren);
    return true;
  }

  void copy(CommandContext context, CommandAddress source, CommandAddress address, boolean replace, boolean force,
      boolean excludeChildren) {
    if (Objects.equals(address, source)) {
      throw new ExecutionException("`<address>` and `<source>` are identical.");
    }
    CommandConfigTree tree = getCommandConfigTree(context, source, excludeChildren);
    checkIfValidCustomCommandAddress(context, address);
    checkIfReplacementValid(context, address, replace, force);
    store(context, address, tree);
  }
}
