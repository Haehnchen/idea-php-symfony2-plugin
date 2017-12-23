#!/bin/bash

ideaVersion="2017.1"
if [ "$PHPSTORM_ENV" == "2017.2" ]; then
    ideaVersion="2017.2.5"
elif [ "$PHPSTORM_ENV" == "2017.2.4" ]; then
    ideaVersion="2017.2.5"
elif [ "$PHPSTORM_ENV" == "2017.3.2" ]; then
    ideaVersion="2017.3.2"
elif [ "$PHPSTORM_ENV" == "eap" ]; then
    ideaVersion="163.5644.15"
fi

travisCache=".cache"

if [ ! -d ${travisCache} ]; then
    echo "Create cache" ${travisCache} 
    mkdir ${travisCache}
fi

function download {

  url=$1
  basename=${url##*[/|\\]}
  cachefile=${travisCache}/${basename}
  
  if [ ! -f ${cachefile} ]; then
      wget --no-check-certificate $url -P ${travisCache};
    else
      echo "Cached file `ls -sh $cachefile` - `date -r $cachefile +'%Y-%m-%d %H:%M:%S'`"
  fi  

  if [ ! -f ${cachefile} ]; then
    echo "Failed to download: $url"
    exit 1
  fi  
}

# Unzip IDEA

if [ -d ./idea  ]; then
  rm -rf idea
  mkdir idea
  echo "created idea dir"  
fi

# Download main idea folder
download "http://download.jetbrains.com/idea/ideaIU-${ideaVersion}.tar.gz"
tar zxf ${travisCache}/ideaIU-${ideaVersion}.tar.gz -C .

# Move the versioned IDEA folder to a known location
ideaPath=$(find . -name 'idea-IU*' | head -n 1)
mv ${ideaPath} ./idea
  
if [ -d ./plugins ]; then
  rm -rf plugins
  mkdir plugins
  echo "created plugin dir"  
fi

if [ "$PHPSTORM_ENV" == "2017.2" ]; then

    #php
    download "http://phpstorm.espend.de/files/proxy/phpstorm-2017.2-php.zip"
    unzip -qo $travisCache/phpstorm-2017.2-php.zip -d ./plugins

    #twig
    download "http://phpstorm.espend.de/files/proxy/phpstorm-2017.2-twig.zip"
    unzip -qo $travisCache/phpstorm-2017.2-twig.zip -d ./plugins


elif [ "$PHPSTORM_ENV" == "2017.2.4" ]; then

    #php
    download "http://phpstorm.espend.de/files/proxy/phpstorm-2017.2.4-php.zip"
    unzip -qo $travisCache/phpstorm-2017.2.4-php.zip -d ./plugins

    #twig
    download "http://phpstorm.espend.de/files/proxy/phpstorm-2017.2.4-twig.zip"
    unzip -qo $travisCache/phpstorm-2017.2.4-twig.zip -d ./plugins

elif [ "$PHPSTORM_ENV" == "2017.3.2" ]; then

    #php
    download "http://phpstorm.espend.de/files/proxy/phpstorm-2017.3.2-php.zip"
    unzip -qo $travisCache/phpstorm-2017.3.2-php.zip -d ./plugins

    #twig
    download "http://phpstorm.espend.de/files/proxy/phpstorm-2017.3.2-twig.zip"
    unzip -qo $travisCache/phpstorm-2017.3.2-twig.zip -d ./plugins

elif [ "$PHPSTORM_ENV" == "eap" ]; then

    #php
    download "https://plugins.jetbrains.com/files/6610/28510/php-163.4830.18.zip"
    unzip -qo $travisCache/php-163.4830.18.zip -d ./plugins

    #twig
    download "https://plugins.jetbrains.com/files/7303/28516/twig-163.4830.18.zip"
    unzip -qo $travisCache/twig-163.4830.18.zip -d ./plugins

    # TODO: extract latest builds for plugins from eap site they are not public
    # https://confluence.jetbrains.com/display/PhpStorm/PhpStorm+Early+Access+Program
    # echo "No configuration for PhpStorm: $PHPSTORM_ENV"
    # exit 1

else
    echo "Unknown PHPSTORM_ENV value: $PHPSTORM_ENV"
    exit 1
fi

rm -f $travisCache/php-annotation.jar
download "http://phpstorm.espend.de/files/proxy/plugins/php-annotation-5.2.jar"
cp $travisCache/php-annotation-5.2.jar ./plugins/php-annotation.jar

rm -f $travisCache/php-toolbox.jar
download "http://phpstorm.espend.de/files/proxy/plugins/php-toolbox.jar"
cp $travisCache/php-toolbox.jar ./plugins

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