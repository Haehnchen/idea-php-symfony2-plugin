# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Symfony Plugin** for IntelliJ IDEA/PhpStorm - a sophisticated IDE plugin that provides intelligent code assistance for Symfony PHP framework development. The plugin supports Symfony 2, 3, 4, and later versions.

**Plugin ID:** `fr.adrienbrault.idea.symfony2plugin`
**Key Dependencies:** Requires PHP Annotation plugin for full functionality

## Build and Development Commands

### Running the Plugin in Development
```bash
./gradlew runIde
```
This launches a sandboxed IntelliJ IDEA instance with the plugin loaded for testing.

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

### Publishing the Plugin
```bash
IJ_TOKEN=yourtoken ./gradlew clean buildPlugin publishPlugin
```

### Additional Development Tasks
```bash
# Verify plugin structure and compatibility
./gradlew verifyPlugin

# Run Qodana code quality checks
./gradlew qodana

# Update Gradle wrapper
./gradlew wrapper
```

## High-Level Architecture

### Core System Components

The plugin uses a **multi-layered architecture** built on IntelliJ Platform's extension point system:

1. **File-Based Indexing Layer** - 22 specialized indexes that parse Symfony configuration files (YAML, XML, PHP) to extract services, routes, parameters, events, form types, Doctrine entities, translations, and Twig templates. This enables real-time IDE features without requiring container compilation.

2. **Resolution Layer** - `ContainerCollectionResolver` aggregates data from indexes and extension points to provide a unified view of the service container, supporting custom service collectors through extension points.

3. **Reference Provider Layer** - 24+ reference providers implement navigation (Ctrl+Click) for Symfony-specific elements like service IDs, route names, template paths, entity classes, form types, translation keys, and event names.

4. **Type Provider Layer** - 6+ PHP type providers integrate with PhpStorm's static analysis to resolve return types for methods like `$container->get()`, `$repository->find()`, and event dispatcher calls.

5. **Completion System** - Hierarchical completion architecture with 15+ language-specific contributors (YAML, XML, PHP, Twig, JavaScript, HTML) providing context-aware autocompletion.

6. **Code Analysis Layer** - 38+ inspections and 9 intention actions for code quality checks and quick fixes specific to Symfony patterns.

### Main Package Organization

The codebase is organized into **28 major packages** under `src/main/java/fr/adrienbrault/idea/symfony2plugin/`:

**Infrastructure:**
- `stubs/` - File-based indexes (22 indexes for services, routes, parameters, Doctrine, Twig, translations, etc.)
- `extension/` - 13 custom extension point interfaces for third-party extensibility
- `util/` - Shared utility classes

**Core Symfony Integration:**
- `dic/` - Dependency Injection Container (76 files) - service resolution, type providers, inspections
- `config/` - Service container configuration (YAML, XML, PHP parsers)
- `routing/` - Route handling, references, and navigation

**Domain Components:**
- `templating/` - Twig template support, variable resolution, template inheritance
- `twig/` - Twig-specific features (macros, filters, functions, extensions)
- `doctrine/` - Doctrine ORM integration (entity resolution, repository type providers, metadata)
- `form/` - Symfony Form types, options, field resolution
- `translation/` - i18n/Translation system support
- `security/` - Security voters and access control
- `profiler/` - Profiler integration

**IDE Integration:**
- `codeInsight/` - Completion providers, goto handlers, reference contributors
- `codeInspection/` - Code inspections and analysis
- `completion/` - Completion contributor system
- `intentions/` - Intention actions (quick fixes)
- `navigation/` - Navigation features (goto declaration, symbol search, related files)
- `assistant/` - Method reference and signature analysis

**Modern Symfony Features:**
- `assetMapper/` - Asset management (Webpack Encore, AssetMapper)
- `ux/` - Symfony UX Components support
- `javascript/` - JavaScript/Stimulus integration

### Key Integration Patterns

#### Service Container Resolution Flow
```
PHP: $container->get('service_name')
  ↓ ServiceReference matched by PhpRouteReferenceContributor
  ↓ ContainerCollectionResolver.getService()
  ↓ Queries ServicesDefinitionStubIndex
  ↓ Returns ServiceSerializable with class name
  ↓ PhpIndex resolves to class definition
  → Enables navigation and type inference
```

#### Twig Variable Resolution
The plugin implements **13+ TwigVariableCollector extensions** that gather variables from:
- Service container globals
- Controller render() calls and route parameters
- Template includes and embeds
- Macro scopes
- Symfony UX Component props
- Framework configuration

Each collector implements `collect()` to populate variable maps for specific contexts.

#### Type Provider Pattern
Type providers (implementing `PhpTypeProvider4`) enable static analysis:
- Match specific method call patterns (e.g., `ContainerInterface::get()`)
- Extract parameters (service ID, entity class, etc.)
- Query indexes for type information
- Return resolved class signatures for IDE type checking

This powers features like autocomplete on `$this->get('service')->` chains.

### Important Base Classes

**Service Models:**
- `ServiceInterface` - Base service contract with serialization support
- `ContainerService` - Resolved service definition with metadata
- `ContainerParameter` - Container parameter representation

**Index Infrastructure:**
- `FileBasedIndexExtension<Key, Value>` - Base for all 22 indexes
- `ServicesDefinitionStubIndex` - Primary service index
- `RoutesStubIndex` - Route definitions
- `TwigExtendsStubIndex` - Template inheritance hierarchy

**Reference System:**
- `PsiReferenceContributor` - Register reference providers
- `ServiceReference`, `RouteReference`, `TemplateReference`, etc. - Specific reference types

**Completion System:**
- `GotoCompletionRegistrar` - Extension point for completion providers
- `GotoCompletionProvider` - Context-specific completion logic
- Custom completion contributors for YAML, XML, PHP, Twig

**Testing:**
- `SymfonyLightCodeInsightFixtureTestCase` - Base test class with assertion helpers
- Provides methods like `assertCompletionContains()`, `assertNavigationMatch()`, `assertIndexContains()`

## Extension Points

The plugin defines **13 custom extension points** allowing third-party plugins to extend functionality:

- `ServiceContainerLoader` - Load services from custom sources
- `RoutingLoader` - Load routes from custom sources
- `DoctrineModelProvider` - Custom Doctrine entity discovery
- `TwigVariableCollector` - Provide Twig template variables
- `ServiceCollector`, `ServiceParameterCollector`, `ServiceDefinitionLocator` - Service discovery
- `MethodSignatureTypeProviderExtension` - Custom PHP type resolution
- `TwigNamespaceExtension` - Custom Twig path configurations
- `ControllerActionGotoRelatedCollector` - Controller navigation targets

These are declared in `src/main/resources/META-INF/plugin.xml`.

## Testing Strategy

Tests extend `SymfonyLightCodeInsightFixtureTestCase` which provides:

- **Completion Testing:** `assertCompletionContains()`, `assertCompletionNotContains()`, `assertCompletionLookupTailEquals()`
- **Navigation Testing:** `assertNavigationContains()`, `assertNavigationMatch()`, `assertNavigationIsEmpty()`
- **Reference Testing:** `assertPhpReferenceResolveTo()`, `assertReferenceMatch()`
- **Index Testing:** `assertIndexContains()`, `assertIndexContainsKeyWithValue()`
- **Inspection Testing:** `assertLocalInspectionContains()`, `assertIntentionIsAvailable()`
- **Line Marker Testing:** `assertLineMarker()`, `assertLineMarkerIsEmpty()`

Test files use dummy fixtures via `myFixture.addFileToProject()` to simulate Symfony project structures.

## Configuration Files

**build.gradle.kts** - Main build configuration
- Uses Gradle IntelliJ Platform Plugin 2.4.0
- Targets platform version from `gradle.properties`
- Dependencies on PHP, Twig, YAML, and JavaScript plugins

**gradle.properties** - Version and platform configuration
- `pluginVersion` - Current plugin version (SemVer)
- `platformVersion` - Target IntelliJ Platform version (2025.1)
- `platformType` - IU (IntelliJ Ultimate)
- `pluginSinceBuild` / `pluginUntilBuild` - Compatibility range
- `javaVersion` - Java 21 required

**src/main/resources/META-INF/plugin.xml** - Plugin descriptor
- Extension point declarations
- Service registrations
- Dependency declarations

## Freemium Model

Since PhpStorm 2022.1, the plugin operates as "Freemium":
- All features in the GitHub repository are free
- Premium features are marked with `[paid]` in documentation
- 15-minute grace period after project opening for premium features
- License managed via PhpStorm's "Help → Register" menu

## Important Development Notes

1. **Plugin Enablement:** The plugin must be enabled per-project via File → Settings → Languages & Framework → PHP → Symfony. Tests automatically enable it via `Settings.getInstance(myFixture.getProject()).pluginEnabled = true`.

2. **Index Invalidation:** If IDE features behave unexpectedly, use "File → Invalidate Caches / Restart" to rebuild indexes.

3. **Dependency Requirements:**
   - PHP plugin (bundled with PhpStorm/IntelliJ Ultimate)
   - Twig plugin (bundled)
   - PHP Annotation plugin (separate install, ID: `de.espend.idea.php.annotation`)
   - PHP Toolbox plugin (optional, ID: `de.espend.idea.php.toolbox`)

4. **Pattern Matching:** The codebase extensively uses `ElementPattern` from IntelliJ Platform for matching PSI elements. Understanding `PlatformPatterns` and `PsiElementPattern` is crucial for adding new features.

5. **Performance:** Always use indexes and caching (`CachedValue`, `CachedValuesManager`) for expensive operations. Never iterate all files in the project directly.

6. **Serialization:** Index values must be serializable. Use `ObjectStreamDataExternalizer` or custom externalizers for complex objects.

7. **Thread Safety:** Follow IntelliJ's threading model - read actions for PSI access, write actions for modifications. Most operations should be read-only.

## Documentation and Resources

- **Plugin Page:** https://plugins.jetbrains.com/plugin/7219
- **Documentation:** https://espend.de/phpstorm/plugin/symfony
- **Changelog:** [CHANGELOG.md](CHANGELOG.md)
- **Build/Deployment Guide:** [MAINTENANCE.md](MAINTENANCE.md)
- **IntelliJ SDK Docs:** http://confluence.jetbrains.com/display/PhpStorm/Setting-up+environment+for+PhpStorm+plugin+development
- **Technical Diagram:** See `plugin-diagram.webp` in repository root

## Common Development Patterns

### Adding a New Index
1. Create index class extending `FileBasedIndexExtension<String, YourValueType>`
2. Implement `getIndexer()` to extract data from files
3. Implement `getKeyDescriptor()` and `getDataExternalizer()` for serialization
4. Register in `plugin.xml` under `<extensions><fileBasedIndex>`

### Adding Completion
1. Create a `GotoCompletionRegistrar` implementation
2. Register completion provider with specific PSI patterns
3. Implement `GotoCompletionProvider` with `getLookupElements()`
4. Register in `plugin.xml` if needed, or use existing registrar

### Adding Navigation/References
1. Create `PsiReferenceContributor` for specific language
2. Define PSI element patterns to match
3. Implement `PsiReference` with `resolve()` method
4. Query indexes or use utility methods to find targets

### Adding Type Provider
1. Implement `PhpTypeProvider4` interface
2. Use `getKey()` to match method call patterns
3. In `getType()`, return type signature with custom key
4. In `complete()`, resolve custom key to actual type
