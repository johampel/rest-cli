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
package de.hipphampel.restcli.command.builtin.env;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.env.Environment;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.KeyValue;
import java.util.List;
import java.util.Map;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

public class EnvCommandUtils {

  public static final Positional CMD_ARG_NAME = positional("<name>")
      .build();
  public static final Positional CMD_ARG_SOURCE = positional("<source>")
      .build();
  public static final Option CMD_OPT_REPLACE = option("-r", "--replace")
      .build();
  public static final Option CMD_OPT_NO_PARENT = option("-n", "--no-parent")
      .exclusionGroup("parent")
      .build();
  public static final Positional CMD_ARG_JSON = positional("<key>=<json>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_JSON = option("-j", "--json")
      .repeatable()
      .parameter(CMD_ARG_JSON)
      .build();
  public static final Positional CMD_ARG_HEADER = positional("<header>=<value>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_HEADER = option("-h", "--header")
      .repeatable()
      .parameter(CMD_ARG_HEADER)
      .build();
  public static final Positional CMD_ARG_VALUE = positional("<key>=<value>")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_VALUE = option("-v", "--value")
      .repeatable()
      .parameter(CMD_ARG_VALUE)
      .build();


  public static final Positional CMD_ARG_PARENT = positional("<parent>")
      .build();

  public static final Option CMD_OPT_PARENT = option("-p", "--parent")
      .exclusionGroup("parent")
      .parameter(CMD_ARG_PARENT)
      .build();

  public static void assertEnvironmentName(String name) {
    if (name.isEmpty() || name.startsWith("_")) {
      throw new ExecutionException("\"%s\" is an invalid environment name - it must not start with a `_` sign.".formatted(name));
    }
  }

  public static void setHeaders(CommandContext context, Environment environment, List<String> headers) {
    List<KeyValue<String>> keyValues = headers.stream()
        .map(KeyValue::fromString)
        .toList();
    Map<String, List<String>> newHeaders = CommandUtils.fillHeaders(context, environment.getLocalHeaders(), keyValues, true);
    environment.setLocalHeaders(newHeaders);
  }

}
