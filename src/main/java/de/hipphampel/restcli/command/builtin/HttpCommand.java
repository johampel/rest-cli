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
package de.hipphampel.restcli.command.builtin;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.config.ApplicationConfig;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.rest.RequestContext;
import de.hipphampel.restcli.rest.RequestExecutor;
import de.hipphampel.restcli.rest.RequestExecutorFactory;
import de.hipphampel.restcli.rest.RequestTemplate;
import de.hipphampel.restcli.rest.ResponseAction;
import de.hipphampel.restcli.rest.ResponseActionFactory;
import de.hipphampel.restcli.template.TemplateRepository;
import de.hipphampel.restcli.utils.CollectionUtils;
import de.hipphampel.restcli.utils.KeyValue;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@ApplicationScoped
@Unremovable
public class HttpCommand extends BuiltinCommand {

  public static final String NAME = "http";

  static final Positional CMD_ARG_HEADER = positional("<header>=<value>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  static final Option CMD_OPT_HEADER = option("-h", "--header")
      .repeatable()
      .parameter(CMD_ARG_HEADER)
      .build();
  static final Positional CMD_ARG_JSON = positional("<key>=<json>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  static final Option CMD_OPT_JSON = option("-j", "--json")
      .repeatable()
      .parameter(CMD_ARG_JSON)
      .build();
  static final Positional CMD_ARG_VALUE = positional("<key>=<value>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  protected static final Option CMD_OPT_VALUE = option("-v", "--value")
      .repeatable()
      .parameter(CMD_ARG_VALUE)
      .build();
  static Positional CMD_ARG_METHOD = positional("<method>")
      .build();
  static Positional CMD_ARG_URI = positional("<uri>")
      .build();
  static Positional CMD_ARG_REQUEST_BODY = positional("<body>")
      .optional()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      `http` allows to execute arbitrary HTTP requests from scratch. The exact output format is specified via the global `--format` or
      `--template` option, so for example `${applicationName} --template abc http ...` uses a different output template than the default
      one.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <method>
            
      >The HTTP method to execute (so `get`, `post`, `put`, `delete` and so on). The `<method>` parameter is implicitly converted to an
      upper case string.
            
      <uri>
            
      >The complete request uri. The uri might contain references to placeholders which might be provided by the environment and/or in the 
      command line. For details please type `${applicationName} help :templates`.
            
      <body>
            
      >This is optional and contains the request body, specified as an input source, see section "Further Infos" for more information about
      the syntax. This might be omitted in case the request does not expect a body.
            
      -h | --header <header>=<value>
            
      >Passes in addition to the headers inherited from the current environment the specified header with the name `<header>` and the specified
      `<value>`. Since it is possible to specify more than one value for a specific header, it is valid to use this option more than once
      per `<header>`. If a header is already defined by the environment, the headers passed via the command line are prepended to the headers
      of the environment.
       
      -j | --json <key>=<json>
            
      >Defines the variable `<key>` having given the object `<json>`; the `<json>` is interpreted as a JSON string, so it might contain a 
      complex data structure like a map or list. If a same named value is already defined in the current environment, the value from the
      this option overwrites it for the request execution.
            
      -v|--value <key>=<value>
            
      >Defines the variable `<key>` having given the string `<value>`. In case that you want to assign a more complex structure to a
      variable, such as a list or map, you should use the `--json` option instead. If a same named value is already defined in the current
      environment, the value from the this option overwrites it for the request execution.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_INPUT_SOURCE);

  @Inject
  RequestExecutorFactory executorFactory;
  @Inject
  ResponseActionFactory responseActionFactory;
  @Inject
  ObjectMapper objectMapper;
  @Inject
  TemplateRepository templateRepository;

  public HttpCommand() {
    super(
        CommandAddress.fromString(NAME),
        "Executes an ad hoc HTTP request.",
        new CommandLineSpec(true, CMD_OPT_HEADER, CMD_OPT_VALUE, CMD_OPT_JSON, CMD_ARG_METHOD, CMD_ARG_URI,
            CMD_ARG_REQUEST_BODY),
        Map.of(HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    RequestContext requestContext = createRequestContext(context, commandLine);
    RequestTemplate requestTemplate = createRequestTemplate(context, requestContext, commandLine);
    ResponseAction<Boolean> responseAction = createResponseAction(context);
    return executeRequest(requestContext, requestTemplate, responseAction);
  }

  RequestContext createRequestContext(CommandContext context, CommandLine commandLine) {
    Map<String, Object> variables = createVariables(context, commandLine);
    return CommandUtils.createRequestContext(context, templateRepository, variables, variables.keySet());
  }

  RequestTemplate createRequestTemplate(CommandContext context, RequestContext requestContext, CommandLine commandLine) {
    Environment environment = context.environment();
    ApplicationConfig applicationConfig = context.applicationConfig();
    String method = commandLine.getValue(CMD_ARG_METHOD).map(String::toUpperCase).orElseThrow();
    String uri = commandLine.getValue(CMD_ARG_URI).orElseThrow();
    List<KeyValue<String>> headerSettings = commandLine.getValues(CMD_ARG_HEADER).stream()
        .map(KeyValue::fromString)
        .toList();
    Map<String, List<String>> headers = CollectionUtils.fillHeaders(context.environment().getHeaders(), headerSettings, false);
    InputStreamProviderConfig body = commandLine.getValue(CMD_ARG_REQUEST_BODY).map(InputStreamProviderConfig::fromString).orElse(null);
    long timeout = environment.getRequestTimeout() == null ? applicationConfig.getRequestTimeout() : environment.getRequestTimeout();
    return new RequestTemplate()
        .method(method)
        .baseUri(uri)
        .headers(headers)
        .expectContinue(false) // Currently unsupported
        .timeout(Duration.of(timeout, ChronoUnit.MILLIS))
        .requestBody(CommandUtils.createInputStreamProvider(context, body, requestContext.templateModel()));
  }

  ResponseAction<Boolean> createResponseAction(CommandContext context) {
    return responseActionFactory.doAndReturn(
        responseActionFactory.returnTrueIfSuccess(),
        responseActionFactory.print(context.out())
    );
  }

  boolean executeRequest(RequestContext requestContext, RequestTemplate requestTemplate, ResponseAction<Boolean> responseAction) {
    RequestExecutor executor = executorFactory.newExecutor(requestContext);
    try {
      return executor.execute(requestTemplate, responseAction);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to execute request", ioe);
    }
  }

  Map<String, Object> createVariables(CommandContext context, CommandLine commandLine) {
    List<KeyValue<?>> keyValues = Stream.concat(
            commandLine.getValues(CMD_ARG_VALUE).stream()
                .map(KeyValue::fromString),
            commandLine.getValue(CMD_ARG_JSON).stream()
                .map(KeyValue::fromString)
                .map(kav -> new KeyValue<>(kav.key(), parseJson(kav.value()))))
        .toList();
    return CommandUtils.fillVariables(context, context.environment().getVariables(), keyValues, false);
  }

  private Object parseJson(String str) {
    try {
      return objectMapper.treeToValue(objectMapper.readTree(str), Object.class);
    } catch (JsonProcessingException e) {
      throw new ExecutionException("Failed to convert value \"%s\" to a JSON object.".formatted(str));
    }
  }

}
