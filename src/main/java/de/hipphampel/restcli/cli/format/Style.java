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

public class Style {

  public final static int NORMAL = 0x00;
  public final static int BOLD = 0x01;
  public final static int ITALIC = 0x02;
  public final static int UNDERLINED = 0x04;
  public final static int STRIKETHROUGH = 0x08;

  private Style() {

  }

  public static boolean isBold(int style) {
    return (style & BOLD) != 0;
  }

  public static boolean isItalic(int style) {
    return (style & ITALIC) != 0;
  }

  public static boolean isUnderlined(int style) {
    return (style & UNDERLINED) != 0;
  }

  public static boolean isStrikeThrough(int style) {
    return (style & STRIKETHROUGH) != 0;
  }

  public static void appendStyleString(StringBuilder buffer, int style) {
    buffer.append("\033[0");
    if (isBold(style)) {
      buffer.append(";1");
    }
    if (isItalic(style)) {
      buffer.append(";3");
    }
    if (isUnderlined(style)) {
      buffer.append(";4");
    }
    if (isStrikeThrough(style)) {
      buffer.append(";9");
    }
    buffer.append("m");
  }

}
