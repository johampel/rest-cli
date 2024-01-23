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

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.command.Command;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandInvoker;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.template.TemplateModel;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
public class HelpCommand extends BuiltinCommand {

  public static final String NAME = "help";
  static final Function<CommandContext, Block> HELP_SECTION_DESCRIPTION = CommandUtils.helpSection("""
      `help` shows help texts for the `${applicationName}` itself, a sub-command, or a general help topic.
      When no argument is specified, the application help is shown, otherwise the help of the specified sub-command or topic.
      """);
  static final Function<CommandContext, Block> HELP_SECTION_ARGS_AND_OPTIONS = CommandUtils.helpSection("""
      <sub-command-spec>
            
      >If given, it shows the help of the specified sub command. For example, if you want to get help for the sub-command `get` of
      the command `cfg`, you have to type:
            
      >>`${applicationName} help cfg get`
            
      > Alternatively you may use the compact command address notation:
            
      >>`${applicationName} help cfg/get`
            
      <topic>
            
      >If you want to get help for a general topic, such as `templates`, you have to prefix the topic with a `:`, like this:
            
      >>`${applicationName} help :templates`
            
      > To get a list of available help topics, type `${applicationName} help :topics`.
      """);

  static final Positional CMD_SUB_COMMAND_OR_TOPIC = positional("<sub-command>")
      .repeatable()
      .optional()
      .build();

  public HelpCommand() {
    super(
        CommandAddress.fromString(NAME),
        "Shows help for a command or general topic.",
        new CommandLineSpec("[<sub-command-spec>|:<topic>]", true, CMD_SUB_COMMAND_OR_TOPIC),
        Map.of(HelpSection.DESCRIPTION, HELP_SECTION_DESCRIPTION,
            HelpSection.ARGS_AND_OPTIONS, HELP_SECTION_ARGS_AND_OPTIONS));
  }

  @Override
  public boolean execute(CommandContext context, CommandLine commandLine) {
    List<String> spec = commandLine.getValues(CMD_SUB_COMMAND_OR_TOPIC);
    if (spec.isEmpty()) {
      showApplicationHelp(context);
    } else if (spec.get(0).startsWith(":")) {
      showTopicHelp(context, spec);
    } else {
      showCommandHelp(context, spec);
    }
    return true;
  }

  void showApplicationHelp(CommandContext context) {
    showCommandHelp(context, List.of());
  }

  void showCommandHelp(CommandContext context, List<String> spec) {
    CommandAddress address = CommandAddress.ROOT;
    for (String child : spec) {
      if (!CommandAddress.isValidCommandName(child)) {
        break;
      }
      address = address.child(child);
    }
    CommandInvoker invoker = context.commandInvoker();

    Pair<CommandAddress, Optional<String>> longest = invoker.getLongestExistingCommandAddress(context, address);
    longest.second().ifPresent(child ->
        CommandUtils.showWarning(context, "Command \"%s\" has no sub-command \"%s\".",
            CommandUtils.getQualifiedCommandName(context, longest.first()),
            child));

    Command command = invoker.getCommand(context, longest.first()).orElseThrow();
    command.showHelp(context, context.out());
  }

  void showTopicHelp(CommandContext context, List<String> spec) {
    if (spec.size() > 1) {
      CommandUtils.showWarning(context, "More than one parameter was given - ignoring the rest.");
    }

    String topic = spec.get(0).substring(1);
    URL topicResource = getTopicUrl(topic);
    if (topicResource == null) {
      CommandUtils.showError(context, "No such topic \"%s\" - showing available topics instead.".formatted(topic));
      topicResource = getTopicUrl("topics");
    }

    String topicContent = context.templateRenderer()
        .render(InputStreamProvider.ofURL(topicResource), CommandUtils.createHelpTextTemplateModel(context));

    context.out().markdown(topicContent);
  }


  URL getTopicUrl(String topic) {
    return HelpCommand.class.getResource("/topics/%s.md".formatted(topic));
  }
}
