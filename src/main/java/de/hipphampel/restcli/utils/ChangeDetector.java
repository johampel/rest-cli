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
package de.hipphampel.restcli.utils;

import java.util.Objects;

public class ChangeDetector<T> {

  private final T original;
  private T current;

  public ChangeDetector(T original) {
    this.original = original;
    this.current = original;
  }

  public T getOriginal() {
    return original;
  }

  public T get() {
    return current;
  }

  public void set(T current) {
    this.current = current;
  }

  public boolean isChanged() {
    return !Objects.equals(this.current, this.original);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChangeDetector<?> that = (ChangeDetector<?>) o;
    return Objects.equals(current, that.current);
  }

  @Override
  public int hashCode() {
    return Objects.hash(current);
  }

  @Override
  public String toString() {
    return "ChangeDetector{" +
        "current=" + current +
        '}';
  }
}
