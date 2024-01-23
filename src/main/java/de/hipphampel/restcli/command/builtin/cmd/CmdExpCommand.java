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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_ADDRESS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_EXCLUDE_CHILDREN;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandInfo;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfigTree;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class CmdExpCommand extends CmdCommandBase {

  static final String NAME = "exp";
  static final Positional CMD_ARG_TARGET = positional("<target>")
      .optional()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Exports the specified commands and sub commands; you may import them again using the `imp` command.

      It is only possible to export custom commands, depending on the options only the command itself, or - if it is a group command - its 
      children as well.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <address>
            
      >The name of the command to export. See section "Further infos" for information about the syntax.
            
      <target>
            
      >The file where to export to. If omitted, the export is written to standard output.
            
      -e | --exclude-children
            
      >If set, only the command identified by the `<address>` is exported and any children are skipped.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS);

  @Inject
  ObjectMapper objectMapper;

  public CmdExpCommand() {
    super(NAME,
        "Exports a command (tree).",
        new CommandLineSpec(true, CMD_OPT_EXCLUDE_CHILDREN, CMD_ARG_ADDRESS, CMD_ARG_TARGET),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    CommandAddress address = commandLine.getValue(CMD_ARG_ADDRESS)
        .map(CommandAddress::fromString)
        .orElseThrow();
    boolean excludeChildren = commandLine.hasOption(CMD_OPT_EXCLUDE_CHILDREN);
    String target = commandLine.getValue(CMD_ARG_TARGET).orElse(null);

    exportCommand(context, address, excludeChildren, target);
    return true;
  }

  void exportCommand(CommandContext context, CommandAddress address, boolean excludeChildren, String target) {
    CommandConfigTree tree = getCommandConfigTree(context, address, excludeChildren);

    try {
      ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
      if (target != null) {
        writer.writeValue(new File(target), tree);
      } else {
        StringWriter buffer = new StringWriter();
        writer.writeValue(buffer, tree);
        context.out().line(buffer.toString());
      }
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to export command \"%s\": %s".formatted(address, ioe.getMessage()));
    }
  }
}
