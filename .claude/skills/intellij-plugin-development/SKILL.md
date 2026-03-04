---
name: intellij-plugin-development
description: IntelliJ plugin development reference when working on extension points, decompiling bundled plugin JARs, finding API usage examples, navigating IntelliJ SDK, or investigating how other plugins implement a feature.
---

# IntelliJ Plugin Development

Quick reference for tools, APIs, and resources used when developing or investigating IntelliJ/PhpStorm plugins in this project.

---

## SDK & Documentation

- **SDK Docs:** https://plugins.jetbrains.com/docs/intellij/
- **Extension Point List:** https://plugins.jetbrains.com/docs/intellij/extension-point-list.html
- **IntelliJ Community Source:** https://github.com/JetBrains/intellij-community
- **Plugin Template:** https://github.com/JetBrains/intellij-platform-plugin-template

---

## Skills in this folder

| File | Purpose |
|---|---|
| `references/extension-point-explorer.md` | Find plugins by extension point; search open-source implementations; download plugins for decompilation |
| `references/decompilation.md` | Decompile plugin JARs with Vineflower; locate bundled JARs in Gradle cache; download ZIPs from Marketplace |

---

## Decompilation (quick reference)

Always use **Vineflower** — not IntelliJ's bundled Fernflower. A local copy is at `decompiled/vineflower.jar`.

```bash
java -jar decompiled/vineflower.jar input.jar output-src/
```

See [`references/decompilation.md`](./references/decompilation.md) for full usage, bundled JAR paths, and Marketplace ZIP downloads.

---

## Extension Point Explorer (quick reference)

Use the JetBrains Marketplace API to find plugins implementing a given extension point, get GitHub source search URLs or download plugin releases from the marketplace for decompilation.

See [`references/extension-point-explorer.md`](./references/extension-point-explorer.md) for step-by-step instructions.

