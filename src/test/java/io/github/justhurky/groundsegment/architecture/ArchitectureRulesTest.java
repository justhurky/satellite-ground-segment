package io.github.justhurky.groundsegment.architecture;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureRulesTest {

    private static final String ROOT_PACKAGE = "io.github.justhurky.groundsegment";

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    void featureSlicesAreFreeOfCycles() {
        SlicesRuleDefinition.slices()
                .matching(ROOT_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .check(productionClasses);
    }

    @Test
    void controllersAreWebEntryPointsOnly() {
        ArchRuleDefinition.noClasses()
                .that().haveSimpleNameNotEndingWith("Controller")
                .should().accessClassesThat().haveSimpleNameEndingWith("Controller")
                .because("controllers are driven by the web layer and must not be called from inside")
                .check(productionClasses);
    }

    @Test
    void controllersReachPersistenceOnlyThroughServices() {
        ArchRuleDefinition.noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().accessClassesThat().areAssignableTo(Repository.class)
                .because("persistence access belongs behind a transactional service")
                .check(productionClasses);
    }

    @Test
    void entitiesStayOutOfTheApiLayer() {
        ArchRuleDefinition.classes()
                .that().areAnnotatedWith(Entity.class)
                .should().resideOutsideOfPackages(ROOT_PACKAGE + "..dto", ROOT_PACKAGE + ".config")
                .because("persistence entities are internal to their feature package")
                .check(productionClasses);
    }

    @Test
    void apiPayloadsAreRecords() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(ROOT_PACKAGE + "..dto")
                .should().beRecords()
                .because("request and response payloads are immutable value types")
                .check(productionClasses);
    }

    @Test
    void servicesAreNotWebComponents() {
        ArchRuleDefinition.noClasses()
                .that().areAnnotatedWith(Service.class)
                .should().beAnnotatedWith(RestController.class)
                .check(productionClasses);
    }

    @Test
    void collaboratorsAreInjectedThroughConstructors() {
        ArchRuleDefinition.noFields()
                .should().beAnnotatedWith(Autowired.class)
                .because("field injection hides dependencies and breaks immutability")
                .check(productionClasses);
    }

    @Test
    void nothingWritesToStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(productionClasses);
    }

    @Test
    void loggingGoesThroughSlf4j() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(productionClasses);
    }

    @Test
    void cryptographicPrimitivesStayInTheirOwningPackages() {
        ArchRuleDefinition.noClasses()
                .that().resideOutsideOfPackages(ROOT_PACKAGE + ".crypto", ROOT_PACKAGE + ".identity")
                .should().accessClassesThat().resideInAPackage("javax.crypto..")
                .because("key handling and primitives are owned by the crypto package")
                .check(productionClasses);
    }
}
