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
package de.hipphampel.restcli.command.builtin.cfg;

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
import de.hipphampel.restcli.config.ApplicationConfig;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class CfgGetCommand extends CfgCommandBase {

  static final String NAME = "get";

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Prints the values of one or more application configuration settings. When omitting any `<key>` all configuration settings are printed,
      otherwise only the specified ones.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <key>
            
      >The key of the application configurations setting to print. This argument can be specified more than once in order to print multiple
      configuration settings. If no `<key>` is specified, all configuration settings are printed.
            
      -b|--beautify
            
      >If specified, the output is decorated with a table header describing the meaning of the columns and table borders. Without this option,
      no table header or borders are printed.
          """);
  static final Positional CMD_ARG_KEY = positional("<key>")
      .repeatable()
      .optional()
      .build();
  static final Option CMD_OPT_BEAUTIFY = option("-b", "--beautify")
      .build();

  public CfgGetCommand() {
    super(NAME,
        "Gets one or more settings from the application configuration.",
        new CommandLineSpec(true, CMD_OPT_BEAUTIFY, CMD_ARG_KEY),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    List<String> keys = commandLine.getValues(CMD_ARG_KEY);
    boolean decorated = commandLine.hasOption(CMD_OPT_BEAUTIFY);

    execute(context, decorated, keys);
    return true;
  }

  void execute(CommandContext context, boolean decorated, List<String> keys) {
    String ls = System.lineSeparator();
    StringBuilder buffer = new StringBuilder();
    if (decorated) {
      buffer.append("| Key | Value |").append(ls);
    }
    buffer.append("|---|---|").append(ls);
    if (keys.isEmpty()) {
      keys = Arrays.stream(ConfigKey.values()).map(ConfigKey::getName).toList();
    }

    ApplicationConfig config = context.applicationConfig();
    getExistingKeysAndThrowIfAllAreUnknown(context, keys)
        .forEach(key -> buffer.append("|").append(key.getName()).append("|").append(key.getGetter().apply(config)).append("|").append(ls));

    context.out().markdown(buffer.toString());
  }


  List<ConfigKey> getExistingKeysAndThrowIfAllAreUnknown(CommandContext context, List<String> keys) {
    List<ConfigKey> configKeys = keys.stream()
        .map(key -> getKeyOrWarn(context, key))
        .filter(Objects::nonNull)
        .toList();
    if (configKeys.isEmpty()) {
      throw new ExecutionException("None of the given keys is known.");
    }
    return configKeys;
  }

  ConfigKey getKeyOrWarn(CommandContext context, String key) {
    return getKey(key)
        .orElseGet(() -> {
          CommandUtils.showWarning(context, "Unknown configuration key \"%s\".".formatted(key));
          return null;
        });
  }
}
