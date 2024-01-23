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
package de.hipphampel.restcli.command.config;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Parameter;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import java.util.Objects;

@RegisterForReflection
public record ParameterConfig(
    Style style,
    String name,
    String variable,
    List<String> optionNames) {

  public enum Style {
    SingleOption(false, false),
    MultiOption(false, true),
    RequiredPositional(true, false),
    OptionalPositional(true, false),
    OptionalMultiPositional(true, true),
    RequiredMultiPositional(true, true);

    public final boolean positional;
    public final boolean repeatable;

    Style(boolean positional, boolean repeatable) {
      this.positional = positional;
      this.repeatable = repeatable;
    }
  }


  @JsonCreator
  public ParameterConfig(
      @JsonProperty("style") Style style,
      @JsonProperty("name") String name,
      @JsonProperty("variable") String variable,
      @JsonProperty("optionNames") List<String> optionNames) {
    this.style = Objects.requireNonNull(style);
    this.name = Objects.requireNonNull(name);
    this.variable = Objects.requireNonNull(variable);
    this.optionNames = optionNames;
  }

  public Parameter toParameter() {
    return switch (style) {
      case SingleOption -> option(optionNames)
          .parameter(positional(name).build())
          .build();
      case MultiOption -> option(optionNames)
          .parameter(positional(name).build())
          .repeatable()
          .build();
      case RequiredPositional -> positional(name)
          .build();
      case OptionalPositional -> positional(name)
          .optional()
          .build();
      case OptionalMultiPositional -> positional(name)
          .optional()
          .repeatable()
          .build();
      case RequiredMultiPositional -> positional(name)
          .repeatable()
          .build();
    };
  }
}
