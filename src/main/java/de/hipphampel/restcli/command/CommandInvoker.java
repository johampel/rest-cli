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

import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLine.Subset;
import de.hipphampel.restcli.cli.commandline.CommandLineParser;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.GridBlock;
import de.hipphampel.restcli.cli.format.GridBlock.Position;
import de.hipphampel.restcli.cli.format.ParagraphBlock;
import de.hipphampel.restcli.cli.format.ParagraphBlock.Alignment;
import de.hipphampel.restcli.cli.format.PreformattedBlock;
import de.hipphampel.restcli.cli.format.SequenceBlock;
import de.hipphampel.restcli.command.builtin.HelpCommand;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentRepository;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.exception.UsageException;
import de.hipphampel.restcli.utils.KeyValue;
import de.hipphampel.restcli.utils.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_ENVIRONMENT;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_FORMAT;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_OUTPUT_PARAMETER;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_TEMPLATE;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_ENVIRONMENT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_FORMAT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_OUTPUT_PARAMETER;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_TEMPLATE;
import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND;
import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND_ARGS;

@ApplicationScoped
public class CommandInvoker {

  @Inject
  CommandRepository repository;

  @Inject
  CommandLineParser commandLineParser;
  @Inject
  EnvironmentRepository environmentRepository;

  public boolean invokeCommand(CommandContext context, CommandAddress address, List<String> args) {
    Command command = repository.getCommand(context.configPath(), address).orElse(null);
    if (command != null) {
      return runCommand(context, command, args);
    } else {
      onCommandNotFound(context, address);
      return false;
    }
  }

  public boolean invokeAliasCommand(CommandContext context, List<String> args, Output out, Output err) {
    CommandLineSpec aliasCommandLineSpec = new CommandLineSpec(false, CMD_OPT_ENVIRONMENT, CMD_OPT_FORMAT, CMD_OPT_TEMPLATE, CMD_OPT_OUTPUT_PARAMETER, CMD_ARG_SUB_COMMAND);
    CommandLine aliasCommandLine = commandLineParser.parseCommandLine(aliasCommandLineSpec, args);
    mergeOutputOptions(context.rootCommandLine(), aliasCommandLine);
    Environment aliasEnvironment = getEnvironment(context, aliasCommandLine);

    CommandContext aliasContext = new CommandContext(context);
    aliasContext.environment(aliasEnvironment);
    aliasContext.rootCommandLine(aliasCommandLine);
    if (out!=null) {
      aliasContext.out(out);
    }
    if (err!=null) {
      aliasContext.err(err);
    }

    String subCommand = aliasCommandLine.getValue(CMD_ARG_SUB_COMMAND)
        .orElseThrow(() -> new ExecutionException("No sub command in alias."));
    List<String> subCommandArgs = aliasCommandLine.getValues(CMD_ARG_SUB_COMMAND_ARGS);
    return context.commandInvoker().invokeCommand(aliasContext, CommandAddress.ROOT.child(subCommand), subCommandArgs);
  }

  Environment getEnvironment(CommandContext context, CommandLine aliasCommandLine) {
    if (!aliasCommandLine.hasOption(CMD_OPT_ENVIRONMENT)) {
      return context.environment();
    }
    String environmentName = aliasCommandLine.getValue(CMD_ARG_ENVIRONMENT).orElseThrow();
    return environmentRepository.getEnvironment(context.configPath(), environmentName)
        .orElseThrow(() -> new ExecutionException("No such environment \"%s\".".formatted(environmentName)));
  }

  void mergeOutputOptions(CommandLine rootCommandLine, CommandLine aliasCommandLine) {
    if (!aliasCommandLine.hasOption(CMD_OPT_FORMAT) && !aliasCommandLine.hasOption(CMD_OPT_TEMPLATE)) {
      if (rootCommandLine.hasOption(CMD_OPT_FORMAT)) {
        aliasCommandLine.addOption(CMD_OPT_FORMAT);
        Subset subset = aliasCommandLine.addSubset(CMD_OPT_FORMAT);
        subset.addValue(CMD_ARG_FORMAT, rootCommandLine.getValue(CMD_ARG_FORMAT).orElseThrow());
      }
      if (rootCommandLine.hasOption(CMD_OPT_TEMPLATE)) {
        aliasCommandLine.addOption(CMD_OPT_TEMPLATE);
        Subset subset = aliasCommandLine.addSubset(CMD_OPT_TEMPLATE);
        subset.addValue(CMD_ARG_TEMPLATE, rootCommandLine.getValue(CMD_ARG_TEMPLATE).orElseThrow());
      }
    }

    List<String> mergedParameters = Stream.concat(rootCommandLine.getValues(CMD_ARG_OUTPUT_PARAMETER).stream(),
            aliasCommandLine.getValues(CMD_ARG_OUTPUT_PARAMETER).stream())
        .collect(Collectors.groupingBy(kv -> KeyValue.fromString(kv).key()))
        .values().stream()
        .map(list -> list.get(list.size() - 1))
        .toList();
    if (!mergedParameters.isEmpty()) {
      List<Subset> existing = aliasCommandLine.getSubsets(CMD_OPT_OUTPUT_PARAMETER);
      if (!existing.isEmpty()) {
        existing.clear();
        aliasCommandLine.setValues(CMD_ARG_OUTPUT_PARAMETER, new ArrayList<>());
      }
      aliasCommandLine.addOption(CMD_OPT_OUTPUT_PARAMETER);
      mergedParameters
          .forEach(p -> aliasCommandLine.addSubset(CMD_OPT_OUTPUT_PARAMETER).addValue(CMD_ARG_OUTPUT_PARAMETER, p));
    }
  }

  public Optional<CommandInfo> getCommandInfo(CommandContext context, CommandAddress address) {
    return repository.getCommandInfo(context.configPath(), address);
  }

  public Optional<Command> getCommand(CommandContext context, CommandAddress address) {
    return repository.getCommand(context.configPath(), address);
  }

  public Pair<CommandAddress, Optional<String>> getLongestExistingCommandAddress(CommandContext context, CommandAddress address) {
    return repository.getLongestExistingCommandAddress(context.configPath(), address);
  }

  public boolean runCommand(CommandContext context, Command command, List<String> args) {
    CommandAddress oldAddress = context.commandAddress();
    try {
      context = context.commandAddress(command.address());
      CommandLine commandLine = commandLineParser.parseCommandLine(command.commandLineSpec(), args);
      return command.execute(context, commandLine);
    } catch (UsageException ue) {
      onUsageException(context, command, ue);
    } catch (ExecutionException ee) {
      onExecutionException(context, ee);
    } catch (Throwable e) {
      onUnexpectedException(context, e);
    } finally {
      context.commandAddress(oldAddress);
    }
    return false;
  }

  void onUsageException(CommandContext context, Command command, UsageException usageException) {
    CommandUtils.showError(context, usageException.getMessage());
    context.err().block(
        new GridBlock(
            Map.of(
                new Position(0, 0),
                new PreformattedBlock("usage: %s ".formatted(CommandUtils.getQualifiedCommandName(context, command.address()))),
                new Position(0, 1), new ParagraphBlock(Alignment.LEFT, command.commandLineSpec().usageString()))));
  }

  static void onExecutionException(CommandContext context, ExecutionException executionException) {
    if (executionException.getCause() == null) {
      CommandUtils.showError(context, executionException.getMessage());
    } else {
      CommandUtils.showError(context, new SequenceBlock(
          new ParagraphBlock(Alignment.LEFT, executionException.getMessage()),
          new PreformattedBlock(stackTraceString(executionException.getCause()))));
    }
  }

  static void onUnexpectedException(CommandContext context, Throwable exception) {
    StringWriter stacktrace = new StringWriter();
    try (PrintWriter traceOut = new PrintWriter(stacktrace)) {
      exception.printStackTrace(traceOut);
      traceOut.flush();
    }
    CommandUtils.showError(context, new SequenceBlock(
        new ParagraphBlock(Alignment.LEFT, "unexpected error: %s.".formatted(exception.getMessage())),
        new PreformattedBlock(stackTraceString(exception))));
  }

  private static String stackTraceString(Throwable exception) {
    StringWriter stacktrace = new StringWriter();
    try (PrintWriter traceOut = new PrintWriter(stacktrace)) {
      exception.printStackTrace(traceOut);
      traceOut.flush();
    }
    return stacktrace.toString();
  }

  void onCommandNotFound(CommandContext context, CommandAddress address) {
    if (address == null || address.isRoot()) {
      CommandUtils.showError(context, "Unable to find application command.");
      return;
    }
    Pair<CommandAddress, Optional<String>> longest = getLongestExistingCommandAddress(context, address);
    CommandAddress existing = longest.first();
    String child = longest.second().orElseThrow();

    if (existing != null && !existing.isRoot()) {
      CommandUtils.showError(context, "Unknown sub-command \"%s\" for \"%s\".",
          child,
          existing.name());
      context.err().markdownf("Type `%s` to get a list of available sub-commands.",
          CommandUtils.getQualifiedCommandName(context, CommandAddress.fromString(HelpCommand.NAME).child(existing)));
    } else {
      CommandUtils.showError(context, "No such command \"%s\".".formatted(address.name()));
    }
  }
}
