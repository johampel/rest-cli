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
package de.hipphampel.restcli.command.builtin.template;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.exception.UsageException;
import de.hipphampel.restcli.template.TemplateAddress;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class TemplateRmCommand extends TemplateCommandBase {

  static String NAME = "rm";

  static final Option CMD_OPT_FORCE = option("-f", "--force")
      .build();
  static final Option CMD_OPT_RECURSIVE = option("-r", "--recursive")
      .build();
  static final Positional CMD_ARG_ADDRESS = positional("<address>")
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      `<address>` is either an output template address or a command address. If it is an output template address, this command deletes
      exactly this template. If it is a command address, it deletes all output template address associated with this command. Note that
      in case you delete all output templates for a command, you have to use the `--force` option.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
            
      <address>
            
      >The `<address>` of the command or output template (see section "Further Infos" for information about the syntax). Note if it is a
      command address, it deletes all output templates directly associated with this command; if you also want to delete the output
      templates of the sub commands, use the `--recursive` option. In any case, if specifying a command address, you have to use the
      `--force` option.
          
      -f | --force
            
      >Force deletion of the selected output templates. This is mandatory if `<address>` is a command address.
            
      -r | --recursive
            
      >If `<address>` is a command address, this option also deletes all templates of the sub-commands. If omitted, only the output templates
      of the command itself are deleted.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_TEMPLATE_ADDRESS);

  public TemplateRmCommand() {
    super(NAME, "Removes one or more output templates.",
        new CommandLineSpec(true, CMD_OPT_FORCE, CMD_OPT_RECURSIVE, CMD_ARG_ADDRESS),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String address = commandLine.getValue(CMD_ARG_ADDRESS).orElseThrow();
    boolean force = commandLine.hasOption(CMD_OPT_FORCE);
    boolean recursive = commandLine.hasOption(CMD_OPT_RECURSIVE);

    if (address.contains("@")) {
      deleteTemplateAddress(context, address, recursive);
    } else {
      deleteCommandAddress(context, address, force, recursive);
    }
    return true;
  }

  void deleteCommandAddress(CommandContext context, String address, boolean force, boolean recursive) {
    CommandAddress commandAddress;
    try {
      commandAddress = CommandAddress.fromString(address);
    } catch (IllegalArgumentException iae) {
      throw new UsageException("\"%s\" is not a valid command address.".formatted(address));
    }

    deleteCommandAddress(context, commandAddress, force, recursive);
  }

  void deleteCommandAddress(CommandContext context, CommandAddress commandAddress, boolean force, boolean recursive) {
    commandRepository.getCommandInfo(context.configPath(), commandAddress)
        .orElseThrow(() -> new ExecutionException("Command \"%s\" not found.".formatted(commandAddress)));
    if (!force) {
      throw new ExecutionException("Deleting output templates of command \"%s\" requires `--force` option.".formatted(commandAddress));
    }

    templateRepository.deleteTemplatesForCommand(context.configPath(), commandAddress, recursive);
  }

  void deleteTemplateAddress(CommandContext context, String address, boolean recursive) {
    if (recursive) {
      CommandUtils.showWarning(context, "Option `--recursive` has no effect when deleting a specific output template.");
    }

    TemplateAddress templateAddress;
    try {
      templateAddress = TemplateAddress.fromString(address);
    } catch (IllegalArgumentException iae) {
      throw new UsageException("\"%s\" is not a valid output template address.".formatted(address));
    }

    if (!templateRepository.deleteTemplate(context.configPath(), templateAddress)) {
      throw new ExecutionException("Output template \"%s\" does not exists.".formatted(templateAddress));
    }
  }
}
