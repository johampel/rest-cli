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
package de.hipphampel.restcli.io;

import de.hipphampel.restcli.exception.ExecutionException;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public sealed interface ClosableInputStreamProvider extends Openable, Closeable {

  static ClosableInputStreamProvider ofStream(InputStream stream, int maxMemorySize) {
    try {
      return new InputStreamWrapper(stream, maxMemorySize);
    } catch (IOException e) {
      throw new ExecutionException("Failed to open stream for reading", e);
    }
  }

  final class InputStreamWrapper implements ClosableInputStreamProvider {

    private final byte[] byteArray;
    private final Path file;

    public InputStreamWrapper(InputStream stream, int maxMemorySize) throws IOException {


      byte[] buffer = new byte[maxMemorySize + 1];
      int len = 0;
      int count = 0;
      do {
        count = stream.read(buffer, len, buffer.length - len);
        len += Math.max(0, count);
      }
      while (count != -1 && len < buffer.length);

      if (len == buffer.length) {
        this.byteArray = null;
        this.file = Files.createTempFile("rest-cli", "tmp");
        try (OutputStream fileOut = new FileOutputStream(file.toFile())) {
          fileOut.write(buffer);
          stream.transferTo(fileOut);
        }
      } else {
        this.file = null;

        this.byteArray = new byte[Math.max(0, len)];
        System.arraycopy(buffer, 0, byteArray, 0, Math.max(0, len));
      }
    }

    @Override
    public InputStream open() {
      try {
        if (this.byteArray != null) {
          return new ByteArrayInputStream(this.byteArray);
        } else {
          return new FileInputStream(this.file.toFile());
        }
      } catch (IOException ioe) {
        throw new ExecutionException("Failed to open stream", ioe);
      }
    }

    @Override
    public void close() {
      if (file != null) {
        try {
          Files.delete(file);
        } catch (IOException e) {
          // Silently discard exception
        }
      }
    }
  }

}
