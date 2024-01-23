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

import de.hipphampel.restcli.cli.format.GridBlock.Position;
import de.hipphampel.restcli.cli.format.ParagraphBlock.Alignment;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class GridBlockTest {

  private static final Block BLOCK1 = new FillerBlock("ab");
  private static final Block BLOCK2 = new PreformattedBlock("""
      line 1
      line 2 a little longer
      """);
  private static final Block BLOCK3 = new ParagraphBlock(Alignment.CENTER, new InlineSequence(true,
      new Inline("bold", Style.BOLD), new Inline(" ", Style.NORMAL),
      new Inline("normal", Style.NORMAL)));

  private GridBlock block;

  @BeforeEach
  void beforeEach() {
    block = new GridBlock(Map.of(
        new Position(0, 0), BLOCK3,
        new Position(0, 1), BLOCK1,
        new Position(1, 1), BLOCK2
    ));
  }


  @ParameterizedTest
  @CsvSource({
      "false, -1, 28, 33, 33, 3",
      "false, 10, 28, 33, 28, 4",
      "true,  60, 28, 33, 33, 3",
  })
  void geometry(boolean fill, int requestedWidth, int expectedMinWidth, int expectedRawWidth, int expectedVisibleWidth,
      int expectedHeight) {
    block.setFill(fill);
    block.setRequestedWidth(requestedWidth);
    assertBlockGeometry(block, fill, expectedMinWidth, expectedRawWidth, expectedVisibleWidth, expectedHeight);
  }

  @ParameterizedTest
  @MethodSource("content_data")
  void content(boolean fill, int requestedWidth, boolean withStyles, int height, String expected) {
    block.setFill(fill);
    block.setRequestedWidth(requestedWidth);

    assertBlockContent(block, withStyles, height, expected);
  }

  static Stream<Arguments> content_data() {
    return Stream.of(
        Arguments.of(false, -1, false, 7, """
            bold normalababababababababababab
                       line 1
                       line 2 a little longer
                        
                        
                        
                        
            """),
        Arguments.of(false, -1, true, 7, """
            \033[0;1mbold\033[0m normalababababababababababab
                       line 1
                       line 2 a little longer
                        
                        
                        
                        
            """),
        Arguments.of(false, 60, true, 7, """
            \033[0;1mbold\033[0m normalababababababababababab
                       line 1
                       line 2 a little longer
                        
                        
                        
                        
            """),
        Arguments.of(true, 60, true, 7, """
            \033[0;1mbold\033[0m normalababababababababababab
                       line 1               \s
                       line 2 a little longer
                                            \s
                                            \s
                                            \s
                                            \s
            """),
        Arguments.of(true, 60, true, 7, """
            \033[0;1mbold\033[0m normalababababababababababab
                       line 1               \s
                       line 2 a little longer
                                            \s
                                            \s
                                            \s
                                            \s
            """)
    );
  }

  @Test
  void toTableUndecorated() {
    GridBlock grid = new GridBlock(Map.of(
        new Position(0, 0), new ParagraphBlock("abc"),
        new Position(0, 1), new ParagraphBlock("def"),
        new Position(1, 0), new ParagraphBlock("ghi"),
        new Position(1, 1), new ParagraphBlock("jkl"),
        new Position(2, 0), new ParagraphBlock("mno"),
        new Position(2, 1), new ParagraphBlock("pqr")
    ));
    GridBlock table = grid.toTable(false);

    assertBlockContent(table, false, 7, """
        abc def
        ghi jkl
        mno pqr
                
                
                
                
        """);
  }

  @Test
  void toTableDecorated() {
    GridBlock grid = new GridBlock(Map.of(
        new Position(0, 0), new ParagraphBlock("abc"),
        new Position(0, 1), new ParagraphBlock("def"),
        new Position(1, 0), new ParagraphBlock("ghi"),
        new Position(1, 1), new ParagraphBlock("jkl"),
        new Position(2, 0), new ParagraphBlock("mno"),
        new Position(2, 1), new ParagraphBlock("pqr")
    ));
    GridBlock table = grid.toTable(true);

    assertBlockContent(table, false, 7, """
        ┌───┬───┐
        │abc│def│
        ├───┼───┤
        │ghi│jkl│
        │mno│pqr│
        └───┴───┘
                
        """);
  }

}
