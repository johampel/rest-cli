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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.builtin.cmd.CmdOpenApiCommand.ImportContext;
import de.hipphampel.restcli.command.builtin.cmd.openapi.Spec;
import de.hipphampel.restcli.command.builtin.cmd.openapi.Spec.OperationCoordinate;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.CommandConfigTree;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterConfig.Style;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.test.junit.QuarkusTest;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class CmdOpenApiCommandTest extends CommandTestBase {

  @Inject
  CmdOpenApiCommand command;

  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr, String expectedAddress,
      String expectedContent) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
    if (expectedAddress != null) {
      CommandConfigTree actual = command.getCommandConfigTree(context, CommandAddress.fromString(expectedAddress), false);
//      System.err.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual));
      CommandConfigTree expected = objectMapper.readValue(expectedContent, CommandConfigTree.class);
      assertThat(actual).isEqualTo(expected);
    }
  }


  static Stream<Arguments> execute_data() throws IOException {
    String spec = TestUtils.getResourceContent("/testdata/openapi/api.yml");
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            false,
            "",
            """
                *** error test-app: Missing required argument "<address>".
                usage: test-app cmd openapi [-b|--base-uri <base-uri>]
                                            [-a|--accept-option <option-name>]
                                            [-c|--content-type-option
                                            <option-name>]
                                            [-d|--include-deprecated-operations]
                                            [-g|--group-by-tag] [-t|--tag
                                            <tag>]... [-o|--operation
                                            <operation>]... [-r|--replace
                                            [-f|--force]] <address> [<spec>]
                """,
            null,
            null),
        // Simple input
        Arguments.of(
            List.of("foo", spec),
            true,
            "",
            "",
            "foo",
            """
                {
                  "config" : {
                    "type" : "Parent",
                    "synopsis" : "TEst API.",
                    "descriptions" : {
                      "DESCRIPTION" : "A sample API, provides some commands.\\n"
                    },
                    "restConfig" : null,
                    "aliasConfig" : null
                  },
                  "subCommands" : {
                    "opB" : {
                      "config" : {
                        "type" : "Http",
                        "synopsis" : "Do operation B.",
                        "descriptions" : {
                          "ARGS_AND_OPTIONS" : "<abc>\\n\\n>Description ABC\\n\\n[--def <def>]\\n\\n>Description DEF\\n\\n[--ghi <ghi>]\\n\\n>Description GHI\\n\\n",
                          "DESCRIPTION" : "Do operation B"
                        },
                        "restConfig" : {
                          "method" : "GET",
                          "baseUri" : "${baseUri}/path1/${abc}",
                          "queryParameters" : {
                            "def" : "${def}"
                          },
                          "headers" : {
                            "Accept" : [ "application/json" ],
                            "ghi" : [ "${ghi}" ]
                          },
                          "parameters" : {
                            "parameters" : [ {
                              "style" : "RequiredPositional",
                              "name" : "<abc>",
                              "variable" : "abc",
                              "optionNames" : null
                            }, {
                              "style" : "SingleOption",
                              "name" : "<def>",
                              "variable" : "def",
                              "optionNames" : [ "--def" ]
                            }, {
                              "style" : "SingleOption",
                              "name" : "<ghi>",
                              "variable" : "ghi",
                              "optionNames" : [ "--ghi" ]
                            } ]
                          },
                          "body" : null
                        },
                        "aliasConfig" : null
                      },
                      "subCommands" : { }
                    },
                    "opA" : {
                      "config" : {
                        "type" : "Http",
                        "synopsis" : "Do operation A.",
                        "descriptions" : {
                          "ARGS_AND_OPTIONS" : "<abc>\\n\\n>Description ABC\\n\\n<body>\\n\\n>Body A\\n\\n",
                          "DESCRIPTION" : "Do operation A"
                        },
                        "restConfig" : {
                          "method" : "POST",
                          "baseUri" : "${baseUri}/path1/${abc}",
                          "queryParameters" : { },
                          "headers" : {
                            "Accept" : [ "application/json" ],
                            "Content-Type" : [ "application/json" ]
                          },
                          "parameters" : {
                            "parameters" : [ {
                              "style" : "RequiredPositional",
                              "name" : "<abc>",
                              "variable" : "abc",
                              "optionNames" : null
                            }, {
                              "style" : "RequiredPositional",
                              "name" : "<body>",
                              "variable" : "_body",
                              "optionNames" : null
                            } ]
                          },
                          "body" : {
                            "content" : null,
                            "variable" : "_body"
                          }
                        },
                        "aliasConfig" : null
                      },
                      "subCommands" : { }
                    }
                  }
                }""")
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            openapi - Imports commands from an OpenAPI spec.

            Usage
              openapi [-b|--base-uri <base-uri>] [-a|--accept-option
                      <option-name>] [-c|--content-type-option
                      <option-name>]
                      [-d|--include-deprecated-operations]
                      [-g|--group-by-tag] [-t|--tag <tag>]...
                      [-o|--operation <operation>]... [-r|--replace
                      [-f|--force]] <address> [<spec>]

            Description
              Reads an OpenAPI specification and creates according
              commands for it.
              There are a couple of options that allow to filter the
              commands you want to import and to influence the exact
              shape of the commands. You may modify the commands via the
              `mod` command after the import.

            Arguments and options
              <address>
                  The address of the command to import to. In most
                  cases, for this address a group command will be
                  created that contains the different commands extracted
                  from the OpenAPI as sub commands. In the special case
                  that you import just a single command by specifying
                  just a single `<operation>`, the `<address>` will
                  point to this single operation.
              <source>
                  The source where to read the OpenAPI specification
                  from. If reading from a file, prefix the file name
                  with an `@` sign: this is specified as an input source
                  (see section "Further Infos").
              -a | --accept-option <option-name>
                  If specified, each generated HTTP command supporting
                  more than one media type for its responses will have
                  the option `<option-name>` in addition to the options
                  imported from the OpenAPI specification. This allows
                  to explicitly specify the `Accept` header for the
                  request. You have to specify the option name without
                  any leading dash.
                  If you do not specify such an option and the HTTP
                  supports more than one content type, it uses the first
                  media type found in the OpenAPI specification as the
                  predefined one.
              -b | --base-uri <uri>
                  The base uri to prefix each imported path. So if the
                  OpenAPI specification contains the path `/some/path`,
                  it is prefixed by the value configured by this option.
                  If this option is omitted, the default prefix is
                  `${baseUri}`.
              -c | --content-type-option <option-name>
                  If specified, each generated HTTP command supporting
                  more than one content type for its request body will
                  have the option `<option-name>` in addition to the
                  options imported from the OpenAPI specification. This
                  allows to explicitly specify the `Content-Type` header
                  for the request. You have to specify the option name
                  without any leading dash.
                  If you do not specify such an option and the HTTP
                  supports more than one content type, it uses the first
                  content type found in the OpenAPI specification as the
                  predefined one.
              -d | --include-deprecated-operations
                  If specified, it imports also operations marked as
                  deprecated. By default, these are skipped.
              -g | --group-by-tag
                  When specified, the commands are grouped by tags. In
                  other words, for each tag, a further group command
                  representing the tag is created (right under the
                  command specified by `<address>`), and all commands
                  belonging to this tag become children of this command.
                  Without this option, no extra commands for the tags
                  are created.
              -o | --operation <operation>
                  If you wish to import individual operations instead of
                  all operations found in the OpenAPI specification, you
                  may specify these operations you are interested in.
                  The `<operation>` argument can be specified more than
                  once. Please note that - even if explicitly specified
                  - an operation might be filtered out due to other
                  options, such as `--tag`.
              -r | --replace
                  If there is already a command with the given
                  `<address>` you have to provide this option in case
                  you really want to replace it. In case of a
                  replacement, as a further cross check, the following
                  option is present:
                  -f | --force
                      This option is required in case that the command
                      to replace has sub-commands. Note that when
                      replacing a group command, all its sub commands
                      are implicitly removed.
              -t | --tag <tag>
                  If specified, import only those commands belonging to
                  the specified `<tag>`. You may specify this option
                  more than once.

            Further infos
              Command addresses:
              A command address uniquely identifies a command in
              test-app. It is basically a path the command, so `foo/bar`
              refers to the command `bar` that is a child command of
              `foo`. The special empty path (``) refers to the root
              command that represents the application itself. For
              details, please type `test-app help :addresses`.
                 \s
              Input source:
              An input source is basically a string literal, which
              represents either a string, refers to a file, an URL, or a
              builtin resource. Detailed information can be obtained by
              typing `test-app help :input-sources`.
            """,
        "");
  }

  @Test
  void createHeaders_withOptions() {
    ImportContext importContext = new ImportContext();
    importContext.acceptOption = "accept";
    importContext.contentTypeOption = "content";
    Spec spec = openApiSpec(Map.of(
        "/path", openApiPathItem(Map.of(
            HttpMethod.POST, openApiOperation("op",
                List.of(
                    openApiParameter("theFoo", "header", true, "foo docu"),
                    openApiParameter("barBar", "query", false, "bar docu")),
                openApiRequestBody(List.of("in/json", "in/xml")),
                openApiResponses(List.of("out/xml", "out/json")))
        ))
    ));
    List<Pair<Parameter, ParameterConfig>> params = CmdOpenApiCommand.createParameterConfigs(spec,
        new OperationCoordinate("/path", HttpMethod.POST), importContext);

    assertThat(CmdOpenApiCommand.createHeaders(spec, new OperationCoordinate("/path", HttpMethod.POST), params, importContext))
        .isEqualTo(Map.of(
            "Accept", List.of("${accept!\"out/xml\"}"),
            "Content-Type", List.of("${content!\"in/json\"}"),
            "theFoo", List.of("${the_foo}")
        ));
  }

  @Test
  void createHeaders_withoutOptions() {
    ImportContext importContext = new ImportContext();
    Spec spec = openApiSpec(Map.of(
        "/path", openApiPathItem(Map.of(
            HttpMethod.POST, openApiOperation("op",
                List.of(
                    openApiParameter("theFoo", "header", true, "foo docu"),
                    openApiParameter("barBar", "query", false, "bar docu")),
                openApiRequestBody(List.of("in/json", "in/xml")),
                openApiResponses(List.of("out/xml", "out/json")))
        ))
    ));
    List<Pair<Parameter, ParameterConfig>> params = CmdOpenApiCommand.createParameterConfigs(spec,
        new OperationCoordinate("/path", HttpMethod.POST), importContext);

    assertThat(CmdOpenApiCommand.createHeaders(spec, new OperationCoordinate("/path", HttpMethod.POST), params, importContext))
        .isEqualTo(Map.of(
            "Accept", List.of("out/xml"),
            "Content-Type", List.of("in/json"),
            "theFoo", List.of("${the_foo}")
        ));
  }

  @Test
  void createBaseUri() {
    ImportContext importContext = new ImportContext();
    importContext.acceptOption = "accept";
    importContext.contentTypeOption = "content";
    importContext.baseUri = "${barBar}";
    Spec spec = openApiSpec(Map.of(
        "/path/{theFoo}", openApiPathItem(Map.of(
            HttpMethod.POST, openApiOperation("op",
                List.of(
                    openApiParameter("theFoo", "path", true, "foo docu"),
                    openApiParameter("barBar", "query", false, "bar docu")),
                openApiRequestBody(List.of("in/json", "in/xml")),
                openApiResponses(List.of("out/xml", "out/json")))
        ))
    ));
    List<Pair<Parameter, ParameterConfig>> params = CmdOpenApiCommand.createParameterConfigs(spec,
        new OperationCoordinate("/path/{theFoo}", HttpMethod.POST), importContext);

    assertThat(CmdOpenApiCommand.createBaseUri(new OperationCoordinate("/path/{theFoo}", HttpMethod.POST), params, importContext))
        .isEqualTo("${barBar}/path/${the_foo}");
  }

  @Test
  void createBody_withBody() {
    Spec spec = openApiSpec(Map.of(
        "/path", openApiPathItem(Map.of(
            HttpMethod.POST, openApiOperation("op",
                null,
                openApiRequestBody(List.of("in/json", "in/xml")),
                null)))
    ));

    assertThat(CmdOpenApiCommand.createBody(spec, new OperationCoordinate("/path", HttpMethod.POST))).isEqualTo(
        new BodyConfig(null, "_body"));
  }

  @Test
  void createBody_noBody() {
    Spec spec = openApiSpec(Map.of(
        "/path", openApiPathItem(Map.of(
            HttpMethod.POST, openApiOperation("op",
                null,
                null,
                null)))
    ));

    assertThat(CmdOpenApiCommand.createBody(spec, new OperationCoordinate("/path", HttpMethod.POST))).isNull();
  }

  @Test
  void createParameterConfigs() {
    ImportContext importContext = new ImportContext();
    importContext.acceptOption = "accept";
    importContext.contentTypeOption = "content";
    Spec spec = openApiSpec(Map.of(
        "/path", openApiPathItem(Map.of(
            HttpMethod.POST, openApiOperation("op",
                List.of(
                    openApiParameter("foo", "path", true, "foo docu"),
                    openApiParameter("bar", "path", false, "bar docu")),
                openApiRequestBody(List.of("in/json", "in/xml")),
                openApiResponses(List.of("out/xml", "out/json")))
        ))
    ));

    List<Pair<Parameter, ParameterConfig>> result = CmdOpenApiCommand.createParameterConfigs(spec,
        new OperationCoordinate("/path", HttpMethod.POST), importContext);

    assertThat(result).hasSize(5);
    assertThat(result.stream().map(Pair::second)).containsExactly(
        new ParameterConfig(Style.SingleOption, "<accept>", "accept", List.of("--accept")),
        new ParameterConfig(Style.SingleOption, "<content>", "content", List.of("--content")),
        new ParameterConfig(Style.RequiredPositional, "<foo>", "foo", null),
        new ParameterConfig(Style.SingleOption, "<bar>", "bar", List.of("--bar")),
        new ParameterConfig(Style.OptionalPositional, "<body>", "_body", null)
    );
  }

  @Test
  void createBodyParameterPair_withBody() {
    Spec spec = openApiSpec(Map.of("/path", openApiPathItem(Map.of(
        HttpMethod.POST, openApiOperation("foo", null, openApiRequestBody(List.of("type")), null)))));

    assertThat(CmdOpenApiCommand.createBodyParameterPair(spec, new OperationCoordinate("/path", HttpMethod.POST))).isPresent();
  }

  @Test
  void createBodyParameterPair_noBody() {
    Spec spec = openApiSpec(Map.of("/path", openApiPathItem(Map.of(
        HttpMethod.POST, openApiOperation("foo", null, null, null)))));

    assertThat(CmdOpenApiCommand.createBodyParameterPair(spec, new OperationCoordinate("/path", HttpMethod.POST))).isEmpty();


  }

  @ParameterizedTest
  @CsvSource({
      "abc, 'app/json;app/xml', 'Allows to specify an alternative `Content-Type` header for the request body. If omitted, the `Content-Type` defaults to `app/json`.\nThis request supports the following content types: `app/json`, `app/xml`.'",
      "abc, 'app/json',         ",
      ",    'app/json;app/xml', "
  })
  void createContentTypeHeaderParameter(String optionName, String contentTypes, String expectedDescription) {
    ImportContext importContext = new ImportContext();
    importContext.contentTypeOption = optionName;
    Spec spec = openApiSpec(Map.of("/path", openApiPathItem(Map.of(
        HttpMethod.POST, openApiOperation("foo", null, openApiRequestBody(TestUtils.stringToList(contentTypes)), null)))));
    Parameter parameter = CmdOpenApiCommand.createContentTypeHeaderParameter(spec, new OperationCoordinate("/path", HttpMethod.POST),
        importContext).orElse(null);

    if (expectedDescription != null) {
      assertThat(parameter).isNotNull();
      assertThat(parameter.getName()).isEqualTo(optionName);
      assertThat(parameter.getRequired()).isFalse();
      assertThat(parameter.getIn()).isEqualTo("header");
      assertThat(parameter.getDescription()).isEqualTo(expectedDescription);
    } else {
      assertThat(parameter).isNull();
    }
  }

  @ParameterizedTest
  @CsvSource({
      "abc, 'app/json;app/xml', 'Allows to specify an alternative `Accept` header for the response. If omitted, the `Content-Type` defaults to `app/json`.\nThis request supports the following content types: `app/json`, `app/xml`.'",
      "abc, 'app/json',         ",
      ",    'app/json;app/xml', "
  })
  void createAcceptHeaderParameter(String optionName, String contentTypes, String expectedDescription) {
    ImportContext importContext = new ImportContext();
    importContext.acceptOption = optionName;
    Spec spec = openApiSpec(Map.of("/path", openApiPathItem(Map.of(
        HttpMethod.POST, openApiOperation("foo", null, null, openApiResponses(TestUtils.stringToList(contentTypes)))))));
    Parameter parameter = CmdOpenApiCommand.createAcceptHeaderParameter(spec, new OperationCoordinate("/path", HttpMethod.POST),
        importContext).orElse(null);

    if (expectedDescription != null) {
      assertThat(parameter).isNotNull();
      assertThat(parameter.getName()).isEqualTo(optionName);
      assertThat(parameter.getRequired()).isFalse();
      assertThat(parameter.getIn()).isEqualTo("header");
      assertThat(parameter.getDescription()).isEqualTo(expectedDescription);
    } else {
      assertThat(parameter).isNull();
    }
  }

  @Test
  void createParameterDescription() {
    List<Parameter> openApiParameters = List.of(
        openApiParameter("param1", "ignore", false, "Documentation 1"),
        openApiParameter("param2", "ignore", false, null),
        openApiParameter("param3", "ignore", true, "Documentation 3"),
        openApiParameter("param4", "ignore", true, null));
    Spec spec = openApiSpec(Map.of());
    List<Pair<Parameter, ParameterConfig>> input = openApiParameters.stream()
        .map(p -> new Pair<>(p, CmdOpenApiCommand.createParameterConfig(p)))
        .toList();

    assertThat(CmdOpenApiCommand.createParameterDescription(spec, input)).isEqualTo("""
        <param3>
                
        >Documentation 3
                 
        <param4>
                 
        >No documentation.
                 
        [--param1 <param1>]
                 
        >Documentation 1
                 
        [--param2 <param2>]
                 
        >No documentation.
                 
        """);
  }

  @ParameterizedTest
  @CsvSource({
      "abcDef,  true,  RequiredPositional, <abc-def>, abc_def, ",
      "abcDef,  false, SingleOption,       <abc-def>, abc_def, --abc-def",
      "a,       false, SingleOption,       <a>,       a,       -a",
  })
  void createParameterConfig(String name, boolean required, Style expectedStyle, String expectedName, String expectedVariable,
      String expectedOptionName) {
    Parameter openApiParameter = openApiParameter(name, "ignore", required, "ignore");
    ParameterConfig expected = new ParameterConfig(expectedStyle, expectedName, expectedVariable,
        expectedOptionName == null ? null : List.of(expectedOptionName));
    assertThat(CmdOpenApiCommand.createParameterConfig(openApiParameter)).isEqualTo(expected);
  }

  @Test
  void commandNameOf() {
    String id = "theId";
    Spec spec = openApiSpec(Map.of(
        "/path", openApiPathItem(Map.of(
            HttpMethod.GET, openApiOperation(id, null, null, null))
        )
    ));
    assertThat(CmdOpenApiCommand.commandNameOf(spec, new OperationCoordinate("/path", HttpMethod.GET))).isEqualTo(id);
  }

  @ParameterizedTest
  @CsvSource({
      "'a',        '<a>'",
      "'abc',      '<abc>'",
      "'abcDef',   '<abc-def>'",
      "'-abcDef',  '<abc-def>'",
      "'_abcDef',  '<abc-def>'",
      "'abc_def',  '<abc-def>'",
  })
  void openApiToArgumentName(String in, String expected) {
    assertThat(CmdOpenApiCommand.openApiToArgumentName(in)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'a',        'a'",
      "'abc',      'abc'",
      "'abcDef',   'abc_def'",
      "'-abcDef',  '_abc_def'",
      "'_abcDef',  '_abc_def'",
      "'abc_def',  'abc_def'",
  })
  void openApiToVariableName(String in, String expected) {
    assertThat(CmdOpenApiCommand.openApiToVariableName(in)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'a',        '-a'",
      "'abc',      '--abc'",
      "'abcDef',   '--abc-def'",
      "'-abcDef',  '--abc-def'",
      "'_abcDef',  '--abc-def'",
      "'abc_def',  '--abc-def'",
  })
  void openApiToOptionName(String in, String expected) {
    assertThat(CmdOpenApiCommand.openApiToOptionName(in)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'---abc',         'abc'",
      "'---a--b-c',      'a--b-c'",
  })
  void stripLeadingDashes(String in, String expected) {
    assertThat(CmdOpenApiCommand.stripLeadingDashes(in)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'abc',           'abc'",
      "'AbcDef',        'abc_def'",
      "'ABCDef',        'abcdef'",
      "'AbCdEf',        'ab_cd_ef'",
  })
  void snakify(String in, String expected) {
    assertThat(CmdOpenApiCommand.snakify(in)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'abc',           'abc'",
      "'AbcDef',        'abc-def'",
      "'ABCDef',        'abcdef'",
      "'AbCdEf',        'ab-cd-ef'",
  })
  void kebabify(String in, String expected) {
    assertThat(CmdOpenApiCommand.kebabify(in)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'abc',           'abc.'",
      "'abc. def',      'abc.'",
      "'abc def',       'abc def.'",
      "'abc\ndef.',     'abc def.'",
      "'abc\ndef. ghi', 'abc def.'"
  })
  void extractSynopsis(String in, String expected) {
    assertThat(CmdOpenApiCommand.extractSynopsis(in)).isEqualTo(expected);
  }

  static Spec openApiSpec(Map<String, PathItem> pathItems) {
    OpenAPI api = new OpenAPI();
    api.setPaths(new Paths());
    api.getPaths().putAll(pathItems);
    return new Spec(api);
  }

  static PathItem openApiPathItem(Map<HttpMethod, Operation> operations) {
    PathItem pathItem = new PathItem();
    pathItem.setGet(operations.get(HttpMethod.GET));
    pathItem.setPost(operations.get(HttpMethod.POST));
    pathItem.setDelete(operations.get(HttpMethod.DELETE));
    pathItem.setPut(operations.get(HttpMethod.PUT));
    pathItem.setOptions(operations.get(HttpMethod.OPTIONS));
    pathItem.setPatch(operations.get(HttpMethod.PATCH));
    pathItem.setTrace(operations.get(HttpMethod.TRACE));
    pathItem.setHead(operations.get(HttpMethod.HEAD));
    return pathItem;
  }

  static Operation openApiOperation(String id, List<Parameter> parameters, RequestBody requestBody, ApiResponses responses) {
    Operation operation = new Operation();
    operation.setOperationId(id);
    operation.setParameters(parameters);
    operation.setRequestBody(requestBody);
    operation.setResponses(responses);
    return operation;
  }

  static ApiResponses openApiResponses(List<String> contentTypes) {
    Content content = new Content();
    for (String contentType : contentTypes) {
      content.addMediaType(contentType, new MediaType());
    }
    ApiResponse response = new ApiResponse();
    response.setContent(content);
    ApiResponses responses = new ApiResponses();
    responses.put("200", response);
    return responses;
  }

  static RequestBody openApiRequestBody(List<String> contentTypes) {
    Content content = new Content();
    for (String contentType : contentTypes) {
      content.addMediaType(contentType, new MediaType());
    }
    RequestBody requestBody = new RequestBody();
    requestBody.setContent(content);
    return requestBody;
  }

  static Parameter openApiParameter(String name, String in, boolean required, String documentation) {
    Parameter parameter = new Parameter();
    parameter.setName(name);
    parameter.setIn(in);
    parameter.setRequired(required);
    parameter.setDescription(documentation);
    return parameter;
  }
}
