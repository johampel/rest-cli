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
package de.hipphampel.restcli.command.custom;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_FORMAT;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_OUTPUT_PARAMETER;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_TEMPLATE;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_FORMAT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_OUTPUT_PARAMETER;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_TEMPLATE;
import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND;
import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND_ARGS;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineParser;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.KeyValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomAliasCommand extends CustomCommand {

  static final Positional CMD_ARG_ALIAS_ARGS = positional("<alias-args>")
      .repeatable()
      .optional()
      .build();

  private final CommandLineParser commandLineParser;

  public CustomAliasCommand(CommandLineParser commandLineParser, CommandAddress address, CommandConfig config) {
    super(address, config);
    this.commandLineParser = Objects.requireNonNull(commandLineParser);
  }

  @Override
  public CommandLineSpec commandLineSpec() {
    return new CommandLineSpec(false, CMD_ARG_ALIAS_ARGS);
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    CommandContext aliasContext = new CommandContext(context);
    CommandLine aliasCommandLine = prepareCommandLine(context, commandLine);
    aliasContext.rootCommandLine(aliasCommandLine);

    String subCommand = aliasCommandLine.getValue(CMD_ARG_SUB_COMMAND)
        .orElseThrow(() -> new ExecutionException("No sub command in alias."));
    List<String> subCommandArgs = aliasCommandLine.getValues(CMD_ARG_SUB_COMMAND_ARGS);
    return context.commandInvoker().invokeCommand(aliasContext, CommandAddress.ROOT.child(subCommand), subCommandArgs);
  }

  CommandLine prepareCommandLine(CommandContext context, CommandLine commandLine) {
    List<String> args = new ArrayList<>(config().getAliasConfig());
    args.addAll(commandLine.getValues(CMD_ARG_ALIAS_ARGS));

    CommandLineSpec aliasCommandLineSpec = new CommandLineSpec(false, CMD_OPT_FORMAT, CMD_OPT_TEMPLATE, CMD_OPT_OUTPUT_PARAMETER,
        CMD_ARG_SUB_COMMAND);
    CommandLine aliasCommandLine = commandLineParser.parseCommandLine(aliasCommandLineSpec, args);
    mergeOutputOptions(context.rootCommandLine(), aliasCommandLine);
    return aliasCommandLine;
  }

  void mergeOutputOptions(CommandLine rootCommandLine, CommandLine aliasCommandLine) {
    if (!aliasCommandLine.hasOption(CMD_OPT_FORMAT) && !aliasCommandLine.hasOption(CMD_OPT_TEMPLATE)) {
      if (rootCommandLine.hasOption(CMD_OPT_FORMAT)) {
        aliasCommandLine.addOption(CMD_OPT_FORMAT);
        aliasCommandLine.addValues(CMD_ARG_FORMAT, rootCommandLine.getValues(CMD_ARG_FORMAT));
      }
      if (rootCommandLine.hasOption(CMD_OPT_TEMPLATE)) {
        aliasCommandLine.addOption(CMD_OPT_TEMPLATE);
        aliasCommandLine.addValues(CMD_ARG_TEMPLATE, rootCommandLine.getValues(CMD_ARG_TEMPLATE));
      }
    }

    List<String> mergedParameters = Stream.concat(rootCommandLine.getValues(CMD_ARG_OUTPUT_PARAMETER).stream(),
            aliasCommandLine.getValues(CMD_ARG_OUTPUT_PARAMETER).stream())
        .collect(Collectors.groupingBy(kv -> KeyValue.fromString(kv).key()))
        .values().stream()
        .map(list -> list.get(list.size() - 1))
        .toList();
    if (!mergedParameters.isEmpty()) {
      aliasCommandLine.addOption(CMD_OPT_OUTPUT_PARAMETER);
      aliasCommandLine.setValues(CMD_ARG_OUTPUT_PARAMETER, mergedParameters);
    }
  }
}
