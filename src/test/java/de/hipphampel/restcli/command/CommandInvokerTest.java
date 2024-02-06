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
package de.hipphampel.restcli.command;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_ENVIRONMENT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_FORMAT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_OUTPUT_PARAMETER;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_TEMPLATE;
import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentRepository;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.exception.UsageException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class CommandInvokerTest extends CommandTestBase {

  @Inject
  EnvironmentRepository environmentRepository;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @Test
  void invokeCommand_notFound() {
    assertExecution(() -> commandInvoker.invokeCommand(context, CommandAddress.fromString("not_found"), List.of()),
        false,
        "",
        """
            *** error test-app: No such command "not_found".
            """);
  }

  @Test
  void invokeCommand_ok() {
    Command command = mockCommand(CommandAddress.fromString("test"));
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(true));
    when(command.execute(any(), any())).thenAnswer(call -> {
      context.out().line("called with address " + context.commandAddress());
      return true;
    });
    assertExecution(() -> commandInvoker.invokeCommand(context, CommandAddress.fromString("test"), List.of()),
        true,
        """
            called with address test
            """,
        "");
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false"})
  void runCommand_ok(String value) {
    Command command = mockCommand(CommandAddress.fromString("test"));
    Positional arg = positional("arg").build();
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(true, arg));
    when(command.execute(any(), any())).thenAnswer(call -> {
      context.out().line(call.getArgument(1, CommandLine.class).getValue(arg).orElseThrow());
      return call.getArgument(1, CommandLine.class).getValue(arg).map(Boolean::parseBoolean).orElseThrow();
    });

    assertExecution(() -> commandInvoker.runCommand(context, command, List.of(value)),
        Boolean.parseBoolean(value),
        value + System.lineSeparator(),
        "");
  }

  @Test
  void runCommand_usageException() {
    Command command = mockCommand(CommandAddress.fromString("test"));
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(true));
    when(command.execute(any(), any())).thenThrow(new IllegalArgumentException("Shouldn't be reached."));

    assertExecution(() -> commandInvoker.runCommand(context, command, List.of("arg")),
        false,
        "",
        """
            *** error test-app: Unexpected argument "arg".
            usage: test-app test\s
            """);
  }

  @Test
  void runCommand_executionException() {
    Command command = mockCommand(CommandAddress.fromString("test"));
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(true));
    when(command.execute(any(), any())).thenThrow(new ExecutionException("boom."));

    assertExecution(() -> commandInvoker.runCommand(context, command, List.of()),
        false,
        "",
        """
            *** error test-app: boom.
            """);
  }

  @Test
  void runCommand_unexpectedException() {
    Command command = mockCommand(CommandAddress.fromString("test"));
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(true));
    when(command.execute(any(), any())).thenThrow(new IllegalArgumentException("Boom"));

    assertThat(commandInvoker.runCommand(context, command, List.of())).isFalse();
    assertStdOut("");
    assertThat(getStdErr()).startsWith("*** error test-app: unexpected error: Boom.");
  }

  @Test
  void onUsageException() {
    Command command = mockCommand(CommandAddress.fromString("command"));
    when(command.commandLineSpec()).thenReturn(new CommandLineSpec(true, positional("arg").repeatable().build()));

    commandInvoker.onUsageException(context, command, new UsageException("Wrong parameters."));

    assertOutput(
        "",
        """
            *** error test-app: Wrong parameters.
            usage: test-app command arg...
            """);
  }

  @Test
  void onExecutionException() {
    CommandInvoker.onExecutionException(context, new ExecutionException("Something went wrong."));

    assertOutput(
        "",
        """
            *** error test-app: Something went wrong.
            """);
  }

  @Test
  void onUnexpectedException() {
    try {
      throw new IllegalArgumentException("Boom");
    } catch (IllegalArgumentException iae) {
      CommandInvoker.onUnexpectedException(context, iae);
      assertStdOut("");
      assertThat(getStdErr()).startsWith("""
          *** error test-app: unexpected error: Boom.
                              java.lang.IllegalArgumentException: Boom
                              \tat de.hipphampel.restcli.command.CommandInvokerTest.onUnexpectedException(""");
    }
  }

  @Test
  void onCommandNotFound_topLevel() {
    commandInvoker.onCommandNotFound(context, CommandAddress.fromString("not_found"));

    assertOutput(
        "",
        """
            *** error test-app: No such command "not_found".
            """);
  }

  @Test
  void onCommandNotFound_child() {
    mockCommand(CommandAddress.fromString("top_level"));
    commandInvoker.onCommandNotFound(context, CommandAddress.fromString("top_level/not_found/also_not_found"));

    assertOutput(
        "",
        """
            *** error test-app: Unknown sub-command "not_found" for
                                "top_level".
            Type `test-app help top_level` to get a list of available
            sub-commands.
            """);
  }

  @Test
  void onCommandNotFound_root() {
    commandInvoker.onCommandNotFound(context, CommandAddress.ROOT);

    assertOutput(
        "",
        """
            *** error test-app: Unable to find application command.
            """);
  }

  @ParameterizedTest
  @CsvSource({
      ",              ,              ",
      "'-troot',      ,              '-troot'",
      "'-troot',      '-tchild',     '-tchild'",
      "'-froot',      ,              '-froot'",
      "'-froot',      '-fchild',     '-fchild'",
      "'-oa=1;-ob=2', ,              '-oa=1;-ob=2'",
      "'-oa=1;-ob=2', '-ob=3;-oc=4', '-oa=1;-ob=3;-oc=4'",
      ",              '-ob=3;-oc=4', '-ob=3;-oc=4'",
  })
  void mergeOutputOptions(String root, String alias, String expected) {
    CommandLineSpec spec = new CommandLineSpec(false, CMD_OPT_ENVIRONMENT, CMD_OPT_FORMAT, CMD_OPT_TEMPLATE, CMD_OPT_OUTPUT_PARAMETER,
        CMD_ARG_SUB_COMMAND);

    CommandLine rootCommandLine = commandLineParser.parseCommandLine(spec, TestUtils.stringToList(root));
    CommandLine aliasCommandLine = commandLineParser.parseCommandLine(spec, TestUtils.stringToList(alias));
    CommandLine expectedCommandLine = commandLineParser.parseCommandLine(spec, TestUtils.stringToList(expected));
    commandInvoker.mergeOutputOptions(rootCommandLine, aliasCommandLine);

    assertThat(aliasCommandLine).isEqualTo(expectedCommandLine);
  }

  @ParameterizedTest
  @CsvSource({
      ",         'root'",
      "-echild,  'child'"
  })
  void getEnvironment_ok(String alias, String expected) {
    Environment rootEnvironment = environmentRepository.createTransientEnvironment("root", null);
    environmentRepository.storeEnvironment(context.configPath(), rootEnvironment, false);
    context.environment(rootEnvironment);
    Environment childEnvironment = environmentRepository.createTransientEnvironment("child", null);
    environmentRepository.storeEnvironment(context.configPath(), childEnvironment, false);
    CommandLineSpec spec = new CommandLineSpec(false, CMD_OPT_ENVIRONMENT, CMD_OPT_FORMAT, CMD_OPT_TEMPLATE, CMD_OPT_OUTPUT_PARAMETER,
        CMD_ARG_SUB_COMMAND);
    CommandLine aliasCommandLine = commandLineParser.parseCommandLine(spec, TestUtils.stringToList(alias));

    Environment environment = commandInvoker.getEnvironment(context, aliasCommandLine);
    assertThat(environment.getName()).isEqualTo(expected);
  }

  @Test
  void getEnvironment_fail() {
    CommandLineSpec spec = new CommandLineSpec(false, CMD_OPT_ENVIRONMENT, CMD_OPT_FORMAT, CMD_OPT_TEMPLATE, CMD_OPT_OUTPUT_PARAMETER,
        CMD_ARG_SUB_COMMAND);
    CommandLine aliasCommandLine = commandLineParser.parseCommandLine(spec, List.of("-enot_found"));

    assertThatThrownBy(() -> commandInvoker.getEnvironment(context, aliasCommandLine))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("No such environment \"not_found\".");
  }
}
