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
package de.hipphampel.restcli.command.builtin;

import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_CONFIG_DIR;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_ENVIRONMENT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_CONFIG;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_ENVIRONMENT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_FORMAT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_OUTPUT_PARAMETER;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_TEMPLATE;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.config.ApplicationConfig;
import de.hipphampel.restcli.config.ApplicationConfigRepository;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentRepository;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.FileUtils;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class ApplicationCommand extends BuiltinParentCommand {

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      The main objective of this tool is to manage and execute HTTP requests. In terms of this application, HTTP requests are represented
      as sub-commands of this tool, which can be dynamically added, e.g. by importing an OpenAPI specification.
      
      Beside the definition of the HTTP commands itself, `${applicationName}` also provides builtin commands to define and apply predefined
      environments for command execution and manage output templates for transforming the responses of the HTTP commands.
          
      >
      
      In order to get information how to import an OpenAPI specification type `${applicationName} help cmd openapi`, or just import it
      via `${applicationName} cmd openapi <command-name> @<file-with-spec>`.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <sub-command> [<sub-command-args>]
      
      >The name of the sub-command to execute. If omitted, this help page is shown. For a list of available sub-commands see the list below.
       
      - c | --config <config>
      
      >If specified, it uses the given `<config>` for the application configuration. The configuration contains several settings that
      influence the behavior of this application (e.g. where to find commands, which environment to use, ...). If not specified,
      it uses the default configuration.
      
      -e | --environment <environment>
      
      >If specified, it uses the given `<environment>` for executing HTTP commands. If this option is omitted, the default environment is
      used; you can obtain the default environment setting by calling `${applicationName} cfg get environment`.
      
      -f | --format <format>
            
      >This option is only evaluated for sub-commands that execute HTTP requests. It defines an output format how to render the response of
      the HTTP request, specified as an input source, see section "Further Infos" for more information about the syntax. This option mutual
      excludes the option `--template`. Similar to named output templates, the `<format>` may contain placeholders that are replaced with
      value taken from the response and other locations. This option is an alternative to the `--template` option in case you want to quickly
      try output formats without having a named template (see `${applicationName} template` commands) in place.
      >Please type `${applicationName} help :templates` to learn more about templates and their syntax. If neither the `--template` nor
      the `--format` option is set, the default template is used (configured via the application configuration).
            
      -o | --output parameter <key>=<value>
            
      >This option is only evaluated for sub-commands that execute HTTP requests. Sets an output parameter for the output format. Which
      output parameters are available depends on the format specified via the `--format` option or the template given by the `--template`
      option.
            
      -t | --template <name-or-address>
            
      >This option is only evaluated for sub-commands that execute HTTP requests. Specifies the template name or address to use for
      rendering the output. This option mutual excludes the option `--format`. See section "Further Infos" for the syntax of template names
      and addresses.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_INPUT_SOURCE);

  @Inject
  ApplicationConfigRepository applicationConfigRepository;
  @Inject
  EnvironmentRepository environmentRepository;

  public ApplicationCommand() {
    super(CommandAddress.ROOT, "...",
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS
        ));
  }

  @Override
  public String name() {
    String applicationName = ProcessHandle.current().info().command().orElse(CommandContext.DEFAULT_APPLICATION_NAME);
    int pos = applicationName.lastIndexOf(FileSystems.getDefault().getSeparator());
    applicationName = applicationName.substring(pos + 1);
    if ("java".equals(applicationName)) {
      applicationName = CommandContext.DEFAULT_APPLICATION_NAME;
    }
    return applicationName;
  }

  @Override
  public String synopsis() {
    return "Utility to manage and execute REST calls.";
  }


  @Override
  public CommandLineSpec commandLineSpec() {
    return new CommandLineSpec(false, CMD_OPT_CONFIG, CMD_OPT_ENVIRONMENT, CMD_OPT_FORMAT, CMD_OPT_TEMPLATE, CMD_OPT_OUTPUT_PARAMETER,
        CMD_ARG_SUB_COMMAND);
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    Path configPath = determineConfigPath(commandLine.getValue(CMD_ARG_CONFIG_DIR).orElse(null));
    ApplicationConfig applicationConfig = loadApplicationConfig(configPath);
    Environment environment = loadEnvironment(configPath, applicationConfig, commandLine.getValue(CMD_ARG_ENVIRONMENT).orElse(null));

    context
        .rootCommandLine(commandLine)
        .applicationName(name())
        .configPath(configPath)
        .applicationConfig(applicationConfig)
        .out(context.out()
            .withStyles(context.applicationConfig().isOutputWithStyles())
            .withOutputWidth(context.applicationConfig().getOutputWidth()))
        .err(context.err()
            .withStyles(context.applicationConfig().isOutputWithStyles())
            .withOutputWidth(context.applicationConfig().getOutputWidth()))
        .environment(environment);

    return super.execute(context, commandLine);
  }

  Environment loadEnvironment(Path configPath, ApplicationConfig applicationConfig, String environmentName) {
    String environmentToLoad = environmentName;
    if (environmentName == null) {
      environmentToLoad = applicationConfig.getEnvironment();
    }

    if (Environment.EMPTY.equals(environmentToLoad)) {
      return Environment.empty();
    }

    Environment environment = environmentRepository.getEnvironment(configPath, environmentToLoad).orElse(null);
    if (environment != null) {
      return environment;
    }

    if (environmentName != null) {
      throw new ExecutionException("Environment \"%s\" does not exist.".formatted(environmentName));
    } else {
      throw new ExecutionException("""
          Environment "%s" (configured in the application configuration) does not exist.
          Call `%s -e "_empty" cfg set environment=<your-default-environment>` to fix the problem.
          """.formatted(name(), environmentName));
    }
  }


  ApplicationConfig loadApplicationConfig(Path configPath) {
    return applicationConfigRepository.getOrCreate(configPath);
  }

  Path determineConfigPath(String configDir) {
    Path path;
    if (configDir != null) {
      path = Path.of(configDir);
      if (!Files.isDirectory(path)) {
        throw new ExecutionException("No such directory \"%s\".".formatted(path.toAbsolutePath()));
      }
    } else {
      path = defaultConfigPath();
      FileUtils.createDirectoryIfNotExists(path);
    }
    return path;
  }

  Path defaultConfigPath() {
    return Path.of(System.getProperty("user.home")).resolve("." + name());
  }
}
