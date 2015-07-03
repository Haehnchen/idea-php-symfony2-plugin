#!/bin/bash

ideaVersion="14.1.4"
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
      wget $url -P ${travisCache};
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


#php
download "https://plugins.jetbrains.com/files/6610/20075/php-141.1534.zip"
unzip -qo $travisCache/php-141.1534.zip -d ./plugins
        
#twig
download "http://plugins.jetbrains.com/files/7303/17519/twig-139.58.zip"
unzip -qo $travisCache/twig-139.58.zip -d ./plugins

download "http://plugins.jetbrains.com/files/7320/19208/php-annotation.jar"
cp $travisCache/php-annotation.jar ./plugins
