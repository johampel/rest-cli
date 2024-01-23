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
package de.hipphampel.restcli.cli.commandline;

import de.hipphampel.restcli.cli.commandline.CommandLine.Subset;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Parameters;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.exception.UsageException;
import de.hipphampel.restcli.utils.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CommandLineParser {

  public CommandLine parseCommandLine(CommandLineSpec spec, List<String> args) {
    CommandLine commandLine = new CommandLine();
    Context context = new Context(commandLine, args, spec.parseMixed());
    parseParameters(spec.params(), context);
    validate(spec.params(), context);
    return commandLine;
  }

  void parseParameters(Parameters parameters, Context context) {
    Map<String, Option> optionMap = parameters.options().stream()
        .flatMap(option -> option.names().stream().map(name -> new Pair<>(name, option)))
        .collect(Collectors.toMap(Pair::first, Pair::second));
    List<Positional> positionals = parameters.positionals().stream().flatMap(Positional::flatten).toList();
    int positionalIndex = 0;
    boolean allowsOptions = true;
    boolean somethingParsed = true;
    while (!context.isEmpty() && somethingParsed) {
      somethingParsed = false;
      if (allowsOptions) {
        if (context.consumeEndOfOptionsMarker()) {
          allowsOptions = false;
          somethingParsed = true;
          continue;
        }
        Option option = context.consumeOption(optionMap).orElse(null);
        if (option != null) {
          parseOption(option, context);
          somethingParsed = true;
          continue;
        } else {
          allowsOptions = context.parseMixed || context.values instanceof Subset;
        }
      }

      if (positionalIndex < positionals.size()) {
        Positional positional = positionals.get(positionalIndex);
        if (parsePositional(positional, context)) {
          somethingParsed = true;
          if (!positional.repeatable()) {
            positionalIndex++;
          }
        }
      }
    }
  }

  boolean parsePositional(Positional positional, Context context) {
    String arg = context.consumeArg(context.values instanceof Subset).orElse(null);
    if (arg == null) {
      return false;
    }
    context.values.addValue(positional, arg);
    return true;
  }

  void parseOption(Option option, Context context) {
    if (context.values.hasOption(option) && !option.repeatable()) {
      throw new UsageException("Duplicate usage of option \"%s\".".formatted(option.name()));
    }
    context.values.addOption(option);

    if (option.params().params().isEmpty()) {
      return;
    }

    Values oldValues = context.values;
    if (context.commandLine == context.values) {
      context.values = context.commandLine.addSubset(option);
    }
    parseParameters(option.params(), context);

    context.values = oldValues;
  }

  void validate(Parameters spec, Context context) {
    validate(spec, context.commandLine);
    if (!context.isEmpty()) {
      throw new UsageException("Unexpected argument \"%s\".".formatted(context.consumeArg(true).orElse("")));
    }
  }
  void validate(Parameters spec, Values values) {
    validatePositionals(spec.positionals(), values);
    validateOptions(spec.options(), values);
  }

  void validateOptions(List<Option> options, Values values) {
    for (Option option : options) {
      if (!values.hasOption(option)) {
        continue;
      }
      if (values instanceof CommandLine commandLine) {
        List<Subset> subsets = commandLine.getSubsets(option);
        // Note: Sub-options cannot be repeatable!
        if (!option.repeatable() && subsets.size() > 1) {
          throw new UsageException("Option \"%s\" is not repeatable.".formatted(option.name()));
        }
        subsets.forEach(subset -> validate(option.params(), subset));
      }
    }
    options.stream()
        .filter(option -> option.exclusionGroup() != null && values.hasOption(option))
        .collect(Collectors.groupingBy(Option::exclusionGroup))
        .values().stream()
        .filter(list -> list.size() > 1)
        .findFirst()
        .map(list -> list.stream().map(Option::name).collect(Collectors.joining("\", \"", "\"", "\"")))
        .ifPresent(names -> {
          throw new UsageException("Options %s exclude each other.".formatted(names));
        });
  }

  void validatePositionals(List<Positional> positionals, Values values) {
    for (Positional positional : positionals) {
      List<String> positionalValues = values.getValues(positional);
      if (positionalValues.isEmpty() && !positional.optional()) {
        throw new UsageException("Missing required argument \"%s\".".formatted(positional.name()));
      }
      if (positionalValues.size() > 1 && !positional.repeatable()) {
        throw new UsageException("Argument \"%s\" specified more than once.".formatted(positional.name()));
      }
      if (positional.validator() != null) {
        positionalValues.forEach(
            value -> positional.validator().accept(positional, value));
      }
      validatePositionals(positional.dependencies(), values);
    }
  }

  static class Context {

    final CommandLine commandLine;
    final List<String> args;
    final boolean parseMixed;
    int argIndex;
    int strIndex;

    Values values;

    public Context(CommandLine commandLine, List<String> args, boolean parseMixed) {
      this.commandLine = commandLine;
      this.args = Objects.requireNonNull(args);
      this.parseMixed = parseMixed;
      this.values = commandLine;
    }

    boolean isEmpty() {
      return argIndex >= args.size();
    }

    boolean consumeEndOfOptionsMarker() {
      if (isEmpty() || strIndex != 0) {
        return false;
      }
      if (args.get(argIndex).equals("--")) {
        argIndex++;
        return true;
      }
      return false;
    }

    Optional<String> consumeArg(boolean forOption) {
      if (isEmpty()) {
        return Optional.empty();
      }
      String arg = args.get(argIndex);
      if (forOption) {
        arg = arg.substring(strIndex);
        strIndex = 0;
        argIndex++;
        return Optional.of(arg);
      } else if (strIndex == 0) {
        argIndex++;
        return Optional.of(arg);
      }
      return Optional.empty();
    }

    Optional<Option> consumeOption(Map<String, Option> optionMap) {
      if (isEmpty()) {
        return Optional.empty();
      }
      String arg = args.get(argIndex);
      if (arg.length() < 2 || arg.charAt(0) != '-') {
        return Optional.empty();
      }

      for (Map.Entry<String, Option> entry : optionMap.entrySet()) {
        String name = entry.getKey();
        if (isLongOptionName(name)) {
          if (strIndex == 0 && Objects.equals(name, arg)) {
            argIndex++;
            return Optional.of(entry.getValue());
          }
        } else if (isShortOptionName(name)) {
          int index = Math.max(1, strIndex);
          if (arg.charAt(index) == name.charAt(1)) {
            strIndex = index + 1;
            if (strIndex >= arg.length()) {
              strIndex = 0;
              argIndex++;
            }
            return Optional.of(entry.getValue());
          }
        }
      }
      return Optional.empty();
    }

    static boolean isShortOptionName(String str) {
      return str.length() == 2 && str.charAt(0) == '-' && str.charAt(1) != '-';
    }

    static boolean isLongOptionName(String str) {
      return str.length() > 2 && str.charAt(0) == '-' && str.charAt(1) == '-';
    }
  }
}
