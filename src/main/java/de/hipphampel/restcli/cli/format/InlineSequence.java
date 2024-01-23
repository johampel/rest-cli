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
import java.util.function.Predicate;

public final class InlineSequence implements Format {

  private final List<Inline> inlines;
  private final boolean compactWhitespace;

  public InlineSequence(boolean compactWhitespace, Inline... inlines) {
    this.inlines = new ArrayList<>(Arrays.asList(inlines));
    this.compactWhitespace = compactWhitespace;
  }

  public InlineSequence copy() {
    InlineSequence copy = new InlineSequence(compactWhitespace);
    copy.inlines.addAll(inlines);
    return copy;
  }

  public int getSize() {
    return this.inlines.size();
  }

  public InlineSequence addInlinesForString(CharSequence chars, int style) {
    int len = chars.length();
    int i = 0;
    while (i < len) {
      int start = i;
      while (i < len && Character.isWhitespace(chars.charAt(i))) {
        i++;
      }
      if (i > start) {
        addInline(new Inline(chars.subSequence(start, i), style));
      }
      start = i;
      while (i < len && !Character.isWhitespace(chars.charAt(i))) {
        i++;
      }
      if (i > start) {
        addInline(new Inline(chars.subSequence(start, i), style));
      }
    }
    return this;
  }

  public InlineSequence addInline(Inline... inlines) {
    Arrays.stream(inlines).forEach(inline -> this.inlines.add(Objects.requireNonNull(inline)));
    return this;
  }

  public Inline getInline(int index) {
    return inlines.get(index);
  }

  public void truncateTo(int size) {
    while (inlines.size() > size) {
      this.inlines.remove(size);
    }
  }

  public int findFirst(int startIndex, Predicate<Inline> predicate) {
    int index = startIndex;
    while (index < this.inlines.size()) {
      Inline inline = this.inlines.get(index);
      if (predicate.test(inline)) {
        return index;
      }
      index++;
    }
    return -1;
  }

  public int copyTo(InlineSequence target, int startIndex, Predicate<Inline> predicate) {
    int index = startIndex;
    while (index < this.inlines.size()) {
      Inline inline = this.inlines.get(index);
      if (!predicate.test(inline)) {
        break;
      }
      target.inlines.add(inline);
      index++;
    }
    return index;
  }

  @Override
  public int getVisibleWidth() {
    return this.inlines.stream()
        .mapToInt(this::getVisibleWidthOf)
        .sum();
  }

  public int getMaxInlineLength() {
    return this.inlines.stream()
        .filter(inline -> !inline.isWhitespace())
        .mapToInt(this::getVisibleWidthOf)
        .max()
        .orElse(0);
  }


  @Override
  public void appendTo(StringBuilder buffer, boolean withStyles) {
    int style = Style.NORMAL;
    for (Inline inline : this.inlines) {
      if (withStyles && inline.style() != style) {
        Style.appendStyleString(buffer, inline.style());
        style = inline.style();
      }
      buffer.append(getCharsOf(inline));
    }
    if (withStyles && style != Style.NORMAL) {
      Style.appendStyleString(buffer, Style.NORMAL);
    }
  }

  CharSequence getCharsOf(Inline inline) {
    return compactWhitespace && inline.isWhitespace() ? " " : inline.chars();
  }

  int getVisibleWidthOf(Inline inline) {
    return compactWhitespace && inline.isWhitespace() ? 1 : inline.getVisibleWidth();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineSequence that = (InlineSequence) o;
    return compactWhitespace == that.compactWhitespace && Objects.equals(inlines, that.inlines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inlines, compactWhitespace);
  }

  @Override
  public String toString() {
    return "InlineSequence{" +
        "inlines=" + inlines +
        ", compactWhitespace=" + compactWhitespace +
        '}';
  }
}
