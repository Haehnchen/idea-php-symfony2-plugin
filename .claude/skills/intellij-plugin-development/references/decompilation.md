# Decompilation

Guide for decompiling IntelliJ plugin JARs to inspect features and implementation details.

---

## Vineflower (recommended)

Vineflower produces significantly better output than IntelliJ's bundled Fernflower. Always prefer it for decompiling plugin JARs.

- **GitHub:** https://github.com/Vineflower/vineflower
- **Download:** https://repo1.maven.org/maven2/org/vineflower/vineflower/1.11.2/vineflower-1.11.2.jar
- **Local copy in this project:** `decompiled/vineflower.jar`

```bash
# Decompile a single JAR into a source directory
java -jar decompiled/vineflower.jar input.jar output-src/

# Decompile all JARs in a directory
java -jar decompiled/vineflower.jar input-dir/ output-src/
```

---

## Bundled plugin JARs (IntelliJ/PhpStorm)

Gradle downloads plugin dependencies into the local cache. Typical path:

```
~/.gradle/caches/<gradle-version>/transforms/*/transformed/<plugin-id>-<intellij-version>/<plugin>/lib/<plugin>.jar
```

Examples:
```bash
# Twig plugin
~/.gradle/caches/9.3.0/transforms/*/transformed/com.jetbrains.twig-253.28294.322/twig/lib/twig.jar

# PHP plugin
~/.gradle/caches/9.3.0/transforms/*/transformed/com.jetbrains.php-253.*/php/lib/php.jar
```

---

## Downloading a plugin JAR from the Marketplace

Use the JetBrains Marketplace API to fetch the latest release ZIP for any plugin ID (see `extension-point-explorer.md` for how to find plugin IDs):

```bash
PLUGIN_ID="7219"

# Get the ZIP download path
curl -s "https://plugins.jetbrains.com/api/plugins/${PLUGIN_ID}/updates?size=1" \
  | jq -r '"https://plugins.jetbrains.com/files/" + .[0].file'

# Download it directly
FILE=$(curl -s "https://plugins.jetbrains.com/api/plugins/${PLUGIN_ID}/updates?size=1" | jq -r '.[0].file')
curl -L -o plugin.zip "https://plugins.jetbrains.com/files/${FILE}"
unzip plugin.zip -d plugin-src/
```

Then decompile the extracted JAR:
```bash
java -jar decompiled/vineflower.jar plugin-src/lib/plugin.jar output-src/
```
