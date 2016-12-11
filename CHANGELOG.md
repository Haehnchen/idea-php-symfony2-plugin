Changelog
=========

### Version names
* 0.12.x: PhpStorm 2016.1+
* 0.11.x: PhpStorm 8, 9, 10 (no support)
* 0.10.x: PhpStorm 7 (no support)
* 0.9.x: PhpStorm 6 (no support)

### 0.12.132
* Support more OptionsResolver options method parameter for references [#821](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/821)
* Add decorates linemarker for yaml and xml container files
* Service ids should be autocompleted for decorates [#834](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/834)
* Add PhpStorm 2016.3 / 2016.3.1 travis environment

### 0.12.131
* Disable Twig icon provider; performance issue? [#809](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/809)
* Support new IF_TAG, SET_TAG tokens for Twig function pattern and fix Twig class constant usages
* Twig class constant string need to be slash escaped
* Add references for 'argument type="constant"' inside container services
* Drop some old PhpStorm / Intellij api workarounds
* Drop all Yaml scalar value workarounds for service and parameter pattern
* Fix no autocomplete for SVG assets [#753](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/753)
* Add form placeholder options to translatable value
* Provide route name completion in routes using the RedirectController [#386](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/386), also support template names for TemplateController
* Add {% endtrans %} and {% endtranschoice %} to autocomplete in Twig templates [#599](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/599)
* Add XLIFF navigation and translation generation if "*.xlf" / "*.xliff" extension is defined as XmlFile [#479](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/479), [#712](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/712)
* Add PHP validator translation references for constraint message, ExecutionContextInterface and ConstraintViolationBuilderInterface

### 0.12.130
* Add indexer for template usages in annotations [#773](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/773)
* Add scope for template index to reduce variable extraction and improve performance [#800](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/800)
* Template usages now also support function scope

### 0.12.129
* Fix navigation for bundle files on linux based system, increase path limit for child path iteration [#803](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/803)

### 0.12.128
* Decouple Twig namespace loading and provide more default namespace which work without a compiled container [#784](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/784) [#654](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/654)
* Add recursive and directory limit for per Twig path visitor [#800](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/800)
* Add icon provider for Twig template files for extends and implementations
* Dropping PhpStorm8 type class constant api workaround

### 0.12.127
* Profiler should support http urls as data source [#798](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/798)
* Profiler in now configurable in plugins settings [#798](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/798)
* Fix app_dev.php urls in profiler [#540](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/540), [#522](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/522)
* Add xml completion, navigation and linemarker for Doctrine 2.5 "Embeddables" [#471](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/471)

### 0.12.126
* Fix empty PSI elements should not be passed to createDescriptor in container case sensitivity inspection [#788](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/788)
* Support public property for form field mapping and dropping custom Doctrine field mapping its part of PropertyAccess component [#786](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/786)
* Fix "Cannot resolve symbol" for factory service regression and drop deprecated getVariant references for factory method completion [#791](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/791)
* Add linemarker provider for decorated services with lazy definition navigation
* Replace timer for caret listener with executor and future pattern [#785](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/785)
* Add linemarker for config tree builder root definition in [security,config]*.yml files and provide navigation for key itself [#793](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/793)
* Fix subscriber method creation type hint class was not imported and fix possible memory leak because of PsiElement references

### 0.12.125
* Dont index translations files without domain prefix
* Add twig path configuration parser of yaml files [#654](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/654)
* Support xml factory method and class tag [#778](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/778)
* Api migration for upcoming PhpStorm 2016.3 eap [#782](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/782)
* Smarter default namespace detection for default Domain of translations extraction dialog for injected html [#776](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/776)
* Add support for "twig.paths" as "add path" Twig namespaces [#654](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/654)

### 0.12.124
* Fix form "csrf_protection" was not found because of Symfony 3.0 interface drop
* Add static "FormType" fallback and visit method "setDefaultOptions", "configureOptions" for extension key
* Support translation_domain and default keys for form OptionsResolver implementation
* Rename "Symfony Installer" to "Symfony" in new project dialog
* Use IntelliJ DialogWrapper for dialog boxes of file templates
* Add service id completion for xml attribute value on class attribute
* Add completion for service id arguments without type attribute but valid service parent

### 0.12.123
* Autocomplete service ids for ContainerBuilder::removeDefinition, removeAlias [#761](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/761)
* Add service resolving for tool box provider and provide tests
* Add doctrine dbal querybuilder "delete" references
* Strip "\Bundle\" only namespaces in default service naming strategy
* Fix route reference not showing in controller that is in a sub namespace on slashes [#763](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/763)
* Add references for Twig blocks in embed tag [#361](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/361), [#513](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/513)
* Fix npe in xml parameter attribute values [#766](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/766)
* Dialog of template creation dialog, translation key extractor, service generator should be relative to editor component
* Made ServiceArgumentSelectionDialog closable on ESC [#751](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/751) @kstenschke
* Support trans_default_domain in embed [#660](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/660)
* Let `trans_default_domain` autocomplete put quotes around domain [#526](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/526)
* Fix Document block for EventSubscriber method creation in PhpStorm >= 2016.1 [#745](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/745)

### 0.12.122
* Service generator should close on escape key event
* Fix nullable condition on service container builder [#754](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/754)
* Fix yaml does not autocomplete route host option [#756](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/756)
* Settings for the plugin may be better placed inside the PHP group, like other frameworks [#735](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/735)
* EAP: Fix nullable value index for container parameter [#737](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/737)
* Fix possible memory leaks in settings because of project reference
* Add navigation for yaml constant "!php/const:" syntax
* Internally: Dropped all container service source, just one collection now

### 0.12.121
* Add support for decorator inner services [#510](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/510)
* Fix NPE exception in RouteHelper [#750](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/750)
* Fix NullPointerException in FormFieldResolver [#747](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/747)
* Add navigation for controller annotation [#748](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/748)
* Service parent key completion should only be valid inside service scope [#744](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/744)

### 0.12.120
* Don't report standalone yaml colon in mapping value @xabbuh [#733](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/733)
* "request" service should only be visible inside supported Symfony version < 3.0
* Inspection for deprecations should only be available with their corresponding Symfony versions [#734](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/734)
* Drop IntelliJ platform api usage of CompletionProgressIndicator: "it's pretty private API, and current usages make it very hard to change things" [#732](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/732)
* Add "shared" yaml completion for replacement of "scope" in Symfony 3.0</li>
* Yaml class autocompletion should only complete inside services for OroCRM plugin [#728](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/728)
* Add Intellij 2016.1.2 and eap channel environment for travis testing
* Fix equals / hashCode violation for eap channel and next PhpStorm release on all indexes and globally force a reindex [#737](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/737)
* Migrate all indexes to object serializable objects
* Add support for new autowiringTypes container property [#699](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/699)
* Extend container tag name completion with index of findTaggedServiceIds [#740](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/740); fix private tags are not autocompleted [#216](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/216)
* Fix private services completed in PHP scope

### 0.12.119
* Add extension points to allow service collecting for external plugins
* Add extension point to locate service declaration in file
* Move default services from static file to collector

### 0.12.118
* Fixing npe in service generator intention [#722](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/722)
* Implement lock for timer clear on caret listener [#722](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/722)
* Add index to provide autocompletion for DIC parameters defined dynamically [#478](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/478)
* Convert service name to lowercase in index and xml navigation should navigation to service name case insensitive
* Add service container class name variants if definition not unique in project
* Add @Event annotation indexer; provide completion, navigation, method type hint [#493](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/493)
* Fix exception Accessing indexes from PhpTypeProvider2 while building indexes violates contract [#670](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/670)
* Fix missing Yaml deprecation detection for colon in unquoted values [#719](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/719)
* Add method type hint class importer for subscriber events [#564](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/564)
* Dropping weak route name inspection, no need for this anymore
* Refactoring route to use object serialization, add route method index and force reindexing [#725](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/725)
* Add inspection for form types as name deprecation; includes a quickfix

### 0.12.117
* Use popover for xml container tag suggestion
* Add class name completion for service generator dialog
* Service generator can now directly insert yaml services
* Some yaml ascii char dont need to be escaped, fix inspection for them and reduce deprecated warning to weak notification [#693](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/693)
* Migrate yaml argument creation and update callback to new lexer
* Add service completion suggestion / highlights for service arguments

### 0.12.116
* Migrate yaml routing controller navigation feature
* Migrate yaml config completion
* Migrate yaml sequence item usages, to fix wrong parameter resolving in call and arguments keys [#710](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/710)
* PhpUse#getOriginal is deprecated, use #getFQN instead @artspb
* Use yaml core utils to generate keys for translation, also support nested keys again [#708](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/708), [#711](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/711)

### 0.11.115 / 0.12.115
* Migrate our yaml features to new yaml plugin and support PhpStorm 2016.1 [#626](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/626)
* Provide additional text for yaml route keys completion
* Add quick fix for wrong service instance [#566](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/566); Use popup overlay for suggestion
* Respect formatting for generated service definitions [#374](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/374)
* Activate xml service generator insert button
* Add deprecated inspection for route and container settings in yaml and xml files
* Add Symfony 2.8 / 3.1 YAML deprecations inspections [#693](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/693), [#601](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/601)
* Fix definition created by "Generate Symfony2 service" is invalid because yaml deprecations [#638](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/638), [#693](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/693)

### 0.11.114
* Reduce access of read thread in webDeployment jobs [#694](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/694)
* Replace custom "instanceof" implementation with core isConvertibleFrom
* Fixing yaml class instance checks for single quote strings
* Increase testing coverage for mainly used yaml related features

### 0.11.113
* Decouple all webDeployment dependencies to extensions points and make all related feature optional [#688](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/688)
* Move remote container files parsing to main service factory, this simulates a local filesystem behavior
* Move plugin settings under "Languages and Frameworks" section [#690](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/690)
* Add Symfony 2 and 3 default routing paths to new implementation
* Add service suggestion intention for yaml and xml container files
* Provide service name suggestion quickfix for class instance check of xml and yaml container arguments
* Add XLIFF 2.0 support [#692](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/692)
* Add some more yaml service keys completion for newly added Symfony features

### 0.11.112
* Extracting webDeployment plugin deps into external file, this resolves crashes for disabled "Remote Hosts Access" plugin [#686](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/686)

### 0.11.111
* Add twig variable type inspection
* Add translation support for .xliff extension [#684](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/684)
* Add support for multiple routes; deprecates single usage [#138](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/138)
* Add controller test template [#584](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/584)
* Add inspection for deprecated twig variable usage; last level only
* Experimental: Add support for webDeployment plugin (Remote Host); supports external container and routing files on a "Default Server"
* Experimental: Extend "Remote Host" context menu with action to download configured remote files
* Experimental: Background task to download external files

### 0.11.110
* Add controller provider for PHP Toolbox
* Add description to PhpTyes @var syntax and allow multiline doc comments [#439](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/439)
* Add @see doc tag support for twig. supports: relative files, controller names, twig files, classes and methods syntax [#439](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/439)
* Add Symfony 3 controller getParameter shortcut support; migrate container getParameter registrar for supporting all proxy methods and navigation [#680](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/680)
* Create template for controller action on annotation should prioritize html files [#681](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/681)
* Migrate template create annotator to twig namespaces handling to not only support bundle files
* Add twig namespace extension point and provide json file for twig namespace configuration "ide-twig.json" see "Twig Settings" for example

### 0.11.109
* Fix autocomplete route name in php and twig not working since Symfony 2.8 [#669](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/669)
* Implement more annotation controller route naming strategies [#673](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/673)
* Add Doctrine model PHP Toolbox provider

### 0.11.108
* Try to fix "unable to get stub builder", looks like input filter is true always in helper class [#630](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/630), [#617](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/617)
* Implement PHP Toolbox providers: services, parameter, routes, templates, translation domains
* Fix autocomplete and goto is missing for service ids in DefinitionDecorator [#667](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/667)

### 0.11.107
* Implement twig block name completion workaround; need to strip block tag content on prefixmatcher [#563](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/563), [#390](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/390), [#460](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/460), WI-24362</li>
* Update yaml service template to match Symfony best practices [#657](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/657) @Ma27
* Add array syntax whitelist for twig "trans" domain extraction and support "transchoice" variable in regex [#662](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/662)
* Update travis test matrix dont allow Java8 and PhpStorm10 failing
* Autowire services must not inspect constructor arguments [#664](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/664)
* Synchronized clearing of CaretTextOverlayListener timer to prevent npe [#642](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/642)
* "Method References" and "Type Provider" are deprecated by now and will replaced by Plugin "PHP Toolbox"

### 0.11.106
* Check null before calling getFormTypeClassOnParameter in FormUtil [#650](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/650)
* Support form getParent syntax of Symfony 2.8 / 3.0 [#651](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/651)
* Dropping service alias "form.type" and "form.type_extension" form sources using interfaces instead
* Add path support, class prefix routes and auto naming for route annotation indexer
* Add new form extension visitor to reuse type visitor and support for nested ExtendedType form; resolves [#623](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/623) [#651](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/651)

### 0.11.105
* Plugin renaming "Symfony2" -> "Symfony"
* Support yml inline service alias [#628](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/628)
* Support form field types as class constants [#623](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/623)
* Add FormType class constant completion and insert handler [#623](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/623)
* Add form intention action and generator for replace string parameter with class constant [#623](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/623)
* Parse branches level for symfony-installer version combobox; wait for next symfony.com deployment [#645](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/645), [#643](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/643)
* Add a navigation going from the constraint class to its constraint validator and vice versa [#632](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/632)
* Add Doctrine class constants intention replacement
* Add class constants completion for Doctrine getRepository and intention
* Controller::redirectToRoute should provide routing auto completion [#614](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/614)
* Whitelist twig "set tag" for twig extension references [#600](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/600)
* Dropping all version strings of "Symfony2", which are not system related
* Add support for Symfony 2.8 and 3 using the new directory structure [#635](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/635), also add auto configuration to set all custom paths and enabled plugin directly out of notification box
* Twig controller method targets now recognize xml and json files to fix @Template annotation doesn't recognize non-html templates [#602](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/602)

### 0.11.104
* Replace deprecated eap "PhpType#add" collection signature with string iteration [#611](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/611), [#622](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/622), [#627](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/627)
* Globally provide references for xml "resources" attributes with Bundle and relative path syntax

### 0.11.103
* All service definitions now indexed as json
* Support service alias for weak services [#391](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/391)
* Add deprecated service inspection [#608](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/608)
* Migrate doctrine metadata index to json and fix npe [#610](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/610)

### 0.11.102
* Support command names inside constant and property strings
* Add autowire attribute to blacklist for service argument inspection [#616](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/616) and add "autowire" and "deprecated" yaml completion
* Add file resource index and add include line marker for routing definition
* Use lazy line marker for class service definitions
* Add route pattern/path provider for Symfony symbol search

### 0.11.101
* Use route names of index for symbol search not only compiler provider
* Secure doctrine metadata indexer for performance reasons [#610](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/610)
* Support Doctrine embedded metadata for xml
* Add field name references on class property for doctrine xml metadata
* Add PhpStorm 10 testing environment
* Double check to not add empty doctrine metadata class to index [#615](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/615)
* Add class name scope for all metadata providers
* Dont provide Doctrine metadata line marker for annotation classes as this results in self navigation [#613](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/613)
* Improvements for completion and navigation of all Doctrine metadata files
* Tag generator indention for yml files is accessible for all services now
* Add new CaretListener extension which shows several type overlays for services

### 0.11.100</h2>
* Add blank fix for empty doctrine repository index value [#609](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/609)

### 0.11.99
* Complete rewrite of Doctrine implementation; metadata on now index [#586](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/586)
* Support Doctrine ODM MongoDB and CouchDB [#319](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/319)
* Add Doctrine relation shortcut for neos / flow annotations
* Add some Doctrine dbal support on newly added metadata index [#395](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/395)
* Add translator.logging parameter [#606](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/606) @mhor
* Dont display configurable forms inside default project [#578](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/578), [#607](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/607), [#593](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/593)
* Fix empty types for all TypeProviders; eg getRepository of Doctrine
* Support more use cases of TypeProvider
* Improve support for Doctrine metadata in xml files [#319](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/319)
* Add navigation for all yaml strings that are possible service names; eg security.yml ids
* Add Doctrine repository linemarker

### 0.11.98
* Full references support for console helpers [#243](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/243)
* Add Doctrine couchdb support; merged into overall odm manager to reuse mongodb implementation
* Doctrine getRepository now returns self instance on an unknown class
* Fix plugin breaks the context menu in the Project view [#575](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/575) thx @steinarer, [#525](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/525)
* Recursively find bundle context for all related action

### 0.11.97
* Class constant signature fixed in PhpStorm9; provide another workaround for supporting both api levels [#541](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/541)
* Event dispatcher should return event class instance [#570](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/570)
* Catch npe issue with plugin enabled check, for global twig navigation [#574](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/574)
* Add "resource" file references for current directory scope [#517](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/517)
* Add assets completion for "absolute_url" [#550](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/550)
* Refactoring and fixing assets handling in PhpStorm9 [#551](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/551)
* Fix invalid inspection on container expressions in yaml files and add LocalInspection testing asserts [#585](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/585)
* Add Travis PhpStorm8, 9 and eap environment switches

### 0.11.96
* Support priority and multiple registering of getSubscribedEvents in indexer
* Provide service tag list on indexed services for service generator
* Add twig filter and functions to symfony symbol search
* Remove deprecated Symfony sidebar, use symbol search instead [#414](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/414)
* Rename Symfony2 to Symfony in presentable strings [#393](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/393)
* Support ternary and array syntax in twig "include" and "extends" tags</li>
* Route indexer saves nullable string value, catch them in Route constructor [#482](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/482), [#508](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/508)
* Remove "defaults" key detection for a valid yaml route id [#518](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/518)
* Dont annotate missing twig template in interpolated or concatenated strings [#488](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/488)
* Fix global twig name navigation in php files, because of some api changes [#450](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/450), [#456](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/456)
* Use CASE_INSENSITIVE_ORDER for service container [#537](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/537)
* Add warning for service name if containing a uppercase char [#537](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/537)
* Remove Nullable or empty key in PhpElementsUtil.getArrayKeyValueMap [#549](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/549)
* Support "Class::class" in form data_class PHP 5.5 [#523](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/523)

### 0.11.95
* Add Doctrine simple_array and json_array for yaml files, on direct interface parsing [#555](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/555)
* Cache: Implement service definition cache layer, invalidates on global psi change [#350](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/350)
* Cache: Implement twig template name cache on psi change invalidation
* Cache: Refactoring TwigExtensionParser and introduce cache
* Cache: Add metadata cache for routing component
* Add PhpClass collector for "kernel.event_listener" events that are defined in xml and yaml [#531](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/531)
* Collect type hints for methods of getSubscribedEvents [#531](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/531), [#529](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/529)
* Implement support of "kernel.event_listener" events in completion, navigation and method creation argument type hints

### 0.11.94
* Remove postfix completion because its a PhpStorm9 core feature [#389](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/389)
* Improvement template name resolving for overwrites: support parent bundle and app resources; overwrite template linemarker [#437](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/437)
* Add Travis CI infrastructure thx to @Sorien [#536](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/536), [#534](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/534)
* Whitelist Twig_Environment parameter completion for template name

### 0.11.93
* Add "kernel.event_subscriber" to known tags and provide some more user feedback in error case [#511](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/511)
* Add _self support for twig macros [#419](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/419)
* Fix newline issue in controller template [#509](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/509)
* Add project generator for symfony installer and demo application [#475](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/475)

### 0.11.92
* Optimize service name generator and provide custom javascript strategy for it [#362](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/362)
* Add navigation form options of setRequired, setOptional, setDefined and refactor form options to visitor strategy [#502](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/502)
* Remove double Controller in classname [#507](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/507)
* Optimize form ui handling of service generator, prepare "insert" button, add generator action [#362](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/362)
* Use ContainerAwareCommand for command template

### 0.11.91
* Add support for doctrine xml metadata [#319](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/319)
* Add support for conditional twig extends tags, and replace regular match with pattern style
* Provide twig completion for html form action attribute [#497](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/497)
* Twig template file create quickfix should use PsiManager to support eg vcs [#498](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/498)
* Support for query_builder in entity form field type [#426](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/426)
* Fix npe in doctrine querybuilder chain processor [#495](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/495)
* Fix multiple resolve issues in php type provider

### 0.11.90
* Add CompilerPass intention and generator action for Bundle context [#484](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/484)
* Add support for new configureOptions in replacement for deprecated setDefaultOptions [#491](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/491), [#486](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/486), [#490](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/490)
* Add more bundle related file templates in NewGroup
* Fix "Missing argument" in services.yml doesn't keep track of factory methods [#492](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/492)

### 0.11.89
* Add yaml service arguments and tags intention / quickfixes [#470](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/470)
* Add xml tag intention and reuse tagged class interface list also for service generator [#470](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/470)
* Add method psi collector to support parent methods of a command [#454](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/454)
* Overall "Controllers as Services" optimize like navigation, related files, ... [#428](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/428)
* Use more stable PsiElements to find twig trans_default_domain domain name instead of regular expressions [#476](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/476)
* Fix multiResolve issue in method instance checks to resolve issue [#477](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/477) in multiple project command classes
* Fix wrong inspection for FormTypeExtensions tags [#483](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/483)
* Fix npe in index route arguments [#482](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/482)
* Fix warning for optional xml arguments [#485](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/485)

### 0.11.88
* Add console "getArgument" and "getOption" references [#454](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/454)
* "%" char in xml arguments now is a valid completion event [#461](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/461)
* Initial "Missing Argument" xml service inspection and quickfix [#470](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/470)
* Some performance improvements in xml and yaml service resolving

### 0.11.87
* Add completion for twig tags of Twig_TokenParserInterface::getTag implementations [#457](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/457)
* Add trans / transchoice twig tag 'from' support [#459](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/459)
* Add completion for Twig_SimpleTest extension in twig files after IS token
* Add twig operator completion in IF tags
* Fix pattern of twig trans_default_domain tag and use translation index for domain completion
* Fix several issues in twig array completion [#463](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/463)

### 0.11.86
* Support new setFactory syntax in yaml and xml [#436](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/436)
* Add service generator in class context of "Generator Popover" and intention in arguments
* Add twig assets completion for img src tags [#438](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/438)
* Add some more yaml service key completion
* Add method support for twig "for" statements [#208](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/208)
* Fix instance annotator in yaml psi pattern arguments on single quote string, after pattern api changes
* Fix completion for twig inline array doc block pattern
* Fix insertHandler for trailing backslash in twig doc var completion
* Note: implemented testing infrastructure [#405](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/405)

### 0.11.85
* Fix npe in custom assets resolving [#427](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/427)

### 0.11.84
* Fixing npe in tagged class inspections [#425](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/425)
* Add function parameter generator for "kernel.event_listener" on method create quickfix [#424](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/424)
* Add support for getSubscribedEvents inside method create quickfix [#424](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/424)
* Add support for custom assets [#353](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/353)
* Add static event parameter hint list for method create quickfix

### 0.11.83
* Add inspection for tagged services to validate corresponding interfaces or extends instances
* Add "Method Create" quickfix for xml files
* Add navigation, quickfix and inspections for methods inside tag statements [#422](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/422)
* Fix non unix eol error in template files [#421](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/421)

### 0.11.82
* Add method create quickfix for yaml files [#415](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/415)
* Remove weak service warning [#399](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/399)

### 0.11.81
* Fix multiresolve issues eg in AbstractManagerRegistry::getRepository [#403](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/403)

### 0.11.80
* Add missing route inspection with method creation quickfix
* Add deprecated inspection warning for service classes [#375](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/375)
* Support static string methods in twig filter and respect needs_context, needs_environment options in completion [#401](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/401) [#314](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/314)
* Allow more valid chars in annotation route index process [#400](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/400)
* Removes newly added leading backslash on phpstorm8 in class inserts [#402](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/402)
* Fix npe case in twig block goto [#397](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/397)

### 0.11.79
* Refactoring routing handling and prepare multiple route files [#138](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/138)
* Smarter route name resolve on indexed names [#392](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/392), [#376](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/376), [#365](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/365)
* Add doctrine 2.5 cache methods for class / repository completion [#203](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/203)
* Fixing IndexNotReadyException and "Read access is allowed" for eap changes [#370](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/370), [#383](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/383)

### 0.11.78
* Add twig embed tag indexer
* Support "include()" function and "embed" tag in twig variable collector
* Experimental: Add postfix completion [#389](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/389)
* Add more possible twig variables syntax from php files
* Add navigation for twig var doc
* Fix error on non unique class name completion in xml, yaml and twig [#387](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/387)
* Remove grouping for code folding, to make strings independent from each other

### 0.10.77 / 0.11.77
* Add weak routes in controller action related popover
* Add index for twig file php usage in render* methods and add variable collector
* Fix for new yaml SCALAR_STRING / SCALAR_DSTRING lexer changes in service instance annotator
* Fix max depth check in getTwigChildList [#360](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/360)
* Fix possible recursive calls in twig variable includes [#360](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/360)
* Note: last version for PhpStorm7!

### 0.10.76 / 0.11.76
* Implement docblock "@var" for twig variables, shortcut without tag is deprecated
* Optimize xlf navigation and references; better getDomainFilePsiElements translation performance
* Provide a global class::method[Action] yaml navigation, usable inside Drupal
* Translation extractor supports text selection
* Provide shortcut completion with function insertHandler for twig tags: href, src (css/js)
* Improve overall support for routes; better xml parser, more data and nicer completion [#369](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/369)
* Fix possible npe in PhpEventDispatcherGotoCompletionRegistrar
* Fix service completion in single quote yaml string values to reflect PhpStorm lexer changes; eg Drupal code convention
* Fix regular expression for trans and transchoice to support more cases [#377](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/377); also fix some whitespace issues
* Fix npe in NewControllerAction on non bundle context [#378](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/378)

### 0.10.75 / 0.11.75
* Add twig constants navigation, completion and folding [#327](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/327)
* Add references for array methods inside EventSubscriberInterface returns
* Add detection for "kernel.event_subscriber" tag on service builder [#352](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/352)
* Add indexer and references for xliff translations
* Quickfix for missing template will generate "block" and "extends" on directory context
* Better completion for class names in yaml and xml [#337](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/337)
* Fix twig missing translation domain pattern on nested filters [#255](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/255)
* Fix out of range exception in querybuilder parameter completion [#371](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/371)

### 0.10.74 / 0.11.74
* Add button in Settings to clean up plugin related indexes
* Add new isEnabledForIndex check, to not force a manually re-indexing for new projects after enabling plugin
* Add references for array methods inside EventSubscriberInterface returns
* Add completion for parameter in doctrine querybuilder "where" context
* Add support for variadic functions on doctrine querybuilder selects [#357](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/357)
* Our heart method "isCallTo" now supports classes and methods instance checks which are not unique in project
* Cleanup quote wrapping in routes key names of yaml files
* Fix npe in annotation template collector [#358](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/358)
* Fix npe in yaml parameter completion [#359](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/359)
* Fix npe in symbole search for non project context [#268](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/268)
* Fix out of range case in getParameterDefinition [#368](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/368)

### 0.10.73 / 0.11.73
* Fix npe in container parameter completion [#351](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/351)
* Add route requirements and options completion for yaml files

### 0.10.72 / 0.11.72
* Replace Form array options references with goto provider for performance improvements
* Support service container in library paths [#347](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/347)
* Use indexer for service parameter references to support weak file

### 0.10.71 / 0.11.71
* Fix whitespace pattern in twig function pattern [#340](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/340)
* Fixed typo in service generator "tags" should be "tag" on xml files [#338](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/338)
* Add extension point for controller actions related files
* Add extension point for GotoCompletionRegistrar
* Replace PsiReference for form type with GotoCompletionRegistrar [#313](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/313)

### 0.10.70 / 0.11.70
* Add linemarker for doctrine targetEntity relations
* Add doctrine query expr parameter completion
* Add support for querybuilder "from" index parameter [#322](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/322)
* Add completion for doctrine querybuilder alias in "createQueryBuilder" and "from" parameter
* Fix template file resolving for twig "app" resources

### 0.10.69 / 0.11.69
* Reworked twig template name resolving, for massive performance improvements [#321](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/321)
* Fix possible npe in TagReference inside php [#331](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/331)
* Hide first parameter in tail completion of twig extensions if its a Twig_Environment type hint [#314](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/314)
* Support twig file bundle overwrite in app folder [#275](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/275)
* Add reference provider for twig "block" function [#266](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/266)
* Provide "form" fallback on unknown from type and support nested strings [#325](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/325)
* Whitelist ".mongodb.yml" for controller related files
* 0.11: Use NavigationUtil for popups to fix eap api changes [#329](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/329)

### 0.10.68 / 0.11.68
* Provide weak form extension option completion [#317](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/317)
* Speedup form option completion [#318](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/318)
* Add new custom abstract reference replacements for deprecated getVariants [#313](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/313)
* Add weak doctrine namespaces on bundle names [#316](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/316)
* Add twig macro statement scope resolve for variables [#315](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/315)
* Add some missing retina icons [#312](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/312)

### 0.10.67 / 0.11.67
* Add array completion for constraints constructor [#304](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/304)
* Add support for twig.extension and form.type_extension in service generator [#308](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/308)
* Add bundle controller path to resource completion whitelist [#307](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/307)
* Map entity class with orm.yml file as linemarker [#309](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/309)
* Add current namespace resolving for yaml targetEntity [#305](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/305)
* Add class linemarker for yaml entities
* Add doctrine entity column names as lookup tail text in querybuilder completion

### 0.10.66 / 0.11.66
* Add weak tag references for xml and yaml container files
* 0.11.x: build against eap to resolve StringPattern#oneOf issues [#299](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/299)
* 0.11.x: reflect renaming of GotoRelatedFileAction [#297](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/297)

### 0.10.65
* Allow window path style in twig template names [#296](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/296)
* Add service indexer for tags in xml and yaml container files [#282](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/282)
* Add weak form types on new service tag indexer [#282](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/282)

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
* Service creator adds form alias as tag where possible [#281](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/281)
* Fix typo inside querybuilder resolver for oneToOne relations

### 0.10.62
* Add support for routes in xml files
* Provide twig context variables for include statements
* Fix some whitespace documents issue in yaml files

### 0.10.61
* Add support for doctrine id orm mapping of yaml files
* Add support for yaml CompoundValues inside routes action linemarker [#289](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/289)
* Fix that yaml files starting with whitespace not indexed for routes and services files
* Fix cast error on php array variables of twig types provider [#290](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/290)

### 0.10.60
* Fix translation annotator to not highlight compiled elements [#262](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/262)
* Fix non reload of translations which are outside PhpStorm index [#262](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/262)
* Add per translation file change indicator [#262](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/262)
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
* Fix npe in config completion [#284](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/284)

### 0.10.57
* Add yaml key completion for config / security files on "config:dump-reference"
* Add completion for QueryBuilder:set
* Make Twig translation key extractor compatible with PhpStorm8 and allow undo [#213](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/213)

### 0.10.56
* Add twig translation extraction action [#213](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/213)
* Fix data_class in form types should autocomplete any class [#280](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/280)
* Add completion for QueryBuilder:(*)where

### 0.10.55
* Finally(?) fix NullPointerException on index values [#277](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/277), [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238)
* Optimize Doctrine QueryBuilder chaining method collector to resolve methods and also fix some errors [#278](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/278), [#274](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/274)
* Reimplementation of Twig @Template goto on PHP Annotations extension [#276](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/276)
* Migrate Route annotator to inspections [#273](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/273)
* Typo fix to support Doctrine OneToOne relations

### 0.10.54
* Initial doctrine querybuilder support, see what is in doc
* Improvements in doctrine field property parser
* Activate twig filter and block name completion for upcoming
  PhpStorm8 (eap); resolved issues WI-19022, WI-20266
* Support entity in sub namespace, use indexer and improve
  performance
* Add repository references for QueryBuilder::update/delete/from [#272](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/272)
* Fix goto for class names without namespace in yaml scalar key [#271](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/271)
* Fix field name array completion for ObjectRepository::findBy
* Fix macro name set collector
* Form field completion should respect property names [#259](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/259)
* Integrate PHP-Annotations plugins as optional dependency
* PHP Annotations: Allow alias for all @Template extension [#236](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/236)
* PHP Annotations: Remove static list fully inside external plugin
* PHP Annotations: Route::service and some other references [#245](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/245)

### 0.10.53
* Fix slow index on large files [#261](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/261)
* Fix weak route annotation goto

### 0.10.52
* Globally use weak service and route index [#261](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/261)
* Add new weak annotator for routes and services
* Add route name indexer for annotation
* Add custom index keys processor for filter them in project context
* Add extension point to load custom doctrine model classes
* Fix annotate blank string values
* Remove duplicate from type completion [#260](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/260)

### 0.10.51
* Add twig macro name indexer
* Add macro include/from indexer and add implements linemarker
* Add custom "Symfony2 Symbol" search (Navigate > Symfony2 Symbol) in replacement for toolwindow [#229](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/229) (pls report possible keyboard shortcuts :) )
* Add twig macro and service index to symbol search
* Allow null keys in all index related stuff to temporary fix [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238)
* Strip quoted values inside yaml container indexer

### 0.10.50
* Add twig include indexer
* Add twig linemarker for includes

### 0.10.49
* Add translation key and domain indexer
* Rewrite and refactoring of all translation related stuff
* Make translations available without a compiled file on indexer as weak references
* Improvements in multiline values and quote key files for translation keys
* Rename parameter indexer key name to force a refresh, pls report npe directly to [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238) with your environment data if still occur

### 0.10.48
* Improvements in repositoryClass detection of doctrine annotations eg namespaces
* Add typename for repository "find*" lookup elements
* Add support for annotations based models inside "find*" repository calls
* Add extension point for container file loading
* Add "Interface" and "ClassInterface" to type provider [#254](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/254)
* Activate $option key references inside FormTypeInterface, because of working api now [#162](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/162)
* Refactoring of container related linemarkers to fix some npe (api break?) [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238)

### 0.10.47
* Add support for scss assets [#251](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/251)
* Migrate custom method references provider to variable resolver to support recursive calls
* Add references provider for console HelperSet [#243](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/243)

### 0.10.46
* Add goto for twig "parent" function [#246](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/246)
* Readd parameter class service annotator [#242](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/242)
* Dont use statusbar in phpstorm < 7.1 is not supported [#235](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/235)
* Make several services thread safe and implement npe fixes [#237](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/237), [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238)
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
* Add ManagerRegistry:getManagerForClass reference provider [#231](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/231)
* Add support for twig form_theme [#232](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/232)
* Add function to twig type whitelist
* Fix some npe in yaml [#227](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/227)

### 0.10.42
* Add twig template folding and strip "Bundle"
* Add twig implements and overwrites block linemarker and provide custom popover [#75](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/75)
* Add basic implementation of enum like completion behavior eg Response::setStatusCode, Request::getMethod
* Add doctrine related files to controller method popup
* Use folding names in related file popup where suitable

### 0.10.41
* Add code folding provider for php with support for route, template and repository
* Add code folding provider for twig path and url function
* Add settings for all code folding provider (default=true)
* Add overwrite linemarker for twig blocks [#75](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/75)
* Add yaml static service config completion (class, arguments, ... )
* Readd twig completion workaround for filters (hell!)
* Fix error on class name with trailing backslash on yaml annotator
* Migrate template references, to resolve [#46](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/46) fully

### 0.10.40
* Add support for "Navigate > Related Files" (Ctrl+Alt+Home) inside controller action [#191](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/191)
* Rename plugin settings key to more unique name "Symfony2PluginSettings" [#209](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/209) [#122](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/122)
* Fix accidently removed UrlGeneratorInterface::generate and EntityManager::getReference
* Fix npe and cme in container index [#207](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/207), [#211](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/211) (use "File > Invalidate Cache", if issue still occur)

### 0.10.39
* Add support of php shortcut methods for repository, route and service references [#46](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/46)
* Add blank filter for service stub indexes [#207](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/207)

### 0.10.38
* Add parameter references for doctrine findOneBy/findBy, on yaml config
* Add goto model config inside getRepository
* Add type resolver for events name
* Fix missing @ in yaml service builder
* Fix npe in container index [#206](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/206)

### 0.10.37
* Add Doctrine MongoDB repository resolver  [#205](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/205)
* Add autopopup for string completion values
* Add support for more form methods  [#162](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/162)
* Add reference provider for form "options" keys [#162](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/162) limited by WI-21563
* Add templates for yaml, xml service files and controller
* Service builder is accessible inside project browser context menu of php files
* Fix for missing vendor libs since phpstorm 7.1 [#180](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/180)

### 0.10.36
* Rewrite of all container stuff, which completely based on index now; with massive performance improvements
* Add service builder/generator for classes (beta) [#77](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/77)
* Add private service indexer [#197](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/197)
* Add service parameter indexer
* Add twig variable completion for class interfaces
* Add support for "PHP 5.5 class constant" in PhpTypeProvider, so Entity::class in getRepository is possible [#193](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/193)
* All PhpTypeProvider support references, not only string parameter
* Use parameter/service index in ContainerBuilder context and mark them as "weak" service
* Service LineMarker use service index and provide goto to definition
* Internal workaround for interface with missing trailing backslash WI-21520
* Fix symfony2.4 expressions detected as service [#202](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/202)
* Replace regular expression translation parser with plain psi collector, also allow multiple translation files [#195](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/195)
* getRepository provides goto for entity and also repository [#201](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/201)

### 0.10.35
* Add new method reference provider Parameter [#196](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/196)
* Add FormFactoryInterface::createForm option keys support
* Add Symbol and File contributor "Navigate > Symbol / File" [#189](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/189)
* Support upcoming "Search Everywhere" of PhpStorm 7.1 [#189](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/189)
* Support optional service reference syntax in yaml [#194](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/194)
* Support twig 1.15 "source" function [#190](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/190)
* Translation annotator check global translation file before fallback to yaml parser [#195](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/195)

### 0.10.34
* Add popover line marker to controller method, showing related files like templates and routes
* Add custom insert handle to not add double "@" on resource paths [#185](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/185)
* Add more twig template name normalizer and fix npe [#186](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/186)
* Prevent add empty and testing service to index
* Fix template annotations pattern are not compatible with phpstorm7 [#184](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/184)
* Fix yaml parameter annotator warnings on concatenate strings [#188](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/188)
* Fix parameter case-sensitivity issues [#179](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/179)
* Move repository to Haehnchen/idea-php-symfony2-plugin

### 0.10.33
* Add reference provider for FormInterface::get/has
* Add more twig template name normalizer [#182](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/182)
* Improve twig completion type lookup names

### 0.10.32
* Service container supports "field" elements eg properties and class constants [#151](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/151)
* Better template name detection on non common usage and performance improvements
* Add new method references provider for translation key with possible domain filter  [#155](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/155)
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