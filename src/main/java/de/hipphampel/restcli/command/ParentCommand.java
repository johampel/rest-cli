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
package de.hipphampel.restcli.command;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.cli.format.FormatBuilder;
import de.hipphampel.restcli.cli.format.ParagraphBlock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ParentCommand extends Command {

  Positional CMD_ARG_SUB_COMMAND_ARGS = positional("<sub-command-args>")
      .repeatable()
      .optional()
      .build();

  Positional CMD_ARG_SUB_COMMAND = positional("<sub-command>")
      .optional()
      .dependency(CMD_ARG_SUB_COMMAND_ARGS)
      .build();

  @Override
  default CommandLineSpec commandLineSpec() {
    return new CommandLineSpec(false, CMD_ARG_SUB_COMMAND);
  }

  @Override
  default boolean execute(CommandContext context, CommandLine commandLine) {
    String subCommand = commandLine.getValue(CMD_ARG_SUB_COMMAND).orElse(null);
    if (subCommand == null) {
      showHelp(context, context.err());
      return false;
    }

    List<String> subCommandArgs = commandLine.getValues(CMD_ARG_SUB_COMMAND_ARGS);

    return context.commandInvoker().invokeCommand(
        context,
        address().child(subCommand),
        subCommandArgs);
  }

  @Override
  default Optional<Block> helpSection(CommandContext context, HelpSection section) {
    return switch (section) {
      case DESCRIPTION -> Optional.of(new ParagraphBlock("""
          `%s` is a collection of sub commands and has no functionality apart from grouping the commands.
          See the following list for the available sub commands.""".formatted(name())));
      case SUB_COMMANDS -> {
        CommandInfo info = context.commandInvoker().getCommandInfo(context, address()).orElseThrow();
        if (info.children().isEmpty()) {
          yield Optional.empty();
        }
        yield Optional.of(
            FormatBuilder.buildFormat(
                "|---|---|---|" + System.lineSeparator() +
                    info.children().stream()
                        .sorted(Comparator.comparing(CommandAddress::toString))
                        .map(address -> context.commandInvoker().getCommandInfo(context, address).orElseThrow())
                        .map(childInfo -> "|%s|-|%s|%n".formatted(childInfo.address().name(), childInfo.synopsis()))
                        .collect(Collectors.joining(""))));
      }
      case ARGS_AND_OPTIONS -> Optional.of(FormatBuilder.buildFormat("""
          <sub-command>
                    
          >The name of the sub command to execute. See the list below for the available sub-commands.
                    
          [<sub-command-args>...]
                    
          >Arguments passed to the sub-command.
          """));
      default -> Command.super.helpSection(context, section);

    };
  }
}
