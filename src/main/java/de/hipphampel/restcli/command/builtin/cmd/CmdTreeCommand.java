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
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class CmdTreeCommand extends CmdCommandBase {

  static final String NAME = "tree";

  static final Option CMD_OPT_ADDRESS = option("-a", "--address")
      .build();
  static final Option CMD_OPT_BEAUTIFY = option("-b", "--beautify")
      .build();
  static final Option CMD_OPT_DETAILS = option("-d", "--details")
      .build();
  static final Positional CMD_ARG_ADDRESS = positional("<address>")
      .validator(Validators.COMMAND_ADDRESS_VALIDATOR)
      .optional()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Displays the available commands in a tree. Depending on the options, the entire command tree is shown or only a sub-tree starting with
      a root command. Also, there are some options to customize the output.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <address>
            
      >The command address to start with (= the root of the tree), see also section "Further Infos:" for details concerning the syntax.
       If omitted, the tree for all commands is shown.
       
       -a | --address
       
       >If specified, the complete addresses of the commands are printed. Without this option just their names are shown.
       
       -b | --beautify
       
       >If this option is specified, some beautfications on the output are done to visualize the tree structure. Without this option the
       tree structure is visualized just be indentation.
       
       -d | --details
       
       >If specified, additional information about the commands are shown, without this option just the name or address.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS);

  public CmdTreeCommand() {
    super(NAME,
        "Shows a tree of the available commands.",
        new CommandLineSpec(true, CMD_OPT_DETAILS, CMD_OPT_BEAUTIFY, CMD_OPT_ADDRESS, CMD_ARG_ADDRESS),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {

    CommandAddress address = commandLine.getValue(CMD_ARG_ADDRESS)
        .map(CommandAddress::fromString)
        .orElse(CommandAddress.ROOT);
    boolean showAddress = commandLine.hasOption(CMD_OPT_ADDRESS);
    boolean beautify = commandLine.hasOption(CMD_OPT_BEAUTIFY);
    boolean details = commandLine.hasOption(CMD_OPT_DETAILS);

    CommandInfo info = commandRepository.getCommandInfo(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Cannot find command \"%s\".".formatted(address)));

    printCommandSummary(context, info, "", details, showAddress);
    printCommandChildTree(context, info, "", beautify, details, showAddress);
    return true;
  }

  void printCommandSummary(CommandContext context, CommandInfo info, String prefix, boolean showDetails, boolean showAddress) {
    CommandAddress address = info.address();
    String label = context.applicationName();
    if (!address.isRoot()) {
      label = showAddress ? info.address().toString() : info.address().name();
    }

    String details = "";
    if (showDetails) {
      StringBuilder buffer = new StringBuilder(" (");
      buffer.append(info.builtin() ? "builtin" : "custom");
      if (!info.builtin()) {
        CommandConfig config = commandConfigRepository.load(context.configPath(), address);
        switch (config.getType()) {
          case Parent -> buffer.append(", parent)");
          case Alias -> buffer.append(", alias): ").append(String.join(" ", config.getAliasConfig()));
          case Http -> buffer.append(", http): %s %s".formatted(config.getRestConfig().getMethod(), config.getRestConfig().getBaseUri()));
        }
      } else if (info.parent()) {
        buffer.append(", parent)");
      } else {
        buffer.append(")");
      }
      details = buffer.toString();
    }

    context.out().linef("%s%s%s", prefix, label, details);

  }

  void printCommandChildTree(CommandContext context, CommandInfo info, String prefix, boolean beautify, boolean showDetails,
      boolean showAddress) {
    if (info.parent()) {
      List<CommandAddress> children = info.children().stream().sorted(Comparator.comparing(CommandAddress::name)).toList();
      for (int i = 0; i < children.size(); i++) {
        CommandAddress child = children.get(i);
        String childPrefix;
        String grandChildPrefix;
        if (beautify) {
          if (i < children.size() - 1) {
            childPrefix = prefix + "├── ";
            grandChildPrefix = prefix + "│   ";
          } else {
            childPrefix = prefix + "└── ";
            grandChildPrefix = prefix + "    ";
          }
        } else {
          childPrefix = prefix + "    ";
          grandChildPrefix = prefix + "    ";
        }

        CommandInfo childInfo = commandRepository.getCommandInfo(context.configPath(), child)
            .orElseThrow(() -> new ExecutionException("Cannot find command \"%s\".".formatted(child)));

        printCommandSummary(context, childInfo, childPrefix, showDetails, showAddress);
        printCommandChildTree(context, childInfo, grandChildPrefix, beautify, showDetails, showAddress);
      }

    }
  }
}
