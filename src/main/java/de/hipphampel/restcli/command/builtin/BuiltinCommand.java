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
import de.hipphampel.restcli.command.Command;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.HelpSection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public abstract class BuiltinCommand implements Command {

  protected final CommandAddress address;
  protected final String synopsis;
  protected final Map<HelpSection, Function<CommandContext, Block>> helpSections;
  protected final CommandLineSpec commandLineSpec;

  public BuiltinCommand(CommandAddress address, String synopsis, CommandLineSpec commandLineSpec,
      Map<HelpSection, Function<CommandContext, Block>> helpSections) {
    this.address = Objects.requireNonNull(address);
    this.synopsis = Objects.requireNonNull(synopsis);
    this.helpSections = Objects.requireNonNull(helpSections);
    this.commandLineSpec = Objects.requireNonNull(commandLineSpec);
  }

  @Override
  public CommandAddress address() {
    return address;
  }

  @Override
  public String synopsis() {
    return synopsis;
  }

  @Override
  public CommandLineSpec commandLineSpec() {
    return commandLineSpec;
  }

  @Override
  public Optional<Block> helpSection(CommandContext context, HelpSection section) {
    return Optional.ofNullable(helpSections.get(section))
        .map(function -> function.apply(context))
        .or(() -> Command.super.helpSection(context, section));
  }

}
