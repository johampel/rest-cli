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


import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class ApplicationConfigRepository {

  static final String NAME = "application-config.json";

  @Inject
  ObjectMapper objectMapper;

  public void store(Path rootDir, ApplicationConfig config) {
    FileUtils.createDirectoryIfNotExists(rootDir);
    try {
      objectMapper.writeValue(rootDir.resolve(NAME).toFile(), config);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to write application configuration: " + ioe.getMessage());
    }
  }

  public ApplicationConfig getOrCreate(Path rootDir) {
    Path path = rootDir.resolve(NAME);
    try {
      if (Files.exists(path)) {
        return objectMapper.readValue(path.toFile(), ApplicationConfig.class);
      } else {
        ApplicationConfig config = new ApplicationConfig();
        store(rootDir, config);
        return config;
      }
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to read application configuration: " + ioe.getMessage());
    }
  }
}
