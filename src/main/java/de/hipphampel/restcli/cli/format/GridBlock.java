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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public final class GridBlock extends Block {

  private final Map<Position, Block> cells;
  private final int rowCount;
  private final int columnCount;
  private final List<Integer> maxColumnWidths;
  private final List<Integer> minColumnWidths;
  private final List<Integer> columnWidths;
  private final List<Integer> rowHeights;
  private final int rawWidth;
  private final int minWidth;

  public GridBlock(Map<Position, Block> cells) {
    this.cells = new HashMap<>(cells);
    this.columnCount = cells.keySet().stream()
        .mapToInt(Position::column)
        .max()
        .orElse(-1) + 1;
    this.rowCount = cells.keySet().stream()
        .mapToInt(Position::row)
        .max()
        .orElse(-1) + 1;
    this.cells.forEach((position, cell) -> cell.setFill(isFill() || position.column < this.columnCount - 1));
    this.maxColumnWidths = IntStream.range(0, columnCount)
        .map(column -> IntStream.range(0, rowCount)
            .map(row -> getCell(row, column).getRawWidth())
            .max()
            .orElse(0))
        .boxed()
        .toList();
    this.minColumnWidths = IntStream.range(0, columnCount)
        .map(column -> IntStream.range(0, rowCount)
            .map(row -> getCell(row, column).getMinWidth())
            .max()
            .orElse(0))
        .boxed()
        .toList();
    this.rawWidth = this.maxColumnWidths.stream()
        .mapToInt(i -> i)
        .sum();
    this.minWidth = this.minColumnWidths.stream()
        .mapToInt(i -> i)
        .sum();
    this.columnWidths = new ArrayList<>();
    this.rowHeights = new ArrayList<>();
    recalculateCellSizes();
  }

  public int getRowCount() {
    return rowCount;
  }

  public int getColumnCount() {
    return columnCount;
  }


  @Override
  public void setFill(boolean fill) {
    super.setFill(fill);
    this.cells.forEach((position, cell) -> cell.setFill(isFill() || position.column < this.columnCount - 1));
  }

  @Override
  public void setRequestedWidth(int requestedWidth) {
    super.setRequestedWidth(requestedWidth);
    recalculateCellSizes();
  }

  @Override
  public int getRawWidth() {
    return rawWidth;
  }

  @Override
  public int getMinWidth() {
    return minWidth;
  }

  @Override
  public int getHeight() {
    return rowHeights.stream()
        .mapToInt(i -> i)
        .sum();
  }

  @Override
  public int getVisibleWidth() {
    return columnWidths.stream()
        .mapToInt(i -> i)
        .sum();
  }

  @Override
  public void appendLineContentTo(StringBuilder buffer, int index, boolean withStyles) {
    for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
      if (index >= 0 && index < rowHeights.get(rowIndex)) {
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
          Block cell = getCell(rowIndex, columnIndex);
          cell.appendLineContentTo(buffer, index, withStyles);
        }
        return;
      } else {
        index -= rowHeights.get(rowIndex);
      }
    }
    super.appendLineContentTo(buffer, index, withStyles);
  }

  void recalculateCellSizes() {
    recalculateColumnWidths();
    recalculateRowHeights();
  }

  void recalculateColumnWidths() {
    this.columnWidths.clear();

    int availableWidth = getRequestedWidth() < 0 ? Integer.MAX_VALUE : getRequestedWidth();
    List<Integer> variableColumns = new ArrayList<>();
    for (int c = 0; c < columnCount; c++) {
      int min = minColumnWidths.get(c);
      int max = maxColumnWidths.get(c);
      this.columnWidths.add(min);
      if (min == max) {
        availableWidth -= min;
      } else {
        variableColumns.add(c);
      }
    }

    for (int i = 0; i < variableColumns.size(); i++) {
      int c = variableColumns.get(i);
      int min = minColumnWidths.get(c);
      int max = maxColumnWidths.get(c);
      int part = availableWidth / (variableColumns.size() - i);
      this.columnWidths.set(c, Math.min(max, Math.max(part, min)));
      availableWidth -= this.columnWidths.get(c);
    }

    for (int c = 0; c < columnCount; c++) {
      int width = columnWidths.get(c);
      for (int r = 0; r < rowCount; r++) {
        getCell(r, c).setRequestedWidth(width);
      }
    }
  }

  void recalculateRowHeights() {
    rowHeights.clear();
    for (int r = 0; r < rowCount; r++) {
      int height = 0;
      for (int c = 0; c < columnCount; c++) {
        height = Math.max(getCell(r, c).getHeight(), height);
      }
      rowHeights.add(height);
    }
  }

  Block getCell(int row, int column) {
    return this.cells.computeIfAbsent(new Position(row, column), key -> new EmptyBlock());
  }


  public GridBlock toTable(boolean decorated) {
    Map<Position, Block> table = new HashMap<>();

    // Add cells for the frames
    if (decorated) {
      // Top Line
      table.put(new Position(0, 0), new FillerBlock("┌"));
      for (int c = 1; c < 2 * columnCount; c++) {
        table.put(new Position(0, c), new FillerBlock(c % 2 == 1 ? "─" : "┬"));
      }
      table.put(new Position(0, columnCount * 2), new FillerBlock("┐"));

      // Header Line
      table.put(new Position(2, 0), new FillerBlock("├"));
      for (int c = 1; c < 2 * columnCount; c++) {
        table.put(new Position(2, c), new FillerBlock(c % 2 == 1 ? "─" : "┼"));
      }
      table.put(new Position(2, columnCount * 2), new FillerBlock("┤"));

      // Bottom Line
      table.put(new Position(rowCount + 3, 0), new FillerBlock("└"));
      for (int c = 1; c < 2 * columnCount; c++) {
        table.put(new Position(rowCount + 3, c), new FillerBlock(c % 2 == 1 ? "─" : "┴"));
      }
      table.put(new Position(rowCount + 3, columnCount * 2), new FillerBlock("┘"));

      // Content and vertical lines
      for (int r = 0; r < rowCount; r++) {
        int tr = r + 1 + (r > 0 ? 1 : 0);
        for (int c = 0; c < columnCount; c++) {
          table.put(new Position(tr, 2 * c + 1), getCell(r, c));
          table.put(new Position(tr, 2 * c), new FillerBlock("│"));
        }
        table.put(new Position(tr, 2 * columnCount), new FillerBlock("│"));
      }
    } else {
      for (int r = 0; r < rowCount; r++) {
        for (int c = 0; c < columnCount; c++) {
          table.put(new Position(r, 2 * c), getCell(r, c));
        }
        for (int c = 0; c < columnCount - 1; c++) {
          table.put(new Position(r, 2 * c + 1), new FillerBlock(" "));
        }
      }
    }
    return new GridBlock(table);
  }

  public record Position(int row, int column) {

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    GridBlock gridBlock = (GridBlock) o;
    return Objects.equals(cells, gridBlock.cells);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), cells, rowCount, columnCount, maxColumnWidths, minColumnWidths, columnWidths, rowHeights,
        rawWidth, minWidth);
  }
}
