# Twig Component MCP Implementation Plan

## Goal
Expose Symfony UX Twig components to AI agents through a new MCP toolset method, with partial-name search and a CSV output tailored for template generation.

`UxUtil` is the primary source for component discovery and metadata.

## Proposed MCP Tool Contract
- Tool ID: `list_twig_components`
- Class: `src/main/kotlin/fr/adrienbrault/idea/symfony2plugin/mcp/toolset/TwigComponentMcpToolset.kt`
- Method signature:
  - `suspend fun list_twig_components(search: String? = null): String`
- Search behavior:
  - Case-insensitive partial match on component name
  - If `search` is null/blank: return all components

## CSV Output Schema
Header:
`component_tag,template_relative_path,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks`

Column details:
- `component_tag`: HTML syntax, eg `<twig:Alert></twig:Alert>`
- `template_relative_path`: template path relative to project root (first resolved template; multiple entries semicolon-separated if needed)
- `twig_component_syntax`: function syntax, eg `{{ component('Alert') }}`
- `component_print_block_syntax`: print block syntax based on provided blocks, eg `{{ block('footer') }}` (semicolon-separated)
- `twig_tag_composition_syntax`: Twig tag-based composition snippet, eg `{% component 'Alert' %}{% block footer %}{% endblock %}{% endcomponent %}`
- `props`: component props available to template (class exposed vars + template props), semicolon-separated
- `template_blocks`: blocks provided by component template, semicolon-separated

## Architecture and Data Flow
1. Entry point in MCP toolset:
   - Validate Symfony plugin state (`Symfony2ProjectComponent.isEnabled`)
   - Validate MCP tool enabled (`McpUtil.checkToolEnabled(project, "list_twig_components")`)
   - Execute in `readAction`
2. Collector/service layer (new):
   - Add `TwigComponentCollector` (or similarly named) in `src/main/kotlin/fr/adrienbrault/idea/symfony2plugin/mcp/`
   - Responsibility: gather/normalize rows and render CSV
3. Source APIs (mainly `UxUtil`):
   - Components: `UxUtil.getAllComponentNames(project)`
   - Template resolution: `UxUtil.getComponentTemplates(project, componentName)`
   - Class resolution for variable props: `UxUtil.getTwigComponentPhpClasses(project, componentName)` + `UxUtil.visitComponentVariables(...)`
   - Template props: `UxUtil.visitComponentTemplateProps(project, componentName, ...)`
   - Template blocks: `TwigUtil.getBlockNamesForFiles(project, virtualFiles)`
4. Relative path conversion:
   - Use project root via `ProjectUtil.getProjectDir(project)`
   - Convert with `VfsUtil.getRelativePath(...)`, fallback to `FileUtil.getRelativePath(...)`

## Implementation Steps
1. Add collector class for deterministic CSV generation
- File: `src/main/kotlin/fr/adrienbrault/idea/symfony2plugin/mcp/TwigComponentCollector.kt`
- Implement:
  - `collect(search: String?): String`
  - CSV escaping helper (same pattern as existing MCP toolsets)
  - Internal row model for readability and testability

2. Add MCP toolset endpoint
- File: `src/main/kotlin/fr/adrienbrault/idea/symfony2plugin/mcp/toolset/TwigComponentMcpToolset.kt`
- Implement `list_twig_components(search: String? = null)`
- Wire to collector inside `readAction`
- Add complete `@McpDescription` with schema and examples for agents

3. Register new toolset
- File: `src/main/resources/META-INF/mcp.xml`
- Add:
  - `<mcpServer.mcpToolset implementation="fr.adrienbrault.idea.symfony2plugin.mcp.toolset.TwigComponentMcpToolset" />`

4. Add MCP settings entry (enable/disable from UI)
- File: `src/main/java/fr/adrienbrault/idea/symfony2plugin/ui/McpSettingsForm.java`
- Add default tool metadata:
  - Tool ID: `list_twig_components`
  - Name: `Twig Components`
  - Description: includes partial search + syntax/props/blocks output

5. Add tests
- File: `src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/mcp/TwigComponentCollectorTest.java`
- Test cases:
  - CSV header structure matches schema exactly
  - Empty result returns header only
  - Partial search (case-insensitive) returns matching rows only
  - Props include both class-exposed variables and template props
  - Blocks are extracted from template and reflected in print/tag syntax columns
  - Relative paths are project-root relative

6. Optional hardening (if needed during implementation)
- Stable ordering for deterministic output:
  - sort by component name, then template path
- Deduplicate props/blocks with insertion-preserving set
- Bound large syntax columns (if very large block sets) with predictable truncation rule

## Edge Cases to Cover
- Anonymous components (`components/Foo/index.html.twig` => `Foo`)
- Nested components (`Admin:Card:Header`)
- Multiple templates resolved for one component
- Components without PHP class (anonymous): no class props, but template props/blocks still available
- Components without template file: keep row with empty template/blocks
- Live components appearing in `UxUtil.getAllComponentNames`: decide whether to include or filter to Twig-only (recommended: include by default, because syntax is still Twig component oriented; optionally add a future boolean filter)

## Acceptance Criteria
- New MCP method is callable and respects project/tool enablement settings.
- `search` supports partial case-insensitive filtering on component name.
- CSV includes all requested columns and valid examples (`<twig:Alert></twig:Alert>`, `{{ component('Alert') }}`, block/tag composition snippets).
- `template_relative_path` is relative to project root.
- `props` and `template_blocks` columns are populated from existing indexed/project PSI utilities, without full-project brute-force scans.
- Tests pass with `./gradlew test --tests "*TwigComponentCollectorTest"`.

