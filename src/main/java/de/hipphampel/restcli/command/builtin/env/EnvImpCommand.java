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
package de.hipphampel.restcli.command.builtin.env;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_NAME;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_PARENT;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_SOURCE;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_NO_PARENT;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_PARENT;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_REPLACE;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentConfig;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class EnvImpCommand extends EnvCommandBase {

  static final String NAME = "imp";

  static final Option CMD_OPT_IGNORE_PARENT = option("--ignore-parent")
      .exclusionGroup("parent")
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Imports an environment based on the data that was previously exported via the `exp` command.
          
      The environment settings are read from the given `<source>` input source.
      If the target environment already exists, the `--replace` option must be set in order to overwrite the settings.
            
      Depending on the `--parent`, `--no-parent`, or `--ignore-parent` option the parent reference of the environment is set to the value
      found in the exported data, taken from the command line, or is unset.
      """);

  static final Function<CommandContext, Block> HELP_FURTHER_INFOS = CommandUtils.helpSection(HelpSnippets.FURTHER_INFOS_INPUT_SOURCE);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <name>
            
      >The name of the environment to import to. If the environment not exist yet, it is created, if it already exists, the `--replace`
      option must be set in order to overwrite the existing settings.
            
      <source>
            
      >The source where to read the environment settings from. This is specified as an *input source* (see section "Further Infos").
            
      --ignore-parent
            
      >Ignores the parent information from the imported settings. If the target environment does not exist yet, the target environment
      is created without a parent. If the target environment already exists, the parent is left unchanged. This option mutual excludes the
      options `--parent` and `--no-parent`.
            
      --no-parent
            
      >Ensures that the target environment has no parent after the import. If the target environment already exists, the reference to the
      parent is removed. This option mutual excludes the options `--parent` and `--ignore-parent`.
            
      -p | --parent <parent>
            
      >Sets the parent reference of the target environment to `<parent>` independent from the settings found in imported data or the maybe
      already existing target environment. This option mutual excludes the options `--no-parent` and `--ignore-parent`.

      -r | --replace
            
      >The option allows to overwrite existing environments. If the target environment already exists, this option must be set in order
      to import the data into it. If the target environment not exists, this option has no effect.
      """);

  protected EnvImpCommand() {
    super(NAME,
        "Imports an environment.",
        new CommandLineSpec(true, CMD_OPT_PARENT, CMD_OPT_IGNORE_PARENT, CMD_OPT_NO_PARENT, CMD_OPT_REPLACE, CMD_ARG_NAME, CMD_ARG_SOURCE),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String name = commandLine.getValue(CMD_ARG_NAME).orElseThrow();
    InputStreamProviderConfig source = commandLine.getValue(CMD_ARG_SOURCE)
        .map(InputStreamProviderConfig::fromString)
        .orElseThrow();
    String parent = commandLine.getValue(CMD_ARG_PARENT).orElse(null);
    boolean ignoreParent = commandLine.hasOption(CMD_OPT_IGNORE_PARENT);
    boolean noParent = commandLine.hasOption(CMD_OPT_NO_PARENT);
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);

    EnvCommandUtils.assertEnvironmentName(name);

    EnvironmentConfig config = readConfig(context, source);
    Environment target = getOrCreateTargetEnvironment(context, name, replace);
    setParent(context, target, parent, noParent, ignoreParent, config);

    target.setLocalHeaders(config.headers());
    target.setLocalVariables(config.variables());

    environmentRepository.storeEnvironment(context.configPath(), target, replace);
    return true;
  }

  Environment getOrCreateTargetEnvironment(CommandContext context, String name, boolean replace) {
    Environment environment = environmentRepository.getEnvironment(context.configPath(), name).orElse(null);
    if (environment != null && !replace) {
      throw new ExecutionException(
          "Environment \"%s\" already exists - use the --replace option if you want to overwrite it.".formatted(name));
    }
    environment = environmentRepository.createTransientEnvironment(name, null);
    return environment;
  }

  void setParent(CommandContext context, Environment environment, String parent, boolean noParent, boolean ignoreParent,
      EnvironmentConfig config) {
    if (parent != null) {
      environment.setParent(parent);
    } else if (noParent) {
      environment.setParent(null);
    } else if (!ignoreParent) {
      environment.setParent(config.parent());
    }

    if (environment.getParent() != null && !environmentRepository.existsEnvironment(context.configPath(), environment.getParent())) {
      throw new ExecutionException("No such environment \"%s\" (referenced as parent).".formatted(environment.getParent()));
    }
  }

  EnvironmentConfig readConfig(CommandContext context, InputStreamProviderConfig source) {
    try (InputStream in = CommandUtils.createInputStreamProvider(context, source, context.environment().getVariables()).open()) {
      return objectMapper.readValue(in, EnvironmentConfig.class);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to read environment settings: %s".formatted(ioe.getMessage()));
    }
  }
}
