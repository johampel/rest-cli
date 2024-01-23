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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record CommandLineSpec(String customUsage, boolean parseMixed, Parameters params) {

  public CommandLineSpec(String customUsage, boolean parseMixed, Parameter... params) {
    this(customUsage, parseMixed, new Parameters(List.of(params)));
  }

  public CommandLineSpec(boolean parseMixed, Parameter... params) {
    this(null, parseMixed, new Parameters(List.of(params)));
  }

  public CommandLineSpec(boolean parseMixed, Parameters params) {
    this(null, parseMixed, params);
  }

  public CommandLineSpec(String customUsage, boolean parseMixed, Parameters params) {
    this.customUsage = customUsage;
    this.parseMixed = parseMixed;
    this.params = Objects.requireNonNull(params);
    List<Option> allOptions = params.options.stream().flatMap(CommandLineSpec::recursiveAllOptions).toList();
    allOptions.stream()
        .flatMap(option -> option.names.stream())
        .collect(Collectors.groupingBy(Function.identity()))
        .values().stream()
        .filter(names -> names.size() > 1)
        .findFirst()
        .ifPresent(names -> {
          throw new IllegalArgumentException("Duplicate definition of option \"%s\".".formatted(names.get(0)));
        });
    List<Positional> allPositionals = params.params().stream().flatMap(CommandLineSpec::recursiveAllPositionals).toList();
    allPositionals.stream()
        .map(Positional::id)
        .collect(Collectors.groupingBy(Function.identity()))
        .values().stream()
        .filter(names -> names.size() > 1)
        .findFirst()
        .ifPresent(names -> {
          throw new IllegalArgumentException("Duplicate definition of positional \"%s\".".formatted(names.get(0)));
        });
    if (!params.positionals.isEmpty()) {
      params.positionals.subList(0, params.positionals.size() - 1).stream()
          .filter(positional -> !positional.fixedSize())
          .findFirst()
          .ifPresent(positonal -> {
            throw new IllegalArgumentException(
                "Positional \"%s\" must be fixed sized, since further positionals follow.".formatted(positonal.name()));
          });
    }
  }

  public String usageString() {
    if (customUsage != null) {
      return customUsage;
    }
    StringBuilder buffer = new StringBuilder();
    params.appendUsageString(buffer);
    return buffer.toString();
  }

  public sealed interface Parameter {

    String name();

    void appendUsageString(StringBuilder buffer);
  }

  public record Parameters(List<Option> options, List<Positional> positionals) {

    public Parameters(List<Option> options, List<Positional> positionals) {
      this.options = Collections.unmodifiableList(options);
      this.positionals = Collections.unmodifiableList(positionals);
    }

    public Parameters(List<? extends Parameter> params) {
      this(
          filterParameters(params, Option.class::isInstance),
          filterParameters(params, Positional.class::isInstance));
    }

    public Parameters(Parameter... params) {
      this(List.of(params));
    }

    public boolean isEmpty() {
      return options.isEmpty() && positionals.isEmpty();
    }

    public List<? extends Parameter> params() {
      return Stream.concat(options.stream(), positionals.stream()).toList();
    }

    public Optional<Positional> positional(String name) {
      return positionals.stream().filter(positional -> Objects.equals(name, positional.name)).findFirst();
    }

    void appendUsageString(StringBuilder buffer) {
      new TreeMap<>(options.stream()
          .collect(Collectors.groupingBy(option -> option.exclusionGroup == null ? "" : "-" + option.exclusionGroup)))
          .forEach((k, v) -> appendUsageStringForOptions(buffer, v, !k.isEmpty()));
      if (!positionals.isEmpty()) {
        positionals.forEach(param -> {
          appendSpaceIfNotEmpty(buffer);
          param.appendUsageString(buffer);
        });
      }
    }

    private static void appendUsageStringForOptions(StringBuilder buffer, List<Option> options, boolean grouped) {
      if (options.isEmpty()) {
        return;
      }
      appendSpaceIfNotEmpty(buffer);
      if (grouped) {
        buffer.append("[");
      }

      boolean first = true;
      for (Option option : options) {
        if (first) {
          first = false;
        } else if (grouped) {
          buffer.append(" | ");
        } else {
          appendSpaceIfNotEmpty(buffer);
        }
        option.appendUsageString(buffer, grouped);
      }
      if (grouped) {
        buffer.append("]");
      }
    }
  }


  public static OptionBuilder option(String... names) {
    return new OptionBuilder(List.of(names));
  }

  public static OptionBuilder option(List<String> names) {
    return new OptionBuilder(names);
  }

  public static class OptionBuilder {

    private final List<String> names;
    private final List<Parameter> parameters;
    private boolean repeatable;
    private String exclusionGroup;

    OptionBuilder(List<String> names) {
      this.names = names;
      this.parameters = new ArrayList<>();
      this.repeatable = false;
      this.exclusionGroup = null;
    }

    public OptionBuilder repeatable() {
      this.repeatable = true;
      return this;
    }

    public OptionBuilder exclusionGroup(String exclusionGroup) {
      this.exclusionGroup = exclusionGroup;
      return this;
    }

    public OptionBuilder parameter(Parameter... params) {
      this.parameters.addAll(List.of(params));
      return this;
    }

    public Option build() {
      return new Option(names, repeatable, exclusionGroup, new Parameters(parameters));
    }
  }

  public record Option(List<String> names, boolean repeatable, String exclusionGroup, Parameters params) implements Parameter {

    public Option(List<String> names, boolean repeatable, String exclusionGroup, Parameters params) {
      this.names = Collections.unmodifiableList(names);
      this.params = Objects.requireNonNull(params);
      this.repeatable = repeatable;
      this.exclusionGroup = exclusionGroup;
      if (names.isEmpty()) {
        throw new IllegalArgumentException("An option must have at least one name.");
      }
      names.stream()
          .filter(name -> name.isEmpty() || "--".equals(name) || !name.startsWith("-"))
          .findFirst().ifPresent(name -> {
            throw new IllegalArgumentException("Invalid option name \"%s\".".formatted(name));
          });
      if (repeatable && params.positionals.isEmpty()) {
        throw new IllegalArgumentException("Repeatable option \"%s\" must have at least one positional argument.".formatted(name()));
      }
      params.options.stream()
          .filter(option -> option.repeatable)
          .findFirst().ifPresent(option -> {
            throw new IllegalArgumentException("Option \"%s\" must have only non-repeatable sub options.".formatted(name()));
          });
      params.positionals.stream()
          .filter(positional -> !positional.fixedSize())
          .findFirst().ifPresent(positonal -> {
            throw new IllegalArgumentException("Option \"%s\" must have only fixed size positional arguments.".formatted(name()));
          });
    }

    public Option(List<String> names, boolean repeatable, String exclusionGroup, Parameter... params) {
      this(names, repeatable, exclusionGroup, new Parameters(params));
    }

    @Override
    public String name() {
      return names.get(0);
    }

    @Override
    public void appendUsageString(StringBuilder buffer) {
      buffer.append(names.stream().collect(Collectors.joining("|", "[", "")));
      params.appendUsageString(buffer);
      buffer.append("]");
      if (repeatable) {
        buffer.append("...");
      }
    }

    public void appendUsageString(StringBuilder buffer, boolean inExclusionGroup) {
      if (!inExclusionGroup) {
        buffer.append("[");
      } else if (!params.isEmpty()) {
        buffer.append("(");
      }
      buffer.append(names.stream().collect(Collectors.joining("|", "", "")));
      params.appendUsageString(buffer);
      if (!inExclusionGroup) {
        buffer.append("]");
      } else if (!params.isEmpty()) {
        buffer.append(")");
      }
      if (repeatable) {
        buffer.append("...");
      }
    }
  }

  public static PositionalBuilder positional(String name) {
    return new PositionalBuilder(name);
  }

  public static class PositionalBuilder {

    private final String name;
    private final List<Positional> dependencies;
    private String id;
    private boolean repeatable;
    private boolean optional;
    private BiConsumer<Positional, String> validator;

    PositionalBuilder(String name) {
      this.id = name;
      this.name = name;
      this.dependencies = new ArrayList<>();
      this.repeatable = false;
      this.optional = false;
      this.validator = null;
    }

    public PositionalBuilder repeatable() {
      this.repeatable = true;
      return this;
    }

    public PositionalBuilder optional() {
      this.optional = true;
      return this;
    }

    public PositionalBuilder validator(BiConsumer<Positional, String> validator) {
      this.validator = validator;
      return this;
    }

    public PositionalBuilder dependency(Positional... dependencies) {
      this.dependencies.addAll(List.of(dependencies));
      return this;
    }

    public PositionalBuilder id(String id) {
      this.id = id;
      return this;
    }

    public Positional build() {
      return new Positional(id, name, repeatable, optional, validator, dependencies);
    }
  }

  public record Positional(String id, String name, boolean repeatable, boolean optional, BiConsumer<Positional, String> validator,
                           List<Positional> dependencies) implements
      Parameter {

    public Positional(String id, String name, boolean repeatable, boolean optional, BiConsumer<Positional, String> validator,
        List<Positional> dependencies) {
      this.id = Objects.requireNonNull(id);
      this.name = Objects.requireNonNull(name);
      this.dependencies = dependencies == null ? List.of() : Collections.unmodifiableList(dependencies);
      this.repeatable = repeatable;
      this.optional = optional;
      this.validator = validator;
      if (name.isEmpty() || name.startsWith("-")) {
        throw new IllegalArgumentException("Positional name \"%s\" must neither be empty nor start with a \"-\".".formatted(name));
      }
      if (repeatable && !this.dependencies.isEmpty()) {
        throw new IllegalArgumentException("Positional \"%s\" must not have dependencies.".formatted(name));
      }
      if (!this.dependencies.isEmpty() &&
          !this.dependencies.subList(0, this.dependencies.size() - 1).stream().allMatch(Positional::fixedSize)) {
        throw new IllegalArgumentException("All dependencies of positional \"%s\" except last must have fixed size.".formatted(name));
      }
    }

    @Override
    public void appendUsageString(StringBuilder buffer) {
      if (optional) {
        buffer.append("[");
      }
      buffer.append(name);
      dependencies.forEach(param -> {
        buffer.append(" ");
        param.appendUsageString(buffer);
      });
      if (repeatable) {
        buffer.append("...");
      }
      if (optional) {
        buffer.append("]");
      }
    }

    public Stream<Positional> flatten() {
      return Stream.concat(Stream.of(this), dependencies.stream().flatMap(Positional::flatten));
    }

    public boolean fixedSize() {
      return !repeatable && !optional && dependencies.stream().allMatch(Positional::fixedSize);
    }
  }

  static Stream<Positional> recursiveAllPositionals(Parameter argument) {
    if (argument instanceof Option option) {
      return option.params.positionals.stream().flatMap(CommandLineSpec::recursiveAllPositionals);
    } else if (argument instanceof Positional positional) {
      return Stream.concat(
          Stream.of(positional),
          positional.dependencies.stream());
    } else {
      return Stream.empty();
    }
  }

  static Stream<Option> recursiveAllOptions(Parameter argument) {
    if (argument instanceof Option option) {
      return Stream.concat(
          Stream.of(option),
          option.params.options.stream().flatMap(CommandLineSpec::recursiveAllOptions)
      );
    } else {
      return Stream.empty();
    }
  }

  private static void appendSpaceIfNotEmpty(StringBuilder buffer) {
    if (!buffer.isEmpty()) {
      buffer.append(" ");
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> List<T> filterParameters(List<? extends Parameter> params, Predicate<Parameter> predicate) {
    return (List<T>) params.stream()
        .filter(predicate)
        .toList();
  }
}
