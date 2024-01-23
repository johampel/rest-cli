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
import java.util.Map.Entry;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class TemplateGetCommand extends TemplateCommandBase {

  static String NAME = "get";

  static final Option CMD_OPT_BEAUTIFY = option("-b", "--beautify")
      .build();
  static final Option CMD_OPT_CONTENT = option("-c", "--content")
      .build();
  static final Option CMD_OPT_VARIABLES = option("-v", "--variables")
      .build();
  static final Positional CMD_ARG_TEMPLATE = positional("<template-address>")
      .validator(Validators.TEMPLATE_ADDRESS_VALIDATOR)
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Gets the template definition and its variables.
            
      Depending on the options provided, both content of the template and variables are shown only one of both.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <template-address>
            
      >The address of the template you want to get. See section "Further Infos" for more information about the syntax.
            
      -b | --beautify
            
      >Add some decorations to the output to let look it more nicely.
            
      -c | --content
            
      >If this option is set (and no `--variables` option is given), only the content of the template is shown. If both options,
      `--content` and `--variables`, are omitted, then variables and content are shown.
        
      -v | --variables
            
      >If this option is set (and no `--content` option is given), only the variables of the template is shown. If both options,
      `--content` and `--variables`, are omitted, then variables and content are shown.
        
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_TEMPLATE_ADDRESS);

  public TemplateGetCommand() {
    super(NAME, "Gets the content and variables of an output template.",
        new CommandLineSpec(true, CMD_OPT_BEAUTIFY, CMD_OPT_CONTENT, CMD_OPT_VARIABLES, CMD_ARG_TEMPLATE),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }


  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    TemplateAddress address = TemplateAddress.fromString(commandLine.getValue(CMD_ARG_TEMPLATE).orElseThrow());
    boolean beautify = commandLine.hasOption(CMD_OPT_BEAUTIFY);
    boolean content = commandLine.hasOption(CMD_OPT_CONTENT) || !commandLine.hasOption(CMD_OPT_VARIABLES);
    boolean variables = commandLine.hasOption(CMD_OPT_VARIABLES) || !commandLine.hasOption(CMD_OPT_CONTENT);

    Template template = templateRepository.getTemplate(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Output template \"%s\" does not exist.".formatted(address)));

    if (variables) {
      writeVariables(context, template, beautify, content);
    }
    if (content) {
      writeContent(context, template, variables);
    }
    return true;
  }

  void writeVariables(CommandContext context, Template template, boolean beautify, boolean showTitle) {
    if (showTitle) {
      context.out().markdown("**Variables:**");
    }
    StringBuilder buffer = new StringBuilder();
    String ls = System.lineSeparator();
    if (beautify) {
      buffer.append("| Name | Default value |").append(ls);
    }
    buffer.append("|---|---|").append(ls);

    template.config().parameters().entrySet().stream()
        .sorted(Entry.comparingByKey())
        .forEach(entry -> buffer.append("|%s|%s".formatted(entry.getKey(),
            (beautify ? "" : " = ") + entry.getValue().defaultValue())).append(ls));

    context.out().markdown(buffer.toString());
  }

  void writeContent(CommandContext context, Template template, boolean showTitle) {
    if (showTitle) {
      context.out().markdown("**Content:**");
    }
    context.out().chars(template.config().content());
    context.out().newline();
  }
}
