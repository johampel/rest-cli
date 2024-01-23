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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class SequenceBlock extends Block {

  private final List<Block> children;
  private final int rawWidth;
  private final int minWidth;

  public SequenceBlock(Block... children) {
    this(Arrays.asList(children));
  }

  public SequenceBlock(List<Block> children) {
    this.children = new ArrayList<>(children);
    this.rawWidth = children.stream()
        .mapToInt(Block::getRawWidth)
        .max()
        .orElse(0);
    this.minWidth = children.stream()
        .mapToInt(Block::getMinWidth)
        .min()
        .orElse(0);
    setFill(false);
    setRequestedWidth(-1);
  }

  public int getSize() {
    return children.size();
  }

  @Override
  public void setFill(boolean fill) {
    super.setFill(fill);
    this.children.forEach(child -> child.setFill(fill));
  }

  @Override
  public void setRequestedWidth(int requestedWidth) {
    super.setRequestedWidth(requestedWidth);
    this.children.forEach(child -> child.setRequestedWidth(requestedWidth));
  }

  @Override
  public int getRawWidth() {
    return this.rawWidth;
  }

  @Override
  public int getMinWidth() {
    return this.minWidth;
  }

  @Override
  public int getHeight() {
    return children.stream()
        .mapToInt(Block::getHeight)
        .sum();
  }

  @Override
  public int getVisibleWidth() {
    int childWidth = children.stream()
        .mapToInt(Block::getVisibleWidth)
        .max()
        .orElse(0);
    if (!isFill() || getRequestedWidth() < 0) {
      return childWidth;
    } else {
      return Math.max(getRequestedWidth(), childWidth);
    }
  }

  @Override
  public void appendLineContentTo(StringBuilder buffer, int index, boolean withStyles) {
    for (Block child : children) {
      if (child.getHeight() > index) {
        child.appendLineContentTo(buffer, index, withStyles);
        return;
      } else {
        index -= child.getHeight();
      }
    }
    super.appendLineContentTo(buffer, index, withStyles);
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
    SequenceBlock that = (SequenceBlock) o;
    return rawWidth == that.rawWidth && minWidth == that.minWidth && Objects.equals(children, that.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), children, rawWidth, minWidth);
  }

  @Override
  public String toString() {
    return "SequenceBlock{" +
        "children=" + children +
        ", rawWidth=" + rawWidth +
        ", minWidth=" + minWidth +
        '}';
  }
}
