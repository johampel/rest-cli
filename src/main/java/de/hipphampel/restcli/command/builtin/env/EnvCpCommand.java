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
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.assertEnvironmentName;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class EnvCpCommand extends EnvCommandBase {

  static final String NAME = "cp";

  static final Option CMD_OPT_DEEP = option("-d", "--deep")
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Creates a copy of environment `<source>` and stores it as environment `<name>`. Depending on the options passed to this command, the
      copy might have a different parent than the original one and the settings of the copy might include the settings inherited from the
      parent of the `<source>` or just the settings directly set on the `<source>` environment.
            
      If the `<name>` environment already exists, the `--replace` option must be specified in order to overwrite it.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <source>
            
      >Name environment to copy from.
            
      <name>
            
      >Name of the copy to create.
            
      -d | --deep
            
      >If specified, create a deep copy of the `<source>` environment, so that the environment `<name>` contains also the settings that
      the `<source>` environment inherits from its parent. If omitted, the copy contains only the settings directly set for the `<source>`
      environment.
            
      -n | --no parent
            
      >If specified, the link to parent of the `<source>` environment is not copied to environment `<name>`. This mutual excludes the option
      `--parent`. If neither option `--no-parent` nor `--parent` is used, the link to the parent will be copied to `<name>`.
            
      -p | --parent <parent>
            
      >If specified, the parent of the `<name>` environment will be `<parent>`. This mutual excludes the option `--no-parent`. If neither
      option `--no-parent` nor `--parent` is used, the link to the parent will be copied to `<name>`.
            
      -r | --replace
            
      >This option must be specified in case that the `<name>` environment already exists. If specified, it overwrites the existing
      environment.
      """);

  public EnvCpCommand() {
    super(
        NAME,
        "Creates a copy of an environment.",
        new CommandLineSpec(true, CMD_OPT_DEEP, CMD_OPT_PARENT, CMD_OPT_NO_PARENT, CMD_OPT_REPLACE, CMD_ARG_SOURCE, CMD_ARG_NAME),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String name = commandLine.getValue(CMD_ARG_NAME).orElseThrow();
    String source = commandLine.getValue(CMD_ARG_SOURCE).orElseThrow();
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);
    boolean deep = commandLine.hasOption(CMD_OPT_DEEP);
    boolean noParent = commandLine.hasOption(CMD_OPT_NO_PARENT);
    String newParent = commandLine.getValue(CMD_ARG_PARENT).orElse(null);

    copy(context, source, name, replace, deep, noParent, newParent);
    return true;
  }

  void copy(CommandContext context, String source, String name, boolean replace, boolean deep, boolean noParent,
      String newParent) {
    if (Objects.equals(name, source)) {
      throw new ExecutionException("`<name>` and `<source>` are identical.");
    }
    assertEnvironmentName(name);

    Environment sourceEnvironment = environmentRepository.getEnvironment(context.configPath(), source)
        .orElseThrow(() -> new ExecutionException("Environment \"%s\" does not exist.".formatted(source)));
    boolean overwrite = environmentRepository.existsEnvironment(context.configPath(), name);
    if (overwrite && !replace) {
      throw new ExecutionException("Environment \"%s\" already exists - use option `--replace` to replace it.".formatted(name));
    }

    Environment parentEnvironment = getParentEnvironment(context, sourceEnvironment, noParent, newParent);
    Environment environment = environmentRepository.createTransientEnvironment(name, parentEnvironment);
    if (deep) {
      environment.setLocalVariables(sourceEnvironment.getVariables());
      environment.setLocalHeaders(sourceEnvironment.getHeaders());
      environment.setRequestTimeout(sourceEnvironment.getRequestTimeout());
    } else {
      environment.setLocalVariables(sourceEnvironment.getLocalVariables());
      environment.setLocalHeaders(sourceEnvironment.getLocalHeaders());
      environment.setRequestTimeout(sourceEnvironment.getLocalConfig().requestTimeout());
    }

    environmentRepository.storeEnvironment(context.configPath(), environment, true);
  }


  Environment getParentEnvironment(CommandContext context, Environment sourceEnvironment, boolean noParent, String newParent) {
    if (noParent) {
      return null;
    }
    String parentName = newParent == null ? sourceEnvironment.getParent() : newParent;
    if (parentName == null) {
      return null;
    }
    return environmentRepository.getEnvironment(context.configPath(), parentName)
        .orElseThrow(() -> new ExecutionException("Environment \"%s\" does not exist.".formatted(parentName)));
  }

}
