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
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.template.TemplateRepository;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@ApplicationScoped
@Unremovable
public class CmdRmCommand extends CmdCommandBase {

  static final String NAME = "rm";

  static final Option CMD_OPT_FORCE = option("-f", "--force")
      .build();
  static final Positional CMD_ARG_ADDRESS = positional("<address>")
      .validator(Validators.COMMAND_ADDRESS_VALIDATOR)
      .repeatable()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Deletes the given commands. Only custom commands can be deleted.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <address>...
            
      >The command addresses to delete, see also section "Further Infos:" for details concerning the syntax. If a command is a parent 
      command, it can be deleted only if it has no children or if the `--force` option is used.
       
       -f | --force
       
       >Forces the deletion in case the command to delete has child commands. If so, the child commands are deleted as well. Without this 
       option, commands having child commands cannot be deleted. 
      """);
  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS);

  @Inject
  TemplateRepository templateRepository;

  public CmdRmCommand() {
    super(NAME,
        "Deletes commands.",
        new CommandLineSpec(true, CMD_OPT_FORCE, CMD_ARG_ADDRESS),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    boolean force = commandLine.hasOption(CMD_OPT_FORCE);
    commandLine.getValues(CMD_ARG_ADDRESS).stream()
        .map(CommandAddress::fromString)
        .flatMap(address -> getCommandsToDelete(context, address, force))
        .forEach(address -> {
          templateRepository.deleteTemplatesForCommand(context.configPath(), address, false);
          commandConfigRepository.delete(context.configPath(), address, false);
        });
    return true;
  }

  Stream<CommandAddress> getCommandsToDelete(CommandContext context, CommandAddress address, boolean force) {
    CommandInfo info = commandRepository.getCommandInfo(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Command \"%s\" not found.".formatted(address)));
    if (info.builtin()) {
      throw new ExecutionException("Command \"%s\" is a builtin and cannot be deleted.".formatted(address));
    } else if (!info.children().isEmpty() && !force) {
      throw new ExecutionException("Command \"”%s\" has child commands. Use --force option to delete.".formatted(address));
    }

    return Stream.concat(
        info.children().stream().flatMap(child -> getCommandsToDelete(context, child, force)),
        Stream.of(address));
  }

}
