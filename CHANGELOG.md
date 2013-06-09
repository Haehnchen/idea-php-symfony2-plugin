Changelog
=========

### 0.9.x

* Provide a global plugin state toggle per project
* Notice: Default plugin state will be "Disabled" in next version
* Provide go to controller name for routing names
* Compilation for _controller on yaml
* Support Yaml value with quote value
* Compilation and go to for routing resources in yaml
* Add translation go to for translation key in yaml and php, for yaml files
* Yaml Annotator for parameter, service and class

### 0.9.3

* Add Annotator which mark unknown route, template, service, assets
* Settings form can disable every Annotator, if its not suitable in environment
* Some compilation and pattern matches fixes and optimization
* Add compilation for class, factory-service, factory-class in yaml and xml
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
