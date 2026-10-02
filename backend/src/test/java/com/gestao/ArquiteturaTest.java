package com.gestao;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * A arquitetura como teste: se alguém quebrar uma fronteira, o build falha.
 *
 * <ul>
 *   <li>Módulos (Spring Modulith): sem dependência circular, e um módulo só usa o que o
 *       outro expõe como interface nomeada (domínio, aplicação, DTOs, segurança).</li>
 *   <li>Camadas (Clean Architecture): as dependências apontam para dentro. O domínio não
 *       conhece aplicação nem infraestrutura; a aplicação não conhece a infraestrutura nem a web.</li>
 * </ul>
 */
class ArquiteturaTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.gestao");

    @Test
    void modulosRespeitamAsFronteiras() {
        ApplicationModules.of(GestaoApplication.class).verify();
    }

    @Test
    void dominioNaoDependeDeAplicacaoNemDeInfraestrutura() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage("..aplicacao..", "..infraestrutura..")
                .check(CLASSES);
    }

    @Test
    void aplicacaoNaoDependeDeInfraestrutura() {
        noClasses().that().resideInAPackage("..aplicacao..")
                .should().dependOnClassesThat().resideInAPackage("..infraestrutura..")
                .check(CLASSES);
    }

    @Test
    void dominioEAplicacaoNaoConhecemWebNemSpringData() {
        noClasses().that().resideInAnyPackage("..dominio..", "..aplicacao..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..", "org.springframework.http..", "jakarta.servlet..",
                        "org.springframework.data..", "org.springframework.security..",
                        "org.springframework.messaging..")
                .check(CLASSES);
    }
}
