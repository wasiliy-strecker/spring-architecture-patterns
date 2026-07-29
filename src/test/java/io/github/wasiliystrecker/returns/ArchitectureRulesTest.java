package io.github.wasiliystrecker.returns;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

  @Test
  void domainAndApplicationLayersStayFrameworkIndependent() {
    var productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.wasiliystrecker.returns");

    noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "jakarta.servlet..")
        .because("domain and application code must remain framework-independent")
        .allowEmptyShould(true)
        .check(productionClasses);
  }
}
