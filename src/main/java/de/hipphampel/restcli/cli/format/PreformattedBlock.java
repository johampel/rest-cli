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

public final class PreformattedBlock extends Block {

  private final List<String> lines;
  private final int rawWidth;

  public PreformattedBlock(String... lines) {
    this(Arrays.asList(lines));
  }

  public PreformattedBlock(List<String> lines) {
    this.lines = lines.stream()
        .map(PreformattedBlock::splitLine)
        .flatMap(List::stream)
        .toList();
    this.rawWidth = this.lines.stream()
        .mapToInt(String::length)
        .max()
        .orElse(0);
  }

  public static List<String> splitLine(String line) {
    String ls = System.lineSeparator();
    List<String> lines = new ArrayList<>();
    if (!line.isEmpty()) {
      int start = 0;
      int pos;
      while ((pos = line.indexOf(ls, start)) != -1) {
        lines.add(line.substring(start, pos));
        start = pos + ls.length();
      }
      if (start < line.length()) {
        lines.add(line.substring(start));
      }
    } else {
      lines.add("");
    }
    return lines;
  }

  @Override
  public int getRawWidth() {
    return this.rawWidth;
  }

  @Override
  public int getMinWidth() {
    return this.rawWidth;
  }

  @Override
  public int getHeight() {
    return lines.size();
  }

  @Override
  public int getVisibleWidth() {
    return getRawWidth() < 0 ? getRawWidth() : Math.max(getRawWidth(), getRequestedWidth());
  }

  @Override
  public void appendLineContentTo(StringBuilder buffer, int index, boolean withStyles) {
    if (index >= 0 && index < lines.size()) {
      String line = lines.get(index);
      buffer.append(line);
      if (isFill()) {
        int spaces = getVisibleWidth() - line.length();
        buffer.append(" ".repeat(Math.max(0, spaces)));
      }
      return;
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
    PreformattedBlock that = (PreformattedBlock) o;
    return rawWidth == that.rawWidth && Objects.equals(lines, that.lines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lines, rawWidth);
  }

  @Override
  public String toString() {
    return "PreformattedBlock{" +
        "lines=" + lines +
        ", rawWidth=" + rawWidth +
        '}';
  }
}
