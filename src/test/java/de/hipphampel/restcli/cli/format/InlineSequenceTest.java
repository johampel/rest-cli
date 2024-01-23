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

import static de.hipphampel.restcli.cli.format.FormatTestBase.assertFormat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InlineSequenceTest {

  private static final Inline ABC = new Inline("abc", Style.BOLD);
  private static final Inline DEF = new Inline("def", Style.BOLD);
  private static final Inline GHI = new Inline("ghi", Style.NORMAL);
  private static final Inline SPACE = new Inline("  ", Style.BOLD);

  @Test
  void copy() {
    InlineSequence sequence = new InlineSequence(true);
    sequence.addInline(ABC, DEF, ABC, SPACE, DEF, SPACE, GHI);
    InlineSequence copy = sequence.copy();
    sequence.truncateTo(2);

    assertFormat(sequence, false, "abcdef");
    assertFormat(copy, false, "abcdefabc def ghi");

  }

  @Test
  void addInlinesForString() {
    InlineSequence sequence = new InlineSequence(false);

    sequence.addInlinesForString("\nabc  def ghi  ", Style.ITALIC);

    assertThat(sequence.getSize()).isEqualTo(7);
    assertThat(sequence.getInline(0)).isEqualTo(new Inline("\n", Style.ITALIC));
    assertThat(sequence.getInline(1)).isEqualTo(new Inline("abc", Style.ITALIC));
    assertThat(sequence.getInline(2)).isEqualTo(new Inline("  ", Style.ITALIC));
    assertThat(sequence.getInline(3)).isEqualTo(new Inline("def", Style.ITALIC));
    assertThat(sequence.getInline(4)).isEqualTo(new Inline(" ", Style.ITALIC));
    assertThat(sequence.getInline(5)).isEqualTo(new Inline("ghi", Style.ITALIC));
    assertThat(sequence.getInline(6)).isEqualTo(new Inline("  ", Style.ITALIC));
  }

  @Test
  void truncateTo() {
    InlineSequence sequence = new InlineSequence(false);
    sequence.addInline(ABC, DEF, ABC, SPACE, DEF, SPACE, GHI);

    sequence.truncateTo(2);

    assertFormat(sequence, false, "abcdef");
  }

  @Test
  void findFirst() {
    InlineSequence sequence = new InlineSequence(false);
    sequence.addInline(ABC, DEF, ABC, SPACE, DEF, SPACE, GHI);
    InlineSequence target = new InlineSequence(false);

    assertThat(sequence.findFirst(1, Inline::isWhitespace)).isEqualTo(3);
    assertThat(sequence.findFirst(1, inline -> inline.chars().equals("x"))).isEqualTo(-1);
  }

  @Test
  void copyTo() {
    InlineSequence sequence = new InlineSequence(false);
    sequence.addInline(ABC, DEF, ABC, SPACE, DEF, SPACE, GHI);
    InlineSequence target = new InlineSequence(false);

    assertThat(sequence.copyTo(target, 1, inline -> !inline.isWhitespace())).isEqualTo(3);
    assertFormat(target, false, "defabc");
  }

  @ParameterizedTest
  @CsvSource({
      "false, 13",
      "true,  11",
  })
  void getVisibleWidth(boolean collapseWhitespace, int expected) {
    InlineSequence sequence = new InlineSequence(collapseWhitespace);
    sequence.addInline(ABC, SPACE, DEF, SPACE, GHI);

    assertThat(sequence.getVisibleWidth()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "false, false, 'abc  def  ghi'",
      "true,  false, 'abc def ghi'",
      "false, true,  '\033[0;1mabc  def  \033[0mghi'",
      "true,  true,  '\033[0;1mabc def \033[0mghi'",
  })
  void appendTo(boolean collapseWhitespace, boolean withStyles, String expected) {
    InlineSequence sequence = new InlineSequence(collapseWhitespace);
    sequence.addInline(ABC, SPACE, DEF, SPACE, GHI);

    assertFormat(sequence, withStyles, expected);
  }

  @ParameterizedTest
  @CsvSource({
      "false, 'abc', 'abc'",
      "false, '   ', '   '",
      "true,  'abc', 'abc'",
      "true,  '   ', ' '",
  })
  void getCharsOf(boolean collapseWhitespace, String chars, String expected) {
    InlineSequence sequence = new InlineSequence(collapseWhitespace);
    Inline inline = new Inline(chars, Style.NORMAL);

    assertThat(sequence.getCharsOf(inline)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "false, 'abc', 3",
      "false, '   ', 3",
      "true,  'abc', 3",
      "true,  '   ', 1",
  })
  void getVisibleWidthOf(boolean collapseWhitespace, String chars, int expected) {
    InlineSequence sequence = new InlineSequence(collapseWhitespace);
    Inline inline = new Inline(chars, Style.NORMAL);

    assertThat(sequence.getVisibleWidthOf(inline)).isEqualTo(expected);
  }
}
