package com.yonglun.itineraryassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.yonglun.itineraryassistant", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dtosAreRecords = classes()
            .that().resideInAPackage("..dto..")
            .should().beAssignableTo(Record.class);

    @ArchTest
    static final ArchRule servicesDoNotDependOnControllers = noClasses()
            .that().resideInAnyPackage("..service..", "..repository..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule objectMapperOnlyBuiltInConfig = noClasses()
            .that().resideOutsideOfPackage("..config..")
            .should().callConstructor(ObjectMapper.class);
}
