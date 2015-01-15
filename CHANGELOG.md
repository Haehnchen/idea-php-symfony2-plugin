Changelog
=========

### Version names
0.11.x: PhpStorm 8
0.10.x: PhpStorm 7 (no support)
0.9.x: PhpStorm 6 (no support)

### 0.11.82
* Add method create quickfix for yaml files #415
* Remove weak service warning #399

### 0.11.81
* Fix multiresolve issues eg in AbstractManagerRegistry::getRepository #403

### 0.11.80
* Add missing route inspection with method creation quickfix
* Add deprecated inspection warning for service classes #375
* Support static string methods in twig filter and respect needs_context, needs_environment options in completion #401 #314
* Allow more valid chars in annotation route index process #400
* Removes newly added leading backslash on phpstorm8 in class inserts #402
* Fix npe case in twig block goto #397

### 0.11.79
* Refactoring routing handling and prepare multiple route files #138
* Smarter route name resolve on indexed names #392, #376, #365
* Add doctrine 2.5 cache methods for class / repository completion #203
* Fixing IndexNotReadyException and "Read access is allowed" for eap changes #370, #383

### 0.11.78
* Add twig embed tag indexer
* Support "include()" function and "embed" tag in twig variable collector
* Experimental: Add postfix completion #389
* Add more possible twig variables syntax from php files
* Add navigation for twig var doc
* Fix error on non unique class name completion in xml, yaml and twig #387
* Remove grouping for code folding, to make strings independent from each other

### 0.10.77 / 0.11.77
* Add weak routes in controller action related popover
* Add index for twig file php usage in render* methods and add variable collector
* Fix for new yaml SCALAR_STRING / SCALAR_DSTRING lexer changes in service instance annotator
* Fix max depth check in getTwigChildList #360
* Fix possible recursive calls in twig variable includes #360
* Note: last version for PhpStorm7!

### 0.10.76 / 0.11.76
* Implement docblock "@var" for twig variables, shortcut without tag is deprecated
* Optimize xlf navigation and references; better getDomainFilePsiElements translation performance
* Provide a global class::method[Action] yaml navigation, usable inside Drupal
* Translation extractor supports text selection
* Provide shortcut completion with function insertHandler for twig tags: href, src (css/js)
* Improve overall support for routes; better xml parser, more data and nicer completion #369
* Fix possible npe in PhpEventDispatcherGotoCompletionRegistrar
* Fix service completion in single quote yaml string values to reflect PhpStorm lexer changes; eg Drupal code convention
* Fix regular expression for trans and transchoice to support more cases #377; also fix some whitespace issues
* Fix npe in NewControllerAction on non bundle context #378

### 0.10.75 / 0.11.75
* Add twig constants navigation, completion and folding #327
* Add references for array methods inside EventSubscriberInterface returns
* Add detection for "kernel.event_subscriber" tag on service builder #352
* Add indexer and references for xliff translations
* Quickfix for missing template will generate "block" and "extends" on directory context
* Better completion for class names in yaml and xml #337
* Fix twig missing translation domain pattern on nested filters #255
* Fix out of range exception in querybuilder parameter completion #371

### 0.10.74 / 0.11.74
* Add button in Settings to clean up plugin related indexes
* Add new isEnabledForIndex check, to not force a manually re-indexing for new projects after enabling plugin
* Add references for array methods inside EventSubscriberInterface returns
* Add completion for parameter in doctrine querybuilder "where" context
* Add support for variadic functions on doctrine querybuilder selects #357
* Our heart method "isCallTo" now supports classes and methods instance checks which are not unique in project
* Cleanup quote wrapping in routes key names of yaml files
* Fix npe in annotation template collector #358
* Fix npe in yaml parameter completion #359
* Fix npe in symbole search for non project context #268
* Fix out of range case in getParameterDefinition #368

### 0.10.73 / 0.11.73
* Fix npe in container parameter completion #351
* Add route requirements and options completion for yaml files

### 0.10.72 / 0.11.72
* Replace Form array options references with goto provider for performance improvements
* Support service container in library paths #347
* Use indexer for service parameter references to support weak file

### 0.10.71 / 0.11.71
* Fix whitespace pattern in twig function pattern #340
* Fixed typo in service generator "tags" should be "tag" on xml files #338
* Add extension point for controller actions related files
* Add extension point for GotoCompletionRegistrar
* Replace PsiReference for form type with GotoCompletionRegistrar #313

### 0.10.70 / 0.11.70
* Add linemarker for doctrine targetEntity relations
* Add doctrine query expr parameter completion
* Add support for querybuilder "from" index parameter #322
* Add completion for doctrine querybuilder alias in "createQueryBuilder" and "from" parameter
* Fix template file resolving for twig "app" resources

### 0.10.69 / 0.11.69
* Reworked twig template name resolving, for massive performance improvements #321
* Fix possible npe in TagReference inside php #331
* Hide first parameter in tail completion of twig extensions if its a Twig_Environment type hint #314
* Support twig file bundle overwrite in app folder #275
* Add reference provider for twig "block" function #266
* Provide "form" fallback on unknown from type and support nested strings #325
* Whitelist ".mongodb.yml" for controller related files
* 0.11: Use NavigationUtil for popups to fix eap api changes #329

### 0.10.68 / 0.11.68
* Provide weak form extension option completion #317
* Speedup form option completion #318
* Add new custom abstract reference replacements for deprecated getVariants #313
* Add weak doctrine namespaces on bundle names #316
* Add twig macro statement scope resolve for variables #315
* Add some missing retina icons #312

### 0.10.67 / 0.11.67
* Add array completion for constraints constructor #304
* Add support for twig.extension and form.type_extension in service generator #308
* Add bundle controller path to resource completion whitelist #307
* Map entity class with orm.yml file as linemarker #309
* Add current namespace resolving for yaml targetEntity #305
* Add class linemarker for yaml entities
* Add doctrine entity column names as lookup tail text in querybuilder completion

### 0.10.66 / 0.11.66
* Add weak tag references for xml and yaml container files
* 0.11.x: build against eap to resolve StringPattern#oneOf issues #299
* 0.11.x: reflect renaming of GotoRelatedFileAction #297

### 0.10.65
* Allow window path style in twig template names #296
* Add service indexer for tags in xml and yaml container files #282
* Add weak form types on new service tag indexer #282

### 0.10.64
* Add completion for repositoryClass in yaml
* Add completion for mappedBy and inversedBy in yaml
* Add referencedColumnName references for yaml and annotations
* Completely remove static doctrine yaml mapping list and use annotations fields
* Fix annotation targetEntity condition
* Prettify form field completion
* (Pls be careful on next PhpStorm 8 eap update!)

### 0.10.63
* Add completion for form alias tag in xml and yaml container files
* Support for yaml sequences in arguments instance annotator
* Service creator adds form alias as tag where possible #281
* Fix typo inside querybuilder resolver for oneToOne relations

### 0.10.62
* Add support for routes in xml files
* Provide twig context variables for include statements
* Fix some whitespace documents issue in yaml files

### 0.10.61
* Add support for doctrine id orm mapping of yaml files
* Add support for yaml CompoundValues inside routes action linemarker #289
* Fix that yaml files starting with whitespace not indexed for routes and services files
* Fix cast error on php array variables of twig types provider #290

### 0.10.60
* Fix translation annotator to not highlight compiled elements #262
* Fix non reload of translations which are outside PhpStorm index #262
* Add per translation file change indicator #262
* Cache twig file linemaker per file change request
* Add linemaker for routes in yaml
* Add duplicate key inspection for container files of yaml and xml
* Add duplicate route name inspection for yaml file

### 0.10.59
* Add extensions for type and reference provider
* Add instance check annotator for service classes of xml arguments
* Add goto for parameter definition inside yaml and xml
* Refactoring of xml service container references to provide many improvements in completion and navigation
* Remove regular expressions from Twig_Extensions parser and use internal lexer to support more use cases
* Add tail text for all Twig extensions and improve navigation

### 0.10.58
* Add completion for yaml config root keys
* Fix npe in config completion #284

### 0.10.57
* Add yaml key completion for config / security files on "config:dump-reference"
* Add completion for QueryBuilder:set
* Make Twig translation key extractor compatible with PhpStorm8 and allow undo #213

### 0.10.56
* Add twig translation extraction action #213
* Fix data_class in form types should autocomplete any class #280
* Add completion for QueryBuilder:(*)where

### 0.10.55
* Finally(?) fix NullPointerException on index values #277, #238
* Optimize Doctrine QueryBuilder chaining method collector to resolve methods and also fix some errors #278, #274
* Reimplementation of Twig @Template goto on PHP Annotations extension #276
* Migrate Route annotator to inspections #273
* Typo fix to support Doctrine OneToOne relations

### 0.10.54
* Initial doctrine querybuilder support, see what is in doc
* Improvements in doctrine field property parser
* Activate twig filter and block name completion for upcoming
  PhpStorm8 (eap); resolved issues WI-19022, WI-20266
* Support entity in sub namespace, use indexer and improve
  performance
* Add repository references for QueryBuilder::update/delete/from #272
* Fix goto for class names without namespace in yaml scalar key #271
* Fix field name array completion for ObjectRepository::findBy
* Fix macro name set collector
* Form field completion should respect property names #259
* Integrate PHP-Annotations plugins as optional dependency
* PHP Annotations: Allow alias for all @Template extension #236
* PHP Annotations: Remove static list fully inside external plugin
* PHP Annotations: Route::service and some other references #245

### 0.10.53
* Fix slow index on large files #261
* Fix weak route annotation goto

### 0.10.52
* Globally use weak service and route index #261
* Add new weak annotator for routes and services
* Add route name indexer for annotation
* Add custom index keys processor for filter them in project context
* Add extension point to load custom doctrine model classes
* Fix annotate blank string values
* Remove duplicate from type completion #260

### 0.10.51
* Add twig macro name indexer
* Add macro include/from indexer and add implements linemarker
* Add custom "Symfony2 Symbol" search (Navigate > Symfony2 Symbol) in replacement for toolwindow #229 (pls report possible keyboard shortcuts :) )
* Add twig macro and service index to symbol search
* Allow null keys in all index related stuff to temporary fix #238
* Strip quoted values inside yaml container indexer

### 0.10.50
* Add twig include indexer
* Add twig linemarker for includes

### 0.10.49
* Add translation key and domain indexer
* Rewrite and refactoring of all translation related stuff
* Make translations available without a compiled file on indexer as weak references
* Improvements in multiline values and quote key files for translation keys
* Rename parameter indexer key name to force a refresh, pls report npe directly to #238 with your environment data if still occur

### 0.10.48
* Improvements in repositoryClass detection of doctrine annotations eg namespaces
* Add typename for repository "find*" lookup elements
* Add support for annotations based models inside "find*" repository calls
* Add extension point for container file loading
* Add "Interface" and "ClassInterface" to type provider #254
* Activate $option key references inside FormTypeInterface, because of working api now #162
* Refactoring of container related linemarkers to fix some npe (api break?) #238

### 0.10.47
* Add support for scss assets #251
* Migrate custom method references provider to variable resolver to support recursive calls
* Add references provider for console HelperSet #243

### 0.10.46
* Add goto for twig "parent" function #246
* Readd parameter class service annotator #242
* Dont use statusbar in phpstorm < 7.1 is not supported #235
* Make several services thread safe and implement npe fixes #237, #238
* Dont fire twig type completion inside string values

### 0.10.45
* Some fixes for phpstorm 7.1.2

### 0.10.44
* Close profiler feature and merge into prod
* Add profiler statusbar widget
* Provide collector for mail, route, controller, template for profiler
* Attach all profiler collector to statusbar widget and provide suitable click targets

### 0.10.43
* Add basic form field support in twig types
* Add twig completion for "form.vars"
* Add ManagerRegistry:getManagerForClass reference provider #231
* Add support for twig form_theme #232
* Add function to twig type whitelist
* Fix some npe in yaml #227

### 0.10.42
* Add twig template folding and strip "Bundle"
* Add twig implements and overwrites block linemarker and provide custom popover #75
* Add basic implementation of enum like completion behavior eg Response::setStatusCode, Request::getMethod
* Add doctrine related files to controller method popup
* Use folding names in related file popup where suitable

### 0.10.41
* Add code folding provider for php with support for route, template and repository
* Add code folding provider for twig path and url function
* Add settings for all code folding provider (default=true)
* Add overwrite linemarker for twig blocks #75
* Add yaml static service config completion (class, arguments, ... )
* Readd twig completion workaround for filters (hell!)
* Fix error on class name with trailing backslash on yaml annotator
* Migrate template references, to resolve #46 fully

### 0.10.40
* Add support for "Navigate > Related Files" (Ctrl+Alt+Home) inside controller action #191
* Rename plugin settings key to more unique name "Symfony2PluginSettings" #209 #122
* Fix accidently removed UrlGeneratorInterface::generate and EntityManager::getReference
* Fix npe and cme in container index #207, #211 (use "File > Invalidate Cache", if issue still occur)

### 0.10.39
* Add support of php shortcut methods for repository, route and service references #46
* Add blank filter for service stub indexes #207

### 0.10.38
* Add parameter references for doctrine findOneBy/findBy, on yaml config
* Add goto model config inside getRepository
* Add type resolver for events name
* Fix missing @ in yaml service builder
* Fix npe in container index #206

### 0.10.37
* Add Doctrine MongoDB repository resolver  #205
* Add autopopup for string completion values
* Add support for more form methods  #162
* Add reference provider for form "options" keys #162 limited by WI-21563
* Add templates for yaml, xml service files and controller
* Service builder is accessible inside project browser context menu of php files
* Fix for missing vendor libs since phpstorm 7.1 #180

### 0.10.36
* Rewrite of all container stuff, which completely based on index now; with massive performance improvements
* Add service builder/generator for classes (beta) #77
* Add private service indexer #197
* Add service parameter indexer
* Add twig variable completion for class interfaces
* Add support for "PHP 5.5 class constant" in PhpTypeProvider, so Entity::class in getRepository is possible #193
* All PhpTypeProvider support references, not only string parameter
* Use parameter/service index in ContainerBuilder context and mark them as "weak" service
* Service LineMarker use service index and provide goto to definition
* Internal workaround for interface with missing trailing backslash WI-21520
* Fix symfony2.4 expressions detected as service #202
* Replace regular expression translation parser with plain psi collector, also allow multiple translation files #195
* getRepository provides goto for entity and also repository #201

### 0.10.35
* Add new method reference provider Parameter #196
* Add FormFactoryInterface::createForm option keys support
* Add Symbol and File contributor "Navigate > Symbol / File" #189
* Support upcoming "Search Everywhere" of PhpStorm 7.1 #189
* Support optional service reference syntax in yaml #194
* Support twig 1.15 "source" function #190
* Translation annotator check global translation file before fallback to yaml parser #195

### 0.10.34
* Add popover line marker to controller method, showing related files like templates and routes
* Add custom insert handle to not add double "@" on resource paths #185
* Add more twig template name normalizer and fix npe #186
* Prevent add empty and testing service to index
* Fix template annotations pattern are not compatible with phpstorm7 #184
* Fix yaml parameter annotator warnings on concatenate strings #188
* Fix parameter case-sensitivity issues #179
* Move repository to Haehnchen/idea-php-symfony2-plugin

### 0.10.33
* Add reference provider for FormInterface::get/has
* Add more twig template name normalizer #182
* Improve twig completion type lookup names

### 0.10.32
* Service container supports "field" elements eg properties and class constants #151
* Better template name detection on non common usage and performance improvements
* Add new method references provider for translation key with possible domain filter  #155
* Implement "twig extends" indexer for upcoming features

### 0.10.31
* Add raw yaml routes parser inside index process to provide line marker for controller actions (limited by RUBY-13914)
* Add new method reference provider ClassInterface
* Add controller line marker for twig file, if a matching file exists
* Xml method reference provider support class parameters eg "calls"
* Twig types support "is" as property shortcut

### 0.10.30
* Add support for twig globals in twig variable types
* Remove twig extension test classes from parser index
* Fix twig file scope variable collector

### 0.10.29
* Add controller variable collector for twig
* Add more twig variables pattern
* Add support for array variables in twig
* Improvement for completion and insert handler of twig variable
* Fix some npe and other exception

### 0.10.28
* Update twig macro pattern to support new twig elements
* Add twig macro alias support
* Add twig variable method resolver for goto provider
* Fix twig route path parameter pattern

### 0.10.27
* Add support for route parameter in php and twig
* Add twig variable type detection with goto and completion
* Add parser for twig globals defined as service and text in container file
* Add twig variable detection on inline doc block with several scopes
* Provide some logs for external file loaders like container. (Help -> Show Log ...)
* Remove deprecated twig workarounds
* Provide native route parser, to get all available route information
* Disable twig block name completion, because its blocked now see WI-20266

### 0.9.26 / 0.10.26
* Add completion, goto and line marker for FormTypeInterface:getParent
* Fix FormBuilderInterface:create signature check
* Last version which support PhpStorm 6

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
