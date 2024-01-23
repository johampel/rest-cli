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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.hipphampel.restcli.exception.ExecutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileUtilsTest {

  Path baseDir;

  @BeforeEach
  void beforeEach(@TempDir Path baseDir) {
    this.baseDir = baseDir;
  }

  @Test
  void createDirectoryIfNotExists_create() {
    Path dir = baseDir.resolve("new/dir");
    assertThat(Files.isDirectory(dir)).isFalse();

    assertThat(FileUtils.createDirectoryIfNotExists(dir)).isEqualTo(dir);
    assertThat(Files.isDirectory(dir)).isTrue();
  }

  @Test
  void createDirectoryIfNotExists_exists() throws IOException {
    Path dir = baseDir.resolve("new/dir");
    Files.createDirectories(dir);
    assertThat(Files.isDirectory(dir)).isTrue();

    assertThat(FileUtils.createDirectoryIfNotExists(dir)).isEqualTo(dir);
    assertThat(Files.isDirectory(dir)).isTrue();
  }

  @Test
  void createDirectoryIfNotExists_fail() throws IOException {
    Path dir = baseDir.resolve("dir");
    Files.writeString(dir, "I am a file");

    assertThatThrownBy(() -> FileUtils.createDirectoryIfNotExists(dir))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("\"" + baseDir + "/dir\" is not a directory.");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void deleteRecursively(boolean self) throws IOException {
    // Arrange
    Files.createDirectories(baseDir.resolve("test/dir/subdir"));
    Files.writeString(baseDir.resolve("test/a"), "foo");
    Files.writeString(baseDir.resolve("test/dir/a"), "foo");
    Files.writeString(baseDir.resolve("test/dir/subdir/a"), "foo");

    FileUtils.deleteRecursively(baseDir.resolve("test"), self);

    assertThat(Files.exists(baseDir.resolve("test"))).isEqualTo(!self);
    assertThat(Files.exists(baseDir.resolve("test/dir/subdir/foo"))).isFalse();
    assertThat(Files.exists(baseDir.resolve("test/dir/subdir"))).isFalse();
    assertThat(Files.exists(baseDir.resolve("test/dir/foo"))).isFalse();
    assertThat(Files.exists(baseDir.resolve("test/dir"))).isFalse();
    assertThat(Files.exists(baseDir.resolve("testfoo"))).isFalse();
  }

}
