package de.hipphampel.restcli.command.builtin;

import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class ApplicationCommandTest extends CommandTestBase {

  @Inject
  ApplicationCommand command;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }


  @Test
  void printVersion() {
    assertExecution(command, List.of("--version"), true,
        CommandUtils.toString(context, InputStreamProviderConfig.fromString("builtin:/version.txt"), Map.of()) + "\n",
        "");

  }
}
