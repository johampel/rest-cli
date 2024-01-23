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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.config.ApplicationConfig;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.KeyValue;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class CfgSetCommand extends CfgCommandBase {

  static final String NAME = "set";

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Sets or resets the values of one or more application configuration settings.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <key>[=<value>]
            
      >Either sets or resets a application configuration setting to its default value. The argument comes in two forms.
      If the argument has the format `<key>=<value>` it sets the application configuration `<key>` to `<value>`. If the value is omitted,
      so only `<key>` without an equal sign is specified, the application configuration setting will be reset the to application default
      value.
      """);

  static final Positional CMD_ARG_SETTING = positional("<key>[=<value>]")
      .repeatable()
      .build();

  public CfgSetCommand() {
    super(NAME,
        "Sets or resets one or more application configuration settings.",
        new CommandLineSpec(true, CMD_ARG_SETTING),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    List<KeyValue<String>> settings = commandLine.getValues(CMD_ARG_SETTING).stream()
        .map(KeyValue::fromString)
        .toList();
    execute(context, settings);
    return true;
  }

  void execute(CommandContext context, List<KeyValue<String>> settings) {
    ApplicationConfig config = applicationConfigRepository.getOrCreate(context.configPath());
    ApplicationConfig defaults = new ApplicationConfig();

    for (KeyValue<String> setting : settings) {
      applySetting(context, config, setting, defaults);
    }

    applicationConfigRepository.store(context.configPath(), config);
  }

  void applySetting(CommandContext context, ApplicationConfig config, KeyValue<String> setting, ApplicationConfig defaults) {
    ConfigKey key = getKey(setting.key())
        .orElseThrow(() -> new ExecutionException("No such configuration key \"%s\".".formatted(setting.key())));
    String value = setting.hasValue() ? setting.value() : key.getGetter().apply(defaults);
    try {
      key.getSetter().set(this, context, config, value);
    } catch (Exception e) {
      throw new ExecutionException("Failed to set \"%s\" to \"%s\": %s"
          .formatted(key.getName(), value, e.getMessage()));
    }
  }


}
