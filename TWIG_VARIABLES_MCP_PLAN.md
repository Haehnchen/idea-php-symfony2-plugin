# Plan: `list_twig_template_variables` MCP Tool

## Goal

Add a new MCP tool that, given a Twig template name or project-relative path, returns a CSV of all variables available in that template with their PHP types and first-level accessible properties (as Twig sees them).

## Output Format

```
variable,type,properties
user,\App\Entity\User,"id,email,name,roles,createdAt"
app,\Symfony\Bridge\Twig\AppVariable,"user,request,session,environment,debug,token,flashes"
products,\App\Entity\Product[],"id,title,price,category,isActive"
```

Three columns:
| Column | Description |
|---|---|
| `variable` | The Twig variable name |
| `type` | PHP FQN(s) joined with `\|` (raw from `PsiVariable.getTypes()`, may include `[]` for arrays) |
| `properties` | Comma-separated first-level Twig-accessible properties (methods via get/is/has shortcut + public fields) |

## Files to Create / Modify

### 1. New: `TwigTemplateVariablesCollector.kt`
**Path:** `src/main/kotlin/fr/adrienbrault/idea/symfony2plugin/mcp/TwigTemplateVariablesCollector.kt`

Responsibility: pure logic, no MCP dependencies — takes `Project` + template input string, returns CSV string.

**Logic:**

```
1. Resolve template name (two strategies, same as TwigTemplateUsageCollector):
   a. Direct template name → TwigUtil.getTemplatePsiElements(project, input)
   b. Project-relative file path → ProjectUtil.getProjectDir(project)
        .findFileByRelativePath(input) → TwigUtil.getTemplateNamesForFile() → getTemplatePsiElements()

2. Take the first resolved PsiFile (TwigFile). If none → return "template not found" message.

3. Call TwigTypeResolveUtil.collectScopeVariables(psiFile) to get Map<String, PsiVariable>
   - Note: PsiFile is a PsiElement, so this works directly.

4. For each variable entry (sorted by name):
   a. varName = key
   b. types = PsiVariable.getTypes() → join with "|"
   c. properties = collect first-level accessible names from all resolved PhpClasses:
      - Strip "[]" from type strings to get base class name
      - Use PhpElementsUtil.getClassInterface(project, baseType) → PhpClass
      - From PhpClass:
        • Public methods where isPropertyShortcutMethod(name) → getPropertyShortcutMethodName(name)
        • Public non-static fields → field.name
      - Deduplicate, sort, join with ","

5. Append row to CSV: variable,type,"properties"
```

**Key Java APIs to call (all available via Java interop):**
- `TwigUtil.getTemplatePsiElements(project, name)` → `Collection<PsiFile>`
- `TwigUtil.getTemplateNamesForFile(project, virtualFile)` → `Collection<String>`
- `TwigTypeResolveUtil.collectScopeVariables(psiElement)` → `Map<String, PsiVariable>`
- `TwigTypeResolveUtil.isPropertyShortcutMethod(methodName)` → `Boolean`
- `TwigTypeResolveUtil.getPropertyShortcutMethodName(methodName)` → `String`
- `PhpElementsUtil.getClassInterface(project, fqn)` → `PhpClass?`
- `ProjectUtil.getProjectDir(project)` → `VirtualFile?`

### 2. New: `TwigTemplateVariablesMcpToolset.kt`
**Path:** `src/main/kotlin/fr/adrienbrault/idea/symfony2plugin/mcp/toolset/TwigTemplateVariablesMcpToolset.kt`

Thin MCP wrapper — same structure as `TwigTemplateUsageMcpToolset`:

```kotlin
@McpTool
@McpDescription("...")
suspend fun list_twig_template_variables(
    @McpDescription("Template name (e.g. 'home/index.html.twig') or project-relative path (e.g. 'templates/home/index.html.twig')")
    template: String
): String
```

Calls `readAction { TwigTemplateVariablesCollector(project).collect(template) }`.

Tool ID for settings check: `"list_twig_template_variables"`.

### 3. Modify: `src/main/resources/META-INF/mcp.xml`

Add one line:
```xml
<mcpServer.mcpToolset implementation="fr.adrienbrault.idea.symfony2plugin.mcp.toolset.TwigTemplateVariablesMcpToolset" />
```

## Edge Cases

| Case | Handling |
|---|---|
| Template not found | Return header row + empty (or a comment row) — do NOT mcpFail, it just means no match |
| Type is primitive (`string`, `int`, `bool`, `mixed`) | No properties row — leave `properties` empty |
| Type ends with `[]` | Strip `[]` to resolve base class; type column keeps `[]` suffix |
| Multiple types on one variable | Join types with `\|`; collect properties from ALL resolved classes merged |
| Constructor / `__construct` in methods | Excluded automatically (not a shortcut method) |
| Non-public methods/fields | Skip — Twig only sees public members |

## Property Collection Detail

Mirrors what Twig's `getTwigPhpNameTargets` does, but inverted: instead of asking "does this class have property X?", we ask "what properties does this class expose?":

```
For each public Method m in phpClass.getMethods():
    if isPropertyShortcutMethod(m.name):  // starts with get/is/has and has suffix
        add getPropertyShortcutMethodName(m.name)
    else if m is public and not constructor and not __*:
        add m.name   // direct method call like {{ user.validate() }}

For each public non-static Field f in phpClass.getFields():
    add f.name
```

Sort and deduplicate the final set.

## Summary of New Files

```
src/main/kotlin/.../mcp/
    TwigTemplateVariablesCollector.kt       ← new
    toolset/
        TwigTemplateVariablesMcpToolset.kt  ← new

src/main/resources/META-INF/mcp.xml         ← +1 line
```
