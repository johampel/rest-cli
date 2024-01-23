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
package de.hipphampel.restcli.command.builtin.env;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.cli.format.GridBlock;
import de.hipphampel.restcli.cli.format.GridBlock.Position;
import de.hipphampel.restcli.cli.format.ParagraphBlock;
import de.hipphampel.restcli.cli.format.PreformattedBlock;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class EnvGetCommand extends EnvCommandBase {

  static final String NAME = "get";

  static final Positional CMD_ARG_NAME = positional("<name>")
      .build();
  static final Option CMD_OPT_BEAUTIFIED = option("-b", "--beautify")
      .build();
  static final Option CMD_OPT_COMMON = option("-c", "--common")
      .build();
  static final Option CMD_OPT_ORIGIN = option("-o", "--origin")
      .build();
  static final Option CMD_OPT_LOCAL = option("-l", "--local")
      .build();
  static final Option CMD_OPT_VARIABLES = option("-v", "--variables")
      .build();
  static final Option CMD_OPT_HEADERS = option("-h", "--headers")
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Gets information about a single environment. In order to obtain the information in a more machine readable way, use the `exp`
      command instead. This command is intended for human usage to investigate which settings are present and where they come from (esp.
      when it comes to inheritance of environments).
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection(""" 
      <name>
            
      >The name of the environment to show.
            
      -b | --beautify
            
      >If set, produce beatified output with tabular print out of all settings. If omitted, the output is a little bit less verbose.
                        
      -c | --common
            
      >Show the common settings of the environment, such as the parent reference or the the request timeout. If none of the options 
      `--headers`, `--common`, or `--variables` is set, this information is shown as well; otherwise, if only one of this group but no 
      `--common` is set, this information is _not_ shown.
           
      -h | --headers
            
      >Show the headers defined by this environment. If none of the options `--headers`, `--common`, or `--variables` is set, the headers
      are shown as well; otherwise, if only one of this group but no `--headers` is set, the headers are _not_ shown.

      -l | --local
            
      >If set, the command output only the settings made in the environment itself, but it does not output those defined in the parent
      environment (if any). If this option is omitted, also the inherited values are shown.
            
      -o | --origin
            
      >If present, print for each variable and header value, in which environment the corresponding value is defined. This option is useful
      in case that the environment to show is inherited from an other one and therefore the values might be defined not only in the
      environment directly.
           
      -v | --variables
            
      >Show the variables defined by this environment. If none of the options `--headers`, `--common`, or `--variables` is set, the variables
      are shown as well; otherwise, if only one of this group but no `--variables` is set, the variables are _not_ shown.
      """);

  public EnvGetCommand() {
    super(
        NAME,
        "Gets the settings of an environment.",
        new CommandLineSpec(true, CMD_OPT_ORIGIN, CMD_OPT_LOCAL, CMD_OPT_BEAUTIFIED, CMD_OPT_COMMON, CMD_OPT_VARIABLES, CMD_OPT_HEADERS,
            CMD_ARG_NAME),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));

  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String name = commandLine.getValue(CMD_ARG_NAME).orElseThrow();
    boolean local = commandLine.hasOption(CMD_OPT_LOCAL);
    boolean origin = commandLine.hasOption(CMD_OPT_ORIGIN);
    boolean beautify = commandLine.hasOption(CMD_OPT_BEAUTIFIED);
    boolean common = commandLine.hasOption(CMD_OPT_COMMON);
    boolean variables = commandLine.hasOption(CMD_OPT_VARIABLES);
    boolean headers = commandLine.hasOption(CMD_OPT_HEADERS);
    if (!common && !variables && !headers) {
      common = true;
      variables = true;
      headers = true;
    }
    Environment environment = environmentRepository.getEnvironment(context.configPath(), name)
        .orElseThrow(() -> new ExecutionException("Environment \"%s\" does not exist.".formatted(name)));

    GridBlock table = buildTable(
        context,
        environment,
        local, origin, beautify,
        common,
        variables,
        headers);
    context.out().block(table);

    return true;
  }

  GridBlock buildTable(CommandContext context, Environment environment, boolean local, boolean origin, boolean beautify, boolean common,
      boolean variables, boolean headers) {

    Map<Position, Block> cells = new HashMap<>();
    int row = 0;

    if (common) {
      cells.put(new Position(row, 0), new ParagraphBlock("Common:"));
      cells.put(new Position(row++, 1), buildCommonSettingsTable(context, environment, beautify));
    }

    if (headers) {
      cells.put(new Position(row, 0), new ParagraphBlock("Headers:"));
      cells.put(new Position(row++, 1), buildHeadersTable(context, environment, local, origin, beautify));
    }

    if (variables) {
      cells.put(new Position(row, 0), new ParagraphBlock("Variables:"));
      cells.put(new Position(row, 1), buildVariablesTable(context, environment, local, origin, beautify));

    }

    return new GridBlock(cells).toTable(false);
  }

  Block buildCommonSettingsTable(CommandContext context, Environment environment, boolean beautify) {
    Map<Position, Block> cells = new HashMap<>();
    int row = 0;

    if (beautify) {
      cells.put(new Position(row, 0), new ParagraphBlock("Setting"));
      cells.put(new Position(row, 1), new ParagraphBlock("Value"));
      row++;
    }

    cells.put(new Position(row, 0), new ParagraphBlock("Parent" + (beautify ? "" : ":")));
    cells.put(new Position(row, 1), new ParagraphBlock(environment.getParent() == null ? "" : environment.getParent()));
    row++;

    cells.put(new Position(row, 0), new ParagraphBlock("Request timeout" + (beautify ? "" : ":")));
    cells.put(new Position(row, 1),
        new ParagraphBlock(environment.getRequestTimeout() == null ? "" : environment.getRequestTimeout() + ""));

    return new GridBlock(cells).toTable(beautify);
  }

  Block buildHeadersTable(CommandContext context, Environment environment, boolean local, boolean origin, boolean beautify) {
    List<String> headers = (local ? environment.getLocalHeaders().keySet() : environment.getHeaders().keySet()).stream()
        .sorted()
        .toList();
    if (headers.isEmpty()) {
      return new ParagraphBlock("--- No headers available ---");
    }

    Map<Position, Block> cells = new HashMap<>();
    int row = 0;

    if (beautify) {
      int column = 0;
      cells.put(new Position(row, column++), new ParagraphBlock("Header"));
      if (origin) {
        cells.put(new Position(row, column++), new ParagraphBlock("Origin"));
      }
      cells.put(new Position(row, column), new ParagraphBlock("Value"));
      row++;
    }

    for (String header : headers) {
      cells.put(new Position(row, 0), new ParagraphBlock(beautify ? header : header + ":"));
      row = builderHeaderRows(context, environment, cells, row, header, local, origin, beautify);
    }

    return new GridBlock(cells).toTable(beautify);
  }

  int builderHeaderRows(CommandContext context, Environment environment, Map<Position, Block> cells, int row, String header, boolean local,
      boolean origin, boolean beautify) {
    while (environment != null) {
      List<String> values = environment.getLocalHeader(header);
      if (values != null) {
        for (String value : values) {
          int column = 1;
          if (origin) {
            cells.put(new Position(row, column++), originParagraphBlock(environment.getName(), beautify));
          }
          cells.put(new Position(row, column), new PreformattedBlock(value));
          row++;
        }
      }
      if (!local) {
        environment = Optional.ofNullable(environment.getParent())
            .map(parent -> environmentRepository.getEnvironment(context.configPath(), parent)
                .orElseThrow(() -> new ExecutionException("Internal error: cannot find environment \"%s\"".formatted(parent))))
            .orElse(null);
      } else {
        environment = null;
      }
    }
    return row;
  }

  Block buildVariablesTable(CommandContext context, Environment environment, boolean local, boolean origin, boolean beautify) {
    List<String> variables = (local ? environment.getLocalVariables().keySet() : environment.getVariables().keySet()).stream()
        .sorted()
        .toList();
    if (variables.isEmpty()) {
      return new ParagraphBlock("--- No variables available ---");
    }
    Map<Position, Block> cells = new HashMap<>();
    int row = 0;

    if (beautify) {
      int column = 0;
      cells.put(new Position(row, column++), new ParagraphBlock("Variable"));
      if (origin) {
        cells.put(new Position(row, column++), new ParagraphBlock("Origin"));
      }
      cells.put(new Position(row, column), new ParagraphBlock("Value"));
      row++;
    }

    for (String variable : variables) {
      int column = 0;
      cells.put(new Position(row, column++),
          new ParagraphBlock(beautify ? variable : variable + ":"));
      if (origin) {
        cells.put(new Position(row, column++),
            originParagraphBlock(determineVariableOrigin(context, environment, variable).orElse("???"), beautify));
      }
      cells.put(new Position(row, column),
          new PreformattedBlock(String.valueOf(environment.getVariables().get(variable))));
      row++;
    }

    return new GridBlock(cells).toTable(beautify);
  }

  Optional<String> determineVariableOrigin(CommandContext context, Environment environment, String variable) {
    while (environment != null) {
      if (environment.getLocalVariables().containsKey(variable)) {
        return Optional.of(environment.getName());
      }
      environment = Optional.ofNullable(environment.getParent())
          .map(parent -> environmentRepository.getEnvironment(context.configPath(), parent)
              .orElseThrow(() -> new ExecutionException("Internal error: cannot find environment \"%s\"".formatted(parent))))
          .orElse(null);
    }
    return Optional.empty();
  }

  ParagraphBlock originParagraphBlock(String origin, boolean beautify) {
    if (beautify) {
      return new ParagraphBlock(origin);
    } else {
      return new ParagraphBlock("(from " + origin + ")");
    }
  }
}
