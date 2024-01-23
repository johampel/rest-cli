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
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class EnvRmCommand extends EnvCommandBase {

  static final String NAME = "rm";

  static final Positional CMD_ARG_NAME = positional("<name>")
      .repeatable()
      .build();
  static final Option CMD_OPT_FORCE = option("-f", "--force")
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Delete the environments specified by the `<name>` parameter. When an environment has a child environment,
      the `--force` option has to be specified in order to delete the child environments as well.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <name>
            
      >Name of the environment to delete. Can be specified more than one.
            
      -f | --force
            
      >Must be used in case the environment to delete has at least one child environment. If so, the child environments
      are deleted as well.
      """);

  public EnvRmCommand() {
    super(
        NAME,
        "Removes environments.",
        new CommandLineSpec(true, CMD_OPT_FORCE, CMD_ARG_NAME),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));

  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    List<String> names = commandLine.getValues(CMD_ARG_NAME);
    boolean force = commandLine.hasOption(CMD_OPT_FORCE);

    checkForRemoval(context, names, force);
    names.forEach(name -> remove(context, name));
    return true;
  }

  void checkForRemoval(CommandContext context, List<String> names, boolean force) {
    for (String name : names) {
      if (!environmentRepository.existsEnvironment(context.configPath(), name)) {
        throw new ExecutionException("Environment \"%s\" does not exist.".formatted(name));
      }

      List<String> children = environmentRepository.listEnvironments(context.configPath(), name);
      if (!children.isEmpty() && !force) {
        throw new ExecutionException(
            "Environment \"%s\" has child environments - use --force option, if you really want to delete.".formatted(name));
      }
    }
  }

  void remove(CommandContext context, String name) {
    environmentRepository.listEnvironments(context.configPath(), name).forEach(child -> remove(context, child));
    environmentRepository.deleteEnvironment(context.configPath(), name, false);
  }
}
