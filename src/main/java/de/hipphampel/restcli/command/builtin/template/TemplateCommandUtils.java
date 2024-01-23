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
package de.hipphampel.restcli.command.builtin.template;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.option;
import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.cli.commandline.Validators;

public class TemplateCommandUtils {

  public static final Positional CMD_ARG_SOURCE = positional("<source>")
      .validator(Validators.TEMPLATE_ADDRESS_VALIDATOR)
      .build();
  public static final Option CMD_OPT_REPLACE = option("-r", "--replace")
      .build();
  public static final Positional CMD_ARG_ADDRESS = positional("<address>")
      .validator(Validators.TEMPLATE_ADDRESS_VALIDATOR)
      .build();
  public static final Positional CMD_ARG_DESCRIPTION = positional("<description>")
      .build();
  public static final Option CMD_OPT_DESCRIPTION = option("-d", "--description")
      .parameter(CMD_ARG_DESCRIPTION)
      .build();
  public static final Positional CMD_ARG_VARIABLE = positional("key=value")
      .validator(Validators.KEY_VALUE_VALIDATOR)
      .build();
  public static final Option CMD_OPT_VARIABLE = option("-v", "--variable")
      .repeatable()
      .parameter(CMD_ARG_VARIABLE)
      .build();
}
