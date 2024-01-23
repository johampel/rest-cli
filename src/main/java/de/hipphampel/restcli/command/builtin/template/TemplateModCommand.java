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
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_ARG_ADDRESS;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_ARG_DESCRIPTION;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_ARG_VARIABLE;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_OPT_DESCRIPTION;
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_OPT_VARIABLE;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import de.hipphampel.restcli.template.TemplateConfig.Parameter;
import de.hipphampel.restcli.utils.KeyValue;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@ApplicationScoped
@Unremovable
public class TemplateModCommand extends TemplateCommandBase {

  static String NAME = "mod";

  static final Positional CMD_ARG_TEMPLATE = positional("<template>")
      .optional()
      .build();
  static final Positional CMD_ARG_REMOVE_VARIABLE = positional("<variable>")
      .build();

  static final Option CMD_OPT_REMOVE_VARIABLE = option("-V", "--remove-variable")
      .repeatable()
      .parameter(CMD_ARG_REMOVE_VARIABLE)
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Allows to modify the existing output template `<address>`. It is possible to modify it content, parameters and description; in order
      to rename it, use the `mv` command instead,
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <address>
            
      >The output template to change, see section "Further Infos" for more information about the syntax.
            
      <template>
            
      >The output template content. It can be omitted in case you do not want to change the content itself.
      This is specified as an input source, see section "Further Infos" for more information about the syntax.

      -d | --description <description>
            
      >The new description of the output template content.
      This is specified as an input source, see section "Further Infos" for more information about the syntax.
                             
      -v | --variable <key>=<value>
            
      >Defines the variable `<key>` having given the default value `<value>`.
            
      -V | --remove-variable <variable>
            
      >Remove the given variable.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_INPUT_SOURCE +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_TEMPLATE_ADDRESS);

  public TemplateModCommand() {
    super(NAME, "Modifies an output template.",
        new CommandLineSpec(true, CMD_OPT_DESCRIPTION, CMD_OPT_VARIABLE, CMD_OPT_REMOVE_VARIABLE, CMD_ARG_ADDRESS, CMD_ARG_TEMPLATE),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }


  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    Environment environment = context.environment();
    TemplateAddress address = commandLine.getValue(CMD_ARG_ADDRESS)
        .map(TemplateAddress::fromString)
        .orElse(null);
    String description = commandLine.getValue(CMD_ARG_DESCRIPTION)
        .map(InputStreamProviderConfig::fromString)
        .map(config -> CommandUtils.toString(context, config, environment.getVariables()))
        .orElse(null);
    List<KeyValue<String>> variables = Stream.concat(
            commandLine.getValues(CMD_ARG_REMOVE_VARIABLE).stream(),
            commandLine.getValues(CMD_ARG_VARIABLE).stream())
        .map(KeyValue::fromString)
        .toList();
    String content = commandLine.getValue(CMD_ARG_TEMPLATE)
        .map(InputStreamProviderConfig::fromString)
        .map(config -> CommandUtils.toString(context, config, environment.getVariables()))
        .orElse(null);

    modify(context, address, description, variables, content);
    return true;
  }

  void modify(CommandContext context, TemplateAddress address, String description,
      List<KeyValue<String>> variables, String content) {
    Template template = templateRepository.getTemplate(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Output template \"%s\" does not exist.".formatted(address)));

    TemplateConfig config = template.config();
    Map<String, Parameter> parameters = config.parameters();
    if (description == null) {
      description = config.description();
    }

    if (!variables.isEmpty()) {
      parameters = getParameters(context, parameters, variables);
    }
    if (content == null) {
      content = config.content();
    }

    Template newTemplate = new Template(
        address,
        new TemplateConfig(description, parameters, content),
        false);

    templateRepository.storeTemplate(context.configPath(), newTemplate, true);
  }

  Map<String, Parameter> getParameters(CommandContext context, Map<String, Parameter> original, List<KeyValue<String>> changes) {
    if (changes.isEmpty()) {
      return original;
    }

    Set<String> changeSet = new HashSet<>();
    Map<String, Parameter> parameters = new HashMap<>(original);
    for (KeyValue<String> kav : changes) {
      String name = kav.key();
      if (!changeSet.add(name)) {
        CommandUtils.showWarning(context, "Variable \"%s\" modified twice - using last definition.", name);
      }
      if (kav.hasValue()) {
        parameters.put(name, new Parameter(kav.value()));
      } else {
        if (!parameters.containsKey(name)) {
          CommandUtils.showWarning(context, "Removal of not existing variable \"%s\".", name);
        }
        parameters.remove(name);
      }
    }
    return parameters;
  }
}
