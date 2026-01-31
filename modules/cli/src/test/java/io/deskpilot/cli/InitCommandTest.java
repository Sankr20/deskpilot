package io.deskpilot.cli;

import org.junit.jupiter.api.Test;


import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class InitCommandTest {

    @Test
    void init_creates_expected_files_junit5_default_package() throws Exception {
        Path dir = Files.createTempDirectory("deskpilot-init-");
        // create a subfolder so temp dir itself is not polluted
        Path projectDir = dir.resolve("proj");

        new InitCommand().run(new String[] {
                projectDir.toString(),
                "junit5"
                // no package -> should use default com.example.deskpilot
        });

        assertTrue(Files.exists(projectDir.resolve("pom.xml")), "pom.xml should exist");
        assertTrue(Files.exists(projectDir.resolve(".gitignore")), ".gitignore should exist");
        assertTrue(Files.exists(projectDir.resolve("README.md")), "README.md should exist");

        Path testFile = projectDir.resolve("src/test/java/com/example/deskpilot/ExampleDeskPilotTest.java");
        assertTrue(Files.exists(testFile), "ExampleDeskPilotTest.java should exist at default package path");

        String testSrc = Files.readString(testFile);
        assertTrue(testSrc.contains("package com.example.deskpilot;"), "Test should declare default package");
    }

    @Test
    void init_creates_expected_files_testng_custom_package() throws Exception {
        Path dir = Files.createTempDirectory("deskpilot-init-");
        Path projectDir = dir.resolve("proj");

        new InitCommand().run(new String[] {
                projectDir.toString(),
                "testng",
                "com.myco.deskpilot"
        });

        assertTrue(Files.exists(projectDir.resolve("pom.xml")), "pom.xml should exist");
        assertTrue(Files.exists(projectDir.resolve(".gitignore")), ".gitignore should exist");
        assertTrue(Files.exists(projectDir.resolve("README.md")), "README.md should exist");

        Path testFile = projectDir.resolve("src/test/java/com/myco/deskpilot/ExampleDeskPilotTest.java");
        assertTrue(Files.exists(testFile), "ExampleDeskPilotTest.java should exist at custom package path");

        String testSrc = Files.readString(testFile);
        assertTrue(testSrc.contains("package com.myco.deskpilot;"), "Test should declare custom package");
    }
}
