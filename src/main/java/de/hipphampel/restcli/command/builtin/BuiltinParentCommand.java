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
package de.hipphampel.restcli.command.builtin;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.ParentCommand;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class BuiltinParentCommand extends BuiltinCommand implements ParentCommand {

  public BuiltinParentCommand(CommandAddress address, String synopsis, CommandLineSpec commandLineSpec,
      Map<HelpSection, Function<CommandContext, Block>> helpSections) {
    super(address, synopsis, commandLineSpec, helpSections);
  }

  public BuiltinParentCommand(CommandAddress address, String synopsis, Map<HelpSection, Function<CommandContext, Block>> helpSections) {
    this(address, synopsis, new CommandLineSpec(false, CMD_ARG_SUB_COMMAND), helpSections);
  }

  public BuiltinParentCommand(CommandAddress address, String synopsis) {
    this(address, synopsis, Map.of());
  }

  @Override
  public Optional<Block> helpSection(CommandContext context, HelpSection section) {
    return Optional.ofNullable(helpSections.get(section))
        .map(function -> function.apply(context))
        .or(() -> ParentCommand.super.helpSection(context, section));
  }
}
