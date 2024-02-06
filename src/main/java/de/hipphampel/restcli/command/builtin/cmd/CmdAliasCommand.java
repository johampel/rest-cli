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
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_SYNOPSIS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_DESCRIPTION;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_FORCE;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_REPLACE;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_SYNOPSIS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.collectDescriptions;

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
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class CmdAliasCommand extends CmdWriteCommandBase {

  static final String NAME = "alias";

  static final Positional CMD_ARG_ALIAS = positional("<alias-definition>")
      .repeatable()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Creates a new alias command. An alias command is a short-cut for other commands, whereas the alias contains the first portion of a
      command line. When calling the alias command, the parameters passed to the alias command are appended to it to form the final command
      to execute. For example:
      ~~~
            
      ${applicationName} cmd alias myAlias http post http://example.com
            
      ~~~
      Creates an alias named `myAlias` for the the `http` command with some of the parameters preset, so calling `myAlias` will execute the
      `http` with the given parameters plus the parameters you may pass in addition, such as `myAlias @/file/with/body`.
            
      The alias definition might also contain some of the global options that influence the output of the command, see the following section 
      below.
            
      In order to update an existing command use the `mod` command.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <address>
            
      >The address of the command to create, see also section "Further Infos:" for details concerning the syntax.
            
      <alias-definition>
            
      >The `<alias-definition>` contains the first portion of or the complete command line to be executed. It might contain also some of the
      global options available for `${applicationName}` in general. For example `-orc=true -obody=false http get http://example.com` will be
      an alias definition for the `http` command that passes the `rc` and `body` output parameters as global output parameters (so that the
      command outputs the status code only). The available global options are `-e | --environment`,  `-o | --output-parameter`, 
      `-f | --format`, and `-t | --template`; type `${applicationName} help` to get more information about them.
           
            
      -d | --description [--main|--arguments|--infos] <description>
            
      >Sets a section of the command help text. The `<description>` itself is specified as an input source, see also section "Further Infos:"
      for details. The sub options `--main`, `--arguments`, or `--infos` specify the section being set (see below). The `--description`option
      can be used multiple times, but once per section:
            
      >--main
            
      >>Sets the "Description" section of the command help. This is the same as leaving out any sub option.
            
      >--arguments
            
      >>Sets the "Arguments and options" section of the command help.

      >--infos
            
      >>Sets the "Further infos" section of the command help.
            
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

  public CmdAliasCommand() {
    super(
        NAME,
        "Creates a new alias command.",
        new CommandLineSpec(true, CMD_OPT_REPLACE, CMD_OPT_DESCRIPTION, CMD_OPT_SYNOPSIS, CMD_ARG_ADDRESS, CMD_ARG_ALIAS),
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
    String synopsis = commandLine.getValue(CMD_ARG_SYNOPSIS).orElse(null);
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);
    boolean force = commandLine.hasOption(CMD_OPT_FORCE);
    List<String> alias = commandLine.getValues(CMD_ARG_ALIAS);

    return createAliasCommand(context, address, synopsis, descriptions, replace, force, alias);
  }

  boolean createAliasCommand(CommandContext context, CommandAddress address, String synopsis, Map<HelpSection, String> descriptions,
      boolean replace, boolean force, List<String> alias) {
    checkIfValidCustomCommandAddress(context, address);
    checkIfReplacementValid(context, address, replace, force);
    CommandConfig config = createCommandConfig(address, synopsis, descriptions, alias);

    store(context, address, config);
    return true;
  }

  CommandConfig createCommandConfig(CommandAddress address, String synopsis, Map<HelpSection, String> descriptions, List<String> alias) {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Alias);
    config.setSynopsis(synopsis == null ? "The " + address.name() + " command." : synopsis);
    config.setDescriptions(descriptions);
    config.setAliasConfig(alias);
    return config;
  }
}
