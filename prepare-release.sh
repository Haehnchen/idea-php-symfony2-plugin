#!/usr/bin/env bash

# --- Find the last tag and calculate new version ---
LAST_TAG=$(git describe --tags --abbrev=0)
LAST_BUILD=$(echo "$LAST_TAG" | awk -F. '{print $NF}')
YEAR=$(echo "$LAST_TAG" | awk -F. '{print $1}')
MAJOR=$(echo "$LAST_TAG" | awk -F. '{print $2}')
NEW_BUILD=$((LAST_BUILD + 1))
NEW_TAG="${YEAR}.${MAJOR}.${NEW_BUILD}"

echo "Last tag: $LAST_TAG -> New tag: $NEW_TAG"

# --- Update version in files ---
sed -i -E "s/^(pluginVersion[[:space:]]*=[[:space:]]*).*/\1${NEW_TAG}/" gradle.properties
sed -i -E "s|(<version>)[^<]+(</version>)|\1${NEW_TAG}\2|" src/main/resources/META-INF/plugin.xml

git submodule update --init --recursive

echo -e "<html>\n<ul>" > change-notes.html
git log `git describe --tags --abbrev=0`..HEAD --no-merges --oneline --pretty=format:" <li>%s (%an)</li>" | sed -E 's/#([0-9]+)/<a href="https:\/\/github.com\/Haehnchen\/idea-php-symfony2-plugin\/issues\/\1">#\1<\/a>/g' >> change-notes.html
echo -e "\n</ul>\n</html>" >> change-notes.html

cp change-notes.html src/main/resources/META-INF/

rm change-notes.html

# --- Prepend changelog entry to CHANGELOG.md ---
{
    echo "## ${NEW_TAG}"
    git log "${LAST_TAG}"..HEAD --no-merges --pretty=format:"%s (%an)" | \
        grep -v "^build [0-9]" | \
        sed -E 's/#([0-9]+)/[#\1](https:\/\/github.com\/Haehnchen\/idea-php-symfony2-plugin\/issues\/\1)/g' | \
        while IFS= read -r line; do [ -n "$line" ] && echo "* ${line}"; done
    echo ""
} > /tmp/changelog_new_entry.md

if grep -q "^## " CHANGELOG.md; then
    {
        sed -n '1,/^## /{ /^## /!p }' CHANGELOG.md
        cat /tmp/changelog_new_entry.md
        sed -n '/^## /,$p' CHANGELOG.md
    } > CHANGELOG.md.tmp
    mv CHANGELOG.md.tmp CHANGELOG.md
else
    cat /tmp/changelog_new_entry.md >> CHANGELOG.md
fi
rm -f /tmp/changelog_new_entry.md

echo ""
echo "=== Release $NEW_TAG prepared ==="
echo "Commit with:  git commit -am \"build $NEW_TAG\""
echo "Tag with:     git tag $NEW_TAG"
