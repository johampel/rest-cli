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
package de.hipphampel.restcli.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.template.TemplateConfig.Parameter;
import de.hipphampel.restcli.utils.FileUtils;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class TemplateRepositoryTest {

  @Inject
  ObjectMapper objectMapper;
  @Inject
  TemplateRepository repository;
  Path rootDir;
  Path templatesDir;

  final Template templateAbc = new Template(
      TemplateAddress.fromString("abc@"),
      new TemplateConfig("abc description", Map.of("abc", new Parameter("123")), "content abc"),
      false);
  final Template templateDef = new Template(
      TemplateAddress.fromString("def@"),
      new TemplateConfig("def description", Map.of("def", new Parameter("456")), "content def"),
      false);
  final Template templateXyzDef = new Template(
      TemplateAddress.fromString("def@xyz"),
      new TemplateConfig("xyz/def description", Map.of("xyzdef", new Parameter("789")), "content xyzdef"),
      false);
  final Template templateXyzGhi = new Template(
      TemplateAddress.fromString("ghi@xyz"),
      new TemplateConfig("xyz/ghi description", Map.of("xyzghi", new Parameter("012")), "content xyzghi"),
      false);
  final Template templateXyzAbcJkl = new Template(
      TemplateAddress.fromString("jkl@xyz/abc"),
      new TemplateConfig("xyz/abc/jkl description", Map.of("xyzabcjkl", new Parameter("345")), "content xyzabcjkl"),
      false);

  @BeforeEach
  void beforeEach(@TempDir Path rootDir) throws IOException {
    this.rootDir = rootDir;
    this.templatesDir = rootDir.resolve("templates");
    for (Template template : List.of(templateAbc, templateDef, templateXyzDef, templateXyzGhi, templateXyzAbcJkl)) {
      FileUtils.createDirectoryIfNotExists(templatesDir.resolve(template.command().toPath()));
      objectMapper.writeValue(templatesDir.resolve(template.command().toPath()).resolve(template.name() + ".json").toFile(),
          template.config());
    }
  }

  @Test
  void getAllTemplates() {
    assertThat(repository.getAllTemplates(rootDir)).isEqualTo(List.of(
        TemplateAddress.fromString("abc@"),
        TemplateAddress.fromString("def@"),
        TemplateAddress.fromString("def@xyz"),
        TemplateAddress.fromString("ghi@xyz"),
        TemplateAddress.fromString("jkl@xyz/abc")
    ));
  }

  @Test
  void storeTemplate_ok_new() throws IOException {
    Template template = new Template(
        TemplateAddress.fromString("template@some/new"),
        new TemplateConfig("test", Map.of("foo", new Parameter("bar")), "content"),
        false);

    repository.storeTemplate(rootDir, template, false);

    Path path = templatesDir.resolve(template.command().toPath()).resolve(template.name() + ".json");

    assertThat(path).isRegularFile();
    assertThat(objectMapper.readValue(path.toFile(), TemplateConfig.class)).isEqualTo(template.config());
  }

  @Test
  void storeTemplate_ok_replace() throws IOException {
    Template template = new Template(
        templateXyzGhi.address(),
        new TemplateConfig("test", Map.of("foo", new Parameter("bar")), "content"),
        false);

    repository.storeTemplate(rootDir, template, true);

    Path path = templatesDir.resolve(template.command().toPath()).resolve(template.name() + ".json");

    assertThat(path).isRegularFile();
    assertThat(objectMapper.readValue(path.toFile(), TemplateConfig.class)).isEqualTo(template.config());
  }

  @Test
  void storeTemplate_fail() throws IOException {
    Template template = new Template(
        templateXyzGhi.address(),
        new TemplateConfig("test", Map.of("foo", new Parameter("bar")), "content"),
        false);

    assertThatThrownBy(() -> repository.storeTemplate(rootDir, template, false))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Template \"ghi\" for command \"xyz\" already exists.");
  }

  @Test
  void getTemplate_ok() {
    assertThat(repository.getTemplate(rootDir, templateXyzAbcJkl.address())).contains(templateXyzAbcJkl);
  }

  @Test
  void getTemplate_builtin() throws IOException {
    assertThat(repository.getTemplate(rootDir, TemplateAddress.fromString("default@"))).contains(
        new Template(
            TemplateAddress.fromString("default@"),
            objectMapper.readValue(TemplateRepository.class.getResource("/templates/default.json"), TemplateConfig.class),
            true)
    );
  }

  @Test
  void getTemplate_notFound() {
    assertThat(repository.getTemplate(rootDir, TemplateAddress.fromString("not@found"))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
      "default@,   true",
      "ghi@xyz,   true",
      "not@found, false"
  })
  void existsTemplate(String address, boolean expected) {
    assertThat(repository.existsTemplate(rootDir, TemplateAddress.fromString(address))).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'not/found', ",
      "'',          'abc@;def@;default@'",
      "'xyz',       'def@xyz;ghi@xyz'",
      "'xyz/abc',   'jkl@xyz/abc'"
  })
  void getTemplatesForCommand(String command, String expected) {
    assertThat(repository.getTemplatesForCommand(rootDir, CommandAddress.fromString(command)))
        .isEqualTo(TestUtils.stringToList(expected).stream()
            .map(TemplateAddress::fromString)
            .toList());
  }

  @ParameterizedTest
  @CsvSource({
      "'not/found', ",
      "'',          'abc@;def@;default@'",
      "'xyz',       'abc@;def@xyz;default@;ghi@xyz'",
      "'xyz/abc',   'abc@;def@xyz;default@;ghi@xyz;jkl@xyz/abc'"
  })
  void getEffectiveTemplates(String command, String expected) {
    assertThat(repository.getEffectiveTemplates(rootDir, CommandAddress.fromString(command)))
        .isEqualTo(TestUtils.stringToList(expected).stream()
            .map(TemplateAddress::fromString)
            .toList());
  }


  @Test
  void deleteTemplate_ok() {
    assertThat(repository.deleteTemplate(rootDir, templateXyzDef.address())).isTrue();
    assertThat(repository.existsTemplate(rootDir, templateXyzDef.address())).isFalse();
  }

  @Test
  void deleteTemplate_notFound() {
    TemplateAddress notFound = TemplateAddress.fromString("not@found");
    assertThat(repository.deleteTemplate(rootDir, notFound)).isFalse();
    assertThat(repository.existsTemplate(rootDir, notFound)).isFalse();
  }

  @Test
  void deleteTemplatesForCommand_all() throws IOException {
    repository.deleteTemplatesForCommand(rootDir, CommandAddress.ROOT, true);
    assertThat(templatesDir).exists();
    assertThat(Files.list(templatesDir)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void deleteTemplatesForCommand_command(boolean recursive) throws IOException {
    repository.deleteTemplatesForCommand(rootDir, CommandAddress.fromString("xyz"), recursive);

    assertThat(repository.existsTemplate(rootDir, templateAbc.address())).isTrue();
    assertThat(repository.existsTemplate(rootDir, templateDef.address())).isTrue();
    assertThat(repository.existsTemplate(rootDir, templateXyzDef.address())).isFalse();
    assertThat(repository.existsTemplate(rootDir, templateXyzGhi.address())).isFalse();
    assertThat(repository.existsTemplate(rootDir, templateXyzAbcJkl.address())).isEqualTo(!recursive);
  }

  @ParameterizedTest
  @CsvSource({
      "abc@,        abc@",
      "def@,        def@",
      "ghi@,        ",
      "jkl@,        ",
      "abc@xyz,     abc@",
      "def@xyz,     def@xyz",
      "ghi@xyz,     ghi@xyz",
      "jkl@xyz,     ",
      "abc@xyz/abc, abc@",
      "def@xyz/abc, def@xyz",
      "ghi@xyz/abc, ghi@xyz",
      "jkl@xyz/abc, jkl@xyz/abc"
  })
  void getEffectiveAddress_address(String in, String expected) {
    assertThat(repository.getEffectiveAddress(rootDir, TemplateAddress.fromString(in))).isEqualTo(
        Optional.ofNullable(expected).map(TemplateAddress::fromString));
  }

  @ParameterizedTest
  @CsvSource({
      "'',        abc, abc@",
      "'',        def, def@",
      "'',        ghi, ",
      "'',        jkl, ",
      "'xyz',     abc, abc@",
      "'xyz',     def, def@xyz",
      "'xyz',     ghi, ghi@xyz",
      "'xyz',     jkl, ",
      "'xyz/abc', abc, abc@",
      "'xyz/abc', def, def@xyz",
      "'xyz/abc', ghi, ghi@xyz",
      "'xyz/abc', jkl, jkl@xyz/abc"
  })
  void getEffectiveAddress_name(String command, String name, String expected) {
    assertThat(repository.getEffectiveAddress(rootDir, CommandAddress.fromString(command), name)).isEqualTo(
        Optional.ofNullable(expected).map(TemplateAddress::fromString));
  }
}
