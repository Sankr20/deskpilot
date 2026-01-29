# 🛠 DeskPilot — Desktop UI Automation Framework

DeskPilot is a universal, application-agnostic **desktop automation framework** designed for reliable, deterministic UI automation on Windows desktops. It brings strong diagnostics, repeatable runs, and a modern automation API — similar to Playwright but for desktop apps.

---

## 📌 Key Principles

DeskPilot is built on these core ideas:

* **Deterministic runs** — each test run creates an artifact folder with screenshots, OCR text, templates, and logs.
* **One blessed entrypoint** — tests attach to the app under test via a single API (`DeskPilot.attachPickWindow`).
* **Stable automation** — built-in stabilization (bring-to-front, idle waits, retries).
* **Semantic verification** — OCR-based assertions, not pixel comparison.
* **Framework neutral** — supports both **JUnit5** and **TestNG** out of the box.

---

## 📦 Installation

Include the DeskPilot engine and testkit in your Maven project:

```xml
<dependencies>
  <!-- Engine -->
  <dependency>
    <groupId>io.deskpilot</groupId>
    <artifactId>engine</artifactId>
    <version>${deskpilot.version}</version>
  </dependency>

  <!-- Test Kit (framework-specific helpers) -->
  <dependency>
    <groupId>io.deskpilot</groupId>
    <artifactId>testkit</artifactId>
    <version>${deskpilot.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

---

## 📁 Runs & Artifacts (PRD Contract)

Each test execution creates a structured run folder at:

```
runs/<TestClass>/<testMethod>/
```

Artifacts include:

* before/after screenshots
* overlay screenshots with locator highlights
* OCR crops and recognized text
* template match diagnostics
* logs and errors

This structure allows for easy debugging and traceability.

---

## 🚀 CLI Overview

DeskPilot comes with a CLI for convenience:

```
deskpilot doctor
deskpilot record
deskpilot run <TestClass>
deskpilot init <dir> <junit5|testng>
deskpilot smoke demo
```

* `doctor` — prints environment info
* `record` — launches the interactive recorder
* `run` — runs a single test class
* `init` — bootstraps a test project
* `smoke` — runs the demo smoke tests

---

## 📄 Project Initialization

Use the CLI to bootstrap a new test project:

```sh
deskpilot init <project-dir> <junit5|testng>
```

This will generate:

* A `pom.xml` configured for DeskPilot
* A sample test that attaches and runs a no-op step
* Appropriate dependencies for your chosen test framework

Then:

```sh
cd <project-dir>
mvn test
```

---

## 🧪 Supported Test Frameworks

DeskPilot supports both **JUnit5** and **TestNG**. When using `deskpilot init`, choose the framework that matches your preference:

* `junit5` — generates a JUnit5 test scaffold
* `testng` — generates a TestNG test scaffold

---

## 💠 Sample Tests

### 🟦 JUnit5 Example

```java
package com.example;

import io.deskpilot.testkit.BaseDeskPilotTestJUnit5;
import org.junit.jupiter.api.Test;

public class ExampleDeskPilotTest extends BaseDeskPilotTestJUnit5 {

    @Test
    void attach_smoke() throws Exception {
        session().step("noop", () -> {});
    }
}
```

---

### 🟩 TestNG Example

```java
package com.example;

import io.deskpilot.testkit.BaseDeskPilotTestTestNG;
import org.testng.annotations.Test;

public class ExampleDeskPilotTest extends BaseDeskPilotTestTestNG {

    @Test
    public void attach_smoke() throws Exception {
        session().step("noop", () -> {});
    }
}
```

---

## 🧠 The Blessed Attach API

DeskPilot tests should always attach to the app under test using the **blessed entrypoint**:

```java
import io.deskpilot.engine.DeskPilot;
import io.deskpilot.engine.RunOptions;
import io.deskpilot.engine.DeskPilotSession;

RunOptions opts = RunOptions.forTest("MyRun");
try (DeskPilotSession session = DeskPilot.attachPickWindow(opts)) {
    session.step("some_action", () -> {
        // automation steps…
    });
}
```

**Do not** instantiate engine internals (e.g., `WindowManager`, `RobotCoords`, `DesktopDriver`) directly in tests.

---

## 📍 Locator System

DeskPilot uses a registry of locators:

* **Point** — fixed UI coordinate
* **Region** — UI area
* **Template** — image pattern matching
* **OCR** — semantic text search

Locators are validated at test startup to ensure correctness.

---

## 🔍 Semantic Verification with OCR

DeskPilot prefers text-based verification:

* Captures images of UI regions
* Runs OCR to extract text
* Dumps cropped images and recognized text per step

This enables resilient assertions less dependent on UI pixel fidelity.

---

## 🛠 Running Tests

Run tests using Maven:

```sh
# For JUnit5
deskpilot init myproject junit5
cd myproject
mvn test

# For TestNG
deskpilot init myproject testng
cd myproject
mvn test
```

---

## ⛑ Troubleshooting

### 🧪 Why is my run folder empty?

Make sure:

* You are using the blessed attach API via the testkit
* The test actually executed at least one step

### 📌 Test not detected?

* JUnit5: confirm you have `@Test`
* TestNG: confirm you included the TestNG dependency and your class is picked up by Surefire

---

## 📚 Next Steps

Once you’re comfortable with the basics:

1. Refine your locators
2. Write stable actions
3. Use OCR for semantic validations
4. Expand your test suite

---

## 🎉 You're Ready

DeskPilot gives you a robust foundation for Windows desktop automation — with reliable diagnostics, framework flexibility, and a clean, repeatable workflow.

---
