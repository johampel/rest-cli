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
package de.hipphampel.restcli.cli.format;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import de.hipphampel.restcli.cli.format.GridBlock.Position;
import de.hipphampel.restcli.cli.format.ParagraphBlock.Alignment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class FormatBuilder {

  private static final Parser parser = Parser
      .builder(new MutableDataSet().set(Parser.EXTENSIONS, List.of(TablesExtension.create())))
      .build();

  public static Block buildFormat(String document) {
    return buildFormat(parser.parse(document));
  }

  public static Block buildFormat(Document document) {
    List<Block> blocks = new ArrayList<>();
    for (Node node : document.getChildren()) {
      blocks.add(createBlockForNode(node, blocks.isEmpty()));
    }
    return toBlock(blocks);
  }

  static Block createBlockForNode(Node node, boolean isFirstChild) {
    if (node instanceof Paragraph paragraph) {
      return createBlockForParagraph(paragraph);
    } else if (node instanceof BulletList bulletList) {
      return createBlockForBulletList(bulletList);
    } else if (node instanceof OrderedList orderedList) {
      return createBlockForOrderedList(orderedList);
    } else if (node instanceof Heading heading) {
      return createBlockForHeading(heading, isFirstChild);
    } else if (node instanceof BlockQuote blockQuote) {
      return createBlockForBlockQuote(blockQuote);
    } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
      return createBlockForIndentedCodeBlock(indentedCodeBlock);
    } else if (node instanceof FencedCodeBlock fencedCodeBlock) {
      return createBlockForFencedCodeBlock(fencedCodeBlock);
    } else if (node instanceof TableBlock tableBlock) {
      return createBlockForTableBlock(tableBlock);
    } else {
      return createUnimplementedBlock(node);
    }
  }

  static Block createBlockForParagraph(Paragraph paragraph) {
    InlineSequence inlines = new InlineSequence(true);
    foreachChild(paragraph, child -> addInlineForNode(inlines, child, 0));
    return new ParagraphBlock(Alignment.LEFT, inlines);
  }

  static Block createBlockForOrderedList(OrderedList orderedList) {
    Map<Position, Block> cells = new HashMap<>();
    foreachChild(orderedList, node -> addListItem(cells, true, node));
    return new GridBlock(cells);
  }

  static Block createBlockForBulletList(BulletList bulletList) {
    Map<Position, Block> cells = new HashMap<>();
    foreachChild(bulletList, node -> addListItem(cells, false, node));
    return new GridBlock(cells);
  }

  static void addListItem(Map<Position, Block> cells, boolean numbered, Node node) {
    int rowIndex = 1 + cells.keySet().stream().mapToInt(Position::row).max().orElse(-1);
    Block itemMarker;
    if (numbered) {

      itemMarker = new ParagraphBlock(Alignment.RIGHT, new InlineSequence(true, new Inline("%d. ".formatted(rowIndex + 1), Style.NORMAL)));
    } else {
      itemMarker = new PreformattedBlock("- ");
    }
    cells.put(new Position(rowIndex, 0), itemMarker);

    if (node instanceof BulletListItem || node instanceof OrderedListItem) {
      List<Block> item = new ArrayList<>();
      for (Node itemElement : node.getChildren()) {
        item.add(createBlockForNode(itemElement, item.isEmpty()));
      }
      cells.put(new Position(rowIndex, 1), toBlock(item));
    } else {
      cells.put(new Position(rowIndex, 0), createUnimplementedBlock(node));
    }
  }

  static Block createBlockForHeading(Heading heading, boolean isFirstChild) {
    List<Block> blocks = new ArrayList<>();
    if (!isFirstChild) {
      blocks.add(createNewLineBlock());
    }
    InlineSequence headerSequence = new InlineSequence(true);
    int style = switch (heading.getLevel()) {
      case 1 -> Style.BOLD | Style.UNDERLINED | Style.ITALIC;
      case 2 -> Style.UNDERLINED | Style.BOLD;
      case 3 -> Style.UNDERLINED;
      default -> 0;
    };
    addInlineForNode(headerSequence, heading, style);
    blocks.add(new ParagraphBlock(Alignment.LEFT, headerSequence));
    return new SequenceBlock(blocks);
  }

  static Block createBlockForBlockQuote(BlockQuote blockQuote) {
    List<Block> content = new ArrayList<>();
    for (Node itemElement : blockQuote.getChildren()) {
      content.add(createBlockForNode(itemElement, content.isEmpty()));
    }
    return new GridBlock(Map.of(
        new Position(0, 0), new PreformattedBlock("    "),
        new Position(0, 1), toBlock(content))
    );
  }

  static Block createBlockForIndentedCodeBlock(IndentedCodeBlock indentedCodeBlock) {
    PreformattedBlock content = new PreformattedBlock(indentedCodeBlock.getContentLines().stream()
        .map(BasedSequence::toString)
        .toList());
    return new GridBlock(Map.of(
        new Position(0, 0), new PreformattedBlock("    "),
        new Position(0, 1), content)
    );
  }

  static Block createBlockForFencedCodeBlock(FencedCodeBlock fencedCodeBlock) {
    return new PreformattedBlock(fencedCodeBlock.getContentLines().stream()
        .map(BasedSequence::toString)
        .toList());
  }

  static Block createBlockForTableBlock(TableBlock tableBlock) {
    Map<Position, Block> cells = new HashMap<>();
    TableRow headerRow = findFirstChild(tableBlock, TableHead.class)
        .flatMap(header -> findFirstChild(header, TableRow.class))
        .orElse(null);
    if (headerRow != null) {
      addTableRow(cells, headerRow, 0);
    }
    findFirstChild(tableBlock, TableBody.class).ifPresent(
        tableBody -> addTableBody(cells, tableBody));
    return new GridBlock(cells).toTable(headerRow != null);
  }

  static void addTableBody(Map<Position, Block> cells, TableBody body) {
    int rowIndex = 1 + cells.keySet().stream().mapToInt(Position::row).max().orElse(-1);
    for (Node node : body.getChildren()) {
      if (node instanceof TableRow row) {
        addTableRow(cells, row, rowIndex++);
      } else {
        cells.put(new Position(rowIndex++, 0), createUnimplementedBlock(node));
      }
    }
  }

  static void addTableRow(Map<Position, Block> cells, TableRow row, int rowIndex) {
    int column = 0;
    for (Node node : row.getChildren()) {
      if (node instanceof TableCell tableCell) {
        cells.put(new Position(rowIndex, column++), createBlockForTableCell(tableCell));
      } else {
        cells.put(new Position(rowIndex, column++), createUnimplementedBlock(node));
      }
    }
  }

  static Block createBlockForTableCell(TableCell tableCell) {
    Alignment alignment = Alignment.LEFT;
    if (tableCell.getAlignment() != null) {
      alignment = switch (tableCell.getAlignment()) {
        case LEFT -> Alignment.LEFT;
        case CENTER -> Alignment.CENTER;
        case RIGHT -> Alignment.RIGHT;
      };
    }
    InlineSequence sequence = new InlineSequence(true);
    foreachChild(tableCell, child -> addInlineForNode(sequence, child, 0));
    return new ParagraphBlock(alignment, sequence);
  }

  static void addInlineForNode(InlineSequence sequence, Node node, int style) {
    if (node instanceof Text text) {
      sequence.addInlinesForString(text.getChars(), style);
    } else if (node instanceof StrongEmphasis strongEmphasis) {
      foreachChild(strongEmphasis, child -> addInlineForNode(sequence, child, style | Style.BOLD));
    } else if (node instanceof Emphasis emphasis) {
      foreachChild(emphasis, child -> addInlineForNode(sequence, child, style | Style.ITALIC));
    } else if (node instanceof SoftLineBreak) {
      sequence.addInline(new Inline(" ", style));
    } else if (node instanceof Heading) {
      foreachChild(node, child -> addInlineForNode(sequence, child, style));
    } else {
      createUnimplementedInline(sequence, node);
    }
  }

  static void foreachChild(Node node, Consumer<Node> consumer) {
    for (Node child : node.getChildren()) {
      consumer.accept(child);
    }
  }

  static <T> Optional<T> findFirstChild(Node node, Class<T> type) {
    for (Node child : node.getChildren()) {
      if (type.isInstance(child)) {
        return Optional.of(type.cast(child));
      }
    }
    return Optional.empty();
  }

  static Block createNewLineBlock() {
    return new PreformattedBlock(System.lineSeparator());
  }

  static Block toBlock(List<Block> blocks) {
    if (blocks.isEmpty()) {
      return new EmptyBlock();
    } else if (blocks.size() == 1) {
      return blocks.get(0);
    } else {
      return new SequenceBlock(blocks);
    }
  }

  static void createUnimplementedInline(InlineSequence sequence, Node node) {
    sequence.addInline(new Inline(node.getChars(), 0));
  }

  static Block createUnimplementedBlock(Node node) {
    return new PreformattedBlock(node.getChars().toString());
  }
}
