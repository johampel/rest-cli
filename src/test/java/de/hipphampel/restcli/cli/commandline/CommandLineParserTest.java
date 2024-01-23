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

import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.cli.commandline.CommandLine.Subset;
import de.hipphampel.restcli.cli.commandline.CommandLineParser.Context;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.exception.UsageException;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CommandLineParserTest {

  private static final Positional arg1 = positional("arg1").build();
  private static final Positional arg2 = positional("arg2").optional().repeatable().build();
  private static final Positional arg3 = positional("arg3").dependency(arg2).build();
  private static final Option flagOption = option("-f", "--flag").build();
  private static final Option exclusiveOption1 = option("--ex1").exclusionGroup("group").build();
  private static final Option exclusiveOption2 = option("--ex2").exclusionGroup("group").build();
  private static final Option optionWithArg = option("-a", "--arg").parameter(arg1, exclusiveOption1, exclusiveOption2).repeatable()
      .build();
  @Inject
  CommandLineParser parser;

  @Test
  void parseCommandLine_ok_notMixed() {
    CommandLineSpec spec = new CommandLineSpec(false, arg3, flagOption, optionWithArg);
    CommandLine commandLine = parser.parseCommandLine(spec, List.of("--flag", "-aabc", "--ex1", "123", "def", "ghi"));

    CommandLine expected = new CommandLine();
    expected.addOption(flagOption);
    expected.addOption(optionWithArg);
    expected.addValue(arg3, "123");
    expected.addValues(arg2, List.of("def", "ghi"));
    Subset subset = expected.addSubset(optionWithArg);
    subset.addValue(arg1, "abc");
    subset.addOption(exclusiveOption1);

    assertThat(commandLine).isEqualTo(expected);
  }

  @Test
  void parseCommandLine_ok_mixed() {
    CommandLineSpec spec = new CommandLineSpec(true, arg3, flagOption, optionWithArg);
    CommandLine commandLine = parser.parseCommandLine(spec, List.of("123", "--flag", "-aabc", "--ex1", "def", "ghi"));

    CommandLine expected = new CommandLine();
    expected.addOption(flagOption);
    expected.addOption(optionWithArg);
    expected.addValue(arg3, "123");
    expected.addValues(arg2, List.of("def", "ghi"));
    Subset subset = expected.addSubset(optionWithArg);
    subset.addValue(arg1, "abc");
    subset.addOption(exclusiveOption1);

    assertThat(commandLine).isEqualTo(expected);
  }

  @Test
  void parseCommandLine_ok_mixed_endOfOptions() {
    CommandLineSpec spec = new CommandLineSpec(true, arg3, flagOption, optionWithArg);
    CommandLine commandLine = parser.parseCommandLine(spec, List.of("123", "--flag", "-aabc", "--", "--ex1", "def", "ghi"));

    CommandLine expected = new CommandLine();
    expected.addOption(flagOption);
    expected.addOption(optionWithArg);
    expected.addValue(arg3, "123");
    expected.addValues(arg2, List.of("--ex1", "def", "ghi"));
    Subset subset = expected.addSubset(optionWithArg);
    subset.addValue(arg1, "abc");

    assertThat(commandLine).isEqualTo(expected);
  }

  @Test
  void parseCommandLine_fail_missing_positional() {
    CommandLineSpec spec = new CommandLineSpec(true, arg3, flagOption, optionWithArg);
    assertThatThrownBy(() -> parser.parseCommandLine(spec, List.of()))
        .isInstanceOf(UsageException.class)
        .hasMessage("Missing required argument \"arg3\".");
  }

  @Test
  void parseCommandLine_fail_exclusiveOptions() {
    CommandLineSpec spec = new CommandLineSpec(true, arg3, flagOption, optionWithArg);
    assertThatThrownBy(() -> parser.parseCommandLine(spec, List.of("-aabc", "--ex1", "--ex2", "123")))
        .isInstanceOf(UsageException.class)
        .hasMessage("Options \"--ex1\", \"--ex2\" exclude each other.");
  }

  @Test
  void parseCommandLine_fail_missingArgument() {
    CommandLineSpec spec = new CommandLineSpec(true, arg3);
    assertThatThrownBy(() -> parser.parseCommandLine(spec, List.of()))
        .isInstanceOf(UsageException.class)
        .hasMessage("Missing required argument \"arg3\".");
  }

  @Test
  void parsePositional_ok() {
    Context context = initContext("abc;def", 0, 0);
    Positional arg = positional("arg").build();

    assertThat(parser.parsePositional(arg, context)).isTrue();
    assertThat(context.values).isSameAs(context.commandLine);
    assertThat(context.commandLine.getValues(arg)).isEqualTo(List.of("abc"));
  }

  @Test
  void parsePositional_notFound() {
    Context context = initContext("abc;def", 2, 0);
    Positional arg = positional("arg").build();

    assertThat(parser.parsePositional(arg, context)).isFalse();
    assertThat(context.values).isSameAs(context.commandLine);
    assertThat(context.commandLine.getValues(arg)).isEqualTo(List.of());
  }

  @Test
  void parseOption_withArgs_first() {
    Context context = initContext("abc;def", 0, 0);
    Positional arg = positional("arg").build();
    Option option = option("--foo")
        .parameter(arg)
        .build();

    parser.parseOption(option, context);
    assertThat(context.values).isSameAs(context.commandLine);
    assertThat(context.commandLine.hasOption(option)).isTrue();
    assertThat(context.commandLine.getValues(arg)).isEqualTo(List.of("abc"));
    assertThat(context.commandLine.hasSubset(option)).isTrue();
    assertThat(context.commandLine.getSubsets(option)).hasSize(1);
    assertThat(context.commandLine.getSubset(option).orElseThrow().getValues(arg)).isEqualTo(List.of("abc"));
  }

  @Test
  void parseOption_noArgs_first() {
    Context context = initContext("abc;def", 0, 0);
    Option option = option("--foo").build();

    parser.parseOption(option, context);
    assertThat(context.values).isSameAs(context.commandLine);
    assertThat(context.commandLine.hasOption(option)).isTrue();
    assertThat(context.commandLine.hasSubset(option)).isFalse();
  }

  @Test
  void parseOption_noArgs_twice() {
    Context context = initContext("abc;def", 0, 0);
    Option option = option("--foo").build();
    context.commandLine.addOption(option);

    assertThatThrownBy(() -> parser.parseOption(option, context))
        .isInstanceOf(UsageException.class)
        .hasMessage("Duplicate usage of option \"--foo\".");
  }

  @ParameterizedTest
  @CsvSource({
      "abc;--;a--, 0, 0, false,   0, 0",
      "abc;--;a--, 1, 0, true,    2, 0",
      "abc;--;a--, 2, 1, false,   2, 1",
  })
  void Context_consumeEndOfOptionsMarker(String argList, int argIndex, int strIndex, boolean expectedMatch, int expectedArgIndex,
      int expectedStrIndex) {
    Context context = initContext(argList, argIndex, strIndex);
    assertThat(context.consumeEndOfOptionsMarker()).isEqualTo(expectedMatch);
    assertThat(context.argIndex).isEqualTo(expectedArgIndex);
    assertThat(context.strIndex).isEqualTo(expectedStrIndex);

  }

  @ParameterizedTest
  @CsvSource({
      "abc;def, 0, 0, false, abc, 1, 0",
      "abc;def, 0, 0, true,  abc, 1, 0",
      "abc;def, 1, 1, false, ,    1, 1",
      "abc;def, 1, 1, true,  ef,  2, 0",
      "abc;def, 2, 0, false, ,    2, 0",
  })
  void Context_consumeArg(String argList, int argIndex, int strIndex, boolean forOption, String expectedArg, int expectedArgIndex,
      int expectedStrIndex) {
    Context context = initContext(argList, argIndex, strIndex);
    assertThat(context.consumeArg(forOption)).isEqualTo(Optional.ofNullable(expectedArg));
    assertThat(context.argIndex).isEqualTo(expectedArgIndex);
    assertThat(context.strIndex).isEqualTo(expectedStrIndex);
  }

  @ParameterizedTest
  @CsvSource({
      "-abcd,   0, 0, -a, 0, 2",
      "-abcd,   0, 2, -b, 0, 3",
      "-abcd,   0, 3, ,   0, 3",
      "--aa,    0, 0, -a, 1, 0",
      "--aab,   0, 0, ,   0, 0",
      "-a;-b,   1, 0, -b, 2, 0",
      "-a;-bc,  1, 0, -b, 1, 2",
  })
  void Context_consumeOption(String argList, int argIndex, int strIndex, String expectedOption, int expectedArgIndex,
      int expectedStrIndex) {
    Option optA = option("-a", "--aa").build();
    Option optB = option("-b", "--bb").build();
    Map<String, Option> options = Stream.of(optA, optB)
        .flatMap(option -> option.names().stream().map(name -> new Pair<>(name, option)))
        .collect(Collectors.toMap(Pair::first, Pair::second));
    Context context = initContext(argList, argIndex, strIndex);

    assertThat(context.consumeOption(options).map(Option::name)).isEqualTo(Optional.ofNullable(expectedOption));
    assertThat(context.argIndex).isEqualTo(expectedArgIndex);
    assertThat(context.strIndex).isEqualTo(expectedStrIndex);
  }

  @ParameterizedTest
  @CsvSource({
      "a,    false",
      "--,   false",
      "--aa, true",
      "-a,   false",
      "-ab,  false",
  })
  void Context_isLongOptionName(String name, boolean expected) {
    assertThat(CommandLineParser.Context.isLongOptionName(name)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "a,    false",
      "-,    false",
      "--,   false",
      "--aa, false",
      "-a,   true",
      "-ab,  false",
  })
  void Context_isShortOptionName(String name, boolean expected) {
    assertThat(CommandLineParser.Context.isShortOptionName(name)).isEqualTo(expected);
  }

  static Context initContext(String argList, int argIndex, int strIndex) {
    Context context = new Context(new CommandLine(), TestUtils.stringToList(argList), false);
    context.argIndex = argIndex;
    context.strIndex = strIndex;
    return context;
  }
}
