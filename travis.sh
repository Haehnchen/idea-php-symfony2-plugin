#!/bin/bash

ideaVersion="14.1.4"

if [ ! -d ./idea ]; then
    # Get our IDEA dependency
    wget http://download.jetbrains.com/idea/ideaIU-${ideaVersion}.tar.gz

    # Unzip IDEA
    tar zxf ideaIU-${ideaVersion}.tar.gz
    rm -rf ideaIU-${ideaVersion}.tar.gz

    # Move the versioned IDEA folder to a known location
    ideaPath=$(find . -name 'idea-IU*' | head -n 1)
    mv ${ideaPath} ./idea
fi

if [ ! -d ./plugins ]; then
    # Download required plugins
    mkdir plugins
    #php
    wget https://plugins.jetbrains.com/files/6610/20075/php-141.1534.zip

    unzip -qo php-141.1534.zip -d ./plugins
    rm -rf php-141.1534.zip
    #twig
    wget http://plugins.jetbrains.com/files/7303/17519/twig-139.58.zip

    unzip -qo twig-139.58.zip -d ./plugins
    rm -rf twig-139.58.zip

    wget http://plugins.jetbrains.com/files/7320/19208/php-annotation.jar
    mv php-annotation.jar ./plugins
fi

# Run the tests
if [ "$1" = "-d" ]; then
    ant -d -f build-test.xml -DIDEA_HOME=./idea
else
    ant -f build-test.xml -DIDEA_HOME=./idea
fi

# Was our build successful?
stat=$?

if [ "${TRAVIS}" != true ]; then
    ant -f build-test.xml -q clean

    if [ "$1" = "-r" ]; then
        rm -rf idea
        rm -rf plugins
    fi
fi

# Return the build status
exit ${stat}
