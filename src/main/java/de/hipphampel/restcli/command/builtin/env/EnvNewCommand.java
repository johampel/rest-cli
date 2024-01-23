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
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_PARENT;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_REPLACE;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.CMD_OPT_VALUE;
import static de.hipphampel.restcli.command.builtin.env.EnvCommandUtils.assertEnvironmentName;

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
import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class EnvNewCommand extends EnvCommandBase {

  static final String NAME = "new";

  static final Positional CMD_ARG_REQUEST_TIMEOUT = positional("<timeout>")
      .validator(Validators.POSITIVE_LONG_VALIDATOR)
      .build();
  static final Option CMD_OPT_REQUEST_TIMEOUT = option("--request-timeout")
      .parameter(CMD_ARG_REQUEST_TIMEOUT)
      .build();
  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Creates a new environment.
            
      Upon creation, you may define its parent and the settings it should know.
            
      An alternative to create an environment is to use the `imp` command that imports an environment definition or the `cp` to create
      a copy from an existing one. Modifications on the environments can be done via the `mv` and `mod` commands.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <name>
            
      >The name of the environment to create.

      -h | --header <header>=<value>
       
      >Defines a new header `<header>` with the header value `<value>`. In opposite to variables, it is possible to define more than one
      string value for a header by using the `--header` option for the same `<header>` multiple times.
            
      -j | --json <key>=<json>
            
      >Defines the variable `<key>` having given the object `<json>`. `<json>` is interpreted as a JSON string, so it might contain a
      complex data structure like a map or list.

      -p | --parent <parent>
            
      >If specified, the created environment has the specified parent. An environment having a parent implicitly inherits the variables and
      settings of the parent, but it is allowed to overwrite them. The inheritance is dynamic, meaning if you change a setting in the parent,
      the change is also visible in this newly created environment.
            
      -r | --replace
            
      >This has only an effect, if the environment already exists. If set, then it allows to replace the already existing environment, of
      omitted, it is not allowed to overwrite already existing environments.
            
      --request-timeout <timeout>
            
      >Specifies the request timout of the environment. `timeout` is measured in milliseconds. If this environment does not define a timeout,
      the timeout is inherited from the parent environment, or - if there is no parent - from the application configuration.
            
      -v | --value <key>=<value>
            
      >Defines the variable `<key>` having given the string `<value>`. In case that you want to assign a more complex structure to a 
      variable, such as a list or map, you should use the `--json` option instead.
      """);

  public EnvNewCommand() {
    super(
        NAME,
        "Creates a new environment.",
        new CommandLineSpec(true, CMD_OPT_REPLACE, CMD_OPT_PARENT, CMD_OPT_REQUEST_TIMEOUT, CMD_OPT_VALUE, CMD_OPT_JSON, CMD_OPT_HEADER,
            CMD_ARG_NAME),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));

  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String name = commandLine.getValue(CMD_ARG_NAME).orElseThrow();
    String parent = commandLine.getValue(CMD_ARG_PARENT).orElse(null);
    Long requestTimeout = commandLine.getValue(CMD_ARG_REQUEST_TIMEOUT)
        .map(Long::parseLong)
        .orElse(null);
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);
    List<String> values = commandLine.getValues(CMD_ARG_VALUE);
    List<String> jsons = commandLine.getValues(CMD_ARG_JSON);
    List<String> headers = commandLine.getValues(CMD_ARG_HEADER);

    createEnvironment(context, name, parent, replace, values, jsons, headers, requestTimeout);

    return true;
  }

  void createEnvironment(CommandContext context, String name, String parent, boolean replace, List<String> values, List<String> jsons,
      List<String> headers, Long requestTimeout) {
    assertEnvironmentName(name);
    boolean overwrite = environmentRepository.existsEnvironment(context.configPath(), name);
    if (overwrite && !replace) {
      throw new ExecutionException("Environment \"%s\" already exists - use option `--replace` to replace it.".formatted(name));
    }

    Environment parentEnvironment = Optional.ofNullable(parent)
        .map(parentName -> environmentRepository.getEnvironment(context.configPath(), parentName)
            .orElseThrow(() -> new ExecutionException("Environment \"%s\" does not exist.".formatted(parent))))
        .orElse(null);
    Environment environment = environmentRepository.createTransientEnvironment(name, parentEnvironment);

    setVariables(context, environment, values, jsons);
    EnvCommandUtils.setHeaders(context, environment, headers);
    environment.setRequestTimeout(requestTimeout);

    environmentRepository.storeEnvironment(context.configPath(), environment, replace);
  }
}
