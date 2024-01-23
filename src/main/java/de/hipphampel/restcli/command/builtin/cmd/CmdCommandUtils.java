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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLine.Subset;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterConfig.Style;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.exception.UsageException;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.utils.KeyValue;
import java.util.List;
import java.util.Map;

public class CmdCommandUtils {

  public static final Positional CMD_ARG_ADDRESS = positional("<address>")
      .validator(Validators.COMMAND_ADDRESS_VALIDATOR)
      .build();

  public static final Positional CMD_ARG_SYNOPSIS = positional("<synopsis>")
      .build();
  public static final Option CMD_OPT_SYNOPSIS = option("-s", "--synopsis")
      .parameter(CMD_ARG_SYNOPSIS)
      .build();
  public static final Option CMD_OPT_EXCLUDE_CHILDREN = option("-e", "--exclude-children")
      .build();
  public static final Option CMD_OPT_FORCE = option("-f", "--force")
      .build();
  public static final Option CMD_OPT_REPLACE = option("-r", "--replace")
      .parameter(CMD_OPT_FORCE)
      .build();
  public static final Positional CMD_ARG_DESCRIPTION = positional("<description>")
      .build();
  public static final Option CMD_OPT_SECTION_MAIN = option("--main")
      .exclusionGroup("help-section")
      .build();
  public static final Option CMD_OPT_SECTION_INFOS = option("--infos")
      .exclusionGroup("help-section")
      .build();
  public static final Option CMD_OPT_SECTION_ARGUMENTS = option("--arguments")
      .exclusionGroup("help-section")
      .build();
  public static final Option CMD_OPT_DESCRIPTION = option("-d", "--description")
      .parameter(CMD_ARG_DESCRIPTION, CMD_OPT_SECTION_ARGUMENTS, CMD_OPT_SECTION_MAIN, CMD_OPT_SECTION_INFOS)
      .repeatable()
      .build();
  public static final Positional CMD_ARG_METHOD = positional("<method>")
      .build();
  public static final Positional CMD_ARG_BASE_URI = positional("<base-uri>")
      .build();
  public static final Positional CMD_ARG_POSITIONAL_NAME_VALUE = positional("<name>=<variable>")
      .id("for-positional")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_POSITIONAL = option("-p", "--positional")
      .parameter(CMD_ARG_POSITIONAL_NAME_VALUE)
      .repeatable()
      .build();
  public static final Positional CMD_ARG_OPTION_NAME_VALUE = positional("<name>=<variable>")
      .id("for-option")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_OPTION = option("-o", "--option")
      .parameter(CMD_ARG_OPTION_NAME_VALUE)
      .repeatable()
      .build();
  public static final Positional CMD_ARG_QUERY_PARAMETER = positional("<key>=<value>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_QUERY_PARAMETER = option("-q", "--query")
      .parameter(CMD_ARG_QUERY_PARAMETER)
      .repeatable()
      .build();
  public static final Positional CMD_ARG_HEADER = positional("<header>=<value>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_HEADER = option("-h", "--header")
      .parameter(CMD_ARG_HEADER)
      .repeatable()
      .build();
  public static final Option CMD_OPT_LOAD_BODY = option("-l", "--load-body")
      .build();

  public static Map<String, List<String>> collectHeaders(CommandContext context, CommandLine commandLine,
      Map<String, List<String>> headers) {
    List<KeyValue<String>> keyValues = commandLine.getValues(CMD_ARG_HEADER).stream()
        .map(KeyValue::fromString)
        .toList();
    return CommandUtils.fillHeaders(context, headers, keyValues, false);
  }

  public static Map<String, String> collectQueryParameters(CommandLine commandLine, Map<String, String> parameters) {
    for (String entry : commandLine.getValues(CMD_ARG_QUERY_PARAMETER)) {
      KeyValue<String> keyValue = KeyValue.fromString(entry);
      if (parameters.containsKey(keyValue.key())) {
        throw new ExecutionException("Duplicate definition of query parameter \"%s\".".formatted(keyValue.key()));
      }
      parameters.put(keyValue.key(), keyValue.value());
    }
    return parameters;
  }

  public static Map<HelpSection, String> collectDescriptions(CommandContext context, CommandLine commandLine,
      Map<HelpSection, String> descriptions) {
    if (!commandLine.hasOption(CMD_OPT_DESCRIPTION)) {
      return descriptions;
    }

    for (Subset subset : commandLine.getSubsets(CMD_OPT_DESCRIPTION)) {
      HelpSection section = HelpSection.DESCRIPTION;
      if (subset.hasOption(CMD_OPT_SECTION_INFOS)) {
        section = HelpSection.FURTHER_INFOS;
      } else if (subset.hasOption(CMD_OPT_SECTION_ARGUMENTS)) {
        section = HelpSection.ARGS_AND_OPTIONS;
      }
      if (descriptions.containsKey(section)) {
        throw new UsageException("Specified more than one description for the same section.");
      }
      String description = subset.getValue(CMD_ARG_DESCRIPTION)
          .map(InputStreamProviderConfig::fromString)
          .map(config -> CommandUtils.toString(context, config, context.environment().getVariables()))
          .orElseThrow();
      descriptions.put(section, description);
    }
    return descriptions;
  }

  public static List<ParameterConfig> collectParameters(CommandLine commandLine, List<ParameterConfig> parameters) {
    commandLine.getValues(CMD_ARG_POSITIONAL_NAME_VALUE).stream()
        .map(KeyValue::fromString)
        .map(kv -> new ParameterConfig(
            Style.RequiredPositional,
            kv.key(),
            kv.value(),
            List.of()))
        .forEach(parameters::add);

    commandLine.getValues(CMD_ARG_OPTION_NAME_VALUE).stream()
        .map(KeyValue::fromString)
        .map(kv -> new ParameterConfig(
            Style.SingleOption,
            kv.key(),
            kv.value(),
            List.of(kv.key().length() == 1 ? "-" + kv.key() : "--" + kv.key())))
        .forEach(parameters::add);
    return parameters;
  }

  public static BodyConfig createBodyConfig(CommandContext context, CommandLine commandLine, Positional bodyArg) {
    String body = commandLine.getValue(bodyArg).orElse(null);
    if (body == null) {
      return null;
    }
    if (body.startsWith("${") && body.endsWith("}")) {
      return new BodyConfig(
          null,
          body.substring(2, body.length() - 3));
    }

    InputStreamProviderConfig rawBody = InputStreamProviderConfig.fromString(commandLine.getValue(bodyArg).orElse(null));
    if (rawBody == null || !commandLine.hasOption(CMD_OPT_LOAD_BODY) || rawBody.type() == InputStreamProviderConfig.Type.string) {
      return new BodyConfig(
          rawBody,
          null);
    }

    InputStreamProviderConfig loadBody = new InputStreamProviderConfig(rawBody.type(), rawBody.value(), false);
    String loadedContent = CommandUtils.toString(context, loadBody, Map.of());

    return new BodyConfig(
        new InputStreamProviderConfig(InputStreamProviderConfig.Type.string, loadedContent, rawBody.interpolate()),
        null);
  }
}
