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
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_BASE_URI;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_METHOD;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_SYNOPSIS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_DESCRIPTION;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_HEADER;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_LOAD_BODY;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_OPTION;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_POSITIONAL;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_QUERY_PARAMETER;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_SYNOPSIS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectDescriptions;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectHeaders;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectParameters;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectQueryParameters;

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
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterListConfig;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.exception.UsageException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class CmdModCommand extends CmdWriteCommandBase {

  static final String NAME = "mod";
  static final String RESET_CLI = "cli";
  static final String RESET_DOCU = "docu";
  static final String RESET_HEADERS = "headers";
  static final String RESET_QUERY = "query";
  static final List<String> RESET_FLAGS = List.of(RESET_CLI, RESET_DOCU, RESET_HEADERS, RESET_QUERY);

  static final Positional CMD_ARG_NEW_ADDRESS = positional("<new-address")
      .validator(Validators.COMMAND_ADDRESS_VALIDATOR)
      .build();
  static final Option CMD_OPT_NEW_ADDRESS = option("-n", "--new-address")
      .parameter(CMD_ARG_NEW_ADDRESS)
      .build();
  static final Positional CMD_ARG_RESET = positional(String.join("|", RESET_FLAGS))
      .validator((positional, value) -> {
        if (!RESET_FLAGS.contains(value)) {
          throw new UsageException("Argument of \"--reset\" must be one of: %s.".formatted(String.join(", ", RESET_FLAGS)));
        }
      })
      .build();
  static final Option CMD_OPT_RESET = option("-r", "--reset")
      .parameter(CMD_ARG_RESET)
      .repeatable()
      .build();
  static final Option CMD_OPT_METHOD = option("-m", "--method")
      .parameter(CMD_ARG_METHOD)
      .build();
  static final Option CMD_OPT_BASE_URI = option("-u", "--uri")
      .parameter(CMD_ARG_BASE_URI)
      .build();
  static final Option CMD_OPT_NO_BODY = option("--no-body")
      .exclusionGroup("body")
      .build();
  static final Positional CMD_ARG_BODY = positional("<body>")
      .build();
  static final Option CMD_OPT_BODY = option("-b", "--body")
      .parameter(CMD_OPT_LOAD_BODY, CMD_ARG_BODY)
      .exclusionGroup("body")
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Modifies an existing command. In case of parent and alias commands, you can modify the synopsis, description, or its address only. For
      custom HTTP commands, you may modify all its properties.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <address>
            
      >The address of the command to modify, see also section "Further Infos:" for details concerning the syntax.

      -b | --body <body>
            
      >If specified, it sets the request body. This mutual excludes option `--no-body` to unset the body. The body is specified as an input
      source, see section "Further Infos" for more information about the syntax. Note that if the input source is interpolated (so it starts 
      with a `%` sign), the interpolation is done when the HTTP request is executed. If a `<body>` is given, you may use the following option as well:
            
      >-l | --load-body
            
      >>In case input source of the body refers to a file, the standard input, or an URL, the `--load-body` option can be used to instruct
      ${applicationName} to load the content of the input source and store this content along with the command definition instead of the
      reference to this file/URL.
            
      -d | --description [--main|--arguments|--infos] <description>
            
      >Adds or overwrites a section of the command help text. The `<description>` itself is specified as an input source, see also section
      "Further Infos:" for details. The sub options `--main`, `--arguments`, or `--infos` specify the section being set (see below). The
      `--description` option can be used multiple times, but only once per section:
            
      >--main
            
      >>Sets the "Description" section of the command help. This is the same as leaving out any sub option.
            
      >--arguments
            
      >>Sets the "Arguments and options" section of the command help.

      >--infos
            
      >>Sets the "Further infos" section of the command help.
          
      -h | --header <header>=<value>
            
      >Sets the HTTP request header `<header>` to `<value>`, whereas the `<value>` might contain references to placeholders that are
      replaced when the HTTP request is executed. You may use this option more than once, even with identical `<header>` keys.
            
      -m | --method <method>
            
      >Only available for custom HTTP requests: changes the HTTP method to use for the request. This is typically something like `get`, 
      `put`, `post`, or `delete`. The `<method>` is implicitly converted to upper case. If the `--method` parameter is omitted, the method
      of the HTTP requests remains unchanged.
            
      -n | --new-address <new-address>
            
      >The new address of the command. If given, the command is moved to the given address, if omitted, no change is done regard its name
      and location in the command tree. See also section "Further Infos:" for details concerning the syntax of command addresses.

      --no-body
            
      >Indicates that the request has no body. This mutually excludes the `--body` option.
             
      -o | --option <name>=<variable>
            
      >Defines an option parameter for the new command. The option `<name>` is specified without the leading dashes. If `<name>` is just one
      single character, you have to specify the option with a single leading dash (so if `<name>` was `x`, the created command will accept
      the option `-x`), otherwise with double leading dashes (so if `<name>` was `xyz`, the created command will accept the option `--xyz`).
      >The option is bound to the given `<variable>`. When executing the command, the value passed to the option are set in the corresponding
      variables. In order to define a positional parameter, use `--positional` instead.
        
      -p | --positional <name>=<variable>
            
      >Defines a positional parameter for the new command. It has a `<name>` that uniquely identifies the parameter and a `<variable>`.
      When executing the command, the values passed to the parameters are set in the corresponding variables. In order to define an option,
      use option `--option` instead.
            
      -q | --query <key>=<value>
            
      >Adds the query parameter `<key>` with the specified `<value>` to the url of the HTTP request. The `<value>`  might contain references
      to placeholders that are replaced when the HTTP request is executed. You may use this option more than once, but the `<key>` part must
      be different then.
            
      -r | --reset cli|docu|headers|query
            
      >Resets given group of settings (for example `--reset docu` will remove all settings regarding the documentation of the command). 
      Typically, this is normally used in combination with other options (for example with the `--description` option to add new 
      documentation later on again).
      >The `--reset` option can be used once per group, so it is ok to use `--reset docu --reset cli` in the command line. The following
      groups are known:
            
      >cli
      >>Only available for custom HTTP commands: removes all parameter settings from the command, they can be added with `--positional`
      and/or `--option` to the command again.
            
      >docu
      >>Removes the documentation from the command. Documentation can be added via the `--description` option.
            
      >headers
      >>Only available for custom HTTP commands: removes all headers settings from teh command. Headers can be added with the `--header`
      option.
            
      >query
      >>Only available for custom HTTP commands: removes all query parameter settings from teh command. Query parameters can be added with 
      the `--query` option.
           
      -s | --synopsis <synopsis>
            
      >The synopsis of the command. This should be a one liner describing the command's purpose.
            
      -u | --uri <base-uri>
            
      >Sets the base URI of the HTTP request. This URI might contain references to placeholders, which are replaced before execution. The 
      final URI might be extended by the query parameters (see option `--query`).
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_INPUT_SOURCE);

  public CmdModCommand() {
    super(
        NAME,
        "Modifies an existing custom command",
        new CommandLineSpec(true, CMD_OPT_RESET, CMD_OPT_DESCRIPTION, CMD_OPT_SYNOPSIS, CMD_OPT_NEW_ADDRESS, CMD_OPT_METHOD,
            CMD_OPT_HEADER, CMD_OPT_BASE_URI, CMD_OPT_QUERY_PARAMETER, CMD_OPT_NO_BODY, CMD_OPT_BODY, CMD_OPT_OPTION, CMD_OPT_POSITIONAL,
            CMD_ARG_ADDRESS),
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
    CommandInfo info = commandRepository.getCommandInfo(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Command \"%s\" not found.".formatted(address)));
    if (info.builtin()) {
      throw new ExecutionException("Cannot modify builtin command \"%s\".".formatted(address));
    }

    CommandConfig config = commandConfigRepository.load(context.configPath(), info.address());
    applyDescriptionChanges(context, commandLine, config);
    applyQueryParameterChanges(commandLine, address, config);
    applyHeaderChanges(context, commandLine, address, config);
    applyParameterChanges(commandLine, address, config);
    applyBodyChanges(context, commandLine, address, config);
    commandConfigRepository.store(context.configPath(), address, config);
    renameIfRequired(context, commandLine, address);
    return true;
  }

  void applyDescriptionChanges(CommandContext context, CommandLine commandLine, CommandConfig config) {
    commandLine.getValue(CMD_ARG_SYNOPSIS).ifPresent(config::setSynopsis);
    if (commandLine.getValues(CMD_ARG_RESET).contains(RESET_DOCU)) {
      config.setDescriptions(new HashMap<>());
    }
    collectDescriptions(context, commandLine, config.getDescriptions());
  }

  void applyQueryParameterChanges(CommandLine commandLine, CommandAddress address, CommandConfig config) {
    if (!commandLine.hasOption(CMD_OPT_QUERY_PARAMETER) && !commandLine.getValues(CMD_ARG_RESET).contains(RESET_QUERY)) {
      return;
    }

    ensureHttpCommand(address, config);
    RestCommandConfig restConfig = config.getRestConfig();
    if (commandLine.getValues(CMD_ARG_RESET).contains(RESET_QUERY)) {
      restConfig.setQueryParameters(new HashMap<>());
    }
    collectQueryParameters(commandLine, restConfig.getQueryParameters());
  }

  void applyHeaderChanges(CommandContext context, CommandLine commandLine, CommandAddress address, CommandConfig config) {
    if (!commandLine.hasOption(CMD_OPT_HEADER) && !commandLine.getValues(CMD_ARG_RESET).contains(RESET_HEADERS)) {
      return;
    }

    ensureHttpCommand(address, config);
    RestCommandConfig restConfig = config.getRestConfig();
    if (commandLine.getValues(CMD_ARG_RESET).contains(RESET_HEADERS)) {
      restConfig.setHeaders(new HashMap<>());
    }
    restConfig.setHeaders(collectHeaders(context, commandLine, restConfig.getHeaders()));
  }

  void applyParameterChanges(CommandLine commandLine, CommandAddress address, CommandConfig config) {
    if (!commandLine.hasOption(CMD_OPT_OPTION) && !commandLine.hasOption(CMD_OPT_POSITIONAL) && !commandLine.getValues(CMD_ARG_RESET)
        .contains(RESET_CLI)) {
      return;
    }

    ensureHttpCommand(address, config);
    RestCommandConfig restConfig = config.getRestConfig();
    List<ParameterConfig> parameters = new ArrayList<>(restConfig.getParameters().getParameters());
    if (commandLine.getValues(CMD_ARG_RESET).contains(RESET_CLI)) {
      parameters.clear();
    }
    restConfig.setParameters(new ParameterListConfig(collectParameters(commandLine, parameters)));
  }

  void applyBodyChanges(CommandContext context, CommandLine commandLine, CommandAddress address, CommandConfig config) {
    if (!commandLine.hasOption(CMD_OPT_NO_BODY) && !commandLine.hasOption(CMD_OPT_BODY)) {
      return;
    }
    ensureHttpCommand(address, config);
    RestCommandConfig restConfig = config.getRestConfig();
    if (commandLine.hasOption(CMD_OPT_NO_BODY)) {
      restConfig.setBody(null);
    } else {
      restConfig.setBody(CmdCommandUtils.createBodyConfig(context, commandLine, CMD_ARG_BODY));
    }
  }

  void renameIfRequired(CommandContext context, CommandLine commandLine, CommandAddress address) {
    CommandAddress newAddress = commandLine.getValue(CMD_ARG_NEW_ADDRESS)
        .map(CommandAddress::fromString)
        .orElse(null);
    if (newAddress == null || newAddress.equals(address)) {
      return;
    }

    checkIfValidCustomCommandAddress(context, newAddress);
    if (newAddress.toString().contains(address.toString())) {
      throw new ExecutionException("Cannot move command \"%s\" to \"%s\".".formatted(address, newAddress));
    }
    commandConfigRepository.move(context.configPath(), address, newAddress);
  }

  void ensureHttpCommand(CommandAddress address, CommandConfig config) {
    if (config.getType() != Type.Http) {
      throw new ExecutionException("Command \"%s\" is not a HTTP command.".formatted(address));
    }
  }
}
