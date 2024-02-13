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
package de.hipphampel.restcli.command.builtin.cmd;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_ARG_ADDRESS;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_FORCE;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_REPLACE;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.HelpSnippets;
import de.hipphampel.restcli.command.builtin.cmd.openapi.Spec;
import de.hipphampel.restcli.command.builtin.cmd.openapi.Spec.OperationCoordinate;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.CommandConfigTree;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterConfig.Style;
import de.hipphampel.restcli.command.config.ParameterListConfig;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.arc.Unremovable;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.converter.SwaggerConverter;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Unremovable
public class CmdOpenApiCommand extends CmdWriteCommandBase {

  static final String NAME = "openapi";
  static final String BODY_VAR = "_body";

  static final Option CMD_OPT_GROUP_BY_TAG = option("-g", "--group-by-tag")
      .build();
  static final Option CMD_OPT_INCLUDE_DEPRECATED_OPERATIONS = option("-d", "--include-deprecated-operations")
      .build();
  static final Positional CMD_ARG_BASE_URI = positional("<base-uri>")
      .build();
  static final Option CMD_OPT_BASE_URI = option("-b", "--base-uri")
      .parameter(CMD_ARG_BASE_URI)
      .build();
  static final Positional CMD_ARG_TAG = positional("<tag>")
      .build();
  static final Option CMD_OPT_TAG = option("-t", "--tag")
      .parameter(CMD_ARG_TAG)
      .repeatable()
      .build();
  static final Positional CMD_ARG_CONTENT_TYPE_OPT = positional("<option-name>")
      .id("<content-type>")
      .build();
  static final Option CMD_OPT_CONTENT_TYPE_OPT = option("-c", "--content-type-option")
      .parameter(CMD_ARG_CONTENT_TYPE_OPT)
      .build();
  static final Positional CMD_ARG_ACCEPT_OPT = positional("<option-name>")
      .id("<accept>")
      .build();
  static final Option CMD_OPT_ACCEPT_OPT = option("-a", "--accept-option")
      .parameter(CMD_ARG_ACCEPT_OPT)
      .build();
  static final Positional CMD_ARG_SPEC = positional("<spec>")
      .optional()
      .build();
  static final Positional CMD_ARG_OPERATIONS = positional("<operation>")
      .build();
  static final Option CMD_OPT_OPERATIONS = option("-o", "--operation")
      .parameter(CMD_ARG_OPERATIONS)
      .repeatable()
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection(""" 
       Reads an OpenAPI specification and creates according commands for it.

      There are a couple of options that allow to filter the commands you want to import and to influence the exact shape of the commands.
      You may modify the commands via the `mod` command after the import.
       """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <address>
            
      >The address of the command to import to. In most cases, for this address a group command will be created that contains
      the different commands extracted from the OpenAPI as sub commands. In the special case that you import just a single command by
      specifying just a single `<operation>`, the `<address>` will point to this single operation.
            
      <source>
            
      >The source where to read the OpenAPI specification from. If reading from a file, prefix the file name with an `@` sign: this is
      specified as an *input source* (see section "Further Infos").
            
      -a | --accept-option <option-name>
            
      >If specified, each generated HTTP command supporting more than one media type for its responses will have the option `<option-name>`
      in addition to the options imported from the OpenAPI specification. This allows to explicitly specify the `Accept` header for the
      request. *You have to specify the option name without any leading dash*.

      >If you do not specify such an option and the HTTP supports more than one content type, it uses the first media type found in
      the OpenAPI specification as the predefined one.
            
      -b | --base-uri <uri>
            
      >The base uri to prefix each imported path. So if the OpenAPI specification contains the path `/some/path`, it is prefixed by the value
      configured by this option. If this option is omitted, the default prefix is `${r"${baseUri}"}`.
                  
      -c | --content-type-option <option-name>
            
      >If specified, each generated HTTP command supporting more than one content type for its request body will have the option
      `<option-name>` in addition to the options imported from the OpenAPI specification. This allows to explicitly specify the `Content-Type`
      header for the request. *You have to specify the option name without any leading dash*.

      >If you do not specify such an option and the HTTP supports more than one content type, it uses the first content type found in
      the OpenAPI specification as the predefined one.
            
      -d | --include-deprecated-operations
            
      >If specified, it imports also operations marked as deprecated. By default, these are skipped.
            
      -g | --group-by-tag
            
      >When specified, the commands are grouped by tags. In other words, for each tag, a further group command representing the tag
      is created (right under the command specified by `<address>`), and all commands belonging to this tag become children of this command.
      Without this option, no extra commands for the tags are created.

      -o | --operation <operation>
            
      >If you wish to import individual operations instead of all operations found in the OpenAPI specification, you may specify these
      operations you are interested in. The `<operation>` argument can be specified more than once. Please note that - even if explicitly
      specified - an operation might be filtered out due to other options, such as `--tag`.

      -r | --replace
            
      >If there is already a command with the given `<address>` you have to provide this option in case you really want to replace it.
      In case of a replacement, as a further cross check, the following option is present:
            
      >-f | --force
            
      >>This option is required in case that the command to replace has sub-commands. Note that when replacing a group command, all its
      sub commands are implicitly removed.
            
      -t | --tag <tag>
            
      >If specified, import only those commands belonging to the specified `<tag>`. You may specify this option more than once.
      """);

  static final Function<CommandContext, Block> HELP_SECTION_FURTHER_INFOS = CommandUtils.helpSection(
      HelpSnippets.FURTHER_INFOS_COMMAND_ADDRESS +
          """
              >
                         
               """ +
          HelpSnippets.FURTHER_INFOS_INPUT_SOURCE);

  public CmdOpenApiCommand() {
    super(NAME,
        "Imports commands from an OpenAPI spec.",
        new CommandLineSpec(true, CMD_OPT_BASE_URI, CMD_OPT_ACCEPT_OPT, CMD_OPT_CONTENT_TYPE_OPT, CMD_OPT_INCLUDE_DEPRECATED_OPERATIONS,
            CMD_OPT_GROUP_BY_TAG, CMD_OPT_TAG, CMD_OPT_OPERATIONS, CMD_OPT_REPLACE, CMD_ARG_ADDRESS, CMD_ARG_SPEC),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS,
            HelpSection.FURTHER_INFOS, HELP_SECTION_FURTHER_INFOS));

  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    InputStreamProviderConfig specName = commandLine.getValue(CMD_ARG_SPEC)
        .map(InputStreamProviderConfig::fromString)
        .orElseGet(() -> InputStreamProviderConfig.fromString("@"));

    ImportContext importContext = new ImportContext();
    importContext.baseUri = commandLine.getValue(CMD_ARG_BASE_URI).orElse("${baseUri}");
    importContext.acceptOption = commandLine.getValue(CMD_ARG_ACCEPT_OPT).orElse(null);
    importContext.contentTypeOption = commandLine.getValue(CMD_ARG_CONTENT_TYPE_OPT).orElse(null);
    importContext.address = commandLine.getValue(CMD_ARG_ADDRESS).map(CommandAddress::fromString).orElseThrow();
    importContext.replace = commandLine.hasOption(CMD_OPT_REPLACE);
    importContext.force = commandLine.hasOption(CMD_OPT_FORCE);
    importContext.operationIds = commandLine.getValues(CMD_ARG_OPERATIONS);
    importContext.tags = commandLine.getValues(CMD_ARG_TAG);
    importContext.includeDeprecated = commandLine.hasOption(CMD_OPT_INCLUDE_DEPRECATED_OPERATIONS);
    importContext.grouped = commandLine.hasOption(CMD_OPT_GROUP_BY_TAG);

    importOpenApi(context, importContext, specName);
    return true;
  }

  void importOpenApi(CommandContext context, ImportContext importContext, InputStreamProviderConfig specName) {

    checkIfValidCustomCommandAddress(context, importContext.address);
    checkIfReplacementValid(context, importContext.address, importContext.replace, importContext.force);

    Spec spec = readOpenAPISpec(context, specName);
    List<OperationCoordinate> coordinates = spec.allOperationCoordinates().stream()
        .filter(coordinate -> isOperationRelevant(spec, coordinate, importContext))
        .toList();

    CommandConfigTree commands;
    if (importContext.grouped) {
      commands = createGroupedOperations(spec, coordinates, importContext);
    } else {
      commands = createUngroupedOperations(spec, coordinates, importContext);
    }
    store(context, importContext.address, commands);
  }

  static Spec readOpenAPISpec(CommandContext context, InputStreamProviderConfig name) {
    String content = CommandUtils.toString(context, name, Map.of());

    // Try version 3.x
    ParseOptions resolve = new ParseOptions();
    resolve.setResolve(true);
    resolve.setFlatten(true);
    resolve.setResolveFully(true);
    SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, List.of(), resolve);

    // Try Version 2.x
    if (result.getOpenAPI() == null) {
      result = new SwaggerConverter().readContents(content, List.of(), resolve);
    }

    if (result.getOpenAPI() != null) {
      for (String message : result.getMessages()) {
        CommandUtils.showWarning(context, "While reading OpenAPI Spec: %s.", message);
      }
      return new Spec(result.getOpenAPI());
    }

    throw new ExecutionException("OpenAPI specification contains problems: %s".formatted(String.join("\n", result.getMessages())));
  }

  static boolean isOperationRelevant(Spec spec, OperationCoordinate coordinate, ImportContext importContext) {
    Operation operation = spec.operationOf(coordinate).orElse(null);
    if (operation == null || operation.getOperationId() == null) {
      return false;
    }
    if (!importContext.operationIds.isEmpty() && !importContext.operationIds.contains(operation.getOperationId())) {
      return false;
    }
    if (Boolean.TRUE.equals(operation.getDeprecated()) && !importContext.includeDeprecated) {
      return false;
    }
    if (importContext.tags.isEmpty()) {
      return true;
    }
    Set<String> operationTags = new HashSet<>(spec.tagsOf(coordinate));
    operationTags.retainAll(importContext.tags);
    return !operationTags.isEmpty();
  }

  static CommandConfigTree createGroupedOperations(Spec spec, List<OperationCoordinate> coordinates, ImportContext importContext) {
    Map<String, List<OperationCoordinate>> coordinatesByTag = new HashMap<>();
    for (OperationCoordinate coordinate : coordinates) {
      Set<String> operationTags = new HashSet<>(spec.tagsOf(coordinate));
      if (!importContext.tags.isEmpty()) {
        operationTags.retainAll(importContext.tags);
      }
      for (String tag : operationTags) {
        coordinatesByTag.computeIfAbsent(tag, ign -> new ArrayList<>()).add(coordinate);
      }
    }

    return new CommandConfigTree()
        .setConfig(createCommandForApi(spec, importContext))
        .setSubCommands(coordinatesByTag.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> openApiToCommandName(entry.getKey()),
                entry -> createTagOperations(spec, entry.getKey(), entry.getValue(), importContext)
            )));
  }

  static CommandConfigTree createTagOperations(Spec spec, String tag, List<OperationCoordinate> coordinates, ImportContext importContext) {
    Map<String, CommandConfig> commands = createCommandsForOperations(spec, coordinates, importContext);
    return new CommandConfigTree()
        .setConfig(createCommandForTag(spec, tag))
        .setSubCommands(commands.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, e -> new CommandConfigTree().setConfig(e.getValue()))));
  }

  static CommandConfig createCommandForTag(Spec spec, String tag) {
    String description = spec.descriptionOfTag(tag, "The %s command".formatted(openApiToCommandName(tag)));
    return new CommandConfig()
        .setSynopsis(extractSynopsis(description))
        .setType(Type.Parent)
        .setDescriptions(Map.of(
            HelpSection.DESCRIPTION, description
        ));
  }

  static CommandConfigTree createUngroupedOperations(Spec spec, List<OperationCoordinate> coordinates, ImportContext importContext) {
    Map<String, CommandConfig> commands = createCommandsForOperations(spec, coordinates, importContext);
    CommandConfigTree commandConfigTree = new CommandConfigTree();
    if (commands.size() > 1) {
      commandConfigTree.setConfig(createCommandForApi(spec, importContext))
          .setSubCommands(commands.entrySet().stream()
              .collect(Collectors.toMap(Entry::getKey, e -> new CommandConfigTree().setConfig(e.getValue()))));
    } else if (commands.size() == 1) {
      commandConfigTree.setConfig(commands.values().iterator().next());
    }
    return commandConfigTree;
  }

  static CommandConfig createCommandForApi(Spec spec, ImportContext importContext) {
    return new CommandConfig()
        .setType(Type.Parent)
        .setSynopsis(spec.synopsisOfApi("The %s command.".formatted(importContext.address.name())))
        .setDescriptions(Map.of(
            HelpSection.DESCRIPTION, spec.descriptionOfApi("No documentation.")));
  }

  static Map<String, CommandConfig> createCommandsForOperations(Spec spec, List<OperationCoordinate> operations,
      ImportContext importContext) {
    return operations.stream()
        .collect(Collectors.toMap(
            coordinate -> commandNameOf(spec, coordinate),
            coordinate -> createCommandForOperation(spec, coordinate, importContext)));
  }

  static CommandConfig createCommandForOperation(Spec spec, OperationCoordinate coordinate, ImportContext importContext) {
    List<Pair<Parameter, ParameterConfig>> parameters = createParameterConfigs(spec, coordinate, importContext);

    String effectiveBaseUri = createBaseUri(coordinate, parameters, importContext);
    Map<String, String> queryParameters = createQueryParameters(parameters);
    Map<String, List<String>> headers = createHeaders(spec, coordinate, parameters, importContext);
    BodyConfig body = createBody(spec, coordinate);
    String operationDescription = spec.descriptionOfOperation(coordinate, "The %s command.".formatted(commandNameOf(spec, coordinate)));
    return new CommandConfig()
        .setType(Type.Http)
        .setSynopsis(extractSynopsis(operationDescription))
        .setDescriptions(Map.of(
            HelpSection.DESCRIPTION, operationDescription == null ? "No documentation." : operationDescription,
            HelpSection.ARGS_AND_OPTIONS, createParameterDescription(spec, parameters)
        ))
        .setRestConfig(new RestCommandConfig()
            .setMethod(coordinate.method().name())
            .setBaseUri(effectiveBaseUri)
            .setQueryParameters(queryParameters)
            .setHeaders(headers)
            .setBody(body)
            .setParameters(new ParameterListConfig(parameters.stream().map(Pair::second).toList())));
  }

  static Map<String, String> createQueryParameters(List<Pair<Parameter, ParameterConfig>> parameters) {
    return parameters.stream()
        .filter(p -> Objects.equals("query", p.first().getIn()))
        .collect(Collectors.toMap(
            p -> p.first().getName(),
            p -> "${%s}".formatted(p.second().variable())
        ));
  }

  static Map<String, List<String>> createHeaders(Spec spec, OperationCoordinate coordinate,
      List<Pair<Parameter, ParameterConfig>> parameters, ImportContext importContext) {

    Map<String, List<String>> headers = new HashMap<>();
    for (Pair<Parameter, ParameterConfig> entry : parameters) {
      if (!Objects.equals("header", entry.first().getIn())) {
        continue;
      }
      if (importContext.contentTypeOption != null && Objects.equals(entry.first().getName(), importContext.contentTypeOption)) {
        continue;
      }
      if (importContext.acceptOption != null && Objects.equals(entry.first().getName(), importContext.acceptOption)) {
        continue;
      }
      List<String> values = headers.computeIfAbsent(entry.first().getName(), ign -> new ArrayList<>());
      values.add("${%s}".formatted(entry.second().variable()));
    }

    List<String> contentTypes = spec.requestBodyMediaTypesOf(coordinate);
    if (!contentTypes.isEmpty()) {
      String contentType = contentTypes.get(0);
      if (importContext.contentTypeOption != null) {
        contentType = "${%s!\"%s\"}".formatted(openApiToVariableName(importContext.contentTypeOption), contentType);
      }
      headers.put("Content-Type", List.of(contentType));
    }

    List<String> acceptTypes = spec.responseMediaTypesOf(coordinate);
    if (!acceptTypes.isEmpty()) {
      String acceptType = acceptTypes.get(0);
      if (importContext.acceptOption != null) {
        acceptType = "${%s!\"%s\"}".formatted(openApiToVariableName(importContext.acceptOption), acceptType);
      }
      headers.put("Accept", List.of(acceptType));
    }

    return headers;
  }

  static String createBaseUri(OperationCoordinate coordinate, List<Pair<Parameter, ParameterConfig>> parameters,
      ImportContext importContext) {
    Map<String, String> nameMap = parameters.stream().
        collect(Collectors.toMap(
            p -> p.first().getName(),
            p -> p.second().variable()));

    String path = coordinate.path();
    for (Map.Entry<String, String> entry : nameMap.entrySet()) {
      path = path.replaceAll("\\{%s\\}".formatted(entry.getKey()), "\\$\\{%s\\}".formatted(entry.getValue()));
    }
    return importContext.baseUri + path;
  }

  static BodyConfig createBody(Spec spec, OperationCoordinate coordinate) {
    Operation operation = spec.operationOf(coordinate).orElseThrow();
    if (operation.getRequestBody() == null) {
      return null;
    }

    return new BodyConfig(null, BODY_VAR);
  }

  static List<Pair<Parameter, ParameterConfig>> createParameterConfigs(Spec spec, OperationCoordinate coordinate,
      ImportContext importContext) {
    return Stream.of(
            createAcceptHeaderParameter(spec, coordinate, importContext).stream()
                .map(p -> new Pair<>(p, createParameterConfig(p))),
            createContentTypeHeaderParameter(spec, coordinate, importContext).stream()
                .map(p -> new Pair<>(p, createParameterConfig(p))),
            spec.parametersOf(coordinate).stream()
                .map(p -> new Pair<>(p, createParameterConfig(p))),
            createBodyParameterPair(spec, coordinate).stream())
        .flatMap(Function.identity())
        .toList();
  }

  static Optional<Pair<Parameter, ParameterConfig>> createBodyParameterPair(Spec spec, OperationCoordinate coordinate) {
    Operation operation = spec.operationOf(coordinate).orElseThrow();
    if (operation.getRequestBody() == null) {
      return Optional.empty();
    }
    Parameter parameter = new Parameter();
    parameter.setRequired(Boolean.TRUE.equals(operation.getRequestBody().getRequired()));
    parameter.setName(BODY_VAR);
    parameter.setDescription(
        operation.getRequestBody().getDescription() == null ? "No description." : operation.getRequestBody().getDescription());
    ParameterConfig config = new ParameterConfig(
        Boolean.TRUE.equals(parameter.getRequired()) ? Style.RequiredPositional : Style.OptionalPositional,
        openApiToArgumentName(parameter.getName()),
        openApiToVariableName(parameter.getName()),
        null);
    return Optional.of(new Pair<>(parameter, config));
  }

  static Optional<Parameter> createContentTypeHeaderParameter(Spec spec, OperationCoordinate coordinate, ImportContext importContext) {
    if (importContext.contentTypeOption == null) {
      return Optional.empty();
    }
    List<String> mediaTypes = spec.requestBodyMediaTypesOf(coordinate);
    if (mediaTypes.size() < 2) {
      return Optional.empty();
    }

    Parameter parameter = new Parameter();
    parameter.setName(importContext.contentTypeOption);
    parameter.setIn("header");
    parameter.setRequired(false);
    parameter.setDescription("""
        Allows to specify an alternative `Content-Type` header for the request body. If omitted, the `Content-Type` defaults to `%s`.
        This request supports the following content types: %s.""".formatted(
        mediaTypes.get(0),
        mediaTypes.stream().collect(Collectors.joining("`, `", "`", "`"))
    ));
    return Optional.of(parameter);
  }

  static Optional<Parameter> createAcceptHeaderParameter(Spec spec, OperationCoordinate coordinate, ImportContext importContext) {
    if (importContext.acceptOption == null) {
      return Optional.empty();
    }
    List<String> mediaTypes = spec.responseMediaTypesOf(coordinate);
    if (mediaTypes.size() < 2) {
      return Optional.empty();
    }

    Parameter parameter = new Parameter();
    parameter.setName(importContext.acceptOption);
    parameter.setIn("header");
    parameter.setRequired(false);
    parameter.setDescription("""
        Allows to specify an alternative `Accept` header for the response. If omitted, the `Content-Type` defaults to `%s`.
        This request supports the following content types: %s.""".formatted(
        mediaTypes.get(0),
        mediaTypes.stream().collect(Collectors.joining("`, `", "`", "`"))
    ));
    return Optional.of(parameter);
  }

  static String createParameterDescription(Spec spec, List<Pair<Parameter, ParameterConfig>> parameters) {
    StringBuilder buffer = new StringBuilder();
    String ls = System.lineSeparator();
    parameters.stream()
        .sorted(Comparator.comparing(p -> p.second().style().positional ? 0 : 1))
        .forEach(p -> {
          p.second().toParameter().appendUsageString(buffer);
          buffer.append(ls)
              .append(ls)
              .append(">").append(spec.descriptionOfParameter(p.first(), "No documentation."))
              .append(ls)
              .append(ls);
        });
    return buffer.toString();
  }

  static ParameterConfig createParameterConfig(Parameter parameter) {
    if (Boolean.TRUE.equals(parameter.getRequired())) {
      return new ParameterConfig(
          Style.RequiredPositional,
          openApiToArgumentName(parameter.getName()),
          openApiToVariableName(parameter.getName()),
          null);
    } else {
      return new ParameterConfig(
          Style.SingleOption,
          openApiToArgumentName(parameter.getName()),
          openApiToVariableName(parameter.getName()),
          List.of(openApiToOptionName(parameter.getName())));
    }
  }

  static String commandNameOf(Spec spec, OperationCoordinate coordinate) {
    return openApiToCommandName(spec.operationOf(coordinate).map(Operation::getOperationId).orElseThrow());
  }

  static String openApiToCommandName(String openApiName) {
    return openApiName;
  }

  static String openApiToArgumentName(String openApiName) {
    return "<" + stripLeadingDashes(kebabify(openApiName)) + ">";
  }

  static String openApiToVariableName(String openApiName) {
    return snakify(openApiName);
  }

  static String openApiToOptionName(String openApiName) {
    return (openApiName.length() == 1 ? "-" : "--") + stripLeadingDashes(kebabify(openApiName));
  }

  static String stripLeadingDashes(String str) {
    return str.replaceAll("^-+", "");
  }

  static String snakify(String str) {
    return kebabify(str).replaceAll("-", "_");
  }

  static String kebabify(String str) {
    StringBuilder buffer = new StringBuilder();
    int len = str.length();
    boolean previousWasUppderCase = false;
    for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      if (Character.isUpperCase(c)) {
        if (!previousWasUppderCase && i > 0) {
          buffer.append('-');
        }
        previousWasUppderCase = true;
        c = Character.toLowerCase(c);
      } else {
        previousWasUppderCase = false;
        c = c == '_' ? '-' : c;
      }
      buffer.append(c);
    }
    return buffer.toString();
  }

  static String extractSynopsis(String description) {
    if (description == null || description.isBlank()) {
      return "No synopsis.";
    }
    int pos = description.indexOf('.');
    if (pos != -1) {
      return description.substring(0, pos + 1).replaceAll(System.lineSeparator(), " ");
    }
    pos = description.indexOf(System.lineSeparator());
    if (pos != -1) {
      return description.substring(0, pos) + ".";
    }
    return description + ".";
  }

  static class ImportContext {

    CommandAddress address;
    String contentTypeOption;
    String acceptOption;
    String baseUri;
    boolean replace;
    boolean force;
    List<String> operationIds;
    List<String> tags;
    boolean includeDeprecated;
    boolean grouped;
  }
}
