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
package de.hipphampel.restcli.command.custom;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterListConfig;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import de.hipphampel.restcli.config.ApplicationConfig;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.rest.RequestContext;
import de.hipphampel.restcli.rest.RequestExecutor;
import de.hipphampel.restcli.rest.RequestExecutorFactory;
import de.hipphampel.restcli.rest.RequestTemplate;
import de.hipphampel.restcli.rest.ResponseAction;
import de.hipphampel.restcli.rest.ResponseActionFactory;
import de.hipphampel.restcli.template.TemplateRepository;
import de.hipphampel.restcli.utils.CollectionUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomHttpCommand extends CustomCommand {

  private final TemplateRepository templateRepository;
  private final RequestExecutorFactory executorFactory;
  private final ResponseActionFactory responseActionFactory;

  public CustomHttpCommand(RequestExecutorFactory executorFactory, ResponseActionFactory responseActionFactory,
      TemplateRepository templateRepository, CommandAddress address, CommandConfig config) {
    super(address, config);
    Objects.requireNonNull(config.getRestConfig());
    this.executorFactory = Objects.requireNonNull(executorFactory);
    this.responseActionFactory = Objects.requireNonNull(responseActionFactory);
    this.templateRepository = Objects.requireNonNull(templateRepository);
  }

  @Override
  public CommandLineSpec commandLineSpec() {
    return config().getRestConfig().getParameters().getCommandLineSpec();
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    RequestContext requestContext = createRequestContext(context, commandLine);
    RequestTemplate requestTemplate = createRequestTemplate(context, requestContext);
    ResponseAction<Boolean> responseAction = createResponseAction(context);
    return executeRequest(requestContext, requestTemplate, responseAction);
  }

  boolean executeRequest(RequestContext requestContext, RequestTemplate requestTemplate, ResponseAction<Boolean> responseAction) {
    RequestExecutor executor = executorFactory.newExecutor(requestContext);
    try {
      return executor.execute(requestTemplate, responseAction);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to execute request", ioe);
    }
  }

  ResponseAction<Boolean> createResponseAction(CommandContext context) {
    return responseActionFactory.doAndReturn(
        responseActionFactory.returnTrueIfSuccess(),
        responseActionFactory.print(context.out())
    );
  }

  RequestTemplate createRequestTemplate(CommandContext context, RequestContext requestContext) {
    RestCommandConfig restConfig = config().getRestConfig();
    Environment environment = context.environment();
    ApplicationConfig applicationConfig = context.applicationConfig();
    long timeout = environment.getRequestTimeout() == null ? applicationConfig.getRequestTimeout() : environment.getRequestTimeout();
    InputStreamProvider body = createBody(context, requestContext);
    return new RequestTemplate()
        .method(restConfig.getMethod())
        .baseUri(restConfig.getBaseUri())
        .headers(CollectionUtils.mergeHeaders(environment.getHeaders(), restConfig.getHeaders()))
        .queryParameters(restConfig.getQueryParameters())
        .expectContinue(false) // Currently unsupported
        .timeout(Duration.of(timeout, ChronoUnit.MILLIS))
        .requestBody(body);
  }

  InputStreamProvider createBody(CommandContext context, RequestContext requestContext) {
    RestCommandConfig restConfig = config().getRestConfig();
    if (restConfig.getBody() == null) {
      return null;
    }
    BodyConfig bodyConfig = restConfig.getBody();
    if (bodyConfig.variable() != null) {
      if (!requestContext.templateModel().containsKey(bodyConfig.variable())) {
        return null;
      }
      Object variableContent = requestContext.templateModel().get(bodyConfig.variable());
      InputStreamProviderConfig config = InputStreamProviderConfig.fromString(String.valueOf(variableContent));
      return CommandUtils.createInputStreamProvider(context, config, requestContext.templateModel());
    } else {
      return CommandUtils.createInputStreamProvider(context, bodyConfig.content(), requestContext.templateModel());
    }
  }

  RequestContext createRequestContext(CommandContext context, CommandLine commandLine) {
    Map<String, Object> variables = collectVariables(commandLine);
    Set<String> declaredVariables = config().getRestConfig().getParameters().getParameters().stream()
        .map(ParameterConfig::variable)
        .collect(Collectors.toSet());
    return CommandUtils.createRequestContext(context, templateRepository, variables, declaredVariables);
  }

  Map<String, Object> collectVariables(CommandLine commandLine) {
    Map<String, Object> variables = new HashMap<>();
    ParameterListConfig parameterListConfig = config().getRestConfig().getParameters();
    for (Map.Entry<ParameterConfig, Positional> entry : parameterListConfig.getPositionalMap().entrySet()) {
      ParameterConfig config = entry.getKey();
      Positional positional = entry.getValue();

      if (config.style().repeatable) {
        variables.put(config.variable(), commandLine.getValues(positional));
      } else {
        commandLine.getValue(positional).ifPresent(value -> variables.put(config.variable(), value));
      }
    }
    return variables;
  }
}
