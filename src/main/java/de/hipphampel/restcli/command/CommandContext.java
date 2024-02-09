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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.api.ApiFactory;
import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.config.ApplicationConfig;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.env.EnvironmentConfig;
import de.hipphampel.restcli.template.TemplateRenderer;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Objects;

public class CommandContext {

  public static final String DEFAULT_APPLICATION_NAME = "restcli";
  public static final Positional CMD_ARG_ENVIRONMENT = positional("<environment>")
      .build();
  public static final Option CMD_OPT_ENVIRONMENT = option("-e", "--environment")
      .parameter(CMD_ARG_ENVIRONMENT)
      .build();
  public static final Positional CMD_ARG_TEMPLATE = positional("<name-or-address>")
      .build();
  public static final Option CMD_OPT_TEMPLATE = option("-t", "--template")
      .parameter(CMD_ARG_TEMPLATE)
      .exclusionGroup("formatOrTemplate")
      .build();
  public static final Positional CMD_ARG_FORMAT = positional("<format>")
      .build();
  public static final Option CMD_OPT_FORMAT = option("-f", "--format")
      .parameter(CMD_ARG_FORMAT)
      .exclusionGroup("formatOrTemplate")
      .build();

  public static final Positional CMD_ARG_OUTPUT_PARAMETER = positional("<key>=<value>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_OUTPUT_PARAMETER = option("-o", "--output-parameter")
      .repeatable()
      .parameter(CMD_ARG_OUTPUT_PARAMETER)
      .build();

  private final CommandInvoker commandInvoker;
  private final ApiFactory templateApiFactory;
  private final TemplateRenderer templateRenderer;
  private boolean interactive;
  private CommandAddress commandAddress;
  private String applicationName;
  private ApplicationConfig applicationConfig;
  private Path configPath;
  private Output out;
  private Output err;
  private InputStream in;
  private Environment environment;
  private HttpClient httpClient;
  private CommandLine rootCommandLine;

  public CommandContext(CommandInvoker commandInvoker, TemplateRenderer templateRenderer, ApiFactory templateApiFactory) {
    this.commandInvoker = Objects.requireNonNull(commandInvoker);
    this.templateRenderer = Objects.requireNonNull(templateRenderer);
    this.templateApiFactory = Objects.requireNonNull(templateApiFactory);
    this.applicationName = DEFAULT_APPLICATION_NAME;
    this.applicationConfig = new ApplicationConfig();
    this.commandAddress = CommandAddress.ROOT;
    this.configPath = null;
    this.out = new Output(System.out);
    this.err = new Output(System.err);
    this.in = System.in;
    this.environment = new Environment(null, Environment.EMPTY, EnvironmentConfig.EMPTY, EnvironmentConfig.EMPTY);
    this.httpClient = HttpClient.newBuilder().build();
    this.rootCommandLine = new CommandLine();
    this.interactive = false;
  }

  public CommandContext(CommandContext source) {
    this.commandInvoker = source.commandInvoker;
    this.templateRenderer = source.templateRenderer;
    this.templateApiFactory = source.templateApiFactory;
    this.applicationName = source.applicationName;
    this.applicationConfig = source.applicationConfig;
    this.commandAddress = source.commandAddress;
    this.configPath = source.configPath;
    this.out = source.out;
    this.err = source.err;
    this.in = source.in;
    this.environment = source.environment;
    this.httpClient = source.httpClient;
    this.rootCommandLine = source.rootCommandLine;
    this.interactive = source.interactive;
  }

  public CommandInvoker commandInvoker() {
    return commandInvoker;
  }

  public TemplateRenderer templateRenderer() {
    return templateRenderer;
  }

  public ApiFactory apiFactory() {
    return templateApiFactory;
  }

  public String applicationName() {
    return applicationName;
  }

  public CommandContext applicationName(String applicationName) {
    this.applicationName = applicationName;
    return this;
  }

  public ApplicationConfig applicationConfig() {
    return applicationConfig;
  }

  public CommandContext applicationConfig(ApplicationConfig applicationConfig) {
    this.applicationConfig = Objects.requireNonNull(applicationConfig);
    return this;
  }

  public CommandAddress commandAddress() {
    return this.commandAddress;
  }

  public CommandContext commandAddress(CommandAddress commandAddress) {
    this.commandAddress = Objects.requireNonNull(commandAddress);
    return this;
  }

  public Path configPath() {
    return configPath;
  }

  public CommandContext configPath(Path configPath) {
    this.configPath = Objects.requireNonNull(configPath);
    return this;
  }

  public Output out() {
    return out;
  }

  public CommandContext out(Output out) {
    this.out = Objects.requireNonNull(out);
    return this;
  }

  public Output err() {
    return err;
  }

  public CommandContext err(Output err) {
    this.err = Objects.requireNonNull(err);
    return this;
  }

  public InputStream in() {
    return in;
  }

  public CommandContext in(InputStream in) {
    this.in = Objects.requireNonNull(in);
    return this;
  }

  public Environment environment() {
    return environment;
  }

  public CommandContext environment(Environment environment) {
    this.environment = Objects.requireNonNull(environment);
    return this;
  }

  public HttpClient httpClient() {
    return httpClient;
  }

  public CommandContext httpClient(HttpClient httpClient) {
    this.httpClient = Objects.requireNonNull(httpClient);
    return this;
  }

  public CommandLine rootCommandLine() {
    return rootCommandLine;
  }

  public CommandContext rootCommandLine(CommandLine rootCommandLine) {
    this.rootCommandLine = rootCommandLine;
    return this;
  }

  public boolean interactive() {
    return interactive;
  }

  public CommandContext interactive(boolean interactive) {
    this.interactive = interactive;
    return this;
  }
}
