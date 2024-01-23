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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Parameters;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommandLineSpecTest {

  @Test
  void Option_ctor_fail_noName() {
    assertThatThrownBy(() ->
        option()
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("An option must have at least one name.");
  }

  @Test
  void Option_ctor_fail_invalidName() {
    assertThatThrownBy(() ->
        option("")
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid option name \"\".");
    assertThatThrownBy(() ->
        option("bad")
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid option name \"bad\".");
    assertThatThrownBy(() ->
        option("--")
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid option name \"--\".");
  }

  @Test
  void Option_ctor_fail_noArgRepeatable() {
    assertThatThrownBy(() ->
        option("-o")
            .repeatable()
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Repeatable option \"-o\" must have at least one positional argument.");
  }

  @Test
  void Option_ctor_fail_repeatableSubOption() {
    assertThatThrownBy(() ->
        option("-o")
            .parameter(
                option("-b")
                    .parameter(
                        positional("arg")
                            .build())
                    .repeatable()
                    .build())
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Option \"-o\" must have only non-repeatable sub options.");
  }

  @Test
  void Option_ctor_fail_variantSizePositional() {
    assertThatThrownBy(() ->
        option("-o")
            .parameter(
                positional("bar")
                    .repeatable()
                    .build())
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Option \"-o\" must have only fixed size positional arguments.");
    assertThatThrownBy(() -> option("-o")
        .parameter(
            positional("bar")
                .optional()
                .build())
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Option \"-o\" must have only fixed size positional arguments.");
  }

  @ParameterizedTest
  @CsvSource({
      "false, false, '[-o|--output]'",
      "false, true,  '[-o|--output arg]'",
      "true,  true, '[-o|--output arg]...'"
  })
  void Option_appendUsageString(boolean repeatable, boolean hasArguments, String expected) {
    Positional arg = positional("arg")
        .build();
    Option option = new Option(
        List.of("-o", "--output"),
        repeatable,
        null,
        new Parameters(hasArguments ? List.of(arg) : List.of()));
    StringBuilder builder = new StringBuilder();

    option.appendUsageString(builder);
    assertThat(builder).hasToString(expected);
  }

  @Test
  void Positional_ctor_fail_invalidName() {
    assertThatThrownBy(() -> positional("")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Positional name \"\" must neither be empty nor start with a \"-\".");
    assertThatThrownBy(() -> positional("-bar")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Positional name \"-bar\" must neither be empty nor start with a \"-\".");
  }

  @Test
  void Positional_ctor_repeatableWithArgs() {
    assertThatThrownBy(() ->
        positional("main")
            .repeatable()
            .dependency(
                positional("sub")
                    .build())
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Positional \"main\" must not have dependencies.");
  }

  @Test
  void Positional_ctor_notLastArgHasNotFixedSize() {
    assertThatThrownBy(() ->
        positional("main")
            .dependency(
                positional("sub1")
                    .repeatable()
                    .build(),
                positional("sub2")
                    .build())
            .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All dependencies of positional \"main\" except last must have fixed size.");
  }

  @ParameterizedTest
  @CsvSource({
      "false, false, false, 'posi'",
      "false, true,  false, '[posi]'",
      "true,  false, false, 'posi...'",
      "true,  true,  false, '[posi...]'",
      "false, false, true,  'posi arg'",
      "false, true,  true,  '[posi arg]'",
  })
  void Positional_appendUsageString(boolean repeatable, boolean optional, boolean hasArguments, String expected) {
    Positional arg = positional("arg").build();
    Positional positional = new Positional(
        "1",
        "posi",
        repeatable,
        optional,
        null,
        hasArguments ? List.of(arg) : List.of());
    StringBuilder builder = new StringBuilder();

    positional.appendUsageString(builder);
    assertThat(builder).hasToString(expected);
  }

  @Test
  void Parameters_appendUsageString() {
    Parameters parameters = new Parameters(
        positional("arg1")
            .optional()
            .dependency(
                positional("arg2")
                    .repeatable()
                    .build())
            .build(),
        option("-a", "--aa")
            .exclusionGroup("group1")
            .build(),
        option("-b", "--bb")
            .exclusionGroup("group1")
            .parameter(
                positional("arg3")
                    .build())
            .build(),
        option("-c", "--cc")
            .build());

    StringBuilder builder = new StringBuilder();

    parameters.appendUsageString(builder);
    assertThat(builder).hasToString("[-c|--cc] [-a|--aa | (-b|--bb arg3)] [arg1 arg2...]");
  }

  @Test
  void CommandLineSpec_ctor_duplicateOption() {
    assertThatThrownBy(() -> new CommandLineSpec(
        false,
        option("-a")
            .parameter(
                option("-b").build())
            .build(),
        option("-b").build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Duplicate definition of option \"-b\".");
  }

  @Test
  void CommandLineSpec_ctor_duplicatePositional() {
    assertThatThrownBy(() -> new CommandLineSpec(
        false,
        option("-a")
            .parameter(
                positional("abc").build())
            .build(),
        positional("abc").build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Duplicate definition of positional \"abc\".");
  }

  @Test
  void CommandLineSpec_ctor_undefinedPositionalSize() {
    assertThatThrownBy(() -> new CommandLineSpec(
        false,
        positional("abc")
            .repeatable()
            .build(),
        positional("def")
            .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Positional \"abc\" must be fixed sized, since further positionals follow.");
  }
}
