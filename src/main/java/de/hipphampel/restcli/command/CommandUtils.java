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
package de.hipphampel.restcli.command;

import de.hipphampel.restcli.cli.format.Block;
import de.hipphampel.restcli.cli.format.FormatBuilder;
import de.hipphampel.restcli.cli.format.GridBlock;
import de.hipphampel.restcli.cli.format.GridBlock.Position;
import de.hipphampel.restcli.cli.format.ParagraphBlock;
import de.hipphampel.restcli.cli.format.ParagraphBlock.Alignment;
import de.hipphampel.restcli.cli.format.PreformattedBlock;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.io.InputStreamProvider.SupplierInputStreamProvider;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import de.hipphampel.restcli.rest.RequestContext;
import de.hipphampel.restcli.rest.RequestContext.OutputFormat;
import de.hipphampel.restcli.template.Template;
import de.hipphampel.restcli.template.TemplateAddress;
import de.hipphampel.restcli.template.TemplateModel;
import de.hipphampel.restcli.template.TemplateRepository;
import de.hipphampel.restcli.utils.CollectionUtils;
import de.hipphampel.restcli.utils.KeyValue;
import de.hipphampel.restcli.utils.Pair;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandUtils {

  public static String getQualifiedCommandName(CommandContext context, CommandAddress address) {
    if (address == null || address.isRoot()) {
      return context.applicationName();
    }

    return context.applicationName() + " " + address.toString().replaceAll("/", " ");
  }

  public static void showError(CommandContext context, String format, Object... args) {
    showError(context, new ParagraphBlock(Alignment.LEFT, format.formatted(args)));
  }

  public static void showError(CommandContext context, Block message) {
    context.err().block(new GridBlock(
        Map.of(
            new Position(0, 0), new PreformattedBlock("*** error %s: ".formatted(context.applicationName())),
            new Position(0, 1), message
        )));
  }

  public static void showWarning(CommandContext context, String format, Object... args) {
    context.err().block(new GridBlock(
        Map.of(
            new Position(0, 0), new PreformattedBlock("warning %s: ".formatted(context.applicationName())),
            new Position(0, 1), new ParagraphBlock(format.formatted(args))
        )));
  }

  public static Function<CommandContext, Block> helpSection(String template) {
    return context ->
        FormatBuilder.buildFormat(context.templateRenderer().render(template, createHelpTextTemplateModel(context)));
  }

  public static TemplateModel createHelpTextTemplateModel(CommandContext context) {
    return new TemplateModel(Map.of(
        "applicationName", context.applicationName()
    ));
  }

  public static TemplateModel templateModelOf(CommandContext context, Map<String, Object> values) {
    return new TemplateModel(values, context.apiFactory().createApi(context));
  }

  public static String toString(CommandContext context, InputStreamProviderConfig config, Map<String, Object> model) {
    try (InputStream in = createInputStreamProvider(context, config, model).open();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      return out.toString(StandardCharsets.UTF_8);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to read \"%s\": %s".formatted(config, ioe.getMessage()), ioe);
    }
  }

  public static InputStreamProvider createInputStreamProvider(CommandContext context, InputStreamProviderConfig config,
      Map<String, Object> model) {
    if (config == null) {
      return null;
    }
    try {
      InputStreamProvider provider = switch (config.type()) {
        case string -> InputStreamProvider.ofString(config.value());
        case url -> InputStreamProvider.ofURL(new URL(config.value()));
        case path -> InputStreamProvider.ofPath(Path.of(config.value()));
        case builtin -> InputStreamProvider.ofBuiltin(config.value());
        case stdin -> new SupplierInputStreamProvider(context::in);
      };
      if (config.interpolate()) {
        provider = InputStreamProvider.interpolated(provider, context.templateRenderer(), templateModelOf(context, model));
      }
      return provider;
    } catch (MalformedURLException mue) {
      throw new ExecutionException("\"%s\" is not a valid URL.".formatted(config.value()));
    }
  }

  public static Map<String, Object> fillVariables(CommandContext context, Map<String, Object> variables,
      List<KeyValue<?>> keyValues, boolean removeAllowed) {
    variables = new HashMap<>(variables);
    Set<String> alreadySeen = new HashSet<>();
    for (KeyValue<?> keyValue : keyValues) {
      String key = keyValue.key();
      if (!alreadySeen.add(key)) {
        showWarning(context, "Modifying variable \"%s\" more than once - using last modification.".formatted(key));
      }
      if (keyValue.hasValue()) {
        variables.put(key, keyValue.value());
      } else {
        if (!removeAllowed) {
          throw new ExecutionException("Removing variables (\"%s\") not allowed.".formatted(key));
        } else if (variables.remove(key) == null) {
          showWarning(context, "Removing not existing variable \"%s\" has no effect.".formatted(key));
        }
      }
    }
    return variables;
  }

  public static Map<String, List<String>> fillHeaders(CommandContext context, Map<String, List<String>> headers,
      List<KeyValue<String>> keyValues, boolean removeAllowed) {
    Map<String, List<String>> localHeaders = new HashMap<>();
    headers = new HashMap<>(headers);
    for (KeyValue<String> keyValue : keyValues) {
      String key = keyValue.key();
      if (keyValue.hasValue()) {
        localHeaders.computeIfAbsent(key, ignore -> new ArrayList<>()).add(keyValue.value());
      } else {
        if (!removeAllowed) {
          throw new ExecutionException("Removing headers (\"%s\") not allowed.".formatted(key));
        } else if (headers.remove(key) == null) {
          showWarning(context, "Removing not existing header \"%s\" has no effect.".formatted(key));
        }
        localHeaders.remove(key);
      }
    }

    return CollectionUtils.mergeHeaders(headers, localHeaders);
  }

  public static RequestContext createRequestContext(CommandContext context, TemplateRepository templateRepository,
      Map<String, Object> variables, Set<String> declaredVariables) {
    Map<String, Object> effectiveVariables = new HashMap<>(context.environment().getVariables());
    effectiveVariables.putAll(variables);
    Set<String> effectiveDeclaredVariables = new HashSet<>(context.environment().getVariables().keySet());
    effectiveDeclaredVariables.addAll(declaredVariables);
    OutputFormat format = createOutputFormat(context, templateRepository, effectiveVariables);
    return new RequestContext(
        context.httpClient(),
        templateModelOf(context, effectiveVariables),
        effectiveDeclaredVariables,
        format,
        context.out(),
        context.err());
  }

  static OutputFormat createOutputFormat(CommandContext context, TemplateRepository templateRepository, Map<String, Object> variables) {
    if (context.rootCommandLine().hasOption(CommandContext.CMD_OPT_FORMAT)) {
      return createOutputFormatForFormatOption(context, variables);
    } else {
      return createOutputFormatForTemplateOption(context, templateRepository);
    }
  }

  static OutputFormat createOutputFormatForTemplateOption(CommandContext context, TemplateRepository templateRepository) {
    String nameOrAddress = context.rootCommandLine().getValue(CommandContext.CMD_ARG_TEMPLATE)
        .orElseGet(() -> context.applicationConfig().getOutputTemplate());
    TemplateAddress address;
    if (TemplateAddress.isValueTemplateAddress(nameOrAddress)) {
      address = TemplateAddress.fromString(nameOrAddress);
    } else {
      address = templateRepository.getEffectiveAddress(context.configPath(), context.commandAddress(), nameOrAddress)
          .orElseThrow(() -> new ExecutionException("Unable to find matching template for name \"%s\".".formatted(nameOrAddress)));
    }
    Template template = templateRepository.getTemplate(context.configPath(), address)
        .orElseThrow(() -> new ExecutionException("Failed to load template for address \"%s\".".formatted(address)));

    Map<String, String> parameters = template.config().parameters().entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().defaultValue()));
    for (Map.Entry<String, String> overwrites : collectParameters(context).entrySet()) {
      String key = overwrites.getKey();
      if (!parameters.containsKey(key)) {
        showWarning(context, "Template \"%s\" does not define output parameter \"%s\".", address, key);
      }
      parameters.put(key, overwrites.getValue());
    }

    return new OutputFormat(
        template.toInputStreamProvider(),
        parameters);
  }

  static OutputFormat createOutputFormatForFormatOption(CommandContext context, Map<String, Object> variables) {
    InputStreamProviderConfig config = context.rootCommandLine().getValue(CommandContext.CMD_ARG_FORMAT)
        .map(InputStreamProviderConfig::fromString)
        .orElseThrow();
    Map<String, String> parameters = collectParameters(context);
    return new OutputFormat(createInputStreamProvider(context, config, variables), parameters);
  }

  static Map<String, String> collectParameters(CommandContext context) {
    return context.rootCommandLine().getValues(CommandContext.CMD_ARG_OUTPUT_PARAMETER).stream()
        .map(Pair::fromString)
        .collect(Collectors.groupingBy(Pair::first))
        .values().stream()
        .map(values -> {
          if (values.size() > 1) {
            showWarning(context, "Output parameter \"%s\" defined more than once, taking last definition.", values.get(0).first());
          }
          return values.get(values.size() - 1);
        })
        .collect(Collectors.toMap(
            Pair::first,
            Pair::second));
  }
}
