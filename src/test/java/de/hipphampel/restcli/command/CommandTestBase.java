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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hipphampel.restcli.api.ApiFactory;
import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.cli.commandline.CommandLineParser;
import de.hipphampel.restcli.command.builtin.BuiltinCommand;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfigRepository;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.template.TemplateRenderer;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;

public class CommandTestBase {

  @Inject
  protected CommandRepository commandRepository;
  @Inject
  protected CommandConfigRepository commandConfigRepository;
  @Inject
  protected CommandInvoker commandInvoker;
  @Inject
  protected CommandLineParser commandLineParser;
  @Inject
  protected TemplateRenderer templateRenderer;
  @Inject
  protected ApiFactory templateApiFactory;
  private StringWriter out;
  private StringWriter err;
  protected CommandContext context;

  protected Path rootDir;

  @BeforeEach
  protected void beforeEach(Path rootDir) throws IOException {
    this.rootDir = rootDir;
    commandRepository.getBuiltins().clear();
    commandRepository.fillBuiltinMapIfRequired();
    this.out = new StringWriter();
    this.err = new StringWriter();
    this.context = new CommandContext(commandInvoker, templateRenderer, templateApiFactory)
        .configPath(rootDir)
        .applicationName("test-app")
        .environment(Environment.empty())
        .err(new Output(err).withStyles(false).withOutputWidth(60))
        .out(new Output(out).withStyles(false).withOutputWidth(60));
  }

  protected void assertExecution(Command command, List<String> args, boolean expectedExitCode, String expectedOut, String expectedErr) {
    assertExecution(() -> command.execute(context, commandLineParser.parseCommandLine(command.commandLineSpec(), args)), expectedExitCode,
        expectedOut,
        expectedErr);
  }

  protected void assertExecution(Supplier<Boolean> execution, boolean expectedExitCode, String expectedOut, String expectedErr) {
    assertThat(execution.get()).isEqualTo(expectedExitCode);
    assertOutput(expectedOut, expectedErr);
  }

  protected void assertOutput(String expectedOut, String expectedErr) {
    assertStdOut(expectedOut);
    assertStdErr(expectedErr);
  }

  protected void assertStdErr(String expected) {
    assertThat(err).hasToString(expected);
  }

  protected String getStdErr() {
    return err.toString();
  }

  protected void assertStdOut(String expected) {
    assertThat(out).hasToString(expected);
  }

  protected void registerCommand(BuiltinCommand command) {
    commandRepository.getBuiltins().put(command.address(), command);
  }

  protected BuiltinCommand mockCommand(CommandAddress address) {
    BuiltinCommand command = mock(BuiltinCommand.class);
    when(command.address()).thenReturn(address);
    when(command.name()).thenReturn(address.name());
    commandRepository.getBuiltins().put(address, command);
    return command;
  }

  protected void storeCommand(CommandAddress address, CommandConfig config) {
    commandConfigRepository.store(context.configPath(), address, config);
  }
}
