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

import com.fasterxml.jackson.databind.ObjectWriter;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentConfig;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class EnvExpCommand extends EnvCommandBase {

  static final String NAME = "exp";

  static final Option CMD_OPT_LOCAL = option("-l", "--local")
      .build();
  static final Positional CMD_ARG_NAME = positional("<name>")
      .build();
  static final Positional CMD_ARG_TARGET = positional("<target>")
      .optional()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Exports the specified environment with the intention to import it (in a different installation) using the `imp` command.
            
      When no options are given, the environment is exported as a standalone environment, containing all settings - even those inherited
      from the parent, but without a reference to the parent. If the `--local` option is given, it exports only the settings that made
      locally for the environment itself.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <name>
            
      >The name of the environment to export.
            
      <target>
            
      >The file where to export to. If omitted, the export is written to standard output.
            
      -l | --local
            
      >If set, only the settings locally defined for this environment are exported. If omitted, all settings, including those inherited from
      the parent are exported.
      """);

  protected EnvExpCommand() {
    super(NAME,
        "Exports an environment.",
        new CommandLineSpec(true, CMD_OPT_LOCAL, CMD_ARG_NAME, CMD_ARG_TARGET),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String name = commandLine.getValue(CMD_ARG_NAME).orElseThrow();
    String target = commandLine.getValue(CMD_ARG_TARGET).orElse(null);
    boolean local = commandLine.hasOption(CMD_OPT_LOCAL);

    exportEnvironment(context, name, target, local);
    return true;
  }

  void exportEnvironment(CommandContext context, String name, String target, boolean local) {
    Environment environment = environmentRepository.getEnvironment(context.configPath(), name)
        .orElseThrow(() -> new ExecutionException("Environment \"%s\" does not exist.".formatted(name)));
    EnvironmentConfig config = new EnvironmentConfig(
        local ? environment.getParent() : null,
        new TreeMap<>(local ? environment.getLocalVariables() : environment.getVariables()),
        new TreeMap<>(local ? environment.getLocalHeaders() : environment.getHeaders()),
        environment.getRequestTimeout()
    );

    try {
      ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
      if (target != null) {
        writer.writeValue(new File(target), config);
      } else {
        StringWriter buffer = new StringWriter();
        writer.writeValue(buffer, config);
        context.out().line(buffer.toString());
      }
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to export environment \"%s\": %s".formatted(name, ioe.getMessage()));
    }
  }
}
