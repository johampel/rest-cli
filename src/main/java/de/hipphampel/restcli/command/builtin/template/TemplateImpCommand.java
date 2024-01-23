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
import static de.hipphampel.restcli.command.builtin.template.TemplateCommandUtils.CMD_OPT_REPLACE;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateConfig;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class TemplateImpCommand extends TemplateCommandBase {

  static String NAME = "imp";

  static final Positional CMD_ARG_TEMPLATE = positional("<template-address>")
      .validator(Validators.TEMPLATE_ADDRESS_VALIDATOR)
      .build();
  static final Positional CMD_ARG_SOURCE = positional("<source>")
      .build();
  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Imports an output template from `<source>` and stores it at address `<template-address>`. The content of `<source>` is typically
      produced by the `exp` command.
            
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <template-address>
            
      >The address of the template you want to create by this import. See section "Further Infos" for more information about the syntax.
            
      <source>
            
      >The source file where to read the output template settings from. This is specified as an input source, see section "Further Infos"
      for more information about the syntax.

      -r | --replace
            
      >The option allows to overwrite existing output templates. If the target output template already exists, this option must be set in order
      to import the data into it. If the target  output template not exists, this option has no effect.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_INPUT_SOURCE +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_TEMPLATE_ADDRESS);

  @Inject
  ObjectMapper objectMapper;

  public TemplateImpCommand() {
    super(NAME, "Imports an output template.",
        new CommandLineSpec(true, CMD_OPT_REPLACE, CMD_ARG_TEMPLATE, CMD_ARG_SOURCE),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }


  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    TemplateAddress address = commandLine.getValue(CMD_ARG_TEMPLATE)
        .map(TemplateAddress::fromString)
        .orElseThrow();
    InputStreamProviderConfig source = commandLine.getValue(CMD_ARG_SOURCE)
        .map(InputStreamProviderConfig::fromString)
        .orElseThrow();
    boolean replace = commandLine.hasOption(CMD_OPT_REPLACE);

    if (commandRepository.getCommandInfo(context.configPath(), address.command()).isEmpty()) {
      throw new ExecutionException("Command \"%s\" does not exist.".formatted(address.command()));
    }

    if (!replace && templateRepository.existsTemplate(context.configPath(), address)) {
      throw new ExecutionException(
          "Output template \"%s\" already exists. Use `--replace` option to allow replacement.".formatted(address));
    }

    TemplateConfig config = readConfig(context, source);
    templateRepository.storeTemplate(context.configPath(), new Template(address, config, false), replace);
    return true;
  }

  TemplateConfig readConfig(CommandContext context, InputStreamProviderConfig source) {
    try (InputStream in = CommandUtils.createInputStreamProvider(context, source, context.environment().getVariables()).open()) {
      return objectMapper.readValue(in, TemplateConfig.class);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to read template settings: %s".formatted(ioe.getMessage()));
    }
  }

}
