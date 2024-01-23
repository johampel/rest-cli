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

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class PreformattedBlockTest {

  private static final String SAMPLE_TEXT = """
      Lorem ipsum dolor sit amet, consetetur sadipscing elitr, 
            sed diam nonumy eirmod tempor 
      invidunt ut
      """;

  @ParameterizedTest
  @CsvSource({
      "false, -1, 56, 56, 56, 3",
      "false, 10, 56, 56, 56, 3",
      "true,  60, 56, 56, 60, 3",
  })
  void geometry(boolean fill, int requestedWidth, int expectedMinWidth, int expectedRawWidth, int expectedVisibleWidth,
      int expectedHeight) {
    Block block = new PreformattedBlock(SAMPLE_TEXT);
    block.setFill(fill);
    block.setRequestedWidth(requestedWidth);
    assertBlockGeometry(block, fill, expectedMinWidth, expectedRawWidth, expectedVisibleWidth, expectedHeight);
  }

  @ParameterizedTest
  @MethodSource("content_data")
  void content(boolean fill, int requestedWidth, boolean withStyles, int height, String expected) {
    Block block = new PreformattedBlock(SAMPLE_TEXT);
    block.setFill(fill);
    block.setRequestedWidth(requestedWidth);

    assertBlockContent(block, withStyles, height, expected);
  }

  static Stream<Arguments> content_data() {
    return Stream.of(
        Arguments.of(false, -1, false, 5, """
            Lorem ipsum dolor sit amet, consetetur sadipscing elitr, 
                  sed diam nonumy eirmod tempor 
            invidunt ut
                        
                        
            """),
        Arguments.of(false, -1, true, 5, """
            Lorem ipsum dolor sit amet, consetetur sadipscing elitr, 
                  sed diam nonumy eirmod tempor 
            invidunt ut
                        
                        
            """),
        Arguments.of(false, 60, true, 5, """
            Lorem ipsum dolor sit amet, consetetur sadipscing elitr, 
                  sed diam nonumy eirmod tempor 
            invidunt ut
                        
                        
            """),
        Arguments.of(true, 60, true, 5, """
            Lorem ipsum dolor sit amet, consetetur sadipscing elitr,   \s
                  sed diam nonumy eirmod tempor                        \s
            invidunt ut                                                \s
                                                                       \s
                                                                       \s
            """)
    );
  }
}
