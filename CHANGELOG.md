Changelog
=========

### Version names
0.12.x: PhpStorm 11 (upcoming)
0.11.x: PhpStorm 8, 9, 10
0.10.x: PhpStorm 7 (no support)
0.9.x: PhpStorm 6 (no support)

### 0.11.112
* Extracting webDeployment plugin deps into external file, this resolves crashes for disabled "Remote Hosts Access" plugin #686

### 0.11.111
* Add twig variable type inspection
* Add translation support for .xliff extension #684
* Add support for multiple routes; deprecates single usage #138
* Add controller test template #584
* Add inspection for deprecated twig variable usage; last level only
* Experimental: Add support for webDeployment plugin (Remote Host); supports external container and routing files on a "Default Server"
* Experimental: Extend "Remote Host" context menu with action to download configured remote files
* Experimental: Background task to download external files

### 0.11.110
* Add controller provider for PHP Toolbox
* Add description to PhpTyes @var syntax and allow multiline doc comments #439
* Add @see doc tag support for twig. supports: relative files, controller names, twig files, classes and methods syntax #439
* Add Symfony 3 controller getParameter shortcut support; migrate container getParameter registrar for supporting all proxy methods and navigation #680
* Create template for controller action on annotation should prioritize html files #681
* Migrate template create annotator to twig namespaces handling to not only support bundle files
* Add twig namespace extension point and provide json file for twig namespace configuration "ide-twig.json" see "Twig Settings" for example

### 0.11.109
* Fix autocomplete route name in php and twig not working since Symfony 2.8 #669
* Implement more annotation controller route naming strategies #673
* Add Doctrine model PHP Toolbox provider

### 0.11.108
* Try to fix "unable to get stub builder", looks like input filter is true always in helper class #630, #617
* Implement PHP Toolbox providers: services, parameter, routes, templates, translation domains
* Fix autocomplete and goto is missing for service ids in DefinitionDecorator #667

### 0.11.107
* Implement twig block name completion workaround; need to strip block tag content on prefixmatcher #563, #390, #460, WI-24362</li>
* Update yaml service template to match Symfony best practices #657 @Ma27
* Add array syntax whitelist for twig "trans" domain extraction and support "transchoice" variable in regex #662
* Update travis test matrix dont allow Java8 and PhpStorm10 failing
* Autowire services must not inspect constructor arguments #664
* Synchronized clearing of CaretTextOverlayListener timer to prevent npe #642
* "Method References" and "Type Provider" are deprecated by now and will replaced by Plugin "PHP Toolbox"

### 0.11.106
* Check null before calling getFormTypeClassOnParameter in FormUtil #650
* Support form getParent syntax of Symfony 2.8 / 3.0 #651
* Dropping service alias "form.type" and "form.type_extension" form sources using interfaces instead
* Add path support, class prefix routes and auto naming for route annotation indexer
* Add new form extension visitor to reuse type visitor and support for nested ExtendedType form; resolves #623 #651

### 0.11.105
* Plugin renaming "Symfony2" -> "Symfony"
* Support yml inline service alias #628
* Support form field types as class constants #623
* Add FormType class constant completion and insert handler #623
* Add form intention action and generator for replace string parameter with class constant #623
* Parse branches level for symfony-installer version combobox; wait for next symfony.com deployment #645, #643
* Add a navigation going from the constraint class to its constraint validator and vice versa #632
* Add Doctrine class constants intention replacement
* Add class constants completion for Doctrine getRepository and intention
* Controller::redirectToRoute should provide routing auto completion #614
* Whitelist twig "set tag" for twig extension references #600
* Dropping all version strings of "Symfony2", which are not system related
* Add support for Symfony 2.8 and 3 using the new directory structure #635, also add auto configuration to set all custom paths and enabled plugin directly out of notification box
* Twig controller method targets now recognize xml and json files to fix @Template annotation doesn't recognize non-html templates #602

### 0.11.104
* Replace deprecated eap "PhpType#add" collection signature with string iteration #611, #622, #627
* Globally provide references for xml "resources" attributes with Bundle and relative path syntax

### 0.11.103
* All service definitions now indexed as json
* Support service alias for weak services #391
* Add deprecated service inspection #608
* Migrate doctrine metadata index to json and fix npe #610

### 0.11.102
* Support command names inside constant and property strings
* Add autowire attribute to blacklist for service argument inspection #616 and add "autowire" and "deprecated" yaml completion
* Add file resource index and add include line marker for routing definition
* Use lazy line marker for class service definitions
* Add route pattern/path provider for Symfony symbol search

### 0.11.101
* Use route names of index for symbol search not only compiler provider
* Secure doctrine metadata indexer for performance reasons #610
* Support Doctrine embedded metadata for xml
* Add field name references on class property for doctrine xml metadata
* Add PhpStorm 10 testing environment
* Double check to not add empty doctrine metadata class to index #615
* Add class name scope for all metadata providers
* Dont provide Doctrine metadata line marker for annotation classes as this results in self navigation #613
* Improvements for completion and navigation of all Doctrine metadata files
* Tag generator indention for yml files is accessible for all services now
* Add new CaretListener extension which shows several type overlays for services

### 0.11.100</h2>
* Add blank fix for empty doctrine repository index value #609

### 0.11.99
* Complete rewrite of Doctrine implementation; metadata on now index #586
* Support Doctrine ODM MongoDB and CouchDB #319
* Add Doctrine relation shortcut for neos / flow annotations
* Add some Doctrine dbal support on newly added metadata index #395
* Add translator.logging parameter #606 @mhor
* Dont display configurable forms inside default project #578, #607, #593
* Fix empty types for all TypeProviders; eg getRepository of Doctrine
* Support more use cases of TypeProvider
* Improve support for Doctrine metadata in xml files #319
* Add navigation for all yaml strings that are possible service names; eg security.yml ids
* Add Doctrine repository linemarker

### 0.11.98
* Full references support for console helpers #243
* Add Doctrine couchdb support; merged into overall odm manager to reuse mongodb implementation
* Doctrine getRepository now returns self instance on an unknown class
* Fix plugin breaks the context menu in the Project view #575 thx @steinarer, #525
* Recursively find bundle context for all related action

### 0.11.97
* Class constant signature fixed in PhpStorm9; provide another workaround for supporting both api levels #541
* Event dispatcher should return event class instance #570
* Catch npe issue with plugin enabled check, for global twig navigation #574
* Add "resource" file references for current directory scope #517
* Add assets completion for "absolute_url" #550
* Refactoring and fixing assets handling in PhpStorm9 #551
* Fix invalid inspection on container expressions in yaml files and add LocalInspection testing asserts #585
* Add Travis PhpStorm8, 9 and eap environment switches

### 0.11.96
* Support priority and multiple registering of getSubscribedEvents in indexer
* Provide service tag list on indexed services for service generator
* Add twig filter and functions to symfony symbol search
* Remove deprecated Symfony sidebar, use symbol search instead #414
* Rename Symfony2 to Symfony in presentable strings #393
* Support ternary and array syntax in twig "include" and "extends" tags</li>
* Route indexer saves nullable string value, catch them in Route constructor #482, #508
* Remove "defaults" key detection for a valid yaml route id #518
* Dont annotate missing twig template in interpolated or concatenated strings #488
* Fix global twig name navigation in php files, because of some api changes #450, #456
* Use CASE_INSENSITIVE_ORDER for service container #537
* Add warning for service name if containing a uppercase char #537
* Remove Nullable or empty key in PhpElementsUtil.getArrayKeyValueMap #549
* Support "Class::class" in form data_class PHP 5.5 #523

### 0.11.95
* Add Doctrine simple_array and json_array for yaml files, on direct interface parsing #555
* Cache: Implement service definition cache layer, invalidates on global psi change #350
* Cache: Implement twig template name cache on psi change invalidation
* Cache: Refactoring TwigExtensionParser and introduce cache
* Cache: Add metadata cache for routing component
* Add PhpClass collector for "kernel.event_listener" events that are defined in xml and yaml #531
* Collect type hints for methods of getSubscribedEvents #531, #529
* Implement support of "kernel.event_listener" events in completion, navigation and method creation argument type hints

### 0.11.94
* Remove postfix completion because its a PhpStorm9 core feature #389
* Improvement template name resolving for overwrites: support parent bundle and app resources; overwrite template linemarker #437
* Add Travis CI infrastructure thx to @Sorien #536, #534
* Whitelist Twig_Environment parameter completion for template name

### 0.11.93
* Add "kernel.event_subscriber" to known tags and provide some more user feedback in error case #511
* Add _self support for twig macros #419
* Fix newline issue in controller template #509
* Add project generator for symfony installer and demo application #475

### 0.11.92
* Optimize service name generator and provide custom javascript strategy for it #362
* Add navigation form options of setRequired, setOptional, setDefined and refactor form options to visitor strategy #502
* Remove double Controller in classname #507
* Optimize form ui handling of service generator, prepare "insert" button, add generator action #362
* Use ContainerAwareCommand for command template

### 0.11.91
* Add support for doctrine xml metadata #319
* Add support for conditional twig extends tags, and replace regular match with pattern style
* Provide twig completion for html form action attribute #497
* Twig template file create quickfix should use PsiManager to support eg vcs #498
* Support for query_builder in entity form field type #426
* Fix npe in doctrine querybuilder chain processor #495
* Fix multiple resolve issues in php type provider

### 0.11.90
* Add CompilerPass intention and generator action for Bundle context #484
* Add support for new configureOptions in replacement for deprecated setDefaultOptions #491, #486, #490
* Add more bundle related file templates in NewGroup
* Fix "Missing argument" in services.yml doesn't keep track of factory methods #492

### 0.11.89
* Add yaml service arguments and tags intention / quickfixes #470
* Add xml tag intention and reuse tagged class interface list also for service generator #470
* Add method psi collector to support parent methods of a command #454
* Overall "Controllers as Services" optimize like navigation, related files, ... #428
* Use more stable PsiElements to find twig trans_default_domain domain name instead of regular expressions #476
* Fix multiResolve issue in method instance checks to resolve issue #477 in multiple project command classes
* Fix wrong inspection for FormTypeExtensions tags #483
* Fix npe in index route arguments #482
* Fix warning for optional xml arguments #485

### 0.11.88
* Add console "getArgument" and "getOption" references #454
* "%" char in xml arguments now is a valid completion event #461
* Initial "Missing Argument" xml service inspection and quickfix #470
* Some performance improvements in xml and yaml service resolving

### 0.11.87
* Add completion for twig tags of Twig_TokenParserInterface::getTag implementations #457
* Add trans / transchoice twig tag 'from' support #459
* Add completion for Twig_SimpleTest extension in twig files after IS token
* Add twig operator completion in IF tags
* Fix pattern of twig trans_default_domain tag and use translation index for domain completion
* Fix several issues in twig array completion #463

### 0.11.86
* Support new setFactory syntax in yaml and xml #436
* Add service generator in class context of "Generator Popover" and intention in arguments
* Add twig assets completion for img src tags #438
* Add some more yaml service key completion
* Add method support for twig "for" statements #208
* Fix instance annotator in yaml psi pattern arguments on single quote string, after pattern api changes
* Fix completion for twig inline array doc block pattern
* Fix insertHandler for trailing backslash in twig doc var completion
* Note: implemented testing infrastructure #405

### 0.11.85
* Fix npe in custom assets resolving #427

### 0.11.84
* Fixing npe in tagged class inspections #425
* Add function parameter generator for "kernel.event_listener" on method create quickfix #424
* Add support for getSubscribedEvents inside method create quickfix #424
* Add support for custom assets #353
* Add static event parameter hint list for method create quickfix

### 0.11.83
* Add inspection for tagged services to validate corresponding interfaces or extends instances
* Add "Method Create" quickfix for xml files
* Add navigation, quickfix and inspections for methods inside tag statements #422
* Fix non unix eol error in template files #421

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
