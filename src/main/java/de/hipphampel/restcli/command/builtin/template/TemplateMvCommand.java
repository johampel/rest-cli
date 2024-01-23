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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_ARG_ADDRESS;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_OPT_REPLACE;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class TemplateMvCommand extends TemplateCommandBase {

  static String NAME = "mv";

  static final Positional CMD_ARG_NEW_ADDRESS = positional("<new-address>")
      .build();
  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Allows to rename the existing output template `<address>` to `<new-address>`.
            
      In order to modify other aspects of the output template, use the `mod` command instead.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <address>
            
      >The output template to move, see section "Further Infos" for more information about the syntax.
            
      <new-address>

      >The output template to move to, see section "Further Infos" for more information about the syntax. If the target
      template already exists, the `--replace` option needs to provided in order to allows to overwrite it.
            
      -r | --replace
            
      >This option allows to overwrite existing output templates.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_INPUT_SOURCE +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_TEMPLATE_ADDRESS);

  public TemplateMvCommand() {
    super(NAME, "Renames an output template.",
        new CommandLineSpec(true, CMD_OPT_REPLACE, CMD_ARG_ADDRESS, CMD_ARG_NEW_ADDRESS),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }


  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    TemplateAddress address = commandLine.getValue(CMD_ARG_ADDRESS)
        .map(TemplateAddress::fromString)
        .orElse(null);
    TemplateAddress newAddress = commandLine.getValue(CMD_ARG_NEW_ADDRESS)
        .map(TemplateAddress::fromString)
        .orElse(null);
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);
    move(context, address, newAddress, replace);
    return true;
  }

  void move(CommandContext context, TemplateAddress address, TemplateAddress newAddress, boolean replace) {
    if (Objects.equals(address, newAddress)) {
      return;
    }

    checkWriteableTemplateAddress(context, newAddress, replace);
    Template template = templateRepository.getTemplate(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Output template \"%s\" does not exist.".formatted(address)));

    Template newTemplate = new Template(newAddress, template.config(), false);
    templateRepository.storeTemplate(context.configPath(), newTemplate, true);
    templateRepository.deleteTemplate(context.configPath(), address);
  }
}
