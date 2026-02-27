# Twig Component Block Completion: Implementation Plan

## Overview

Provide completion for Twig component blocks when typing inside a `<twig:ComponentName>` tag body. The completion inserts `<twig:block name="blockName"></twig:block>` with the caret positioned between the tags.

## User Experience

### Current Behavior
User types:
```twig
<twig:Alert>
    <ca>
</twig:Alert>
```
No completion available for component blocks.

### Desired Behavior
User types:
```twig
<twig:Alert>
    <ca
</twig:Alert>
```
Completion shows available blocks from `Alert.html.twig` (e.g., `header`, `footer`, `content`).

Selecting `footer` inserts:
```twig
<twig:Alert>
    <twig:block name="footer">|</twig:block>
</twig:Alert>
```
(where `|` is the caret position)

## Technical Design

### 1. Completion Pattern

**Location:** `TwigHtmlCompletionUtil.java`

Add a pattern to detect text content inside a `<twig:ComponentName>` tag:

```java
/**
 * Matches text content inside a <twig:ComponentName> tag (not inside <twig:block>).
 * <twig:Alert><caret></twig:Alert>
 * <twig:Alert>  <caret>  </twig:Alert>
 */
public static PsiElementPattern.Capture<PsiElement> getTwigComponentBodyPattern() {
    return PlatformPatterns.psiElement()
        .withParent(
            PlatformPatterns.psiElement(XmlText.class).withParent(
                PlatformPatterns.psiElement(XmlTag.class).with(new PatternCondition<>("twig:component body") {
                    @Override
                    public boolean accepts(@NotNull XmlTag xmlTag, ProcessingContext context) {
                        String name = xmlTag.getName();
                        // Must be a twig: component, but NOT twig:block
                        return name.startsWith("twig:") && !"twig:block".equals(name);
                    }
                })
            )
        )
        .inFile(XmlPatterns.psiFile()
            .withName(XmlPatterns.string().endsWith(".twig"))
        );
}
```

### 2. Completion Contributor Extension

**Location:** `TwigHtmlCompletionContributor.java`

Add a new completion provider:

```java
// <twig:Alert><caret></twig:Alert>
// Provides completion for blocks defined in Alert component template
extend(
    CompletionType.BASIC,
    TwigHtmlCompletionUtil.getTwigComponentBodyPattern(),
    new CompletionProvider<>() {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            PsiElement position = parameters.getOriginalPosition();
            if (position == null) {
                position = parameters.getPosition();
            }

            if (!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            XmlTag componentTag = PsiTreeUtil.getParentOfType(position, XmlTag.class);
            if (componentTag == null) {
                return;
            }

            String tagName = componentTag.getName();
            if (!tagName.startsWith("twig:") || "twig:block".equals(tagName)) {
                return;
            }

            String rawComponentName = tagName.substring(5);
            Project project = position.getProject();

            String componentName = UxUtil.resolveTwigComponentName(project, rawComponentName);
            if (componentName == null) {
                return;
            }

            // Collect blocks from component template(s)
            Collection<VirtualFile> virtualFiles = new HashSet<>();
            Map<VirtualFile, PsiFile> templateFiles = new HashMap<>();

            for (PsiFile templateFile : UxUtil.getComponentTemplates(project, componentName)) {
                VirtualFile virtualFile = templateFile.getVirtualFile();
                if (virtualFile != null) {
                    virtualFiles.add(virtualFile);
                    templateFiles.put(virtualFile, templateFile);
                }
            }

            if (virtualFiles.isEmpty()) {
                return;
            }

            Map<VirtualFile, Collection<String>> blocks = TwigUtil.getBlockNamesForFiles(project, virtualFiles);
            Set<String> seen = new HashSet<>();

            for (Map.Entry<VirtualFile, Collection<String>> entry : blocks.entrySet()) {
                PsiFile templateFile = templateFiles.get(entry.getKey());
                String typeText = templateFile != null ? templateFile.getName() : null;

                for (String blockName : entry.getValue()) {
                    if (!seen.add(blockName)) {
                        continue;
                    }

                    resultSet.addElement(
                        new TwigComponentBlockLookupElement(blockName, typeText)
                    );
                }
            }
        }
    }
);
```

### 3. Custom LookupElement with Insert Handler

**Location:** `TwigComponentBlockLookupElement.java` (new file)

```java
package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lookup element for Twig component blocks that inserts <twig:block name="..."></twig:block>
 * with caret positioned between the tags.
 */
public class TwigComponentBlockLookupElement extends LookupElement {
    private final String blockName;
    private final String typeText;

    public TwigComponentBlockLookupElement(@NotNull String blockName, @Nullable String typeText) {
        this.blockName = blockName;
        this.typeText = typeText;
    }

    @Override
    public @NotNull String getLookupString() {
        return blockName;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(blockName);
        presentation.setIcon(Symfony2Icons.SYMFONY);

        if (typeText != null) {
            presentation.setTypeText(typeText, true);
        }
    }

    @Override
    public InsertHandler<? super LookupElement> getInsertHandler() {
        return this::handleInsert;
    }

    private void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        Editor editor = context.getEditor();

        // Build the full block tag with closing tag
        // <twig:block name="blockName"></twig:block>
        String endTag = "</twig:block>";
        String content = "<twig:block name=\"" + blockName + "\">" + endTag;

        // Delete the already inserted text (the block name)
        int startOffset = context.getStartOffset();
        int tailOffset = context.getTailOffset();
        context.getDocument().deleteString(startOffset, tailOffset);

        // Insert the full block tag
        PhpInsertHandlerUtil.insertStringAtCaret(editor, content);

        // Move caret between the tags: <twig:block name="...">|</twig:block>
        editor.getCaretModel().moveCaretRelatively(-endTag.length(), 0, false, false, true);
    }
}
```

### 4. Alternative: Inline Insert Handler

If a separate class is not preferred, the insert handler can be implemented inline:

```java
// Inside the completion provider:
for (String blockName : entry.getValue()) {
    if (!seen.add(blockName)) {
        continue;
    }

    String typeText = templateFile != null ? templateFile.getName() : null;
    String endTag = "</twig:block>";

    LookupElementBuilder builder = LookupElementBuilder.create(blockName)
        .withIcon(Symfony2Icons.SYMFONY)
        .withInsertHandler((context, item) -> {
            Editor editor = context.getEditor();

            // Build and insert: <twig:block name="blockName"></twig:block>
            String content = "<twig:block name=\"" + blockName + "\">" + endTag;

            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();
            context.getDocument().deleteString(startOffset, tailOffset);

            PhpInsertHandlerUtil.insertStringAtCaret(editor, content);

            // Position caret between tags
            editor.getCaretModel().moveCaretRelatively(-endTag.length(), 0, false, false, true);
        });

    if (typeText != null) {
        builder = builder.withTypeText(typeText, true);
    }

    resultSet.addElement(builder);
}
```

## Implementation Checklist

- [ ] Add `getTwigComponentBodyPattern()` method to `TwigHtmlCompletionUtil.java`
- [ ] Add completion extension to `TwigHtmlCompletionContributor.java`
- [ ] Create `TwigComponentBlockLookupElement.java` (optional, or use inline approach)
- [ ] Add test class `TwigHtmlBlockCompletionTest.java`
- [ ] Test with nested components
- [ ] Test with anonymous components
- [ ] Test caret positioning

## Test Cases

### Test File Location
`src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/completion/TwigHtmlBlockCompletionTest.java`

### Test Scenarios

```java
public class TwigHtmlBlockCompletionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testBlockCompletionInsideComponentTag() {
        // Setup: Create component class with template containing blocks
        myFixture.addFileToProject("src/Twig/Components/Alert.php", """
            <?php
            namespace App\\Twig\\Components;
            use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;

            #[AsTwigComponent]
            class Alert {
            }
            """);

        myFixture.addFileToProject("templates/components/Alert.html.twig", """
            {% block header %}Header{% endblock %}
            {% block content %}Content{% endblock %}
            {% block footer %}Footer{% endblock %}
            """);

        myFixture.configureByText("test.html.twig", """
            <twig:Alert>
                <ca
            </twig:Alert>
            """);

        assertCompletionContains("header", "content", "footer");
    }

    public void testBlockCompletionInsertsFullTagWithCaretInside() {
        // Similar setup...
        myFixture.configureByText("test.html.twig", """
            <twig:Alert>
                <caret>
            </twig:Alert>
            """);

        // Trigger completion and select "footer"
        // Expected result:
        // <twig:Alert>
        //     <twig:block name="footer">|</twig:block>
        // </twig:Alert>
    }

    public void testNoBlockCompletionInsideTwigBlockTag() {
        // Should NOT provide block completion inside <twig:block>
        myFixture.configureByText("test.html.twig", """
            <twig:Alert>
                <twig:block name="header">
                    <ca
                </twig:block>
            </twig:Alert>
            """);

        assertCompletionNotContains("header", "content", "footer");
    }

    public void testBlockCompletionWithNamespacedComponent() {
        // Test with namespaced component like <twig:Button:Primary>
        myFixture.addFileToProject("src/Twig/Components/Button/Primary.php", """
            <?php
            namespace App\\Twig\\Components\\Button;
            use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;

            #[AsTwigComponent]
            class Primary {
            }
            """);

        myFixture.addFileToProject("templates/components/Button/Primary.html.twig", """
            {% block label %}Label{% endblock %}
            {% block icon %}Icon{% endblock %}
            """);

        myFixture.configureByText("test.html.twig", """
            <twig:Button:Primary>
                <ca
            </twig:Button:Primary>
            """);

        assertCompletionContains("label", "icon");
    }
}
```

## Edge Cases

1. **Empty component template** - No completion if no blocks defined
2. **Anonymous components** - Should work the same way
3. **Multiple template files** - Aggregate blocks from all matching templates
4. **Nested components** - Completion should work inside nested component bodies
5. **Existing `<twig:block>` children** - Don't show blocks that are already overridden in the current scope

## Performance Considerations

- Use `TwigUtil.getBlockNamesForFiles()` which leverages `TwigBlockIndexExtension` (indexed data)
- Use `UxUtil.getComponentTemplates()` which is already cached
- Completion is only triggered inside `<twig:Component>` tags, not everywhere

## Related Files

| File | Purpose |
|------|---------|
| `TwigHtmlCompletionContributor.java` | Main completion contributor |
| `TwigHtmlCompletionUtil.java` | Pattern utilities |
| `TwigComponentBlockLookupElement.java` | Custom lookup element (new) |
| `UxUtil.java` | Component name and template resolution |
| `TwigUtil.java` | `getBlockNamesForFiles()` method |
| `TwigBlockIndexExtension.java` | Block index used by `getBlockNamesForFiles()` |

## Implementation Steps

1. **Add pattern method** to `TwigHtmlCompletionUtil.java`
2. **Add completion extension** to `TwigHtmlCompletionContributor.java`
3. **Create test class** with all test scenarios
4. **Run tests** and verify completion behavior
5. **Manual testing** with a real Symfony UX project

## Notes

- The pattern must exclude `<twig:block>` tags to avoid providing block completion inside block tags (which would be confusing)
- The insert handler must delete the already-inserted lookup string before inserting the full tag
- Caret positioning uses `moveCaretRelatively(-endTag.length(), 0, false, false, true)`
