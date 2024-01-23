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

import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.ParentCommand;
import de.hipphampel.restcli.command.config.CommandConfig;
import java.util.Optional;

public class CustomParentCommand extends CustomCommand implements ParentCommand {

  public CustomParentCommand(CommandAddress address, CommandConfig config) {
    super(address, config);
  }

  @Override
  public CommandLineSpec commandLineSpec() {
    return ParentCommand.super.commandLineSpec();
  }

  @Override
  public Optional<Block> helpSection(CommandContext context, HelpSection section) {
    return Optional.ofNullable(config().getDescriptions().get(section))
        .map(description -> CommandUtils.helpSection(description).apply(context))
        .or(() -> ParentCommand.super.helpSection(context, section));
  }
}
