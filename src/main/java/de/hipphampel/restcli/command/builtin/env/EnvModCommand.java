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
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_HEADER;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_JSON;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_NAME;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_PARENT;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_ARG_VALUE;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_HEADER;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_JSON;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_NO_PARENT;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_PARENT;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_REPLACE;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_VALUE;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

@ApplicationScoped
@Unremovable
public class EnvModCommand extends EnvCommandBase {

  static final String NAME = "mod";

  static final Positional CMD_ARG_REQUEST_TIMEOUT = positional("<timeout>")
      .validator(Validators.POSITIVE_LONG_VALIDATOR)
      .build();
  static final Option CMD_OPT_REQUEST_TIMEOUT = option("--request-timeout")
      .parameter(CMD_ARG_REQUEST_TIMEOUT)
      .exclusionGroup("<requesttimeout>")
      .build();
  static final Option CMD_OPT_NO_REQUEST_TIMEOUT = option("--no-request-timeout")
      .exclusionGroup("<requesttimeout>")
      .build();
  static final Positional CMD_ARG_REMOVE_HEADER = positional("<key>")
      .build();
  static final Option CMD_OPT_REMOVE_HEADER = option("-H", "--remove-header")
      .parameter(CMD_ARG_REMOVE_HEADER)
      .repeatable()
      .build();
  static final Positional CMD_ARG_REMOVE_VARIABLE = positional("<variable>")
      .build();
  static final Option CMD_OPT_REMOVE_VARIABLE = option("-V", "--remove-variable")
      .parameter(CMD_ARG_REMOVE_VARIABLE)
      .repeatable()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Allows to modify the settings of an existing environment. Note that in order to change the name of the environment, you should use the
      `mv` command instead.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <name>
            
      >Name environment to be changed
            
      -h | --header <header>=<value>
       
      >Defines a new header `<header>` with the header value `<value>`. Note that header have a list of values. So it is legal to specific
      this option more than once with the same `<header>` value. Also, if the environment already has values for the given header, the new
      values are prepended to the list of values. In order to remove a header, use the `--remove-header` option. This option can be used
      more than once.

      -H | --remove-header <key>
            
      >Removes the header `<key>` from the environment. This option can be used more than once.
            
      -j | --json <key>=<json>
            
      >Sets the variable `<key>` having given the object `<json>`. The `<json>` is interpreted as a JSON string, so it might contain a
      complex data structure like a map or list. Any previous value is overwritten. In order to remove a variable, use the `--remove-variable`
      option. This option can be used more than once.

      --request-timeout <timeout> | --no-request-timeout
            
      >`--request-timeout` sets the request timout of the environment. `timeout` is measured in milliseconds. If this environment does not
      define a timeout, the timeout is inherited from the parent environment, or - if there is no parent - from the application
      configuration. In order to un-define the timeout use the `--no-request-timeout` option instead. 

      --no-parent
            
      >Unsets the parent of the environment. Mutual excludes the `--parent` option.

      -p | --parent <new-parent>
            
      >Specifies a new parent of the environment. In order to unset the parent for this environment use the `--no-parent` option instead.
            
      -v | --value <key>=<value>
            
      >Defines the variable `<key>` having given the string `<value>`. Any previous value is overwritten. In order to remove a variable, 
      use the `--remove-variable` option. This option can be used more than once.
            
      -V | --remove-variable <variable>
            
      >Removes the variable `<variable>`from the environment. This option can be used more than once. 
      """);

  public EnvModCommand() {
    super(
        NAME,
        "Modifies an existing environment.",
        new CommandLineSpec(true, CMD_OPT_PARENT, CMD_OPT_NO_PARENT, CMD_OPT_REQUEST_TIMEOUT, CMD_OPT_NO_REQUEST_TIMEOUT,
            CMD_OPT_VALUE, CMD_OPT_REMOVE_VARIABLE, CMD_OPT_JSON, CMD_OPT_HEADER, CMD_OPT_REMOVE_HEADER, CMD_ARG_NAME),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));

  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String name = commandLine.getValue(CMD_ARG_NAME).orElseThrow();
    String newParent = commandLine.getValue(CMD_ARG_PARENT).orElse(null);
    boolean noParent = commandLine.hasOption(CMD_OPT_NO_PARENT);
    boolean noRequestTimeout = commandLine.hasOption(CMD_OPT_NO_REQUEST_TIMEOUT);
    Long requestTimeout = commandLine.getValue(CMD_ARG_REQUEST_TIMEOUT)
        .map(Long::parseLong)
        .orElse(null);
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);
    List<String> values = Stream.concat(
        commandLine.getValues(CMD_ARG_REMOVE_VARIABLE).stream(),
        commandLine.getValues(CMD_ARG_VALUE).stream()).toList();
    List<String> jsons = commandLine.getValues(CMD_ARG_JSON);
    List<String> headers = Stream.concat(
        commandLine.getValues(CMD_ARG_REMOVE_HEADER).stream(),
        commandLine.getValues(CMD_ARG_HEADER).stream()).toList();
    Environment environment = environmentRepository.getEnvironment(context.configPath(), name)
        .orElseThrow(() -> new ExecutionException("Environment \"%s\" does not exist.".formatted(name)));

    boolean parentChanges =
        (noParent && environment.getParent() != null) || (newParent != null && !Objects.equals(newParent, environment.getParent()));
    boolean variablesChanges = !values.isEmpty() || !jsons.isEmpty();
    boolean headersChanges = !headers.isEmpty();

    if (variablesChanges) {
      setVariables(context, environment, values, jsons);
    }
    if (headersChanges) {
      EnvCommandUtils.setHeaders(context, environment, headers);
    }
    if (parentChanges) {
      environment.setParent(newParent);
    }
    if (noRequestTimeout) {
      environment.setRequestTimeout(null);
    } else if (requestTimeout != null) {
      environment.setRequestTimeout(requestTimeout);
    }

    environmentRepository.storeEnvironment(context.configPath(), environment, replace);

    return true;
  }

}
