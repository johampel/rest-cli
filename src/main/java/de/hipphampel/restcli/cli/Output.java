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
package de.hipphampel.restcli.cli;

import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.cli.format.FormatBuilder;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Objects;

public class Output {

  private final PrintWriter delegate;
  private final boolean withStyles;
  private final int outputWidth;

  public Output(PrintWriter printWriter, int outputWidth, boolean withStyles) {
    this.delegate = Objects.requireNonNull(printWriter);
    this.outputWidth = outputWidth;
    this.withStyles = withStyles;
  }

  public Output(PrintWriter printWriter) {
    this(printWriter, -1, true);
  }

  public Output(Writer writer) {
    this(new PrintWriter(writer, true));
  }

  public Output(PrintStream printStream) {
    this(new PrintWriter(printStream, true));
  }

  boolean withStyles() {
    return this.withStyles;
  }

  public Output withStyles(boolean withStyles) {
    return new Output(this.delegate, this.outputWidth, withStyles);
  }

  public int withOutputWidth() {
    return this.outputWidth;
  }

  public Output withOutputWidth(int outputWidth) {
    return new Output(this.delegate, outputWidth, this.withStyles);
  }

  public Output charsf(String format, Object... args) {
    delegate.printf(format, args);
    return this;
  }

  public Output chars(String chars) {
    delegate.print(chars);
    return this;
  }

  public Output linef(String format, Object... args) {
    delegate.printf(format + "%n", args);
    return this;
  }

  public Output line(String line) {
    delegate.println(line);
    return this;
  }

  public Output newline() {
    delegate.println();
    return this;
  }

  public Output markdownf(String format, Object... args) {
    return markdown(format.formatted(args));
  }

  public Output markdown(String markdown) {
    return block(FormatBuilder.buildFormat(markdown));
  }

  public Output block(Block block) {
    block.setRequestedWidth(this.outputWidth);
    int height = block.getHeight();
    for (int i = 0; i < height; i++) {
      delegate.println(block.getLine(i, withStyles));
    }
    return this;
  }

  public PrintWriter asWriter() {
    return delegate;
  }

}
