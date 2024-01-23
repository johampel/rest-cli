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
import java.util.List;
import java.util.Objects;

public final class ParagraphBlock extends Block {

  public enum Alignment {
    LEFT,
    CENTER,
    RIGHT
  }

  private final InlineSequence inlines;
  private final Alignment alignment;
  private List<InlineSequence> lines;

  public ParagraphBlock(Alignment alignment, InlineSequence inlines) {
    this.alignment = Objects.requireNonNull(alignment);
    this.inlines = inlines.copy();
  }

  public ParagraphBlock(Alignment alignment, String content) {
    this(
        alignment,
        new InlineSequence(true).addInlinesForString(content, Style.NORMAL));
  }

  public ParagraphBlock(String content) {
    this(
        Alignment.LEFT,
        new InlineSequence(true).addInlinesForString(content, Style.NORMAL));
  }

  @Override
  public void setFill(boolean fill) {
    super.setFill(fill);
    lines = null;
  }

  @Override
  public void setRequestedWidth(int requestedWidth) {
    super.setRequestedWidth(requestedWidth);
    lines = null;
  }

  @Override
  public int getVisibleWidth() {
    recalculateLinesIfRequired();
    int maxVisibleLineWidth = lines.stream()
        .mapToInt(InlineSequence::getVisibleWidth)
        .max()
        .orElse(0);
    int requestedWidth = getRequestedWidth();
    if (requestedWidth < 0) {
      return maxVisibleLineWidth;
    } else if (isFill()) {
      return Math.max(maxVisibleLineWidth, requestedWidth);
    } else {
      return maxVisibleLineWidth;
    }
  }

  @Override
  public int getRawWidth() {
    return inlines.getVisibleWidth();
  }

  @Override
  public int getMinWidth() {
    return this.inlines.getMaxInlineLength();
  }

  @Override
  public int getHeight() {
    recalculateLinesIfRequired();
    return lines.size();
  }

  @Override
  public void appendLineContentTo(StringBuilder buffer, int index, boolean withStyles) {
    recalculateLinesIfRequired();
    if (index >= 0 && index < getHeight()) {
      InlineSequence line = this.lines.get(index);
      int visibleWidth = getVisibleWidth();
      int spaces = visibleWidth - line.getVisibleWidth();
      if (spaces > 0) {
        switch (alignment) {
          case CENTER -> buffer.append(" ".repeat(spaces / 2));
          case RIGHT -> buffer.append(" ".repeat(spaces));
        }
      }
      line.appendTo(buffer, withStyles);
      if (spaces > 0 && isFill()) {
        switch (alignment) {
          case LEFT -> buffer.append(" ".repeat(spaces));
          case CENTER -> buffer.append(" ".repeat(spaces - spaces / 2));
        }
      }
    } else {
      super.appendLineContentTo(buffer, index, withStyles);
    }
  }

  void recalculateLinesIfRequired() {
    if (lines != null) {
      return;
    }
    recalculateLines();
  }

  void recalculateLines() {
    this.lines = new ArrayList<>();
    if (getRequestedWidth() < 0) {
      this.lines.add(this.inlines.copy());
      return;
    }

    int i = 0;
    int size = inlines.getSize();
    while (i < size) {
      InlineSequence line = new InlineSequence(true);
      int lineEnd = fillLine(line, i);
      if (lineEnd == i) {
        throw new IllegalStateException("Must not happen - consumed no inline");
      }
      i = lineEnd;
      if (line.getSize() > 0) {
        this.lines.add(line);
      }
    }
  }

  int fillLine(InlineSequence line, int start) {
    int index = start;
    int availableWidth = getRequestedWidth();

    // Try to copy first non empty block
    index = inlines.copyTo(line, index, inline -> !inline.isWhitespace());

    // Fill until line size is big enough
    while (index < this.inlines.getSize() && line.getVisibleWidth() <= availableWidth) {
      int lineSize = line.getSize();
      if (inlines.getInline(index).isWhitespace()) {
        line.addInline(inlines.getInline(index++));
      }
      int nextWordStart = inlines.findFirst(index, inline -> !inline.isWhitespace());
      if (nextWordStart != -1) {
        int end = inlines.copyTo(line, nextWordStart, inline -> !inline.isWhitespace());
        if (line.getVisibleWidth() <= availableWidth) {
          index = end;
        } else {
          line.truncateTo(lineSize);
          break;
        }
      } else {
        // No further word -> rest are whitespace to be skipped
        line.truncateTo(lineSize);
        return inlines.getSize();
      }
    }
    return index;
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
    ParagraphBlock that = (ParagraphBlock) o;
    return Objects.equals(inlines, that.inlines) && alignment == that.alignment;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), inlines, alignment);
  }

  @Override
  public String toString() {
    return "ParagraphBlock{" +
        "inlines=" + inlines +
        ", alignment=" + alignment +
        ", lines=" + lines +
        '}';
  }
}


