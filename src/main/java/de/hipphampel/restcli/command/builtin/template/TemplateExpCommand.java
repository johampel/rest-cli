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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
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
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class TemplateExpCommand extends TemplateCommandBase {

  static String NAME = "exp";

  static final Positional CMD_ARG_TEMPLATE = positional("<template-address>")
      .validator(Validators.TEMPLATE_ADDRESS_VALIDATOR)
      .build();
  static final Positional CMD_ARG_TARGET = positional("<target>")
      .optional()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Exports the template identified by `<template-address>` in a format that can be used to import it again via the `imp` command.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <template-address>
            
      >The address of the template you want to export, see also section "Further Infos:".
            
      <target>
            
      >The file where to export to. If omitted, the export is written to standard output.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_TEMPLATE_ADDRESS);

  @Inject
  ObjectMapper objectMapper;

  public TemplateExpCommand() {
    super(NAME, "Exports the the given output template.",
        new CommandLineSpec(true, CMD_ARG_TEMPLATE, CMD_ARG_TARGET),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }


  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    TemplateAddress address = TemplateAddress.fromString(commandLine.getValue(CMD_ARG_TEMPLATE).orElseThrow());
    String target = commandLine.getValue(CMD_ARG_TARGET).orElse(null);

    Template source = templateRepository.getTemplate(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Output template \"%s\" does not exist.".formatted(address)));

    try {
      ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
      if (target != null) {
        writer.writeValue(new File(target), source.config());
      } else {
        StringWriter buffer = new StringWriter();
        writer.writeValue(buffer, source.config());
        context.out().line(buffer.toString());
      }
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to export output template \"%s\": %s".formatted(address, ioe.getMessage()));
    }

    return true;
  }

}
