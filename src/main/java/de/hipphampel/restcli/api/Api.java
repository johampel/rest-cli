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
package de.hipphampel.restcli.api;


import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_FORMAT;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_OUTPUT_PARAMETER;
import static de.hipphampel.restcli.command.CommandContext.CMD_ARG_TEMPLATE;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_FORMAT;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_OUTPUT_PARAMETER;
import static de.hipphampel.restcli.command.CommandContext.CMD_OPT_TEMPLATE;
import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND;
import static de.hipphampel.restcli.command.ParentCommand.CMD_ARG_SUB_COMMAND_ARGS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineParser;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.rest.BodyAndHeaders;
import de.hipphampel.restcli.utils.KeyValue;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

@RegisterForReflection
public class Api {

  private final CommandContext context;
  private final ObjectMapper objectMapper;
  private final Transformer transformer;
  private final CommandLineParser commandLineParser;

  public Api(CommandContext context, ObjectMapper objectMapper, Transformer transformer, CommandLineParser commandLineParser) {
    this.context = Objects.requireNonNull(context);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.transformer = Objects.requireNonNull(transformer);
    this.commandLineParser = Objects.requireNonNull(commandLineParser);
  }


  public String beautify(BodyAndHeaders<?> body) throws IOException {
    // First try JSON
    try {
      return beautifyJson(body.getJsonBody());
    } catch (Exception e) {
      // Fall through...
    }

    // Next try XML
    try {
      return beautifyXml(body.getXmlBody());
    } catch (Exception e) {
      // Fall through...
    }
    return body.getStringBody();
  }

  String beautifyJson(Object value) throws JsonProcessingException {
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
  }

  String beautifyXml(Document value) throws TransformerException, IOException {
    try (StringWriter out = new StringWriter()) {
      transformer.transform(new DOMSource(value), new StreamResult(out));
      return out.toString();
    }
  }

  public Object jq(Object value, String path) {
    return JsonPath.read(value, path);
  }

  public String url_encode(String str) {
    return URLEncoder.encode(str, StandardCharsets.UTF_8);
  }

  public String sh(String... args) throws IOException, InterruptedException {
    Process process = new ProcessBuilder(args).start();
    String stdout;
    String stderr;
    try (InputStream in = process.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      stdout = out.toString(StandardCharsets.UTF_8);
    }
    try (InputStream in = process.getErrorStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      stderr = out.toString(StandardCharsets.UTF_8);
    }
    if (process.waitFor() != 0) {
      throw new ExecutionException("Command %s failed: %s".formatted(String.join(" ", args), stderr));
    }
    return stdout;
  }


  public String call(String... args) throws IOException {
    try (StringWriter out = new StringWriter(); StringWriter err = new StringWriter()) {
      CommandContext aliasContext = new CommandContext(context)
          .out(new Output(out).withOutputWidth(-1).withStyles(false))
          .err(new Output(err).withOutputWidth(-1).withStyles(false));

      CommandLineSpec aliasCommandLineSpec = new CommandLineSpec(false, CMD_OPT_FORMAT, CMD_OPT_TEMPLATE, CMD_OPT_OUTPUT_PARAMETER,
          CMD_ARG_SUB_COMMAND);
      CommandLine aliasCommandLine = commandLineParser.parseCommandLine(aliasCommandLineSpec, Arrays.asList(args));
      mergeOutputOptions(context.rootCommandLine(), aliasCommandLine);
      aliasContext.rootCommandLine(aliasCommandLine);

      String subCommand = aliasCommandLine.getValue(CMD_ARG_SUB_COMMAND)
          .orElseThrow(() -> new ExecutionException("No sub command in alias."));
      List<String> subCommandArgs = aliasCommandLine.getValues(CMD_ARG_SUB_COMMAND_ARGS);
      if (!context.commandInvoker().invokeCommand(aliasContext, CommandAddress.ROOT.child(subCommand), subCommandArgs)) {
        throw new ExecutionException("sub command failed: %s.".formatted(err.toString()));
      }

      return out.toString();
    }
  }

  void mergeOutputOptions(CommandLine rootCommandLine, CommandLine aliasCommandLine) {
    if (!aliasCommandLine.hasOption(CMD_OPT_FORMAT) && !aliasCommandLine.hasOption(CMD_OPT_TEMPLATE)) {
      if (rootCommandLine.hasOption(CMD_OPT_FORMAT)) {
        aliasCommandLine.addOption(CMD_OPT_FORMAT);
        aliasCommandLine.addValues(CMD_ARG_FORMAT, rootCommandLine.getValues(CMD_ARG_FORMAT));
      }
      if (rootCommandLine.hasOption(CMD_OPT_TEMPLATE)) {
        aliasCommandLine.addOption(CMD_OPT_TEMPLATE);
        aliasCommandLine.addValues(CMD_ARG_TEMPLATE, rootCommandLine.getValues(CMD_ARG_TEMPLATE));
      }
    }

    List<String> mergedParameters = Stream.concat(rootCommandLine.getValues(CMD_ARG_OUTPUT_PARAMETER).stream(),
            aliasCommandLine.getValues(CMD_ARG_OUTPUT_PARAMETER).stream())
        .collect(Collectors.groupingBy(kv -> KeyValue.fromString(kv).key()))
        .values().stream()
        .map(list -> list.get(list.size() - 1))
        .toList();
    if (!mergedParameters.isEmpty()) {
      aliasCommandLine.addOption(CMD_OPT_OUTPUT_PARAMETER);
      aliasCommandLine.setValues(CMD_ARG_OUTPUT_PARAMETER, mergedParameters);
    }
  }

}
