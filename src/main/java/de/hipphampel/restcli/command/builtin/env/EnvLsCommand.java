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
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.env.EnvironmentConfig;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Unremovable
public class EnvLsCommand extends EnvCommandBase {

  static final String NAME = "ls";

  static final Positional CMD_ARG_ROOT = positional("<root>")
      .optional()
      .build();
  static final Option CMD_OPT_BEAUTIFY = option("-b", "--beautify")
      .build();
  static final Option CMD_OPT_TREE = option("-t","--tree")
      .build();

  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      Lists the available environments, either in list or in tree format:
            
      - When `--tree` is given, it shows a tree of the environments starting with the `<root>` environment and all its direct or indirect
        children or, if no `<root>` is given, the trees of really all available environments.
        
      - When `--tree` is omitted it either shows a list of all direct children of the `<root>` environment or, if no `<root>` is given,
        a list of really all environments.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <root>
            
      >The root environment to start with. For details please refer to section "Description" above.
            
      -b | --beautify
            
      >This option has only an effect in combination with the `--tree` option and adds some characters to the output that visualizes the
      tree structure.
            
      -t | --tree
            
      >If set, print a tree visualizing the parent - child relationship of the environments. If omitted, print a list.
      """);

  public EnvLsCommand() {
    super(
        NAME,
        "Lists available environments.",
        new CommandLineSpec(true, CMD_OPT_BEAUTIFY, CMD_OPT_TREE, CMD_ARG_ROOT),
        Map.of(
            HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));

  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    String root = commandLine.getValue(CMD_ARG_ROOT).orElse(null);
    boolean beautify = commandLine.hasOption(CMD_OPT_BEAUTIFY);
    boolean tree = commandLine.hasOption(CMD_OPT_TREE);

    Map<String, EnvironmentConfig> configs = environmentRepository.streamConfigs(context.configPath())
        .collect(Collectors.toMap(Pair::first, Pair::second));

    if (tree) {
      getRoots(configs, root).forEach(env -> {
        context.out().line(env);
        showChildTree(context, configs, env, "", beautify);
      });
    } else if (root == null) {
      configs.keySet().stream()
          .sorted()
          .forEach(env -> context.out().line(env));
    } else {
      getChildrenOf(configs, root)
          .forEach(env -> context.out().line(env));
    }
    return true;
  }

  void showChildTree(CommandContext context, Map<String, EnvironmentConfig> configs, String parent, String prefix, boolean beautify) {
    List<String> children = getChildrenOf(configs, parent)
        .toList();
    for (int i = 0; i < children.size(); i++) {
      String childPrefix;
      String grandChildPrefix;
      if (beautify) {
        if (i < children.size() - 1) {
          childPrefix = prefix + "├── ";
          grandChildPrefix = prefix + "│   ";
        } else {
          childPrefix = prefix + "└── ";
          grandChildPrefix = prefix + "    ";
        }
      } else {
        childPrefix = prefix + "    ";
        grandChildPrefix = childPrefix;
      }

      context.out().line(childPrefix + children.get(i));
      showChildTree(context, configs, children.get(i), grandChildPrefix, beautify);
    }
  }

  List<String> getRoots(Map<String, EnvironmentConfig> configs, String root) {
    if (root == null) {
      return getChildrenOf(configs, null)
          .toList();
    } else if (configs.containsKey(root)) {
      return List.of(root);
    } else {
      throw new ExecutionException("Environment \"%s\" does not exist.".formatted(root));
    }
  }

  Stream<String> getChildrenOf(Map<String, EnvironmentConfig> configs, String parent) {
    if (parent != null && !configs.containsKey(parent)) {
      throw new ExecutionException("Environment \"%s\" does not exist.".formatted(parent));
    }
    return configs.entrySet().stream()
        .filter(entry -> Objects.equals(parent, entry.getValue().parent()))
        .map(Entry::getKey)
        .sorted();
  }
}
