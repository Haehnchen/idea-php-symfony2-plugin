Changelog
=========

### Version names
0.9.x: PhpStorm 6
0.10.x: PhpStorm 7

### 0.9.25 / 0.10.25
* Translation key and domain annotator for php and twig with yaml key creation quick fix
* Hack to support twig filter completion on char type event (see blocker) and goto
* Add yaml and xml service indexer
* Provide a service definition line marker for classes, based on service index
* Some more form builder completions

### 0.9.24 / 0.10.24
* Provide settings for service line marker and disable it on default

### 0.9.23 / 0.10.23
* Provide a service line marker
* Provide goto for class service definition (click on class name) if available in any suitable yaml or xml file
* Optimize twig assets wildcard detection and goto filter
* 0.10.23: Migrate javascripts and stylesheets to be compatible with twig plugin

### 0.9.22
* Add annotator for php instances inside yaml "calls" and "arguments" services
* Add annotator for method names of yaml "calls"
* Fix twig function insert handler insert double braces

### 0.9.21
* Support EventDispatcher calls inside php dispatcher and subscriber
* Improvements of Event and Tag completion / goto in all languages
* Provide global template goto in yaml
* Improvements in xml to reflected features of previous release
* Support locale routing of I18nRoutingBundle

### 0.9.20
* Mass improvements in php Container Builder (setAlias, Definition, Reference, Alias, findTaggedServiceIds)
* Provide goto for tagged container classes in php and yaml
* Support php template files
* Add ui for custom signature type providers
* Improvements in class doc hash provider and add new one #Interface

### 0.9.19
* Many improvements in template detection
* Support for translation_domain inside OptionsResolverInterface:setDefaults
* Hash tag docblocks are now searched on parent methods not only in current file
* New provider for form options

### 0.9.18
* Directly goto into form options definition not only to method
* Add form child name (underscore method) support on form builder resolve from setDefaultOptions:data_class
* Resolve parent calls inside setDefaultOptions eg for getting base form options
* Fix completion option on incomplete array definition (array key)
* Add php type resolve on form type parameter to not only support form types aliases

### 0.9.17
* Refactor of FormTypes reference contributor to provide goto and custom provider
* Provide form extension and default option array key completion / goto inside FormBuilder calls

### 0.9.16
* Improve twig extension parser to support goto and icons
* Provide domain goto and completion for twig trans_default_domain tag
* Add factory_method tag support inside yaml
* "Create Template" annotator is now also available in php and twig render calls

### 0.9.15
* Implement method parameter completion / goto on custom signatures
* Provide method parameter completion / goto on docblock hashtag
* Update help page for new features

### 0.9.14
* Fix for Settings saving
* Support PhpStorm EAP 7 build 130.1293
* Types for getRepository calls dont need backreferences anymore

### 0.9.13
* Add multi container support
* Some improvements for Twig namespace ui
* Settings ui cleanups and improvements
* Implement help page with reStructuredText and Sphinx, available on GitHub
* Assets annotator support wildcard folder

### 0.9.12
* Rework of XML Pattern to not fire on HTML
* Add local Parameter parser for Yaml
* Add local Parameter and Service parser for XML
* Fix all unsecured MethodReference casting
* Make Symfony "web" and "app" folder configurable in Settings form
* Introduce a Twig ui to manage template namespace (beta)

### 0.9.11
* Fix icon issue in PhpStorm 7 EAP
* Support translation and entity goto / completion in FormTypes arrays
* Quickfix to not fire plugin completion in HTML content since it also interpreted as XML

### 0.9.10
* Support Controller:forward in php
* Resolve repositoryClass on yaml or annotation config
* Support transchoice in php and twig
* Use trans_default_domain as fallback translation domain in twig
* Improvements in twig import, set and macro completion / goto
* Controller goto and completion for twig controller function

### 0.9.9
* Fix for parameter completion in yaml
* Provide global template goto in quoted strings of php and twig files
* Support completion and types for ObjectManager::find calls
* Implement twig extension parser to support function and filter completion (need JetBrains fix for full support WI-19022)
* Reduce build limit to make plugin installable on IntelliJ IDEA 12.1.4

### 0.9.8
* Activate doctrine entity PhpTypes on default
* Implement basic event and tag autocomplete in yaml and xml on known container elements
* Add Service method calls autocomplete in yaml and xml
* Implement a current file scope service parser for yaml, so private services are detected
* Add autocomplete for macro imports on "from" tag in twig

### 0.9.7
* Drop outdated PhpTypeProvider which were removed by PhpStorm 6.0.3
* Support new PhpTypeProvider2 to resolve ide freeze
* Fix for twig addPath paths
* Fix for twig template pattern, so include function is supported again
* Some smaller pattern fixes in yaml and php

### 0.9.6
* Add search panel (left sidebar) to find internal known Symfony components and go to them
* Fix assets "web" reader on Linux
* Filter yaml parameter annotator on token values
* Add icons for all known symfony2 components

### 0.9.5
* Add controller services support for go to and autocomplete
* Support strict=false syntax in yaml
* Fix for NullPointerException of plugin enabled check and routing indexing
* Plugin default state is now "Disabled" per project
* Get registered extra twig templates path on addPath of container
* Fix for YamlKeyFinder which provides better matching for translation go to

### 0.9.4

* Provide a global plugin state toggle per project
* Notice: Default plugin state will be "Disabled" in next version
* Provide go to controller method of routing names
* Autocomplete for _controller in yaml
* Support Yaml value with quote value
* Autocomplete and go to for routing resources in yaml
* Add translation go to for translation key in yaml and php, for yaml files
* Yaml Annotator for parameter, service and class
* Many PsiElement Pattern fixes

### 0.9.3

* Add Annotator which mark unknown route, template, service, assets
* Settings form can disable every Annotator, if its not suitable in environment
* Some autocomplete and pattern matches fixes and optimization
* Add autocomplete for class, factory-service, factory-class in yaml and xml
* Add notice for missing route and container file on project startup

### 0.9.2

* Autocomplete for twig blocks
* Go to for extended twig block
* Some twig translation fixes
* Yaml: Php class completion for service parameter ending with .class
* Yaml: Php class completion list service class parameter


### 0.9.1

* Temporary PhpTypes cache which reduce ide freeze (until fixed on JetBrains side)
* Add PhpTypes cache lifetime settings
* Add some more Annotation support
* Add Annotator and Action to create twig file directly on @Template clicking
* Autocomplete for FormTypes in FormBuilder
* Autocomplete of classes in yaml and xml
* Autocomplete for translation in trans twig and translate php
* Optimize twig templates searching, which sometimes generated outdated listing
* Auto use import of some supported Annotation

### 0.9.0

* Support app level twig templates (::layout.html.twig)
* Ability to disable php types providers in the settings (if you eccounter freezes when autocompleting classes etc)
* Support bundles assets (@AcmeDemoBundle/Resources/xxx)
* Add {% javascripts and {% stylesheets assets autocompletion
* Add assets go to file

### 0.8.1

* Should improve performance and fix some issues with use statements

### 0.8.0

* Autocomplete twig files in @Template annotations
* Go to twig file on @Template annotation
* Autocomplete container parameters in php/xml/yaml
* Autocomplete doctrine getRepository argument
* Go to entity class on getRepository argument
* Detect getRepository() result type
* Detect EntityRepository::find/findOneBy/findAll/findBy result type

### 0.7.1

* Add assets autocompletion in twig asset() calls

### 0.7.0

* Fix the fix about ide freezes
* Add auto completion inside doctrine's .orm.yml config files
* Add @ORM\\ annotations auto completion inside docblocks
* Add services auto completion inside yaml files
* Add services auto completion inside services.xml files
* Add class go to definition inside services.xml files

### 0.6.2

* Should fix ide freezes with class autocompletion (use XXX).

### 0.6.1

* Service aliases support
* Resolve services classes case insensitively

### 0.6.0

* Autocomplete route name in php and twig
* Should fix IDE freezes and StackoverflowException etc :)
* Performance improvment
* No more proxy method detection, the plugin has to know them (for example Controller::get)
* Smarter detection of functions call in twig (ie: {% set var = render('<caret>') %} should work)

### 0.5.3

* Fix a bug on windows when using an absolute path for the container.xml

### 0.5.2

* The plugin settings for the container.xml path, has now a file chooser, and allow paths outside the project

### 0.5.1

* Support {% embed autocomplete in twig

### 0.5.0

* Use a symfony2 icon instead of class icon in services autocomplete :)
* You can now click on templates name in twig to go to file (awesome!)
* "{{ include(" now autocomplete the template name

### 0.4.0

* Autocomplete template name in render() calls
* Clickable template name in render() calls
* Autocomplete template name in twig templates

### 0.3.3

* Better description, and integrate the changelog into the plugin

### 0.3.2

* Services id completion popup also show the class on the right

### 0.3.1

* Fix small cache issue

### 0.3.0

* Services id are now clickable (go to class definition), and autocompletable (CTRL+SPACE).
* Should support all ContainerInterface::get proxies as long as the id is the first argument
  (Previously, only direct calls to ContainerInterface::get or Controller::get)

### 0.2.1

* Fixed required idea build

### 0.2.0

* The `appDevDebugProjectContainer.xml` path can now be configured in the project settings.

### 0.1.0

* Detect ContainerInterface::get result type
