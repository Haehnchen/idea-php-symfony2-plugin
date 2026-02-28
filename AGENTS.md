# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Symfony Plugin** for IntelliJ IDEA/PhpStorm - a sophisticated IDE plugin that provides intelligent code assistance for Symfony PHP framework development. The plugin supports Symfony 2, 3, 4, and later versions.

**Plugin ID:** `fr.adrienbrault.idea.symfony2plugin`
**Key Dependencies:** Requires PHP Annotation plugin for full functionality

## Build and Development Commands

### Building the Plugin

```bash
./gradlew clean buildPlugin
```

The distributable ZIP artifact will be in `build/distributions/`.

### Running Tests

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "fr.adrienbrault.idea.symfony2plugin.tests.dic.SymfonyContainerTypeProviderTest"

# Run tests matching a pattern
./gradlew test --tests "*ContainerTest"
```

## High-Level Architecture

### Main Package Organization

The codebase is organized into **28 major packages** under `src/main/java/fr/adrienbrault/idea/symfony2plugin/`:

**Infrastructure:**
- `stubs/` - File-based indexes (indexes for services, routes, parameters, Doctrine, Twig, translations, etc.)
- `extension/` - Extension point interfaces for third-party extensibility
- Type providers (implementing `PhpTypeProvider4`) enable static analysis
- **TwigVariableCollector** that gathers variables for Twig templates
- `src/main/resources/META-INF/plugin.xml` for restiering extension points

**Testing:**
- `SymfonyLightCodeInsightFixtureTestCase` - Base test class with assertion helpers
- **Completion Testing:** `assertCompletionContains()`, `assertCompletionNotContains()`, `assertCompletionLookupTailEquals()`
- **Navigation Testing:** `assertNavigationContains()`, `assertNavigationMatch()`, `assertNavigationIsEmpty()`
- **Reference Testing:** `assertPhpReferenceResolveTo()`, `assertReferenceMatch()`
- **Index Testing:** `assertIndexContains()`, `assertIndexContainsKeyWithValue()`
- **Inspection Testing:** `assertLocalInspectionContains()`, `assertIntentionIsAvailable()`
- **Line Marker Testing:** `assertLineMarker()`, `assertLineMarkerIsEmpty()`

Test files use dummy fixtures via `myFixture.addFileToProject()` to simulate Symfony project structures.

### Unit Test VFS Limitations

- Light tests use in-memory VFS (`temp://` protocol) - standard path resolution doesn't work
- `TwigPath.getDirectory()` has a fallback using `FilenameIndex.getVirtualFilesByName()` to find directories by name
- For template path tests: copy fixtures first so they're available to the index

## Freemium Model

- All features in the GitHub repository are free
- Premium features are marked with `[paid]` mainly all in `de.espend.idea.symfony` package and not public

## Important Development Notes

- **Performance:** Always use indexes and caching (`CachedValue`, `CachedValuesManager`) for expensive operations. Never iterate all files in the project directly.
- **Thread Safety:** Follow IntelliJ's threading model - read actions for PSI access, write actions for modifications. Most operations should be read-only.

## Common Development Patterns

- **Adding a New Index** Class extending `FileBasedIndexExtension<String, YourValueType>`
- **Adding Completion** Create a `GotoCompletionRegistrar` implementation
- **Adding Navigation** Create a `GotoDeclarationHandler` implementation

## Decompiler Tools

For analyzing bundled plugins like Twig and PHP you MUST use **vineflower** and NOT **Fernflower** from IntelliJ (less quality):

**vineflower**

- **GitHub:** https://github.com/Vineflower/vineflower
- **Download:** https://repo1.maven.org/maven2/org/vineflower/vineflower/1.11.2/vineflower-1.11.2.jar
- **Local copy:** `decompiled/vineflower.jar`
- **Usage:** `java -jar vineflower.jar input.jar output/`

**Bundled Plugin JARs (for decompilation):**
- **Location:** `~/.gradle/caches/[gradle-version]/transforms/*/transformed/com.jetbrains.[plugin]-[intellij-version]/[plugin]/lib/[plugin].jar`
- **Example:** `~/.gradle/caches/9.3.0/transforms/*/transformed/com.jetbrains.twig-253.28294.322/twig/lib/twig.jar`
