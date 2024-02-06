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
package de.hipphampel.restcli.command.custom;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.config.CommandConfig;
import java.util.ArrayList;
import java.util.List;

public class CustomAliasCommand extends CustomCommand {

  static final Positional CMD_ARG_ALIAS_ARGS = positional("<alias-args>")
      .repeatable()
      .optional()
      .build();

  public CustomAliasCommand(CommandAddress address, CommandConfig config) {
    super(address, config);
  }

  @Override
  public CommandLineSpec commandLineSpec() {
    return new CommandLineSpec(false, CMD_ARG_ALIAS_ARGS);
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    List<String> args = new ArrayList<>(config().getAliasConfig());
    args.addAll(commandLine.getValues(CMD_ARG_ALIAS_ARGS));
    return context.commandInvoker().invokeAliasCommand(context, args, null, null);
  }
}
