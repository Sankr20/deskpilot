package io.deskpilot.cli;


import io.deskpilot.engine.EngineActionSmoke;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.Arrays;

public final class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(2);
        }

        switch (args[0].toLowerCase()) {
            case "doctor":
                doctor();
                break;
            case "init":
                init(slice(args));
                break;
            case "record":
                RecorderCLI.recordToFile(slice(args));
                break;
            case "run":
                runTest(args);
                break;
            case "smoke":
                smoke(args);
                break;
            default:
                System.err.println("Unknown command: " + args[0]);
                usage();
                System.exit(2);
        }
    }

    private static void doctor() {
        System.out.println("DeskPilot doctor");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("OK");
    }

    private static void runTest(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: deskpilot run <fully.qualified.TestClass>");
            System.exit(2);
        }

        LauncherDiscoveryRequest request =
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(args[1]))
                        .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new java.io.PrintWriter(System.out));

        System.exit(summary.getFailures().isEmpty() ? 0 : 1);
    }

    private static void smoke(String[] args) throws Exception {
        if (args.length < 2 || !"demo".equalsIgnoreCase(args[1])) {
            System.err.println(
                "Usage:\n" +
                "    deskpilot smoke demo"
            );
            System.exit(2);
        }
        EngineActionSmoke.main(new String[0]);
    }

    private static void init(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println(
                "Usage:\n" +
                "    deskpilot init <dir> <junit5|testng>"
            );
            System.exit(2);
        }

        java.nio.file.Path dir = java.nio.file.Paths.get(args[0]);
        String framework = args[1].toLowerCase();

        if (!framework.equals("junit5") && !framework.equals("testng")) {
            System.err.println("Framework must be junit5 or testng");
            System.exit(2);
        }

        java.nio.file.Files.createDirectories(dir);

        String groupId = "com.example";
        String artifactId = dir.getFileName().toString().replaceAll("[^a-zA-Z0-9_-]", "-");
        String deskpilotVersion = "0.1.0";

        String pom;
        if (framework.equals("junit5")) {
            pom = String.format("""
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0.0-SNAPSHOT</version>

                    <properties>
                        <maven.compiler.source>17</maven.compiler.source>
                        <maven.compiler.target>17</maven.compiler.target>
                        <deskpilot.version>%s</deskpilot.version>
                    </properties>

                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit</groupId>
                                <artifactId>junit-bom</artifactId>
                                <version>5.10.3</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>

                    <dependencies>
                        <dependency>
                            <groupId>io.deskpilot</groupId>
                            <artifactId>engine</artifactId>
                            <version>${deskpilot.version}</version>
                        </dependency>

                        <dependency>
                            <groupId>io.deskpilot</groupId>
                            <artifactId>testkit</artifactId>
                            <version>${deskpilot.version}</version>
                            <scope>test</scope>
                        </dependency>

                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>

                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.3.1</version>
                                <configuration>
                                    <useModulePath>false</useModulePath>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """, groupId, artifactId, deskpilotVersion);
        } else {
            pom = String.format("""
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0.0-SNAPSHOT</version>

                    <properties>
                        <maven.compiler.source>17</maven.compiler.source>
                        <maven.compiler.target>17</maven.compiler.target>
                        <deskpilot.version>%s</deskpilot.version>
                    </properties>

                    <dependencies>
                        <dependency>
                            <groupId>io.deskpilot</groupId>
                            <artifactId>engine</artifactId>
                            <version>${deskpilot.version}</version>
                        </dependency>

                        <dependency>
                            <groupId>io.deskpilot</groupId>
                            <artifactId>testkit</artifactId>
                            <version>${deskpilot.version}</version>
                            <scope>test</scope>
                        </dependency>

                        <dependency>
                            <groupId>org.testng</groupId>
                            <artifactId>testng</artifactId>
                            <version>7.10.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>

                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.3.1</version>
                                <configuration>
                                    <useModulePath>false</useModulePath>
                                    <includes>
                                        <include>**/*Test.java</include>
                                    </includes>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """, groupId, artifactId, deskpilotVersion);
        }

        java.nio.file.Files.writeString(dir.resolve("pom.xml"), pom);

        java.nio.file.Path testDir = dir.resolve("src/test/java/com/example");
        java.nio.file.Files.createDirectories(testDir);

        if (framework.equals("junit5")) {
            String test = """
                package com.example;

                import io.deskpilot.testkit.BaseDeskPilotTestJUnit5;
                import org.junit.jupiter.api.Test;

                public class ExampleDeskPilotTest extends BaseDeskPilotTestJUnit5 {

                    @Test
                    void attach_smoke() throws Exception {
                        session().step("noop", () -> {});
                    }
                }
                """;
            java.nio.file.Files.writeString(testDir.resolve("ExampleDeskPilotTest.java"), test);
        } else {
            String test = """
                package com.example;

                import io.deskpilot.testkit.BaseDeskPilotTestTestNG;
                import org.testng.annotations.Test;

                public class ExampleDeskPilotTest extends BaseDeskPilotTestTestNG {

                    @Test
                    public void attach_smoke() throws Exception {
                        session().step("noop", () -> {});
                    }
                }
                """;
            java.nio.file.Files.writeString(testDir.resolve("ExampleDeskPilotTest.java"), test);
        }

        System.out.println("Initialized DeskPilot project at: " + dir.toAbsolutePath());
        System.out.println("Next: mvn test");
    }

    private static void usage() {
        System.out.println(
            "DeskPilot CLI\n" +
            "    deskpilot doctor\n" +
            "    deskpilot record\n" +
            "    deskpilot run <TestClass>\n" +
            "    deskpilot init <dir> <junit5|testng>\n" +
            "    deskpilot smoke demo"
        );
    }

    private static String[] slice(String[] args) {
        return args.length <= 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
    }
}
