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
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_BASE_URI;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_METHOD;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_SYNOPSIS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_DESCRIPTION;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_FORCE;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_HEADER;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_LOAD_BODY;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_OPTION;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_POSITIONAL;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_QUERY_PARAMETER;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_REPLACE;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_SYNOPSIS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectDescriptions;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectHeaders;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectParameters;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectQueryParameters;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.createBodyConfig;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.ParameterListConfig;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class CmdHttpCommand extends CmdWriteCommandBase {

  static final String NAME = "http";

  static final Positional CMD_ARG_BODY = positional("<body>")
      .optional()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Creates a new HTTP command. When creating a HTTP command you have to specify the HTTP request you want to execute and the parameters
      you want to have in place in order to customize the request.
            
      The HTTP is specified by its method, request URL, and optionally body, headers, and query parameters. All these may contain
      references to placeholders that are replaced with actual values, either taken from the current environment or variables. The
      variables can be also bound to command line parameters, which can be passed along with the command. For example, the following defines
      a command named `getEntity` that has a parameter named `id` which is bound to the variable `var1`. It executes a simple HTTP GET
      request, whereas parts of the request URL are replaced with the value for variable `var1`:
      ~~~
            
         ${applicationName} cmd http getEntity -p id=var1 get 'http://localhost:8080/entity/${r"${var1}"}'
         
      ~~~
      Calling `${applicationName} getEntity foo` will execute a HTTP GET request for http://localhost:8080/entity/foo then.
                  
      In order to update an existing command use the `mod` command.

      There are several options to fine-tune the command, see the following section.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <address>
            
      >The address of the command to create, see also section "Further Infos:" for details concerning the syntax.

      <method>
            
      >The HTTP method to use for the request. This is typically something like `get`, `put`, `post`, or `delete`. The `<method>` is
      implicitly converted to upper case.
            
      <base-uri>
            
      >The base URI of the HTTP request. This URI might contain references to placeholders, which are replaced before execution. The final
      URI might be extended by the query parameters (see option `--query`).
            
      <body>
            
      >This is an optional parameter containing the request body. The body is specified as an input source, see section "Further Infos" for
      more information about the syntax. This might be omitted in case the request does not expect a body. Note that if the input source is
      interpolated (so it starts with a `%` sign), the interpolation is done when the HTTP request is executed. If a `<body>` is given, you
      may use the following option as well:
            
      >-l | --load-body
            
      >>In case input source of the body refers to a file, the standard input, or an URL, the `--load-body` option can be used to instruct
      ${applicationName} to load the content of the input source and store this content along with the command definition instead of the
      reference to this file/URL.
                  
      -d | --description [--main|--arguments|--infos] <description>
            
      >Sets a section of the command help text. The `<description>` itself is specified as an input source, see also section "Further Infos:"
      for details. The sub options `--main`, `--arguments`, or `--infos` specify the section being set (see below). The `--description` option
      can be used multiple times, but only once per section:
            
      >--main
            
      >>Sets the "Description" section of the command help. This is the same as leaving out any sub option.
            
      >--arguments
            
      >>Sets the "Arguments and options" section of the command help.

      >--infos
            
      >>Sets the "Further infos" section of the command help.
          
      -h | --header <header>=<value>
            
      >Sets the HTTP request header `<header>` to `<value>`, whereas the `<value>` might contain references to placeholders that are
      replaced when the HTTP request is executed. You may use this option more than once, even with identical `<header>` keys.
            
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
            
      -r | --replace
            
      >If there is already a command with the given `<address>` you have to provide this option in case you really want to replace it.
      In case of a replacement, as a further cross check, the following option is present:
            
      >-f | --force
            
      >>This option is required in case that the command to replace has sub-commands. Note that when replacing a group command, all its
      sub commands are implicitly removed.
            
      -s | --synopsis <synopsis>
            
      >The synopsis of the command. This should be a one liner describing the command's purpose.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_INPUT_SOURCE);

  public CmdHttpCommand() {
    super(
        NAME,
        "Creates a new HTTP command.",
        new CommandLineSpec(true, CMD_OPT_REPLACE, CMD_OPT_DESCRIPTION, CMD_OPT_SYNOPSIS, CMD_OPT_HEADER, CMD_OPT_QUERY_PARAMETER,
            CMD_OPT_OPTION, CMD_OPT_POSITIONAL, CMD_OPT_LOAD_BODY, CMD_ARG_ADDRESS, CMD_ARG_METHOD, CMD_ARG_BASE_URI, CMD_ARG_BODY),
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
    Map<HelpSection, String> descriptions = collectDescriptions(context, commandLine, new HashMap<>());
    RestCommandConfig restCommandConfig = createRestCommandConfig(context, commandLine);
    String synopsis = commandLine.getValue(CMD_ARG_SYNOPSIS).orElse(null);
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);
    boolean force = commandLine.hasOption(CMD_OPT_FORCE);

    return createHttpCommand(context, address, synopsis, descriptions, restCommandConfig, replace, force);
  }

  boolean createHttpCommand(CommandContext context, CommandAddress address, String synopsis, Map<HelpSection, String> descriptions,
      RestCommandConfig restCommandConfig, boolean replace, boolean force) {
    checkIfValidCustomCommandAddress(context, address);
    checkIfReplacementValid(context, address, replace, force);
    CommandConfig config = createCommandConfig(address, synopsis, restCommandConfig, descriptions);

    store(context, address, config);
    return true;
  }

  static CommandConfig createCommandConfig(CommandAddress address, String synopsis,
      RestCommandConfig restCommandConfig, Map<HelpSection, String> descriptions) {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Http);
    config.setRestConfig(restCommandConfig);
    config.setSynopsis(synopsis == null ? "The " + address.name() + " command." : synopsis);
    config.setDescriptions(descriptions);
    return config;
  }

  static RestCommandConfig createRestCommandConfig(CommandContext context, CommandLine commandLine) {
    return new RestCommandConfig()
        .setMethod(commandLine.getValue(CMD_ARG_METHOD).orElseThrow())
        .setBaseUri(commandLine.getValue(CMD_ARG_BASE_URI).orElseThrow())
        .setQueryParameters(collectQueryParameters(commandLine, new HashMap<>()))
        .setHeaders(collectHeaders(context, commandLine, new HashMap<>()))
        .setParameters(new ParameterListConfig(collectParameters(commandLine, new ArrayList<>())))
        .setBody(createBodyConfig(context, commandLine, CMD_ARG_BODY));
  }
}
