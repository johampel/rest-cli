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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FormatBuilderTest {

  @Test
  void paragraph() {
    String input = """
        Hello
        *lovely*
        **World**!""";

    assertBlock(input, -1, false,
        """
            Hello lovely World!
            """);
    assertBlock(input, 15, false,
        """
            Hello lovely
            World!
            """);
    assertBlock(input, 15, true,
        """
            Hello \033[0;3mlovely\033[0m
            \033[0;1mWorld\033[0m!
            """);
  }

  @Test
  void bulletList() {
    String input = """
        - Item
        - List item with
                
          further line
        - sub list with child items
          - a - äh
          - b - *bäh*
          - c - **zeh**
        """;

    assertBlock(input, -1, false,
        """
            - Item
            - List item with
              further line
            - sub list with child items
              - a - äh
              - b - bäh
              - c - zeh
             """);
    assertBlock(input, 15, false,
        """
            - Item
            - List item
              with
              further line
            - sub list with
              child items
              - a - äh
              - b - bäh
              - c - zeh
            """);
    assertBlock(input, 15, true,
        """
            - Item
            - List item
              with
              further line
            - sub list with
              child items
              - a - äh
              - b - \033[0;3mbäh\033[0m
              - c - \033[0;1mzeh\033[0m
            """);
  }

  @Test
  void numberedList() {
    String input = """
        1. Item
        1. List item with
                
           further line
        1. sub list with child items
           1. a - äh
           1. b - *bäh*
           1. c - **zeh**
        """;

    assertBlock(input, -1, false,
        """
            1. Item
            2. List item with
               further line
            3. sub list with child items
               1. a - äh
               2. b - bäh
               3. c - zeh
             """);
    assertBlock(input, 15, false,
        """
            1. Item
            2. List item
               with
               further line
            3. sub list
               with child
               items
               1. a - äh
               2. b - bäh
               3. c - zeh
            """);
    assertBlock(input, 15, true,
        """
            1. Item
            2. List item
               with
               further line
            3. sub list
               with child
               items
               1. a - äh
               2. b - \033[0;3mbäh\033[0m
               3. c - \033[0;1mzeh\033[0m
            """);
  }

  @Test
  void heading() {
    String input = """
        # Heading 1
        ## Heading 2
        ### Heading 3
        # Heading 1
        """;

    assertBlock(input, -1, false,
        """
            Heading 1

            Heading 2
                        
            Heading 3
                        
            Heading 1
            """);
    assertBlock(input, 8, false,
        """
            Heading
            1

            Heading
            2
                        
            Heading
            3
                        
            Heading
            1
            """);
    assertBlock(input, 10, true,
        """
            \033[0;1;3;4mHeading 1\033[0m

            \033[0;1;4mHeading 2\033[0m
                        
            \033[0;4mHeading 3\033[0m
                        
            \033[0;1;3;4mHeading 1\033[0m
            """);
  }

  @Test
  void blockQuote() {
    String input = """
        >Hello
         *lovely*
         **World**!
        """;

    assertBlock(input, -1, false,
        """
                Hello lovely World!
            """);
    assertBlock(input, 16, false,
        """
                Hello lovely
                World!
            """);
    assertBlock(input, 16, true,
        """
                Hello \033[0;3mlovely\033[0m
                \033[0;1mWorld\033[0m!
            """);
  }

  @Test
  void indentedCodeBlock() {
    String input = """
            Hel lo
            *love ly*
            **Worl d**!
        """;

    assertBlock(input, -1, false,
        """
                Hel lo
                *love ly*
                **Worl d**!
            """);
    assertBlock(input, 3, false,
        """
                Hel lo
                *love ly*
                **Worl d**!
            """);
    assertBlock(input, 16, true,
        """
                Hel lo
                *love ly*
                **Worl d**!
            """);
  }

  @Test
  void fencedCodeBlock() {
    String input = """
        ~~~
        Hel lo
        *love ly*
        **Worl d**!
        ~~~
        """;

    assertBlock(input, -1, false,
        """
            Hel lo
            *love ly*
            **Worl d**!
            """);
    assertBlock(input, 3, false,
        """
            Hel lo
            *love ly*
            **Worl d**!
            """);
    assertBlock(input, 16, true,
        """
            Hel lo
            *love ly*
            **Worl d**!
            """);
  }

  @Test
  void decoratedTable() {
    String input = """
        |1|2|3|
        |:---|:---:|---:|
        | *Foo 1* | A longer text having more words | short 1 |
        | **Foo 2** | short 2 | short 3 |
        | Foo 3 | short 4 | A longer text having more words |
        """;

    assertBlock(input, -1, false,
        """
            ┌─────┬───────────────────────────────┬───────────────────────────────┐
            │1    │               2               │                              3│
            ├─────┼───────────────────────────────┼───────────────────────────────┤
            │Foo 1│A longer text having more words│                        short 1│
            │Foo 2│            short 2            │                        short 3│
            │Foo 3│            short 4            │A longer text having more words│
            └─────┴───────────────────────────────┴───────────────────────────────┘
            """);
    assertBlock(input, 10, false,
        """
            ┌───┬──────┬──────┐
            │1  │  2   │     3│
            ├───┼──────┼──────┤
            │Foo│  A   │ short│
            │1  │longer│     1│
            │   │ text │      │
            │   │having│      │
            │   │ more │      │
            │   │words │      │
            │Foo│short │ short│
            │2  │  2   │     3│
            │Foo│short │     A│
            │3  │  4   │longer│
            │   │      │  text│
            │   │      │having│
            │   │      │  more│
            │   │      │ words│
            └───┴──────┴──────┘
            """);
    assertBlock(input, 30, false,
        """
            ┌─────┬──────────┬───────────┐
            │1    │    2     │          3│
            ├─────┼──────────┼───────────┤
            │Foo 1│ A longer │    short 1│
            │     │   text   │           │
            │     │  having  │           │
            │     │more words│           │
            │Foo 2│ short 2  │    short 3│
            │Foo 3│ short 4  │   A longer│
            │     │          │text having│
            │     │          │ more words│
            └─────┴──────────┴───────────┘
            """);
    assertBlock(input, 30, true,
        """
            ┌─────┬──────────┬───────────┐
            │1    │    2     │          3│
            ├─────┼──────────┼───────────┤
            │\033[0;3mFoo 1\033[0m│ A longer │    short 1│
            │     │   text   │           │
            │     │  having  │           │
            │     │more words│           │
            │\033[0;1mFoo 2\033[0m│ short 2  │    short 3│
            │Foo 3│ short 4  │   A longer│
            │     │          │text having│
            │     │          │ more words│
            └─────┴──────────┴───────────┘
            """);
  }

  @Test
  void headlessTable() {
    String input = """
        |:---|:---:|---:|
        | *Foo 1* | A longer text having more words | short 1 |
        | **Foo 2** | short 2 | short 3 |
        | Foo 3 | short 4 | A longer text having more words |
        """;

    assertBlock(input, -1, false,
        """
            Foo 1 A longer text having more words short 1
            Foo 2             short 2             short 3
            Foo 3             short 4             A longer text having more words
            """);
    assertBlock(input, 10, false,
        """
            Foo   A    short
            1   longer     1
                 text \s
                having\s
                 more \s
                words \s
            Foo short  short
            2     2        3
            Foo short       A
            3     4    longer
                         text
                       having
                         more
                        words
            """);
    assertBlock(input, 30, false,
        """
            Foo 1  A longer   short 1
                  text having\s
                  more words \s
            Foo 2   short 2   short 3
            Foo 3   short 4      A longer
                              text having
                               more words
            """);
    assertBlock(input, 30, true,
        """
            \033[0;3mFoo 1\033[0m  A longer   short 1
                  text having\s
                  more words \s
            \033[0;1mFoo 2\033[0m   short 2   short 3
            Foo 3   short 4      A longer
                              text having
                               more words
            """);
  }

  @Test
  void newline() {
    String input = """
        foo
        >
        bar
        """;
    assertBlock(input, -1, false,
        """
            foo
               \s
            bar
            """);
    assertBlock(input, 2, false,
        """
            foo
               \s
            bar
            """);
  }

  @Test
  void unimplementedInline() {
    String input = """
        &uuml;
        """;

    assertBlock(input, -1, false,
        """
            &uuml;
            """);
  }

  @Test
  void unimplementedBlock() {
    String input = """
        <html>
        """;

    assertBlock(input, -1, false,
        """
            <html>
            """);
  }

  static void assertBlock(String input, int requestedWidth, boolean withStyles, String expected) {
    Block block = FormatBuilder.buildFormat(input);
    block.setRequestedWidth(requestedWidth);
    StringBuilder buffer = new StringBuilder();
    block.appendTo(buffer, withStyles);
    assertThat(buffer).hasToString(expected);
  }
}
