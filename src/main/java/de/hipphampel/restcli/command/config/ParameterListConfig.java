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
package de.hipphampel.restcli.command.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Parameter;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Parameters;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RegisterForReflection
public class ParameterListConfig {

  private List<ParameterConfig> parameters;
  private Map<ParameterConfig, Positional> positionalMap;
  private Map<Parameter, ParameterConfig> parameterToConfigs;

  private CommandLineSpec commandLineSpec;

  public ParameterListConfig() {
    this(List.of());
  }

  public ParameterListConfig(List<ParameterConfig> parameterConfigs) {
    this.parameters = new ArrayList<>(parameterConfigs);
    recalculateCommandLine();
  }

  public List<ParameterConfig> getParameters() {
    return Collections.unmodifiableList(parameters);
  }

  public void setParameters(List<ParameterConfig> parameters) {
    this.parameters = Objects.requireNonNull(parameters);
    recalculateCommandLine();
  }

  @JsonIgnore
  public Map<Parameter, ParameterConfig> getConfigMap() {
    return Collections.unmodifiableMap(parameterToConfigs);
  }

  @JsonIgnore
  public Map<ParameterConfig, Positional> getPositionalMap() {
    return Collections.unmodifiableMap(positionalMap);
  }

  @JsonIgnore
  public CommandLineSpec getCommandLineSpec() {
    return commandLineSpec;
  }

  void recalculateCommandLine() {
    this.positionalMap = new HashMap<>();
    this.parameterToConfigs = new HashMap<>();
    List<Parameter> parameters = new ArrayList<>();
    Set<String> variables = new HashSet<>();
    for (ParameterConfig parameterConfig : this.parameters) {
      if (!variables.add(parameterConfig.variable())) {
        throw new ExecutionException("Variable \"%s\" bound twice in parameter list.".formatted(parameterConfig.variable()));
      }
      Parameter parameter = parameterConfig.toParameter();
      parameters.add(parameter);
      if (parameterConfig.style().positional) {
        positionalMap.put(parameterConfig, (Positional) parameter);
      } else {
        positionalMap.put(parameterConfig, ((Option) parameter).params().positionals().get(0));
      }
      parameterToConfigs.put(parameter, parameterConfig);
    }
    try {
      this.commandLineSpec = new CommandLineSpec(true, new Parameters(parameters));
    } catch (IllegalArgumentException iae) {
      throw new ExecutionException(iae.getMessage());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ParameterListConfig that = (ParameterListConfig) o;
    return Objects.equals(parameters, that.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameters);
  }

  @Override
  public String toString() {
    return "ParameterListConfig{" +
        "parameters=" + parameters +
        '}';
  }
}
