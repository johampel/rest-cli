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
package de.hipphampel.restcli.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class ApplicationConfigRepositoryTest {

  @Inject
  ApplicationConfigRepository repository;

  @Inject
  ObjectMapper objectMapper;

  Path baseDir;

  @BeforeEach
  void beforeEach(@TempDir Path baseDir) {
    this.baseDir = baseDir;
  }

  @Test
  void getOrCreate_new() {
    assertThat(baseDir.resolve(ApplicationConfigRepository.NAME)).doesNotExist();

    ApplicationConfig config = repository.getOrCreate(baseDir);
    assertThat(config).isNotNull();

    assertThat(baseDir.resolve(ApplicationConfigRepository.NAME)).exists();
  }

  @Test
  void getOrCreate_existing() throws IOException {
    ApplicationConfig original = new ApplicationConfig();
    original.setOutputWidth(original.getOutputWidth() - 1); //  There is no int with  x == x - 1, so value is changed for sure!
    objectMapper.writeValue(baseDir.resolve(ApplicationConfigRepository.NAME).toFile(), original);

    assertThat(baseDir.resolve(ApplicationConfigRepository.NAME)).exists();
    assertThat(repository.getOrCreate(baseDir)).isEqualTo(original);
  }

  @Test
  void store() {
    ApplicationConfig original = new ApplicationConfig();
    original.setOutputWidth(original.getOutputWidth() - 1); //  There is no int with  x == x - 1, so value is changed for sure!

    repository.store(baseDir, original);

    assertThat(baseDir.resolve(ApplicationConfigRepository.NAME)).exists();
    assertThat(repository.getOrCreate(baseDir)).isEqualTo(original);
  }
}
