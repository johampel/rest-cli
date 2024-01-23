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

import de.hipphampel.restcli.exception.ExecutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileUtils {

  private FileUtils() {
  }

  public static Path createDirectoryIfNotExists(Path dir) {
    if (!Files.exists(dir)) {
      try {
        Files.createDirectories(dir);
      } catch (IOException ioe) {
        throw new ExecutionException("Failed to create directory \"%s\": %s".formatted(dir.toAbsolutePath(), ioe.getMessage()));
      }
    } else if (!Files.isDirectory(dir)) {
      throw new ExecutionException("\"%s\" is not a directory.".formatted(dir.toAbsolutePath()));
    }
    return dir;
  }

  public static void deleteRecursively(Path path, boolean deleteSelf) {
    if (Files.isDirectory(path)) {
      try (Stream<Path> children = Files.list(path)) {
        children.forEach(child -> deleteRecursively(child, true));
        if (deleteSelf) {
          Files.delete(path);
        }
      } catch (IOException ioe) {
        throw new ExecutionException("Failed to delete directory \"%s\"".formatted(path), ioe);
      }
    } else if (deleteSelf) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException ioe) {
        throw new ExecutionException("Failed to delete file \"%s\"".formatted(path), ioe);
      }
    }
  }

}
