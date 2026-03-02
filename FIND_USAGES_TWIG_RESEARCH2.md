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

## Summary

The Symfony plugin already has all the necessary indexing infrastructure. The main work needed is:

1. **Implement `FindUsagesProvider`** for Twig language
2. **Ensure `PsiReference` coverage** for all template usage patterns
3. **Optionally add `QueryExecutor`** for index-based search fallback

The existing indexes (`TwigIncludeStubIndex`, `TwigExtendsStubIndex`, `PhpTwigTemplateUsageStubIndex`) already track all the usage locations - they just need to be connected to the Find Usages infrastructure.
