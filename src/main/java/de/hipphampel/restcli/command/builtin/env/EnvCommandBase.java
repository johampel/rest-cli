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
package de.hipphampel.restcli.command.builtin.env;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.builtin.BuiltinCommand;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentRepository;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.KeyValue;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class EnvCommandBase extends BuiltinCommand {

  @Inject
  protected EnvironmentRepository environmentRepository;

  @Inject
  protected ObjectMapper objectMapper;

  protected EnvCommandBase(String name, String synopsis, CommandLineSpec commandLineSpec,
      Map<HelpSection, Function<CommandContext, Block>> helpSections) {
    super(
        CommandAddress.fromString(EnvCommandParent.NAME).child(name),
        synopsis,
        commandLineSpec,
        helpSections);
  }

  protected void setVariables(CommandContext context, Environment environment, List<String> values, List<String> jsons) {
    List<KeyValue<?>> keyValues = Stream.concat(
            values.stream()
                .map(KeyValue::fromString),
            jsons.stream()
                .map(KeyValue::fromString)
                .map(kav -> new KeyValue<>(kav.key(), parseJson(kav.value()))))
        .toList();
    Map<String, Object> variables = CommandUtils.fillVariables(context, environment.getLocalVariables(), keyValues, true);
    environment.setLocalVariables(variables);
  }

  private Object parseJson(String str) {
    try {
      return objectMapper.treeToValue(objectMapper.readTree(str), Object.class);
    } catch (JsonProcessingException e) {
      throw new ExecutionException("Failed to convert value \"%s\" to a JSON object.".formatted(str));
    }
  }

}