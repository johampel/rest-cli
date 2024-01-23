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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
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
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class TemplateCpCommand extends TemplateCommandBase {

  static final String NAME = "cp";
  static final Positional CMD_ARG_SOURCE = positional("<source>")
      .validator(Validators.TEMPLATE_ADDRESS_VALIDATOR)
      .build();
  static final Option CMD_OPT_REPLACE = option("-r", "--replace")
      .build();
  static final Positional CMD_ARG_ADDRESS = positional("<address>")
      .validator(Validators.TEMPLATE_ADDRESS_VALIDATOR)
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Creates a copy of template `<source>` and stores it as a new template `<address>`. If the `<address>` template already exists, the 
      `--replace` option must be specified to overwrite it.

      Details about output templates can be found in the `templates` help topic, type `${applicationName} help :templates` to learn more
      about it.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <address>
            
      >The address of the output template to create, see section "Further Infos" for more information about the syntax.
            
      <source>
            
      >The address of the template to copy from, see section "Further Infos" for more information about the syntax.
            
      -r|--replace
            
      >This option is required in case you like to replace an already existing output template. Without this option the command will fail
      in case the output template already exists.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_INPUT_SOURCE +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_TEMPLATE_ADDRESS);

  public TemplateCpCommand() {
    super(NAME, "Copies an output template.",
        new CommandLineSpec(true, CMD_OPT_REPLACE, CMD_ARG_SOURCE, CMD_ARG_ADDRESS),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    TemplateAddress address = commandLine.getValue(CMD_ARG_ADDRESS)
        .map(TemplateAddress::fromString).orElseThrow();
    Template source = commandLine.getValue(CMD_ARG_SOURCE).map(TemplateAddress::fromString)
        .map(ta -> templateRepository.getEffectiveAddress(context.configPath(), ta)
            .flatMap(eta -> templateRepository.getTemplate(context.configPath(), eta))
            .orElseThrow(() -> new ExecutionException("Output template \"%s\" for command \"%s\" does not exist.".formatted(
                ta.name(), ta.command()))))
        .orElseThrow();
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);

    copy(context, address, source, replace);
    return true;
  }

  void copy(CommandContext context, TemplateAddress address, Template source, boolean replace) {
    checkWriteableTemplateAddress(context, address, replace);
    Template template = new Template(address, source.config(), false);
    templateRepository.storeTemplate(context.configPath(), template, replace);
  }
}