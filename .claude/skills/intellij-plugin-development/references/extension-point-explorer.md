# IntelliJ Extension Point Explorer

Use this skill to find plugins that implement a given IntelliJ extension point and to get GitHub source code search URLs for usage examples.

This skill also covers **downloading any plugin for decompilation and feature investigation** — see Section 3.

> **Important:** Extension point names are **case-sensitive** (e.g., `com.intellij.psi.referenceContributor` ≠ `com.intellij.psi.ReferenceContributor`). Use the exact name as declared in `plugin.xml`.

---

## 1. Search for extension points by keyword

Before querying for plugins, find the **exact extension point name** by searching the JetBrains Marketplace extension point registry. Search is **case-insensitive** (the filtering is done client-side with `grep -i`), but the name you pass to Step 1 must be **exact and case-sensitive**.

```bash
# Partial search — find all extension points containing a keyword
KEYWORD="completion"

curl -s "https://plugins.jetbrains.com/api/extension-points" \
  | jq -r '.[].implementationName' \
  | grep -i "$KEYWORD" \
  | sort
```

Examples:
- `KEYWORD="contributor"` → lists all `*contributor*` extension points
- `KEYWORD="completion.contributor"` → narrows to completion contributors
- `KEYWORD="inspection"` → lists all inspection-related extension points

Once you have the exact name (e.g., `com.intellij.completion.contributor`), use it in Step 2.

---

## 2. Find plugins implementing an extension point

Query the JetBrains Plugin Repository GraphQL API to find open-source plugins that use a specific extension point:

```bash
EXTENSION_POINT="com.intellij.psi.referenceContributor"

curl -s -X POST "https://plugins.jetbrains.com/api/search/graphql" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"{ plugins(search: { max: 24, offset: 0, filters: [{ field: \\\"fields.extensionPoints\\\", value: \\\"${EXTENSION_POINT}\\\" }, { field: \\\"hasSource\\\", value: \\\"true\\\" }, { field: \\\"family\\\", value: \\\"intellij\\\" }], sortBy: DOWNLOADS }) { total, plugins { id, name, downloads, sourceCodeUrl, lastUpdateDate, organization { id, verified } } } }\"}" \
  | jq -r --arg ep "$EXTENSION_POINT" '
    .data.plugins |
    "Total plugins found: \(.total)\n",
    (.plugins[] | select(.sourceCodeUrl != null and .sourceCodeUrl != "") |
      "Plugin:    \(.name)",
      "ID:        \(.id)",
      "Downloads: \(.downloads)",
      "Source:    \(.sourceCodeUrl)",
      "Updated:   \(.lastUpdateDate)",
      "Verified:  \(.organization.verified // false)",
      "Marketplace: https://plugins.jetbrains.com/plugin/\(.id)",
      "Search:    \("https://github.com/search?q=" + ("repo:" + (.sourceCodeUrl | ltrimstr("https://github.com/") | rtrimstr("/")) + " " + $ep | @uri) + "&type=code")",
      "---"
    )
  '
```

## 3. Download a plugin for decompilation and feature investigation

> **Note:** This section is **not** limited to extension point research. Use it any time you want to download a plugin JAR/ZIP for decompilation, reverse engineering, or feature investigation — regardless of how you found the plugin ID.

The plugin ID can come from:
- **Step 2** results (`ID: 7219`)
- A Marketplace URL: `https://plugins.jetbrains.com/plugin/<ID>`

The `sourceCodeUrl` (GitHub URL) from Step 2 gives you two options:
- **Search the repo** for usage examples → use the `Search:` URL from Step 2 output, or browse `https://github.com/<owner>/<repo>`
- **Download the latest release** for decompilation → use the Marketplace API below to get the direct ZIP URL

Given a plugin ID, fetch its metadata and the direct ZIP download URL of the latest release:

```bash
PLUGIN_ID="7219"

# Plugin metadata
curl -s "https://plugins.jetbrains.com/api/plugins/${PLUGIN_ID}" \
  | jq '{
      id: .id,
      name: .name,
      xmlId: .xmlId,
      downloads: .downloads,
      source: .urls.sourceCodeUrl,
      marketplace: ("https://plugins.jetbrains.com/plugin/" + (.id | tostring))
    }'

# Latest release — includes direct ZIP download URL
curl -s "https://plugins.jetbrains.com/api/plugins/${PLUGIN_ID}/updates?size=1" \
  | jq '.[0] | {
      version: .version,
      date: (.cdate | tonumber / 1000 | strftime("%Y-%m-%d")),
      downloads: .downloads,
      size_kb: (.size / 1024 | floor),
      channel: (if .channel == "" then "stable" else .channel end),
      download_url: ("https://plugins.jetbrains.com/files/" + .file),
      notes: (.notes | gsub("<[^>]+>"; "") | gsub("&gt;"; ">") | gsub("&lt;"; "<") | gsub("&amp;"; "&") | split("\n") | map(select(length > 0)) | .[:5] | join("\n"))
    }'
```

Once you have the ZIP URL, download and decompile with **vineflower** (see [`references.md`](./references.md)):
```bash
curl -L -o plugin.zip "https://plugins.jetbrains.com/files/7219/974671/Symfony_Plugin-2026.1.289.zip"
unzip plugin.zip -d plugin-extracted/
java -jar decompiled/vineflower.jar plugin-extracted/lib/plugin.jar decompiled-src/
```

---

## Notes

- **Case sensitivity:** `com.intellij.completion.contributor` ≠ `com.intellij.CompletionContributor` — always verify the exact name in `plugin.xml` or IntelliJ SDK docs.
- The API returns max 24 results per request; use `offset` to paginate.
- `sortBy` options: `DOWNLOADS`, `UPDATE_DATE`, `RATING`.
- Only plugins with `hasSource: true` and a non-empty `sourceCodeUrl` are useful for code examples.
- `jq` and `curl` are required; `jq` must support `@uri` (version ≥ 1.6).
- For decompilation tooling and API references, see [`references.md`](./references.md).
