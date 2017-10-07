IntelliJ IDEA / PhpStorm Symfony Plugin
========================
[![Build Status](https://travis-ci.org/Haehnchen/idea-php-symfony2-plugin.svg?branch=master)](https://travis-ci.org/Haehnchen/idea-php-symfony2-plugin)
[![Version](http://phpstorm.espend.de/badge/7219/version)](https://plugins.jetbrains.com/plugin/7219)
[![Downloads](http://phpstorm.espend.de/badge/7219/downloads)](https://plugins.jetbrains.com/plugin/7219)
[![Downloads last month](http://phpstorm.espend.de/badge/7219/last-month)](https://plugins.jetbrains.com/plugin/7219)
[![Donate to this project using Paypal](https://img.shields.io/badge/paypal-donate-yellow.svg)](https://www.paypal.me/DanielEspendiller)

Key         | Value
----------- | -----------
Plugin Url  | https://plugins.jetbrains.com/plugin/7219
ID          | fr.adrienbrault.idea.symfony2plugin
Changelog   | [CHANGELOG](CHANGELOG.md)

Install
---------------------
* Install the plugin by going to `Settings -> Plugins -> Browse repositories` and then search for `Symfony`.
* Enabled it per project (File -> Settings -> Languages & Framework -> PHP -> Symfony)
* Install [Php Annotation](http://plugins.jetbrains.com/plugin/7320) plugin to enjoy all the annotation stuff
* Plugin needs a valid PhpStorm indexer, use "File > Invalidate Caches / Restart" if something crazy is going on

Version
---------------------

* This plugin supports Symfony 2, 3, 4, ...

Documentation and tutorials
---------------------

* Documentation [read online](http://symfony2-plugin.espend.de/) or [fork doc](https://github.com/Haehnchen/idea-php-symfony2-plugin-doc)
* JetBrains: [Symfony Development using PhpStorm](https://confluence.jetbrains.com/display/PhpStorm/Symfony+Development+using+PhpStorm)
* KnpUniversity: [Lean and Mean Dev with PhpStorm (for Symfony)](https://knpuniversity.com/screencast/phpstorm)
* Slides: [PhpStorm: Symfony2 Plugin](https://www.slideshare.net/Haehnchen/phpstorm-symfony2-plugin)

Autocomplete (or something else) is not working! Help! :open_mouth:
-------------------------------------------------------------------

* You usually need to trigger the autocomplete popup yourself, by pressing CTRL+SPACE (may be a different shortcut depending on your keymap).
* Check your File -> Settings -> Languages & Framework -> PHP -> Symfony -> Enable Plugin for this Project

Building, debugging and other
--------------------

* Open the project in IntelliJ, and follow the steps here : http://confluence.jetbrains.com/display/PhpStorm/Setting-up+environment+for+PhpStorm+plugin+development
* See how to debug and get into PhpStorm dev [german only](http://www.espend.de/artikel/wissenwertes-ueber-die-intellij-idea-phpstorm-plugin-entwicklung.html)
* Want to donate? nice go [here](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5ZTGW6H4Y7MT8)

What? Something still does not work? Damn! :cry:
------------------------------------------------

You can browse the existing issues at https://github.com/Haehnchen/idea-php-symfony2-plugin/issues

If your issue already exists, don't hesitate to add a comment to help contributors resolve the issue more easily.
If your issue does not exist, open a new issue :smiley:.

Make sure to provide the maximum amount of informations, such as:
* What version of Phptorm are you using ?
* What version of the plugin are you using ?
* The stack trace if an error occurred
* Check if you are in PhpStorm eap channel
