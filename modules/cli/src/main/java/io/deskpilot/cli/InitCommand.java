package io.deskpilot.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class InitCommand {

    /**
     * args = [dir, junit5|testng, optional package]
     * return 0 success, 2 usage
     */
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println(
                    "Usage:\n" +
                    "    deskpilot init <dir> <junit5|testng> [package]\n" +
                    "Examples:\n" +
                    "    deskpilot init myproj junit5\n" +
                    "    deskpilot init myproj testng com.myco.deskpilot"
            );
            return 2;
        }

        Path dir = Paths.get(args[0]);
        String framework = args[1].toLowerCase();

        String packageName = (args.length >= 3 && !args[2].isBlank())
                ? args[2]
                : "com.example.deskpilot";
        String packagePath = packageName.replace('.', '/');

        if (!framework.equals("junit5") && !framework.equals("testng")) {
            System.err.println("Framework must be junit5 or testng");
            return 2;
        }

        Files.createDirectories(dir);

        String groupId = "com.example";
        String artifactId = dir.getFileName().toString().replaceAll("[^a-zA-Z0-9_-]", "-");

        String deskpilotVersion = InitCommand.class.getPackage().getImplementationVersion();
        if (deskpilotVersion == null || deskpilotVersion.isBlank()) {
            deskpilotVersion = "0.1.0";
        }

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
                            <artifactId>deskpilot-core</artifactId>
                            <version>${deskpilot.version}</version>
                        </dependency>

                        <dependency>
                            <groupId>io.deskpilot</groupId>
                            <artifactId>deskpilot-testkit</artifactId>
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
                            <artifactId>deskpilot-testkit</artifactId>
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

        Files.writeString(dir.resolve("pom.xml"), pom);

        // .gitignore
        String gitignore = """
            target/
            runs/
            .idea/
            .vscode/
            *.iml
            .DS_Store
            Thumbs.db
            """;
        Files.writeString(dir.resolve(".gitignore"), gitignore);

        // README.md
        String readme = """
            # DeskPilot Automation Project

            ## Run tests
            ```bash
            mvn test
            ```

            ## What happens
            - DeskPilot will prompt you to pick a window.
            - Run artifacts will be created under `runs/`.

            ## Notes
            - Do not call low-level APIs directly (Robot/JNA/WindowManager).
            - Use DeskPilot's session API through the provided BaseTest.
            """;
        Files.writeString(dir.resolve("README.md"), readme);

        Path testDir = dir.resolve("src/test/java").resolve(packagePath);
        Files.createDirectories(testDir);

        if (framework.equals("junit5")) {
            String test = String.format("""
                package %s;

                import io.deskpilot.testkit.BaseDeskPilotTestJUnit5;
                import org.junit.jupiter.api.Test;

                public class ExampleDeskPilotTest extends BaseDeskPilotTestJUnit5 {

                    @Test
                    void attach_smoke() throws Exception {
                        session().step("attached", () -> {
                            // No-op step: forces artifact creation and validates attach wiring.
                        });
                    }
                }
                """, packageName);

            Files.writeString(testDir.resolve("ExampleDeskPilotTest.java"), test);
        } else {
            String test = String.format("""
                package %s;

                import io.deskpilot.testkit.BaseDeskPilotTestTestNG;
                import org.testng.annotations.Test;

                public class ExampleDeskPilotTest extends BaseDeskPilotTestTestNG {

                    @Test
                    public void attach_smoke() throws Exception {
                        session().step("attached", () -> {
                            // No-op step: forces artifact creation and validates attach wiring.
                        });
                    }
                }
                """, packageName);

            Files.writeString(testDir.resolve("ExampleDeskPilotTest.java"), test);
        }

        return 0;
    }
}
