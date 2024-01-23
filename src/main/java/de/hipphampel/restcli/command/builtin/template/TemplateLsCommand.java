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
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandInfo;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.template.TemplateAddress;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class TemplateLsCommand extends TemplateCommandBase {

  static String NAME = "ls";

  static final Option CMD_OPT_BEAUTIFY = option("-b", "--beautify")
      .build();
  static final Option CMD_OPT_DETAILS = option("-d", "--details")
      .parameter(CMD_OPT_BEAUTIFY)
      .build();
  static final Option CMD_OPT_LOCAL = option("-l", "--local")
      .build();
  static final Positional CMD_ARG_COMMAND = positional("<command-address>")
      .validator(Validators.COMMAND_ADDRESS_VALIDATOR)
      .optional()
      .build();
  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Lists the output templates that are available for the given command.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <command-address>
            
      >The command address of the command the output templates should be listed for. See section "Further Infos" for more information
      about the syntax.
            
      >If omitted, the global output templates are listed.

      -d | --details
            
      >Show details for the output templates being listed, such as the type of the command (builtin or custom) and for which command the
      output template was actually defined.

      >-b | --beautify
            
      >>The `--beautify` option is only available in combination with the `--details` option. If set, the listing is enriched with additional
      decorations.
            
      -l | --local
            
      >If set, it shows only those output templates that are defined exactly for the given `<command-address>`. If omitted, also the output
      templates inherited from the parent commands are shown.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS);

  public TemplateLsCommand() {
    super(NAME, "Lists available output templates.",
        new CommandLineSpec(true, CMD_OPT_LOCAL, CMD_OPT_DETAILS, CMD_ARG_COMMAND),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }


  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    CommandAddress commandAddress = commandLine.getValue(CMD_ARG_COMMAND)
        .map(CommandAddress::fromString)
        .orElse(CommandAddress.ROOT);
    boolean beautify = commandLine.hasOption(CMD_OPT_BEAUTIFY);
    boolean local = commandLine.hasOption(CMD_OPT_LOCAL);
    boolean details = commandLine.hasOption(CMD_OPT_DETAILS);

    CommandInfo commandInfo = commandRepository.getCommandInfo(context.configPath(), commandAddress).orElseThrow(
        () -> new ExecutionException("No such command \"%s\"".formatted(commandAddress)));

    List<TemplateAddress> templateAddresses = getTemplateAddresses(context, commandInfo.address(), local);
    showList(context, templateAddresses, beautify, details);
    return true;
  }

  void showList(CommandContext context, List<TemplateAddress> templateAddresses, boolean beautify, boolean details) {

    if (details) {
      StringBuilder buffer = new StringBuilder();
      String ls = System.lineSeparator();
      if (beautify) {
        buffer.append("| Name | Owning Command | Type |").append(ls);
      }
      buffer.append("|---|---|---|").append(ls);
      for (TemplateAddress templateAddress : templateAddresses) {
        boolean builtin = templateRepository.isBuiltin(context.configPath(), templateAddress);
        buffer.append("|").append(templateAddress.name());
        buffer.append("|").append(templateAddress.command().isRoot() ? "<global>" : templateAddress.command().toString());
        buffer.append("|").append(builtin ? "builtin" : "custom");
        buffer.append("|").append(ls);
      }
      context.out().markdown(buffer.toString());
    } else {
      templateAddresses.forEach(address -> context.out().line(address.name()));
    }
  }

  List<TemplateAddress> getTemplateAddresses(CommandContext context, CommandAddress commandAddress, boolean local) {
    if (local) {
      return templateRepository.getTemplatesForCommand(context.configPath(), commandAddress);
    } else {
      return templateRepository.getEffectiveTemplates(context.configPath(), commandAddress);
    }
  }
}
