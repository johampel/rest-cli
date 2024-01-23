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

public record Inline(CharSequence chars, int style) implements Format {

  public Inline {
    Objects.requireNonNull(chars);
  }

  public boolean isWhitespace() {
    int len = chars.length();
    for (int i = 0; i < len; i++) {
      if (!Character.isWhitespace(chars.charAt(i))) {
        return false;
      }
    }
    return len > 0;
  }

  public boolean isEmpty() {
    return chars.isEmpty();
  }

  @Override
  public int getVisibleWidth() {
    return chars.length();
  }

  @Override
  public void appendTo(StringBuilder buffer, boolean withStyles) {
    if (withStyles && style != 0) {
      Style.appendStyleString(buffer, style);
    }
    buffer.append(chars);
    if (withStyles && style != 0) {
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
    Inline inline = (Inline) o;
    return style == inline.style && Objects.equals(String.valueOf(chars), String.valueOf(inline.chars));
  }

  @Override
  public int hashCode() {
    return Objects.hash(chars, style);
  }
}
