package io.deskpilot.cli;

import io.deskpilot.common.SafePaths;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class InitCommand {

    /**
     * Usage:
     *   deskpilot init [--force] <dir> <junit5|testng> [package]
     *   deskpilot init --bootstrap [--force] <dir> <junit5|testng> [package]
     *
     * Return:
     *   0 success, 2 usage
     */
    public int run(String[] args) throws Exception {
        if (args == null) args = new String[0];

        boolean bootstrap = false;
boolean force = false;

// Accept flags anywhere
java.util.List<String> rest = new java.util.ArrayList<>();
for (String a : args) {
    if (a == null) continue;
    String s = a.trim();
    if (s.isEmpty()) continue;

    if ("--bootstrap".equalsIgnoreCase(s)) {
        bootstrap = true;
    } else if ("--force".equalsIgnoreCase(s)) {
        force = true;
    } else {
        rest.add(s);
    }
}

if (rest.size() < 2) {
    System.err.println(
            "Usage:\n" +
                    "    deskpilot init [--force] <dir> <junit5|testng> [package]\n" +
                    "    deskpilot init --bootstrap [--force] <dir> <junit5|testng> [package]\n" +
                    "Examples:\n" +
                    "    deskpilot init myproj junit5\n" +
                    "    deskpilot init myproj testng com.myco.deskpilot\n" +
                    "    deskpilot init --force myproj junit5\n" +
                    "    deskpilot init --bootstrap --force . junit5 com.myco.tests\n"
    );
    return 2;
}

String dirArgRaw = rest.get(0).trim();
String framework = rest.get(1).trim().toLowerCase();

String packageName = (rest.size() >= 3 && !rest.get(2).isBlank())
        ? rest.get(2).trim()
        : "com.example.deskpilot";


        if (!framework.equals("junit5") && !framework.equals("testng")) {
            System.err.println("Framework must be junit5 or testng");
            return 2;
        }

        SafePaths.validateJavaPackageOrThrow(packageName);

        // ✅ Allow "." and ".." (especially for bootstrap) by resolving them to real paths
        Path dirPath;
        if (".".equals(dirArgRaw) || ".\\".equals(dirArgRaw) || "./".equals(dirArgRaw)) {
            dirPath = Paths.get("").toAbsolutePath().normalize();
        } else if ("..".equals(dirArgRaw) || "..\\".equals(dirArgRaw) || "../".equals(dirArgRaw)) {
            dirPath = Paths.get("..").toAbsolutePath().normalize();
        } else {
            // For a normal folder name/path, keep your reserved-name guardrail
            SafePaths.rejectReservedWindowsName(dirArgRaw);
            dirPath = Paths.get(dirArgRaw);
        }

        Path dir = SafePaths.root(dirPath);
        SafePaths.ensureDir(dir);

        String deskpilotVersion = System.getProperty("deskpilot.version");
        if (deskpilotVersion == null || deskpilotVersion.isBlank()) {
            deskpilotVersion = InitCommand.class.getPackage().getImplementationVersion();
        }
        if (deskpilotVersion == null || deskpilotVersion.isBlank()) {
            deskpilotVersion = "0.1.2";
        }

        if (bootstrap) {
            // Bootstrap mode: DO NOT overwrite pom/src, only drop props + wrappers.
            writeDeskpilotProps(dir, deskpilotVersion, framework, packageName, force);
            writeWrapperCmd(dir, force);
            writeWrapperSh(dir, force);

            System.out.println("Bootstrap complete.");
            System.out.println("Try:");
            System.out.println("  .\\deskpilotw.cmd --version");
            System.out.println("  .\\deskpilotw.cmd doctor");
            System.out.println("  .\\deskpilotw.cmd record --projectDir .");
            return 0;
        }

        // Normal init (scaffold) still requires empty dir unless --force
        SafePaths.ensureDirEmptyOrForce(dir, force);

        String groupId = "com.example";
        String artifactId = dir.getFileName().toString().replaceAll("[^a-zA-Z0-9_-]", "-");

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

            ## Notes
            - DeskPilot will prompt you to pick a window.
            - Run artifacts will be created under `runs/`.
            """;
        SafePaths.writeString(SafePaths.under(dir, "README.md"), readme, force);

        writeDeskpilotProps(dir, deskpilotVersion, framework, packageName, force);
        writeWrapperCmd(dir, force);
        writeWrapperSh(dir, force);

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

  private static void writeDeskpilotProps(Path dir, String version, String framework, String pkg, boolean force) throws Exception {
    String props =
            "deskpilot.version=" + version + "\n" +
            "deskpilot.framework=" + framework + "\n" +
            "deskpilot.package=" + pkg + "\n";

    Path p = SafePaths.under(dir, "deskpilot.properties");
    SafePaths.writeString(p, props, force);   // ✅ key line
}





  private static void writeWrapperCmd(Path dir, boolean force) throws Exception {
    String cmd = """
        @echo off
        setlocal enabledelayedexpansion

        set "ROOT=%~dp0"
        set "PROPS=%ROOT%deskpilot.properties"

        echo [deskpilotw] ROOT=%ROOT%
        echo [deskpilotw] PROPS=%PROPS%

        if not exist "%PROPS%" (
          echo [deskpilotw][ERROR] deskpilot.properties not found: %PROPS%
          exit /b 2
        )

        for /f "usebackq tokens=1,* delims==" %%A in ("%PROPS%") do (
          if /i "%%A"=="deskpilot.version" set "DP_VER=%%B"
        )

        if "%DP_VER%"=="" (
          echo [deskpilotw][ERROR] deskpilot.version missing in deskpilot.properties
          exit /b 2
        )

        set "CACHE=%ROOT%.deskpilot\\cli\\%DP_VER%"
        set "JAR=%CACHE%\\deskpilot.jar"

        echo [deskpilotw] DP_VER=%DP_VER%
        echo [deskpilotw] JAR=%JAR%

        if not exist "%CACHE%" mkdir "%CACHE%" >nul 2>&1

        if not exist "%JAR%" (
          echo [deskpilotw] CLI jar missing. Downloading...

          set "TAG=v%DP_VER%"
          set "URL=https://github.com/Sankr20/deskpilot/releases/download/%TAG%/deskpilot.jar"

          powershell -NoProfile -ExecutionPolicy Bypass -Command ^
            "try { iwr -UseBasicParsing '%URL%' -OutFile '%JAR%' } catch { exit 1 }"

          if not exist "%JAR%" (
            echo [deskpilotw][ERROR] Download failed: %URL%
            exit /b 2
          )
        )

        echo [deskpilotw] running: java -jar "%JAR%" %*
        java -jar "%JAR%" %*
        set "RC=%ERRORLEVEL%"
        echo [deskpilotw] exit code=%RC%
        exit /b %RC%
        """;

    SafePaths.writeString(SafePaths.under(dir, "deskpilotw.cmd"), cmd, force);
}


    private static void writeWrapperSh(Path dir, boolean force) throws Exception {
    String sh = """
        #!/usr/bin/env bash
        set -euo pipefail

        ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
        PROPS="$ROOT/deskpilot.properties"

        if [[ ! -f "$PROPS" ]]; then
          echo "[deskpilotw][ERROR] deskpilot.properties not found in $ROOT" >&2
          exit 2
        fi

        DP_VER="$(grep '^deskpilot.version=' "$PROPS" | head -n1 | cut -d= -f2- | tr -d '\\r')"
        if [[ -z "${DP_VER}" ]]; then
          echo "[deskpilotw][ERROR] deskpilot.version missing in deskpilot.properties" >&2
          exit 2
        fi

        CACHE="$ROOT/.deskpilot/cli/$DP_VER"
        JAR="$CACHE/deskpilot.jar"
        mkdir -p "$CACHE"

        echo "[deskpilotw] ROOT=$ROOT"
        echo "[deskpilotw] DP_VER=$DP_VER"
        echo "[deskpilotw] JAR=$JAR"

        if [[ ! -f "$JAR" ]]; then
          TAG="v$DP_VER"
          URL="https://github.com/Sankr20/deskpilot/releases/download/$TAG/deskpilot.jar"
          TMP="$JAR.tmp"

          echo "[deskpilotw] CLI jar missing. Downloading..."
          echo "[deskpilotw] URL=$URL"

          if command -v curl >/dev/null 2>&1; then
            curl -fL "$URL" -o "$TMP"
          elif command -v wget >/dev/null 2>&1; then
            wget -O "$TMP" "$URL"
          else
            echo "[deskpilotw][ERROR] Need curl or wget to download the CLI jar." >&2
            exit 2
          fi

          mv "$TMP" "$JAR"
        fi

        exec java -jar "$JAR" "$@"
        """;

    Path out = SafePaths.under(dir, "deskpilotw");
    SafePaths.writeString(out, sh, force);

    // Nice-to-have: mark executable on Unix-ish systems (no-op on Windows)
    try {
        java.nio.file.Files.setPosixFilePermissions(
                out,
                java.util.Set.of(
                        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                        java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE,
                        java.nio.file.attribute.PosixFilePermission.GROUP_READ,
                        java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE,
                        java.nio.file.attribute.PosixFilePermission.OTHERS_READ,
                        java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE
                )
        );
    } catch (Exception ignored) {
        // Windows/FS may not support POSIX perms; ignore
    }
}

}
