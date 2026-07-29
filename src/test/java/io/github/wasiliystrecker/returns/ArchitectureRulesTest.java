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

  @Test
  void internalDependenciesPointTowardTheDomain() {
    var productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.wasiliystrecker.returns");

    noClasses()
        .that()
        .resideInAnyPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..application..", "..adapter..")
        .because("the domain is the innermost layer")
        .allowEmptyShould(true)
        .check(productionClasses);

    noClasses()
        .that()
        .resideInAnyPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..adapter..")
        .because("application ports must not depend on their adapters")
        .allowEmptyShould(true)
        .check(productionClasses);
  }
}
