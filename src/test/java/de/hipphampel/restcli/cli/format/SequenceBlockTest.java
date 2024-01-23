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

class SequenceBlockTest {

  private static final Block BLOCK1 = new ParagraphBlock(Alignment.RIGHT, new InlineSequence(true, new Inline("top", Style.BOLD)));
  private static final Block BLOCK2 = new PreformattedBlock("""
      Lorem ipsum dolor sit amet, consetetur sadipscing elitr, 
            sed diam nonumy eirmod tempor 
      invidunt ut
      """);
  private static final Block BLOCK3 = new ParagraphBlock(Alignment.CENTER, new InlineSequence(true, new Inline("bottom", Style.BOLD)));

  @ParameterizedTest
  @CsvSource({
      "false, -1, 3, 56, 56, 5",
      "false, 10, 3, 56, 56, 5",
      "true,  60, 3, 56, 60, 5",
  })
  void geometry(boolean fill, int requestedWidth, int expectedMinWidth, int expectedRawWidth, int expectedVisibleWidth,
      int expectedHeight) {
    Block block = new SequenceBlock(BLOCK1, BLOCK2, BLOCK3);
    block.setFill(fill);
    block.setRequestedWidth(requestedWidth);
    assertBlockGeometry(block, fill, expectedMinWidth, expectedRawWidth, expectedVisibleWidth, expectedHeight);
  }

  @ParameterizedTest
  @MethodSource("content_data")
  void content(boolean fill, int requestedWidth, boolean withStyles, int height, String expected) {
    Block block = new SequenceBlock(BLOCK1, BLOCK2, BLOCK3);
    block.setFill(fill);
    block.setRequestedWidth(requestedWidth);

    assertBlockContent(block, withStyles, height, expected);
  }

  static Stream<Arguments> content_data() {
    return Stream.of(
        Arguments.of(false, -1, false, 7, """
            top
            Lorem ipsum dolor sit amet, consetetur sadipscing elitr, 
                  sed diam nonumy eirmod tempor 
            invidunt ut
            bottom
                        
                        
            """),
        Arguments.of(false, -1, true, 7, """
            \033[0;1mtop\033[0m
            Lorem ipsum dolor sit amet, consetetur sadipscing elitr, 
                  sed diam nonumy eirmod tempor 
            invidunt ut
            \033[0;1mbottom\033[0m
                        
                        
            """),
        Arguments.of(false, 60, true, 7, """
            \033[0;1mtop\033[0m
            Lorem ipsum dolor sit amet, consetetur sadipscing elitr, 
                  sed diam nonumy eirmod tempor 
            invidunt ut
            \033[0;1mbottom\033[0m
                        
                        
            """),
        Arguments.of(true, 60, true, 7, """
                                                                     \033[0;1mtop\033[0m
            Lorem ipsum dolor sit amet, consetetur sadipscing elitr,   \s
                  sed diam nonumy eirmod tempor                        \s
            invidunt ut                                                \s
                                       \033[0;1mbottom\033[0m                          \s
                                                                       \s
                                                                       \s
            """)
    );
  }

}
