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

import static de.hipphampel.restcli.cli.format.FormatTestBase.assertBlockContent;
import static de.hipphampel.restcli.cli.format.FormatTestBase.assertBlockGeometry;

import de.hipphampel.restcli.cli.format.ParagraphBlock.Alignment;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ParagraphBlockTest {

  private final String SAMPLE_TEXT1 = """
      1
      123
      12345
      1234567
      """;
  private final String SAMPLE_TEXT2 = """
      123456789
      12345678901
      1234567890123
      12345678901
      123456789
      """;
  private final String SAMPLE_TEXT3 = """
      1234567
      12345
      123
      1""";

  @ParameterizedTest
  @CsvSource({
      "LEFT,   false, -1, 13, 97, 97,  1",
      "LEFT,   false, 20, 13, 97, 19,  7",
      "LEFT,   false, 10, 13, 97, 13, 11",
      "LEFT,   true,  10, 13, 97, 13, 11",
      "CENTER, false, -1, 13, 97, 97,  1",
      "CENTER, false, 20, 13, 97, 19,  7",
      "CENTER, false, 10, 13, 97, 13, 11",
      "CENTER, true,  10, 13, 97, 13, 11",
      "RIGHT,  false, -1, 13, 97, 97,  1",
      "RIGHT,  false, 20, 13, 97, 19,  7",
      "RIGHT,  false, 10, 13, 97, 13, 11",
      "RIGHT,  true,  10, 13, 97, 13, 11",
  })
  void geometry(Alignment alignment, boolean fill, int requestedWidth, int exppectedMinWidth, int expectedRawWidth,
      int expectedVisibleWidth, int expectedHeight) {
    ParagraphBlock block = new ParagraphBlock(alignment, new InlineSequence(true)
        .addInlinesForString(SAMPLE_TEXT1, Style.ITALIC)
        .addInlinesForString(SAMPLE_TEXT2, Style.BOLD)
        .addInlinesForString(SAMPLE_TEXT3, Style.UNDERLINED));
    block.setFill(fill);
    block.setRequestedWidth(requestedWidth);
    assertBlockGeometry(block, fill, exppectedMinWidth, expectedRawWidth, expectedVisibleWidth, expectedHeight);
  }

  @ParameterizedTest
  @MethodSource("content_data")
  void content(Alignment alignment, boolean fill, int requestedWidth, boolean withStyles, int height, String expected) {
    ParagraphBlock block = new ParagraphBlock(alignment, new InlineSequence(true)
        .addInlinesForString(SAMPLE_TEXT1, Style.ITALIC)
        .addInlinesForString(SAMPLE_TEXT2, Style.BOLD)
        .addInlinesForString(SAMPLE_TEXT3, Style.UNDERLINED));
    block.setFill(fill);
    block.setRequestedWidth(requestedWidth);

    assertBlockContent(block, withStyles, height, expected);
  }

  static Stream<Arguments> content_data() {
    return Stream.of(
        Arguments.of(Alignment.LEFT, false, -1, false, 3, """
            1 123 12345 1234567 123456789 12345678901 1234567890123 12345678901 123456789 1234567 12345 123 1            
                        
                        
            """),
        Arguments.of(Alignment.LEFT, false, 20, false, 10, """
            1 123 12345 1234567
            123456789
            12345678901
            1234567890123
            12345678901
            123456789 1234567
            12345 123 1
                        
                        
                        
            """),
        Arguments.of(Alignment.LEFT, false, 40, false, 5, """
            1 123 12345 1234567 123456789
            12345678901 1234567890123 12345678901
            123456789 1234567 12345 123 1
                        
                        
            """),
        Arguments.of(Alignment.LEFT, true, 40, false, 5, """
            1 123 12345 1234567 123456789          \s
            12345678901 1234567890123 12345678901  \s
            123456789 1234567 12345 123 1          \s
                                                   \s
                                                   \s
            """),
        Arguments.of(Alignment.LEFT, true, 40, true, 5, """
            \033[0;3m1 123 12345 1234567 \033[0;1m123456789\033[0m          \s
            \033[0;1m12345678901 1234567890123 12345678901\033[0m  \s
            \033[0;1m123456789 \033[0;4m1234567 12345 123 1\033[0m          \s
                                                   \s
                                                   \s
            """),
        Arguments.of(Alignment.CENTER, false, 40, false, 5, """
                1 123 12345 1234567 123456789
            12345678901 1234567890123 12345678901
                123456789 1234567 12345 123 1
                       
                       
            """),
        Arguments.of(Alignment.CENTER, true, 40, false, 5, """
                 1 123 12345 1234567 123456789     \s
             12345678901 1234567890123 12345678901 \s
                 123456789 1234567 12345 123 1     \s
                                                   \s
                                                   \s
            """),
        Arguments.of(Alignment.RIGHT, false, 40, false, 5, """
                    1 123 12345 1234567 123456789
            12345678901 1234567890123 12345678901
                    123456789 1234567 12345 123 1
                       
                       
            """),
        Arguments.of(Alignment.RIGHT, true, 40, false, 5, """
                       1 123 12345 1234567 123456789
               12345678901 1234567890123 12345678901
                       123456789 1234567 12345 123 1
                                                   \s
                                                   \s
            """)

    );
  }
}
