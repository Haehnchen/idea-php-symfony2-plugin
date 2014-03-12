IDEA PHP Symfony2 Plugin
========================

Plugin url : http://plugins.jetbrains.com/plugin?pr=&pluginId=7219

Autocomplete (or something else) is not working! Help! :open_mouth:
-------------------------------------------------------------------

* Open Documentation [read online](http://symfony2-plugin.espend.de/) or [fork doc](https://github.com/Haehnchen/idea-php-symfony2-plugin-doc)
* You usually need to trigger the autocomplete popup yourself, by pressing CTRL+SPACE (may be a different shortcut depending on your keymap).
* Check your File -> Settings -> Symfony 2 Plugin -> Enable Plugin for this Project
* Many features require the `app/cache/dev/appDevDebugProjectContainer.xml` file to exist. It is generated when you boot your app in dev environment (open `/app_dev.php` in a browser or `php app/console`).
* Routing features require the `app/cache/dev/appDevUrlGenerator.php` file to exist. It is generated the first time a call to `$urlGenerator->generate()` is made at runtime (open a page in your browser that generate at least 1 url).
* Make sure that you use the latest version of the plugin. You can find which you're using in `Settings -> Plugins -> Symfony2 Plugin -> Version`, and which is the latest at http://plugins.jetbrains.com/plugin?pr=&pluginId=7219 .

What? Something still does not work? Damn! :cry:
------------------------------------------------

You can browse the existing issues at https://github.com/adrienbrault/idea-php-symfony2-plugin/issues?state=open .

If your issue already exists, don't hesitate to add a comment to help contributors resolve the issue more easily.
If your issue does not exist, open a new issue :smiley:.

Make sure to provide the maximum amount of informations, such as:
* What version of PHPStorm are you using ?
* What version of the plugin are you using ?
* What kind of project have you enccountered the issue with ? (Symfony 2.1/2.2, etc, custom/full stack ? silex ?)
* The stack trace if an error occured

Installing the plugin
---------------------

The plugin requires that you have the latest PHPStorm EAP build (6.0.1 EAP 129.196, Apr 5), available at http://confluence.jetbrains.com/display/PhpStorm/PhpStorm+Early+Access+Program

You can install the plugin by going to `Settings -> Plugins -> Browse repositories` and then search for `Symfony2`. **RECOMMENDED WAY**

You can also [download the plugin .jar from github](https://github.com/adrienbrault/idea-php-symfony2-plugin/raw/master/symfony2-plugin.jar) and then install it in phpstorm: `Settings -> Plugins -> Install plugin from disk`.

Building & Debugging
--------------------

Open the project in IntelliJ, and follow the steps here : http://confluence.jetbrains.com/display/PhpStorm/Setting-up+environment+for+PhpStorm+plugin+development
