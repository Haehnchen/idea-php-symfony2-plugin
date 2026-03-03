# Find Usages for Twig Templates - Research Findings

## Goal

Extend IntelliJ's "Find Usages" feature so that right-clicking on a Twig template file finds all usages of that template (includes, extends, controller render calls, component usages, etc.).

## Required Extension Points

### 1. `com.intellij.lang.findUsagesProvider`

**Interface:** `FindUsagesProvider`

This is the primary extension point for customizing Find Usages behavior.

```xml
<extensions defaultExtensionNs="com.intellij">
    <findUsagesProvider language="Twig" implementationClass="...TwigFindUsagesProvider"/>
</extensions>
```

**Key methods to implement:**
- `WordsScanner getWordsScanner()` - Returns a scanner for indexing words in files
- `boolean canFindUsagesFor(PsiElement psiElement)` - Returns true if element supports Find Usages
- `String getHelpId(PsiElement psiElement)` - Help topic ID
- `String getType(Element element)` - User-readable element type (e.g., "template")
- `String getDescriptiveName(Element element)` - Element name for UI display
- `String getNodeText(Element element, boolean useFullName)` - Text representation

### 2. `com.intellij.psi.referenceContributor`

**Interface:** `PsiReferenceContributor`

For creating references from template names in PHP/Twig files to Twig template files.

```xml
<extensions defaultExtensionNs="com.intellij">
    <psi.referenceContributor language="PHP" implementation="...TwigTemplateReferenceContributor"/>
    <psi.referenceContributor language="Twig" implementation="...TwigIncludeReferenceContributor"/>
</extensions>
```

**Pattern:**
```java
public class TwigTemplateReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // Register for string literals that contain template paths
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new TwigTemplateReferenceProvider()
        );
    }
}
```

### 3. `com.intellij.usageTypeProvider`

**Interface:** `UsageTypeProvider` (optional, for categorizing usage types)

```xml
<extensions defaultExtensionNs="com.intellij">
    <usageTypeProvider implementation="...TwigUsageTypeProvider"/>
</extensions>
```

## Key Interfaces

### `PsiReference` and `PsiPolyVariantReference`

References resolve from usage locations to the target element (Twig template file).

```java
public class TemplateReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String templateName;

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        // Find all Twig files matching the template name
        // Return array of PsiElementResolveResult
    }

    @Override
    public @NotNull Object @NotNull [] getVariants() {
        // Provide completion variants
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }
}
```

### `PsiNamedElement` (for target files)

Twig template files should implement `PsiNamedElement` to support rename refactoring:

```java
public interface PsiNamedElement extends PsiElement {
    String getName();
    PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException;
}
```

## Existing Plugin Infrastructure

### Already Implemented Indexes

The Symfony plugin already has comprehensive indexing for Twig templates:

| Index | Purpose |
|-------|---------|
| `PhpTwigTemplateUsageStubIndex` | Tracks PHP controller `$this->render()`, `$this->renderView()` calls |
| `TwigIncludeStubIndex` | Tracks `{% include %}`, `{% embed %}`, `{% import %}` tags |
| `TwigExtendsStubIndex` | Tracks `{% extends %}` tags |
| `TwigComponentUsageStubIndex` | Tracks Symfony UX component usages (`<twig:Component>`) |

### Existing Reference Implementations (Patterns to Follow)

1. **`TemplateReference.java`** - `src/main/java/fr/adrienbrault/idea/symfony2plugin/templating/TemplateReference.java`
   - Resolves template names to Twig files

2. **`TranslationReference.java`** - `src/main/java/fr/adrienbrault/idea/symfony2plugin/translation/TranslationReference.java`
   - Pattern for string-to-element references

3. **`RouteReference.java`** - `src/main/java/fr/adrienbrault/idea/symfony2plugin/routing/RouteReference.java`
   - Pattern for reference with multiple resolve targets

### MCP Collector (Usage Collection Logic)

`src/main/kotlin/fr/adrienbrault/idea/symfony2plugin/mcp/collector/TwigTemplateUsageCollector.kt`

Contains logic for collecting all usages of a template - can be reused for Find Usages.

## Implementation Strategy

### Step 1: Create `TwigFindUsagesProvider`

```java
public class TwigFindUsagesProvider implements FindUsagesProvider {
    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return psiElement instanceof TwigFile;
    }

    @Override
    public String getType(@NotNull Element element) {
        return "Twig Template";
    }

    @Override
    public String getDescriptiveName(@NotNull Element element) {
        if (element instanceof TwigFile) {
            return ((TwigFile) element).getName();
        }
        return element.getText();
    }
    // ... other methods
}
```

### Step 2: Ensure Proper References

Make sure all template usages have `PsiReference` pointing to the target Twig file:

- PHP string literals in `render()`, `renderView()`, `@Template()` annotations
- Twig `{% include "template" %}`, `{% extends "template" %}` tags
- Twig `{{ include('template') }}` functions

### Step 3: Register in plugin.xml

```xml
<extensions defaultExtensionNs="com.intellij">
    <findUsagesProvider language="Twig"
                        implementationClass="fr.adrienbrault.idea.symfony2plugin.templating.findusages.TwigFindUsagesProvider"/>

    <!-- Reference contributors for PHP files -->
    <psi.referenceContributor language="PHP"
                              implementation="fr.adrienbrault.idea.symfony2plugin.templating.reference.PhpTemplateReferenceContributor"/>

    <!-- Reference contributors for Twig files -->
    <psi.referenceContributor language="Twig"
                              implementation="fr.adrienbrault.idea.symfony2plugin.templating.reference.TwigTemplateReferenceContributor"/>
</extensions>
```

## Template Name Normalization

Symfony uses multiple formats for template names:

| Format | Example |
|--------|---------|
| Bundle notation | `@AppBundle/Resources/views/default/index.html.twig` |
| Short bundle notation | `AppBundle:Default:index.html.twig` (deprecated) |
| Path notation | `default/index.html.twig` |

The `TwigFile` and related utilities must normalize these for proper matching.

## Official IntelliJ Documentation

- **Find Usages:** https://plugins.jetbrains.com/docs/intellij/find-usages.html
- **PSI References:** https://plugins.jetbrains.com/docs/intellij/psi-references.html
- **References and Resolve:** https://plugins.jetbrains.com/docs/intellij/references-and-resolve.html

## How Find Usages Works (Architecture)

1. **Word Indexing:** `WordsScanner` indexes significant words in files
2. **Reference Search:** `ReferencesSearch` searches for `PsiReference` elements pointing to the target
3. **Custom Search:** `QueryExecutor` implementations can provide additional search logic
4. **Usage Presentation:** `UsageTarget` and `Usage` objects present results in the UI

## Alternative: Custom QueryExecutor

If references are not fully set up, you can implement a custom `QueryExecutor`:

```xml
<extensions defaultExtensionNs="com.intellij">
    <referencesSearch implementation="fr.adrienbrault.idea.symfony2plugin.templating.findusages.TemplateReferencesSearchExecutor"/>
</extensions>
```

```java
public class TemplateReferencesSearchExecutor implements QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    @Override
    public boolean execute(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
        PsiElement target = queryParameters.getElementToSearch();
        if (!(target instanceof TwigFile)) {
            return true;
        }

        String templateName = getTemplateName((TwigFile) target);

        // Search using existing indexes
        // Process results through consumer.process(ref)

        return true;
    }
}
```

## Key Finding: GotoDeclarationHandler vs PsiReference

### Current State

The Symfony plugin uses **`GotoDeclarationHandler`** for Twig elements, NOT **`PsiReference`**:

- **`TwigTemplateGoToDeclarationHandler`** (`src/main/java/fr/adrienbrault/idea/symfony2plugin/templating/TwigTemplateGoToDeclarationHandler.java`)
  - Handles Ctrl+Click navigation (Go to Declaration)
  - Works for: includes, extends, routes, translations, components, etc.
  - Does NOT provide `PsiReference` objects

- **`TemplateReference`** (`src/main/java/fr/adrienbrault/idea/symfony2plugin/templating/TemplateReference.java`)
  - IS a `PsiPolyVariantReference`
  - Used ONLY for PHP `StringLiteralExpression` elements
  - Registered via `SymfonyPhpReferenceContributor`, `TemplateAnnotationReferences`, etc.

### The Problem

**Find Usages requires `PsiReference` objects.**

Current situation:
| Element Type | Navigation (Ctrl+Click) | Find Usages |
|-------------|------------------------|-------------|
| PHP `$this->render('template.html.twig')` | ✅ Works | ✅ Works (via `TemplateReference`) |
| Twig `{% include 'template.html.twig' %}` | ✅ Works (via `GotoDeclarationHandler`) | ❌ **NOT Working** (no `PsiReference`) |
| Twig `{% extends 'template.html.twig' %}` | ✅ Works (via `GotoDeclarationHandler`) | ❌ **NOT Working** (no `PsiReference`) |
| Twig `{{ include('template.html.twig') }}` | ✅ Works (via `GotoDeclarationHandler`) | ❌ **NOT Working** (no `PsiReference`) |

## Debug Extensions

Created debug extensions to investigate Twig PSI reference coverage:

### 1. TwigReferenceDebugContributor
`src/main/java/fr/adrienbrault/idea/symfony2plugin/debug/TwigReferenceDebugContributor.java`

Logs PsiReference events for Twig STRING_TEXT and IDENTIFIER elements.

Activate in plugin.xml:
```xml
<psi.referenceContributor language="Twig"
    implementation="fr.adrienbrault.idea.symfony2plugin.debug.TwigReferenceDebugContributor"/>
```

### 2. TwigFindUsagesDebugProvider
`src/main/java/fr/adrienbrault/idea/symfony2plugin/debug/TwigFindUsagesDebugProvider.java`

Logs FindUsagesProvider method calls for Twig files.

Activate in plugin.xml:
```xml
<findUsagesProvider language="Twig"
    implementationClass="fr.adrienbrault.idea.symfony2plugin.debug.TwigFindUsagesDebugProvider"/>
```

### 3. TwigReferencesSearchDebugExecutor
`src/main/java/fr/adrienbrault/idea/symfony2plugin/debug/TwigReferencesSearchDebugExecutor.java`

Logs ReferencesSearch queries to understand Find Usages flow.

Activate in plugin.xml:
```xml
<referencesSearch implementation="fr.adrienbrault.idea.symfony2plugin.debug.TwigReferencesSearchDebugExecutor"/>
```

### How to Test

1. Add the debug extensions to `plugin.xml` temporarily
2. Build and run the plugin: `./gradlew runIde`
3. Open a Symfony project with Twig templates
4. Trigger Find Usages on a Twig file (right-click → Find Usages)
5. Check IDE logs: `Help → Show Log in Files`

## Decompiling the Twig Plugin

The Twig plugin is bundled with PhpStorm/IntelliJ. To decompile:

```bash
# Find Twig plugin JAR
find ~/.gradle/caches -name "twig.jar" 2>/dev/null

# Or use the vineflower decompiler
java -jar decompiled/vineflower.jar ~/.gradle/caches/.../twig.jar decompiled/twig-plugin/
```

Key classes to look for:
- `TwigFile` - PSI file implementation
- `TwigReference` - Reference implementations
- `TwigFindUsagesProvider` - Find Usages support (if any)

## Implementation Options

### Option A: Add PsiReference for Twig Elements (Proper Solution)

Create a `PsiReferenceContributor` for Twig that adds references to template names:

```java
public class TwigTemplateReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // {% include 'template.html.twig' %}
        // {% extends 'template.html.twig' %}
        // {{ include('template.html.twig') }}
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withText(PlatformPatterns.string().endsWith(".twig")),
            new TwigTemplateReferenceProvider()
        );
    }
}
```

**Pros:** Proper solution, Find Usages works naturally
**Cons:** Need to create references for all patterns

### Option B: Custom ReferencesSearch Executor (Index-Based)

Use the existing indexes directly without creating PsiReferences:

```java
public class TwigTemplateReferencesSearchExecutor implements QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    @Override
    public boolean execute(@NotNull ReferencesSearch.SearchParameters params, @NotNull Processor<? super PsiReference> consumer) {
        PsiElement target = params.getElementToSearch();
        if (!(target instanceof TwigFile)) return true;

        String templateName = getTemplateName((TwigFile) target);

        // Search TwigIncludeStubIndex
        // Search TwigExtendsStubIndex
        // Search PhpTwigTemplateUsageStubIndex

        // Create synthetic references for results
        return true;
    }
}
```

**Pros:** Uses existing indexes, no need to modify PSI
**Cons:** Less integrated, may not support all Find Usages features

### Option C: Hybrid Approach

1. Add `FindUsagesProvider` for Twig language (enables right-click menu)
2. Add `ReferencesSearch` executor that uses existing indexes
3. Optionally add PsiReferences for better integration

## Summary

The Symfony plugin already has all the necessary indexing infrastructure. The main work needed is:

1. **Implement `FindUsagesProvider`** for Twig language
2. **Add PsiReference for Twig elements** OR **implement ReferencesSearch executor**
3. **Connect to existing indexes** (`TwigIncludeStubIndex`, `TwigExtendsStubIndex`, `PhpTwigTemplateUsageStubIndex`)

The existing indexes already track all the usage locations - they just need to be connected to the Find Usages infrastructure.

## Next Steps

1. Run debug extensions to understand current Twig PSI reference coverage
2. Decompile Twig plugin to see if it provides any reference infrastructure
3. Decide between Option A (PsiReference) or Option B (ReferencesSearch executor)
4. Implement the chosen solution

## Quick Test Instructions

1. Add debug extensions to `plugin.xml` (temporarily):
```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- Debug extensions - remove after testing -->
    <findUsagesProvider language="Twig"
        implementationClass="fr.adrienbrault.idea.symfony2plugin.debug.TwigFindUsagesDebugProvider"/>
    <referencesSearch implementation="fr.adrienbrault.idea.symfony2plugin.debug.TwigReferencesSearchDebugExecutor"/>
    <psi.referenceContributor language="Twig"
        implementation="fr.adrienbrault.idea.symfony2plugin.debug.TwigReferenceDebugContributor"/>
</extensions>
```

2. Build and run: `./gradlew runIde`

3. Open a Symfony project with Twig templates

4. Right-click on a Twig file → Find Usages

5. Check logs: `Help → Show Log in Files` (look for "TwigFindUsagesDebug" and "ReferencesSearch Debug")

## Files Created

- `src/main/java/fr/adrienbrault/idea/symfony2plugin/debug/TwigFindUsagesDebugProvider.java` - Debug FindUsagesProvider
- `src/main/java/fr/adrienbrault/idea/symfony2plugin/debug/TwigReferencesSearchDebugExecutor.java` - Debug ReferencesSearch executor
- `src/main/java/fr/adrienbrault/idea/symfony2plugin/debug/TwigReferenceDebugContributor.java` - Debug PsiReference contributor
