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
package de.hipphampel.restcli.command.builtin.template;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandInfo;
import de.hipphampel.restcli.command.CommandRepository;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.builtin.BuiltinCommand;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateRepository;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.function.Function;

public abstract class TemplateCommandBase extends BuiltinCommand {

  @Inject
  protected TemplateRepository templateRepository;

  @Inject
  protected CommandRepository commandRepository;

  protected TemplateCommandBase(String name, String synopsis, CommandLineSpec commandLineSpec,
      Map<HelpSection, Function<CommandContext, Block>> helpSections) {
    super(
        CommandAddress.fromString(TemplateCommandParent.NAME).child(name),
        synopsis,
        commandLineSpec,
        helpSections);
  }

  protected void checkWriteableTemplateAddress(CommandContext context, TemplateAddress address, boolean replace) {
    if (templateRepository.existsTemplate(context.configPath(), address) && !replace) {
      throw new ExecutionException(
          "Output template \"%s\" for command \"%s\" already exists. Use `--replace` option if you wish to overwrite.".formatted(
              address.name(), address.command()));
    }
    if (templateRepository.isBuiltin(context.configPath(), address)) {
      throw new ExecutionException(
          "Output template \"%s\" for command \"%s\" is a builtin and cannot be overwritten.".formatted(address.name(), address.command()));
    }

    if (address.command().equals(CommandAddress.ROOT)) {
      return;
    }

    CommandInfo info = commandRepository.getCommandInfo(context.configPath(), address.command()).orElse(null);
    if (info==null) {
      throw new ExecutionException("Command \"%s\" does not exist.".formatted(address.command()));
    }else if (info.builtin()) {
      throw new ExecutionException("Command \"%s\" is not suitable for output templates.".formatted(address.command()));
    }
  }
}
