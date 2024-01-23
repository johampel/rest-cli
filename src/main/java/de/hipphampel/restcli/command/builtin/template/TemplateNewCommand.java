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
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_ARG_DESCRIPTION;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_ARG_VARIABLE;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_OPT_DESCRIPTION;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_OPT_REPLACE;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_OPT_VARIABLE;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import de.hipphampel.restcli.template.TemplateConfig.Parameter;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class TemplateNewCommand extends TemplateCommandBase {

  static final String NAME = "new";

  static final Positional CMD_ARG_TEMPLATE = positional("<template>")
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
      Creates a new output template, either based on an existing one or from scratch. The output template can be adapted via the `mod`
      command after creation.

      Details about output templates can be found in the `templates` help topic, type `${applicationName} help :templates` to learn more
      about it.
            
      An alternative way to create an output template is the usage of the `imp` command which imports previously exported templates, or
      `cp`, which copies templates.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <address>
            
      >The address of the output template to create, see section "Further Infos" for more information about the syntax.
            
      <template>
            
      >The content of the template, specified as an *input source*, see section "Further Infos" for more information about the syntax.
            
      -d | --description <description>
            
      >The description of the template, specified as an input source, see section "Further Infos" for more information about the syntax.
            
      -r | --replace
            
      >This option is required in case you like to replace an already existing output template. Without this option the command will fail
      in case the output template already exists.
            
      -v | --variable <key>=<value>
            
      >Defines the output parameter `<key>` having the default value `<value>`. Each output template parameter should have such a default
      value.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_INPUT_SOURCE +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_TEMPLATE_ADDRESS);

  public TemplateNewCommand() {
    super(NAME, "Creates a new output template.",
        new CommandLineSpec(true, CMD_OPT_REPLACE, CMD_OPT_VARIABLE, CMD_OPT_DESCRIPTION, CMD_ARG_ADDRESS, CMD_ARG_TEMPLATE),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    TemplateAddress address = commandLine.getValue(CMD_ARG_ADDRESS)
        .map(TemplateAddress::fromString).orElseThrow();
    InputStreamProviderConfig description = commandLine.getValue(CMD_ARG_DESCRIPTION).map(InputStreamProviderConfig::fromString)
        .orElse(null);
    List<Pair<String, String>> variables = commandLine.getValues(CMD_ARG_VARIABLE).stream().map(Pair::fromString).toList();
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);
    InputStreamProviderConfig template = commandLine.getValue(CMD_ARG_TEMPLATE).map(InputStreamProviderConfig::fromString).orElse(null);

    createTemplate(context, address, description, variables, template, replace);
    return true;
  }

  void createTemplate(CommandContext context, TemplateAddress address, InputStreamProviderConfig description,
      List<Pair<String, String>> variables, InputStreamProviderConfig content, boolean replace) {
    checkWriteableTemplateAddress(context, address, replace);

    String contentString = CommandUtils.toString(context, content, context.environment().getVariables());
    String descriptionString = description == null ? null : CommandUtils.toString(context, description, context.environment().getVariables());
    Map<String, Parameter> parameters = getParameters(context, variables);

    Template template = new Template(
        address,
        new TemplateConfig(
            descriptionString,
            parameters,
            contentString),
        false);

    templateRepository.storeTemplate(context.configPath(), template, replace);
  }

  Map<String, Parameter> getParameters(CommandContext context, List<Pair<String, String>> variables) {
    Map<String, Parameter> parameters = new HashMap<>();
    for (Pair<String, String> entry : variables) {
      if (parameters.containsKey(entry.first())) {
        CommandUtils.showWarning(context, "Parameter \"%s\" modified twice - using last definition.", entry.first());
      }
      parameters.put(entry.first(), new Parameter(entry.second()));
    }
    return parameters;
  }
}