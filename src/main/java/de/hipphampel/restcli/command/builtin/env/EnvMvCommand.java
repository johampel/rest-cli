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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_NAME;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_REPLACE;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.assertEnvironmentName;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
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
public class EnvMvCommand extends EnvCommandBase {

  static final String NAME = "mv";

  static final Positional CMD_ARG_TARGET = positional("<target>")
      .build();
  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Renames environment `<name>` to `<target>`. If the target environment already exists, the `--replace` option must be specified in
      order to overwrite it.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <name>
            
      >Name environment to be moved.
            
      <target>
      
      >New name of the environment.
      
      -r | --replace
            
      >This option must be specified in case that the `<target>` environment already exists. If specified, it overwrites the existing
      environment.
      """);

  public EnvMvCommand() {
    super(
        NAME,
        "Renames an existing environment.",
        new CommandLineSpec(true, CMD_OPT_REPLACE, CMD_ARG_NAME, CMD_ARG_TARGET),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String name = commandLine.getValue(CMD_ARG_NAME).orElseThrow();
    String newName = commandLine.getValue(CMD_ARG_TARGET).orElseThrow();
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);

    move(context, name, newName, replace);
    return true;
  }

  void move(CommandContext context, String name, String newName, boolean replace) {
    if (Objects.equals(name, newName)) {
      throw new ExecutionException("`<name>` and `<target>` are identical.");
    }
    assertEnvironmentName(newName);

    Environment source = environmentRepository.getEnvironment(context.configPath(), name).orElseThrow(
        () -> new ExecutionException("Environment \"%s\" does not exist.".formatted(name)));
    boolean overwrite = environmentRepository.existsEnvironment(context.configPath(), newName);
    if (overwrite && !replace) {
      throw new ExecutionException("Environment \"%s\" already exists - use option `--replace` to replace it.".formatted(newName));
    }

    source.setName(newName);
    environmentRepository.storeEnvironment(context.configPath(), source, replace);
  }
}
