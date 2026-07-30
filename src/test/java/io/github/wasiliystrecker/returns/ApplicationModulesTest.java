package io.github.wasiliystrecker.returns;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ApplicationModulesTest {

  private static final ApplicationModules MODULES =
      ApplicationModules.of(SpringArchitecturePatternsApplication.class);

  @Test
  void verifiesModuleBoundaries() {
    MODULES.verify();

    assertThat(MODULES.stream().map(module -> module.getIdentifier().toString()))
        .containsExactlyInAnyOrder(
            "intake", "inspection", "resolution", "refund", "query", "operations");
  }

  @Test
  void generatesArchitectureDocumentation() {
    new Documenter(MODULES)
        .writeModulesAsPlantUml()
        .writeIndividualModulesAsPlantUml()
        .writeModuleCanvases();
  }
}
