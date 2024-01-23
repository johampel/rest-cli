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

public final class FillerBlock extends Block {

  private final Inline filler;

  public FillerBlock(Inline filler) {
    this.filler = Objects.requireNonNull(filler);
  }

  public FillerBlock(CharSequence chars) {
    this(new Inline(chars, Style.NORMAL));
  }

  @Override
  public int getRawWidth() {
    return filler.getVisibleWidth();
  }

  @Override
  public int getMinWidth() {
    return 1;
  }

  @Override
  public int getHeight() {
    return 1;
  }

  @Override
  public int getVisibleWidth() {
    return getRequestedWidth() < 0 ? filler.getVisibleWidth() : getRequestedWidth();
  }

  @Override
  public void appendLineContentTo(StringBuilder buffer, int index, boolean withStyles) {
    if (withStyles && filler.style() != Style.NORMAL) {
      Style.appendStyleString(buffer, filler.style());
    }

    int rest = getVisibleWidth();
    CharSequence chars = filler.chars();
    while (rest > 0) {
      int len = Math.min(rest, chars.length());
      buffer.append(chars, 0, len);
      rest -= len;
    }
    if (withStyles && filler.style() != 0) {
      Style.appendStyleString(buffer, Style.NORMAL);
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
    if (!super.equals(o)) {
      return false;
    }
    FillerBlock that = (FillerBlock) o;
    return Objects.equals(filler, that.filler);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), filler);
  }
}
