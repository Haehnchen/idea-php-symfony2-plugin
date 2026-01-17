#!/usr/bin/env bash

git submodule update --init --recursive

echo -e "<html>\n<ul>" > change-notes.html
git log `git describe --tags --abbrev=0`..HEAD --no-merges --oneline --pretty=format:" <li>%s (%an)</li>" | sed -E 's/#([0-9]+)/<a href="https:\/\/github.com\/Haehnchen\/idea-php-symfony2-plugin\/issues\/\1">#\1<\/a>/g' >> change-notes.html
echo -e "\n</ul>\n</html>" >> change-notes.html

cp change-notes.html src/main/resources/META-INF/

rm change-notes.html
