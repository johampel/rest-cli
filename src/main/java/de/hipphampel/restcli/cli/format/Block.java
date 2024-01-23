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

import java.util.Objects;

public abstract sealed class Block implements Format permits EmptyBlock, FillerBlock, GridBlock, ParagraphBlock, PreformattedBlock,
    SequenceBlock {

  private boolean fill;
  private int requestedWidth;


  public boolean isFill() {
    return fill;
  }

  public void setFill(boolean fill) {
    this.fill = fill;
  }

  public int getRequestedWidth() {
    return requestedWidth;
  }

  public void setRequestedWidth(int requestedWidth) {
    this.requestedWidth = requestedWidth;
  }

  public abstract int getRawWidth();

  public abstract int getMinWidth();

  public abstract int getHeight();

  public void appendLineContentTo(StringBuilder buffer, int index, boolean withStyles) {
    if (fill) {
      buffer.append(" ".repeat(getVisibleWidth()));
    }
  }

  public String getLine(int index, boolean withStyles) {
    StringBuilder buffer = new StringBuilder();
    appendLineContentTo(buffer, index, withStyles);
    return buffer.toString();
  }

  @Override
  public void appendTo(StringBuilder buffer, boolean withStyles) {
    int height = getHeight();
    for (int i = 0; i < height; i++) {
      appendLineContentTo(buffer, i, withStyles);
      buffer.append(System.lineSeparator());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Block block = (Block) o;
    return fill == block.fill && requestedWidth == block.requestedWidth;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fill, requestedWidth);
  }
}
