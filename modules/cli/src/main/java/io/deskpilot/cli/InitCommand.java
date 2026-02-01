package io.deskpilot.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class InitCommand {

    /**
     * Usage:
     *   deskpilot init [--force] <dir> <junit5|testng> [package]
     *
     * return 0 success, 2 usage
     */
    public int run(String[] args) throws Exception {
        if (args == null) args = new String[0];

        boolean force = false;
        int i = 0;
        if (args.length > 0 && "--force".equalsIgnoreCase(args[0])) {
            force = true;
            i = 1;
        }

        if (args.length - i < 2) {
            System.err.println(
                    "Usage:\n" +
                    "    deskpilot init [--force] <dir> <junit5|testng> [package]\n" +
                    "Examples:\n" +
                    "    deskpilot init myproj junit5\n" +
                    "    deskpilot init myproj testng com.myco.deskpilot\n" +
                    "    deskpilot init --force myproj junit5\n"
            );
            return 2;
        }

        String dirArg = args[i];
        String framework = args[i + 1].trim().toLowerCase();
        String packageName = (args.length - i >= 3 && !args[i + 2].isBlank())
                ? args[i + 2].trim()
                : "com.example.deskpilot";

        if (!framework.equals("junit5") && !framework.equals("testng")) {
            System.err.println("Framework must be junit5 or testng");
            return 2;
        }

        SafePaths.validateJavaPackageOrThrow(packageName);
        SafePaths.rejectReservedWindowsName(dirArg);

        Path dir = SafePaths.root(Paths.get(dirArg));
        SafePaths.ensureDirEmptyOrForce(dir, force);
        SafePaths.ensureDir(dir);

        String groupId = "com.example";
        String artifactId = dir.getFileName().toString().replaceAll("[^a-zA-Z0-9_-]", "-");

        String deskpilotVersion = InitCommand.class.getPackage().getImplementationVersion();
        if (deskpilotVersion == null || deskpilotVersion.isBlank()) {
            deskpilotVersion = "0.1.0";
        }

        String pom;
        String packagePath = packageName.replace('.', '/');

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

        SafePaths.writeString(SafePaths.under(dir, "pom.xml"), pom, force);

        String gitignore = """
            target/
            runs/
            .idea/
            .vscode/
            *.iml
            .DS_Store
            Thumbs.db
            """;
        SafePaths.writeString(SafePaths.under(dir, ".gitignore"), gitignore, force);

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
        SafePaths.writeString(SafePaths.under(dir, "README.md"), readme, force);

        Path testDir = SafePaths.under(dir, "src", "test", "java", packagePath);
        SafePaths.ensureDir(testDir);

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

            SafePaths.writeString(SafePaths.under(testDir, "ExampleDeskPilotTest.java"), test, force);
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

            SafePaths.writeString(SafePaths.under(testDir, "ExampleDeskPilotTest.java"), test, force);
        }

        return 0;
    }
}
