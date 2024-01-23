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

import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.cli.format.FillerBlock;
import de.hipphampel.restcli.cli.format.GridBlock;
import de.hipphampel.restcli.cli.format.GridBlock.Position;
import de.hipphampel.restcli.cli.format.Inline;
import de.hipphampel.restcli.cli.format.InlineSequence;
import de.hipphampel.restcli.cli.format.ParagraphBlock;
import de.hipphampel.restcli.cli.format.ParagraphBlock.Alignment;
import de.hipphampel.restcli.cli.format.PreformattedBlock;
import de.hipphampel.restcli.cli.format.SequenceBlock;
import de.hipphampel.restcli.cli.format.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Command {

  CommandAddress address();

  String synopsis();

  default String name() {
    return address().name();
  }

  CommandLineSpec commandLineSpec();

  boolean execute(CommandContext context, CommandLine commandLine);

  default void showHelp(CommandContext context, Output output) {
    List<Block> blocks = new ArrayList<>();

    blocks.add(new ParagraphBlock(Alignment.LEFT, new InlineSequence(true)
        .addInline(new Inline(name(), Style.BOLD))
        .addInlinesForString(" - " + synopsis(), Style.NORMAL)
    ));

    for (HelpSection section : HelpSection.values()) {
      helpSection(context, section)
          .ifPresent(content -> {
            blocks.add(new PreformattedBlock(""));
            blocks.add(new ParagraphBlock(Alignment.LEFT, new InlineSequence(true)
                .addInlinesForString(section.getName(), Style.BOLD)));
            blocks.add(new GridBlock(Map.of(
                new Position(0, 0), new PreformattedBlock("  "),
                new Position(0, 1), content
            )));
          });
    }

    output.block(new SequenceBlock(blocks));
  }

  default Optional<Block> helpSection(CommandContext context, HelpSection section) {
    return switch (section) {
      case DESCRIPTION -> Optional.of(new ParagraphBlock("No help available."));
      case USAGE -> Optional.of(new GridBlock(Map.of(
          new Position(0, 0), new ParagraphBlock(name()),
          new Position(0, 1), new FillerBlock(" "),
          new Position(0, 2), new ParagraphBlock(commandLineSpec().usageString()))
      ));
      default -> Optional.empty();
    };
  }
}
