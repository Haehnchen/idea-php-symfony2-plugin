Changelog
=========

# Version names
* 2022.x: PhpStorm 2022.1+
* 0.23.x: PhpStorm 2021.1+ (no support)
* 0.22.x: PhpStorm 2020.3+ (no support)
* 0.21.x: PhpStorm 2020.2+ (no support)
* 0.20.x: PhpStorm 2020.0+ (no support)
* 0.19.x: PhpStorm 2019.2+ (no support)
* 0.18.x: PhpStorm 2019.1+ (no support)
* 0.17.x: PhpStorm 2018.2+ (no support)
* 0.16.x: PhpStorm 2017.3.2+ (no support)
* 0.15.x: PhpStorm 2017.2+ (no support)
* 0.14.x: PhpStorm 2017.1+ (no support)
* 0.13.x: PhpStorm 2016.3.1 (no support)
* 0.12.x: PhpStorm 2016.1+ (no support)
* 0.11.x: PhpStorm 8, 9, 10 (no support)
* 0.10.x: PhpStorm 7 (no support)
* 0.9.x: PhpStorm 6 (no support)

## 2022.1.236
* Add quickfix for missing property: try to find a valid service injection and autowire it (Daniel Espendiller)
* Remove custom implemention to fix phpstorm meta data files and use plugin "libraryRoot" path (Daniel Espendiller)
* Psi reference only need to be provided when plugin is active (Daniel Espendiller)
* [#2025](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/2025) fix "Not a JSON Object" java error for Webpack manifest parsing (Daniel Espendiller)
* [paid] Inspection for properly missing 'throw' for exception (Daniel Espendiller)
* [paid] Inspection for supported shortcut method existence for Exceptions (Daniel Espendiller)
* [paid] Inspection for supported shortcut method existence for "BinaryFileResponse" (Daniel Espendiller)
* [paid] Inspection for supported shortcut method existence (Daniel Espendiller)

## 2022.1.235
* [#2015](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/2015) [Translation] Add inspection, autocompletion and navigation for named arguments (Daniel Espendiller)
* [#2017](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/2017) fix [Twig] Go to function definition does not work when using the first-class callable syntax (Daniel Espendiller)
* Add linemarker for serializer usage for classes (Daniel Espendiller)
* [paid] Add inspection for RequestStack usages in router action (Daniel Espendiller)
* [paid] Add description for heavy constructor usages (Daniel Espendiller)
* [paid] Add deprecation for injection "Session" and "FlashBag" services (Daniel Espendiller)

## 2022.1.234
* Support attributes targets for route name navigation and allow multiple targets (Daniel Espendiller)
* Provide related goto and linemarker for form classes (Daniel Espendiller)
* [#2006](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/2006) Support manifest.json w/o Webpack Encore (Daniel Espendiller)

## 2022.1.233
* Support "_self" syntax for Twig macros (Daniel Espendiller)
* Support webencore entry target for "encore_entry_link_tags" and "encore_entry_script_tags" (Daniel Espendiller)
* [#1464](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1464) reflect api changes to Twig "macro" feature (Daniel Espendiller)

## 2022.1.232
* Provide yaml route path value completion (Daniel Espendiller)
* Provide completion for route urls inside php annotations (Daniel Espendiller)
* Update to "2022.2"; reimplement features for new ast node structure (Daniel Espendiller)
* Support asset packages (like Twig) also in PHP (Daniel Espendiller)
* Support missing service inspection in AsDecorator (Daniel Espendiller)

## 2022.1.231
* [#1991](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1991) support "tags" inside "Autoconfigure" attribute (Daniel Espendiller)
* [#1991](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1991) support "decorates" inside "AsDecorator" attribute (Daniel Espendiller)
* [#1984](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1984) support parameter inside "TaggedLocator" attribute (Daniel Espendiller)
* [#1984](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1984) detected deprecated services usage inside "Autowire" attribute (Daniel Espendiller)
* [#1984](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1984) detected missing services inside "Autowire" attribute (Daniel Espendiller)
* [#1984](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1984) support service inside "Autowire" attribute (Daniel Espendiller)
* [#1984](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1984) support parameter inside "TaggedIterator" attribute (Daniel Espendiller)
* [#1984](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1984) support parameters inside "Autowire" attribute (Daniel Espendiller)
* [Paid] add inspection for deprecated conditional Twig "for" syntax: "{% for u in us if u.act %}" (Daniel Espendiller)
* [#907](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/907) linemarker for linking a data_class to related forms (Daniel Espendiller)
* provide a form data_class linemarker (Daniel Espendiller)
* Fix typo in notification (Gabriel Wanzek)

## 2022.1.230
* [#1969](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1969) Support translation autocomplete on form constraints with named arguments (Daniel Espendiller)
* [paid]  [#1977](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1977) fix inspection for querybuilder chaining (Daniel Espendiller)
* Support tags can also be strings in yaml (Daniel Espendiller)

## 0.23.229 / 2022.1.229
* Provide complete for tag name of yaml "!tagged_iterator" (Daniel Espendiller)
* Named argument completion can now reflect the "bind" or "service" scope (Daniel Espendiller)
* Provide incomplete named argument complete for yaml with: "tagged_iterator", dotenv, service names and parameters (Daniel Espendiller)
* Fix incomplete named argument key complete pattern, for newest PhpStorm versions (Daniel Espendiller)
* Ignore suggestion for some service names (Daniel Espendiller)
* Provide navigation for "tagged_iterator" tag in yaml (Daniel Espendiller)
* Inspection for named arguments in yaml (Daniel Espendiller)

## 0.23.228 / 2022.1.228
* Provide linemarker for route annotations imports (Daniel Espendiller)
* Inspection for deprecated "controller" targets on route (Daniel Espendiller)
* Support conditional enviroment for yaml file to index resources (Daniel Espendiller)
* Add global resolving of resources (Daniel Espendiller)
* Add "resources" outside of routing to index (Daniel Espendiller)
* Add more (getReference, getClassMetadata, getPartialReference) Doctrine entity class constant completion (Daniel Espendiller)
* Prioritize class constant parameter completion for Doctrine repository and form types (Daniel Espendiller)
* Try to detect yaml translation files also on language pattern (Daniel Espendiller)
* Twig form linemarker should only be triggered on smallest (leaf) elements (Daniel Espendiller)
* PhpLineMarkerProvider linemarker should only be triggered on smallest (leaf) elements (Daniel Espendiller)
* Constraint message property linemarker should only be triggered on smallest (leaf) elements (Daniel Espendiller)
* Remove annotation linemarker as its also catched by resources linemarker now (Daniel Espendiller)
* Extend resources / import index with context options (Daniel Espendiller)
* [#1940](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1940) add translation support for TranslatableMessage class (Daniel Espendiller)
* "+intl-icu" remove suffix from compiled translation domains (Daniel Espendiller)
* Prevent empty translation domains (Daniel Espendiller)
* Provide "glob" pattern extraction for string to provide a file scanner (Daniel Espendiller)
* Support file and directory resolving for resources (Daniel Espendiller)
* Fixed bad links to jetbrains plugin marketplace (Adrian Rudnik)
* [paid] Add class constant instance inspection for check only FormTypeInterface can be added to a form (Daniel Espendiller)
* [paid] Inspection for wrong querybuilder method chaining because of overwrite (Daniel Espendiller)
* [paid] Add ConstructorCommandHeavyConstructorInspection (Daniel Espendiller)

## 0.23.227 / 2022.1.227
* Provide service class linemarker for prototype resource of xml files (Daniel Espendiller)
* Support resource namespace navigation for xml prototype (Daniel Espendiller)
* Support latest command name extraction logic also in search everywhere (Daniel Espendiller)
* Provide navigation for "resource" and "exclude" of yaml files (Daniel Espendiller)
* [#1541](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1541) fix supporting "asCommand" name as default value (Daniel Espendiller)

## 0.23.226 / 2022.1.226
* [#1429](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1429) fix security matcher starting string replacement and support php attributes (Daniel Espendiller)
* Fixed duplicated results in service completion (Adam Wójs)
* Fixed duplicated results in service completion (tests) (Adam Wójs)
* [#769](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/769) provided related twig symbols navigation (Daniel Espendiller)
* [#1029](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1029) fix route not indexed if the annotation is empty (Daniel Espendiller)
* remove requirement for ending method name with "Action" for related controller methods symbol targets (Daniel Espendiller)
* Reworked test for code completion in service definition parent (Adam Wójs)
* Added service name reference contributor for YAML DIC files (Adam Wójs)

## 0.23.225 / 2022.1.225
* [#567](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/567) support Twig blocks in search everywhere (Daniel Espendiller)
* Underscore should be the preferred template creation quickfix (Daniel Espendiller)
* Index usages of twig files inside "Template" php attribute (Daniel Espendiller)
* Support global naming for twig method resolving (Daniel Espendiller)
* [#1541](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1541) provide linemarker to run Symfony command via internal console terminal (Daniel Espendiller)
* [#1285](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1285) migrate template file annotator to inspection and support php attributes (Daniel Espendiller)
* [#1548](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1548) optimize inspection for notify missing extends/implements tags instances: support multiple tag classes, allow service id be a classes (Daniel Espendiller)
* [#1536](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1536) [#1020](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1020) support manifest.json inside assets (Daniel Espendiller)
* [#1233](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1233) provide plugin error submitter (Daniel Espendiller)
* Replace direct method name for template recognition with a simple "contains" pattern. valid: "template" and "render" (Daniel Espendiller)
* [#1509](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1509) catch xlf parser exception (Daniel Espendiller)
* [#1684](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1684) description for compiled routes (Daniel Espendiller)
* [#1736](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1736) change default for public files (Daniel Espendiller)
* [#1736](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1736) change default for compiled translation class to catch more modern structures (Daniel Espendiller)
* [#1366](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1366) mark classed used if its tagged via "kernel.event_listener" server defintion (Daniel Espendiller)
* [#1366](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1366) mark Constraint classes used if validator class for it was found (Daniel Espendiller)
* [#1366](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1366) mark EntityRepository classes as used code, if any metadata exists with them (Daniel Espendiller)
* [#1366](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1366) mark TwigExtension classes as used code, if any implementation exists (Daniel Espendiller)
* [#1366](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1366) mark registered voter class as used code (Daniel Espendiller)
* [#1366](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1366) mark public method callbacks inside getSubscribedEvents as used code (Daniel Espendiller)
* [#1366](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1366) mark command class register as service as used code (Daniel Espendiller)
* [#1366](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1366) mark controller and its action as "used" code (Daniel Espendiller)
* Fix support resolving controller methods ending with "Action" (Daniel Espendiller)
* Routes can be cached based on index and compiled files (Daniel Espendiller)

## 0.23.224 / 2022.1.224
* Prefix missing parameter inspection with "Symfony" (Daniel Espendiller)
* Prevent duplicate complete for already known path / url twig lookup elements (Daniel Espendiller)
* [#1890](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1890) pipe also the fake php file to completion proxying for php (Daniel Espendiller)
* [#1893](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1893) workaround in wrongly implemented attributes handling; catch errors and check for direct attribute value via string (Daniel Espendiller)
* Use central container parameter naming as its now highly optimized (Daniel Espendiller)
* [paid] [#1888](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1888) Inspection #FormViewTemplate has no description (PluginException) (Daniel Espendiller)

## 0.23.223 / 2022.1.223
* Replaced timed cache for container to use its file modification counter (Daniel Espendiller)
* Smarter detection for compiled translation files (Daniel Espendiller)
* Update PhpStorm library: - https://github.com/King2500/symfony-phpstorm-meta - https://github.com/King2500/doctrine-phpstorm-meta (Daniel Espendiller)
* Change static libraryRoots to dynamic LibraryRootProvider implementations (Thomas Schulz)
* Try to guess the translations file even more (Daniel Espendiller)
* Refactor compiled routing loading; to use internal cache implementation (Daniel Espendiller)
* [#1705](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1705) support constants for route name and path via first level self owning (Daniel Espendiller)
* Replace custom compiled translations caching result with CachedValue (Daniel Espendiller)
* Support download of Symfony CLI for project creation (Daniel Espendiller)
* [#1764](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1764) support translations in php files (Daniel Espendiller)
* [#774](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/774) [#1310](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1310) [#1334](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1334) fix "Symfony" project create feature with using Symfony CLI installer (Daniel Espendiller)

## 0.23.222 / 2022.1.222
* Proxy Twig function parameter completion to PHP completion (Daniel Espendiller)
* Support controller action linemarker for "__invoke" and all others resolving (Daniel Espendiller)
* Support navigation for "Symfony 5.3: Configure Multiple Environments in a Single File" @see https://symfony.com/blog/new-in-symfony-5-3-configure-multiple-environments-in-a-single-file (Daniel Espendiller)
* [#1858](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1858) fix external phpindex "Stub ids not found for key in index" by replacing phpclass filter namespacing (Daniel Espendiller)
* Provide navigation and completion for Symfony http client options (Daniel Espendiller)
* Replace deprecated "com.intellij.util.containers.HashMap" (Daniel Espendiller)
* Replace deprecated "Deprecated fields usages" (Daniel Espendiller)
* Replace deprecated "com.intellij.util.containers.HashSet" (Daniel Espendiller)
* Provide fuzzy similar search for fix missing routes (Daniel Espendiller)
* Provide fuzzy similar search for fix missing translation domain (Daniel Espendiller)
* Provide fuzzy similar search for fix missing translation keys (Daniel Espendiller)
* Sync changelog (Daniel Espendiller)

## 0.23.221 / 2022.1.221
* Globally cache service and parameter collector (Daniel Espendiller)
* [#1574](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1574) [#1834](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1834) fix all psi references which are not attached to itself (Daniel Espendiller)
* Provide some more lazy loading and batching for service inspections (Daniel Espendiller)
* Replaced deprecated popover usage of "Symfony: Services Definitions" (Daniel Espendiller)
* Replace all deprecated "NavigationGutterIconBuilder::setCellRenderer" usages (Daniel Espendiller)
* Replace all deprecated "NotNullLazyValue" usages (Daniel Espendiller)
* PHP service missing inspection must not match on all leaf elements (Daniel Espendiller)
* PHP service deprecation must not match on all leaf elements (Daniel Espendiller)
* Remove "lowercase letters for service and parameter" inspection as its not the common way anymore in favor for class names (Daniel Espendiller)
* Template create must not match on all leaf elements (Daniel Espendiller)
* Provide fuzzy template search for invalid template names (Daniel Espendiller)
* Replace deprecated "WriteCommandAction.Simple" and "createListPopupBuilder" (Daniel Espendiller)
* Replace deprecated usages for template create quickfix and sort templates by possible weight (Daniel Espendiller)
* [paid] Better service detection (Daniel Espendiller)
* [paid] Add createView check for passing form to template (Daniel Espendiller)
* [paid] Add inspection for "A template that extends another one cannot include content outside Twig blocks" (Daniel Espendiller)

## 0.23.220 / 2022.1.220
* Fix SymfonyProfilerWidget api compatibility (Daniel Espendiller)
* Fix 2022.1 compatibility issues (Daniel Espendiller)
* [paid] Add project start grace period for paid feature (Daniel Espendiller)

## 0.23.219 / 2022.1.219
* Use new toolbox version (Daniel Espendiller)
* Design plugin project notification with action links (Daniel Espendiller)
* Fix optional plugins dependencies need a config file (Daniel Espendiller)
* Dynamic plugin support (Daniel Espendiller)

## 0.23.218 / 2022.1.218
* Provide incomple block completion (Daniel Espendiller)
* [#1730](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1730) TwigPath dont sort via class comparable and dont use clone for being cached elements (Daniel Espendiller)
* Migrate project notification (Daniel Espendiller)
* Replace the webdeployment project component to allow the plugin be dynamic (Daniel Espendiller)
* Replace the project component to allow the plugin be dynamic (Daniel Espendiller)
* Migrate Symfony statusbar to extension (Daniel Espendiller)
* [#1767](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1767) replace method reference resolving with core way (Daniel Espendiller)
* split plugin config file (Daniel Espendiller)
* Deprecated "GotoCompletionRegistrar" extension in favor of core features (Daniel Espendiller)
* Xml file references should only trigger with "xml" extension (Daniel Espendiller)
* Xml line marker should only trigger with "xml" extension (Daniel Espendiller)
* Prevent indexing all xml file types; filter by extension (Daniel Espendiller)
* Prevent indexing long strings (Daniel Espendiller)
* [#1706](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1706) support "ContainerBagInterface" (Daniel Espendiller)

## 0.23.217 / 2022.1.217
* Fix [#1793](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1793) Symfony twig path previews are not complete when Route is set on Controller class (Daniel Espendiller)
* Add "embed" incomplete completion, caches and smarter filter (Daniel Espendiller)
* [#1706](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1706) fix method references and multi resolve (Daniel Espendiller)
* Twig icon extends should also support extension point (Daniel Espendiller)
* [#1706](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1706) add support for ParameterBagInterface (Daniel Espendiller)
* [paid] Heavy twig inspection (Daniel Espendiller)
* [paid] Inspection to favor "kernel.project_dir" instead of KernelInterface injection (Daniel Espendiller)
* [paid] Inspection to notice inject a controller into a service (Daniel Espendiller)
* [paid] Provide inspection for warning injection for unsupported classes on autowire (Daniel Espendiller)
* [paid] Constructor inspections (Daniel Espendiller)

## 0.23.216
* Fix api issues (Daniel Espendiller)

## 0.23.215
* Provide incomplete "if" completion for Twig (Daniel Espendiller)
* Provide incomplete "for" completion for Twig based on variable scope (Daniel Espendiller)
* Provide incomplete tags / function completion for Twig (Daniel Espendiller)
* Add Twig "include" and "extends" lookup element weight sorting (Daniel Espendiller)
* Add Twig "extends" generator action (Daniel Espendiller)
* Add Twig block overwrite generator action (Daniel Espendiller)
* Reflect changes in instanceOf check for PhpClass (Daniel Espendiller)

## 0.23.214
* Support more use cases for php controller linemarker (Daniel Espendiller)
* Support class constants for Twig extension navigation (Daniel Espendiller)
* Adding a linemarker to php route config (mamazu)
* Provide pattern tests for Twig macro syntax (Daniel Espendiller)
* Secure some long template filenames to be not indexed (Daniel Espendiller)
* SymfonyWebpackUtil: migrate from "org.json.simple" to "com.google.gson" (Daniel Espendiller)

## 0.23.213
* Fixed [#1716](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1716): Deprecated LineMarkerInfo constructor usage (Adam Wójs)
* Add missing Symfony icon to new project setup (Shyim)
* Added missing services keywords to YamlCompletionContributor (Adam Wójs)

## 0.23.212
* Smarter Doctrine querybuilder "where" condition navigation to fields (Daniel Espendiller)
* Add PHP8 attributes support for Doctrine metadata (Daniel Espendiller)
* Fixed [#1675](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1675): "AWT events are not allowed inside write action" exception while creating dialog window for resolving arguments ambiguity (Adam Wójs)

## 0.23.211
* Support default function parameter values for extracting template render scope (Daniel Espendiller)
* Fixed [#1674](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1674): duplicated route prefix in profiler urls (Adam Wójs)
* Fixed [#1667](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1667): Rename refactoring ignores constants inside yaml files (tests) (Adam Wójs)

## 0.23.210
* [#1661](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1661) Fix container nullable in yaml context (Daniel Espendiller)
* Smarter public asset folder, fix twig form linemarker psi pattern, remove symfony check from notication window (Daniel Espendiller)
* Update gradle git versions plugin (Daniel Espendiller)
* Gradle to github actions migration (Daniel Espendiller)
* Support webencore inside "encore_entry_*" twig functions (Daniel Espendiller)
* Excluded non-public consts from !php/const autocompletion (Adam Wójs)
* Fixed [#1631](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1631): PhpConstGotoCompletionProvider throws IndexOutOfBoundsException when cursor is before scope operator (Adam Wójs)

## 0.23.209
* [#1614](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1614) fix "ListPopupStep" issues of PhpStorm 2021.1 (Daniel Espendiller)
* Fixed [#1640](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1640): Go to declaration throws ArrayIndexOutOfBoundsException on empty class const name (Adam Wójs)
* Added unit test for [#1640](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1640) (Adam Wójs)

## 0.23.208
* [#1624](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1624) secure yaml key class resolving for PhpStorm 2021.3 (Daniel Espendiller)
* PhpStorm 2021.1 gradle build (Daniel Espendiller)
* Fixed [#1575](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1575): Inspection "Container sensitivity" is not expected to be emitted for default env values (Adam Wójs)
* Fixed [#1599](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1599): Added protection against infinite recursion in PhpElementsUtil.getImplementedMethods (Adam Wójs)

## 0.22.207
* Removed hardcoded background in output textarea of Symfony Create Service Form (Adam Wójs)

## 0.22.206
* Support routes definition inside PHP8 attributes (Daniel Espendiller) [#1567](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1567)
* Higher java compatibility version for builds (Daniel Espendiller)

## 0.22.205
* Change gradle build to use PhpStorm 2020.3.1 packages (Daniel Espendiller)
* 2020.3 support: fix yaml indents (Aleksandr Slapoguzov) [#1568](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1568)
* 2020.3 support: handle a new psi structure for twig variables and fields (Aleksandr Slapoguzov) [#1568](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1568)
* 2020.3 support: removed go_to_declaration_handler for sets because now it works out of the box (Aleksandr Slapoguzov) [#1568](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1568)
* 2020.3 support: removed invalid case - else branch cannot contain any conditions (Aleksandr Slapoguzov) [#1568](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1568)
* 2020.3 support: bump plugin versions (Aleksandr Slapoguzov) [#1568](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1568)

## 0.21.204
* Support autowired resources services inside "argument bind" navigation (Daniel Espendiller)
* Support arrays for "resource" and "exclude" on autowrite which is use on Symfony >= 5 as default instead of global pattern (Daniel Espendiller)
* Support form "help" option inside translations (Daniel Espendiller)

## 0.21.203
* Fix typo in build instructions (Matthias Gutjahr)
* Remove leading slash from storage location ([#1543](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1543)) (Jens Schulze)

## 0.21.202
* Only "form" and "form_start" are valid linemarker on Twig types (Daniel Espendiller)
* Support for PhpStorm 2020.2: get rid of start comment tokens in doc patterns & migrate to non-atomic PsiComment structure (Aleksandr Slapoguzov)
* Support for PhpStorm 2020.2: process all class references for parameter (union types support) - now a parameter can contain several class references (Aleksandr Slapoguzov)
* Support for PhpStorm 2020.2: change signatures for LineMarkerProvider implementations (Aleksandr Slapoguzov)

## 0.20.201
* Use simple code flow for formBuilder field extraction (Daniel Espendiller)
* Support form type extraction of FormFactoryInterface::createNamed inside Twig type resolving (Daniel Espendiller)
* Provide form type linemarker inside Twig templates and provide navigation and extended completion for form fields (Daniel Espendiller)

## 0.20.200
* Change annotation controller icon and provide link to Symfony documentation (Daniel Espendiller)
* Catch exception for nested resource glob resource syntax [#1517](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1517) (Daniel Espendiller)
* Provide navigation targets for service definition inside php files (Daniel Espendiller)
* Provide navigation, completion and linemarker for "Constraint::message\*" properties (Daniel Espendiller)
* Dont mark abstract classes inside service linemarker (Daniel Espendiller)

## 0.20.199
* Support constraint translation message navigation and completion in annotations (Daniel Espendiller)
* Provide linemarker for yaml service resource (Daniel Espendiller)
* Add navigation for services inside "\_instanceof" yaml keys (Daniel Espendiller)

## 0.20.198
* Fix TwigPath caching issue on extracting config path from yaml file [#1358](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1358) [#1506](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1506) (Daniel Espendiller)
* Use Service index key as cache indicator (Daniel Espendiller)
* Fix configuration file resolving for YAML file on startup: "class org.jetbrains.plugins.textmate.psi.TextMateFile cannot be cast to class org.jetbrains.yaml.psi.YAMLFile" [#1492](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1492) (Daniel Espendiller)
* Index tag attributes of services (Daniel Espendiller)
* Fix possible array issues on service resource linemarker (Daniel Espendiller)

## 0.20.197
* Provide checkbox to disable Twig file icon decoration [#1485](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1485) (Daniel Espendiller)
* Enrich compile service data with metadata indexer to support configuration like autowire (Daniel Espendiller)
* Use internal icons for service linemarker (Daniel Espendiller)
* Provide linemarker for a constructor which supports autowire (Daniel Espendiller)
* Provide yaml navigation for services defined via resource (Daniel Espendiller)
* Provide resources index for service and use it in linemarker classes to indicate it and also provide a tagged icon (Daniel Espendiller)

## 0.20.196
* Ignore Doctrine repository return type provider on magic method pattern if already in repository [#1481](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1481) (Daniel Espendiller)
* Provide custom Twig file overlay to indicate "extends" and attached controller template types [#1485](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1485) (Daniel Espendiller)
* Index service definition for PHP files (Daniel Espendiller)
* Support global Symfony DIC function: "service" and "param" (Daniel Espendiller)

## 0.20.195
* Fix possible long signature truncate and split on type resolver for repository find* (Daniel Espendiller)

## 0.20.194
* Support direct type resolving "ObjectRepository:find\*" usages without having getRepository having in same context (Daniel Espendiller)
* Support querybuilder model resolving parent constructor call of ServiceEntityRepository (Daniel Espendiller)
* Ignore trait class in Doctrine Entity folder (Daniel Espendiller)
* Support type resolving for magic Doctrine methods "findBy\*" "findOneBy\*" #149 (Daniel Espendiller)
* Support traits and extends class resolving in Doctrine annotation metadata (Daniel Espendiller)
* Doctrine ORM target entity should support class constant resolving #1468 (Daniel Espendiller)
* Refactor Doctrine repositoryClass fetching via class constants to support Symfony 5.1 maker bundle style #1468 (Daniel Espendiller)
* Migrate all type provider to "PhpTypeProvider4" extension (Daniel Espendiller)
* Use getIndexModificationStamp to cache index result based on it index change key (Daniel Espendiller)
* Remove support for PhpStorm versions < "2020.1" (Daniel Espendiller)
* Migrate Doctrine entity type resolving for "find\*" methods #1434 (Daniel Espendiller)

## 0.19.193
* Filter some special debug service and move the into lower priority (Daniel Espendiller)
* Extract project directory from the file context; use directly the project to prevent "com.intellij.util.indexing.FileContentImpl$IllegalDataException: Cannot obtain text for binary file type : Unknown" on xlf files #1459 (Daniel Espendiller)

## 0.19.192
* Allow to disable Twig bundle namespace and support in autoconfigure (Daniel Espendiller)
* Wrapped the deprecated "getBaseDir" for getting the project root directory (Daniel Espendiller)
* Bundle ending is not needed for Twig namespaces (Daniel Espendiller)
* Added reference and inspection support for TranslatorHelper ([#1454](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1420)) (Thomas Schulz)
* Add support for service class inside tag adding indention (Daniel Espendiller)
* Provide completion support for named argument inside yaml arguments context (Daniel Espendiller)
* Add support for autocompletion inside the MessageSubscriberInterface::getHandledMessages method (Stefano Arlandini)

## 0.19.191
* Filter out service id which are random like service\_locator from compiled service container file (Daniel Espendiller)
* Replace CachedValuesManager#createCachedValue usage with direct fetch and build result API #getCachedValue (Daniel Espendiller)
* Take "controller.service\_arguments" for yaml named arguments of controller binding into account (Daniel Espendiller)
* Support usage of translation domain adding "resource\_files" compiled debug container (Daniel Espendiller)
* Refactored the compiled translation target handling based on container debug file (Daniel Espendiller)
* Provide better detection for translation directory inside based on the cached "translations" folder (Daniel Espendiller)

## 0.19.190
* Support changes in path extraction for "twig.loader" (Daniel Espendiller)
* Support more use cases for compiled service path detection (Daniel Espendiller)
* Provide relative path support for Twig paths configuration via yaml (Daniel Espendiller)
* Provide support for compiled Symfony 4 / 5 route names (Daniel Espendiller)

## 0.19.189
* Provide resolving of Twig globals and variables with multiple types and targets (Daniel Espendiller) [#1421](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1421)
* Support also "yaml" for config files to extract the globals (Daniel Espendiller) [#1420](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1420)

## 0.19.188
* Provide controller render navigation from PHP controller to its template (Daniel Espendiller) [#1418](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1418)
* Support user implementation resolving for Twig "app.user" usages (Daniel Espendiller) [#1416](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1416)
* Support multiple Twig global types on same variable like "app" (Daniel Espendiller) [#1415](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1415)
* Provide deprecated inspection for Twig token tags on PhpClass (Daniel Espendiller) [#1414](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1414)
* Support Twig functions in "for" statements (Daniel Espendiller) [#1413](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1413)
* Remove parent check for Twig tags to fix force implemented token TAG by PhpStorm like spaceless; support end tags to be navigation targets (Daniel Espendiller) [#1412](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1412)
* Fix template usage extraction issues with inline method reference using wrong type cast [#1410](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1410) (Daniel Espendiller)
* Support Twig function navigation inside IF statement (Daniel Espendiller) [#1408](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1408)
* Attempt to use new persistance namespaces without breaking BC (Jakub Caban) [#1407](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1407)

## 0.19.187
* Support shortcuts instances of "Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController" (Daniel Espendiller) [#1401](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1401) [#1405](https://github.com/Haehnchen/idea-php-symfony2-plugin/pull/1405)
* Support Symfony5 / DoctrineBundle 2 persistence library interface (Daniel Espendiller) [#1401](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1401) [#1404](https://github.com/Haehnchen/idea-php-symfony2-plugin/pull/1404)
* Support indexing of template assignment expression for Twig template names (Daniel Espendiller) [#1400](https://github.com/Haehnchen/idea-php-symfony2-plugin/pull/1400)
* Added support for namespaced Twig classes (Marcel Rummens) [#1394](https://github.com/Haehnchen/idea-php-symfony2-plugin/pull/1394)

## 0.19.186
* Twig settings are not available while indexing (Ruud Kamphuis) [#1393](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1393)
* Fix browse folder buttons (Ruud Kamphuis) [#1392](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1392)
* WebDeploymentIcons.Download will be removed soon (Elena Shaverdova) [#1390](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1390)
* Support new snake_case templates (Ruud Kamphuis) [#1389](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1389)

## 0.19.185
* Add support for Twig apply tag filters (Daniel Espendiller) [#1388](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1388)

## 0.19.184
* Provide extension for twig include and extends usage (Daniel Espendiller)

## 0.18.183
* Add linemarker icon for navigate to "extends" tag of the given Twig [#1376](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1376) (Daniel Espendiller)
* Optimize Twig "extends" tag indexing performance [#1374](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1374) (Daniel Espendiller)

## 0.18.182
* Remove custom related template search for controller and migrate to indexer visitor [#1370](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1370) (Daniel Espendiller)
* Provide template guesser for @Template with default property is empty [#1368](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1368) (Daniel Espendiller)
* Added path for default debug container in Symfony 4 [#1367](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1367) (Artem Oliynyk)

## 0.18.181
* Completion for YAML tags, keywords and PHP constants [#1336](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1336) [#1364](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1364) (Thomas Schulz)
* Support template name references resolve when complete / navigate to Twig variables on twig render() parameter [#1361](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1361) (Daniel Espendiller)

## 0.18.180
* Twig variable is searching based on template index, which supports much more use cases [#1356](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1356) (Daniel Espendiller)

## 0.18.179
* Extract index and compiled translations into extension [#1355](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1355) (Daniel Espendiller)
* Provide placeholder completion for Symfony translation contract implementation (Daniel Espendiller)
* Support coalesce and ternary resolving in template name usage for php [#1354](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1354) (Daniel Espendiller)
* Provide extension for configure some global plugin constants [#1353](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1353) (Daniel Espendiller)
* Support same directory in Twig namespace json (Daniel Espendiller)
* Extract more possible Docker env keys [#1349](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1349) (Daniel Espendiller)

## 0.18.178
* Support new node tree structure of Symfony configuration and fix pattern for possible empty file of config file detection [#1344](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1344) (Daniel Espendiller)
* Support more dotenv files [#1345](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1345) (Daniel Espendiller)

## 0.18.177
* Support default form types for different Symfony versions (Daniel Espendiller) [#1343](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1343)
* Improve completion support for FQCN::method routes (Daniel Espendiller) [#1231](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1231) [#1159](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1159)
* Support @IsGranted attribute in annotations (Daniel Espendiller) [#1341](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1341) [#1189](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1189)
* ContainerBuilderStubIndex: remove accessing to another indexes in the indexer, use more suitable API for extracting classes from file (AlexMovsesov) [#1339](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1339) [#1340](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1340)

## 0.18.176
* Fixed injection range for heredoc/nowdoc literals in automatic injectors (Andrey Sokolov) [#1337](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1337)
* Fix typo/wrong YAML service attribute for autowiring_types (Thomas Schulz) [#1335](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1335)
* Container stub index performance: move to unrecursive indexer (AlexMovsesov) [#1333](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1333)

## 0.18.175
* Follow the redirect for symfony installer [#1325](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1325) (Daniel Espendiller)

## 0.18.174
* Some code cleanup (Daniel Espendiller) [#1325](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1325)
* Strip format from Twig template references index: ".html.twig" => ".twig" (Daniel Espendiller) [#1324](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1324)
* Twig template references should resolve local scope for getting template name from vars, const, fields, ... (Daniel Espendiller) [#1324](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1324)
* Added support for constants as FormOption (Thomas Rothe) [#1323](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1323)
* Update Symfony- and Doctrine-phpstorm-meta files (Thomas Schulz) [#1318](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1318)

## 0.18.173
* Remove "Plugin" from plugin name (Daniel Espendiller) [#1312](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1312)
* Support double backslashes for Twig controller references (Daniel Espendiller) [#1316](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1316)
* Fixed Symfony installer download to use https (Tugdual Saunier) [#1311](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1311)
* Fix changed indentation rules in 2018.3 with YAML (Cedric Ziel) [#1307](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1307)
* Add DQL language injection for $dql variables (Thomas Schulz)
* Add language injection for method parameters (CSS, XPath, DQL, JSON) (Thomas Schulz) [#1301](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1301)
* Add Symfony plugin SVG icons (for light & dark themes) (Thomas Schulz) [#1298](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1298)

## 0.17.172
* Fix service generation when a class name contains "Bundle" (Vincent Dechenaux) [#1293](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1293)
* Services with tag twig.extension should implement Twig_ExtensionInterface (Vincent Dechenaux) [#1292](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1292)
* Add doctrine-meta folder as PHP library into plugin (Thomas Schulz) [#1290](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1290)
* Add doctrine-phpstorm-meta as submodule (Thomas Schulz) [#1290](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1290)
* Updated symfony-phpstorm-meta files (Thomas Schulz) [#1289](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1289)

## 0.17.171
* Add symfony-meta folder as PHP library into plugin (Thomas Schulz) [#1286](https://github.com/Haehnchen/idea-php-symfony2-plugin/pull/1286)
* Routes: Adding support for inlined wildcard requirements [#1273](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1273) (Daniel Espendiller)
* Symfony 4.3: Support "Always Include Route Default Values" parameter syntax [#1271](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1271) (Daniel Espendiller)

## 0.17.170
* Add simple parsing for ICU MessageFormat placeholder (Markus Fasselt) [#1269](https://github.com/Haehnchen/idea-php-symfony2-plugin/pull/1269)
* Add supports for translation domains with +intl-icu suffix (Markus Fasselt) [#1269](https://github.com/Haehnchen/idea-php-symfony2-plugin/pull/1269)
* Add class name autocompletion to new service definition (Shyim) [#1265](https://github.com/Haehnchen/idea-php-symfony2-plugin/pull/1265)

## 0.17.169
* Provide completion for yaml arguments in \_defaults -> bind (Daniel Espendiller)
* Dont inspection service resources a classes for inspection [#1255](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1255) (Daniel Espendiller)
* Allow other plugins to extend container parameters [#1259](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1259) (Shyim)
* Fix service.xml creating [#1256](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1256) (Shyim)
* Symfony 4.2: Translation detection fixed for new TranslatorInterface namespace [#1254](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1254) (Michael Wolf)

## 0.17.168
* Symfony 4.2: Support improved form type extensions [#1246](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1246) (Daniel Espendiller)
* Named arguments in bind should provide navigation with [#1241](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1241) (Daniel Espendiller)
* Provide class existing inspection for class named service [#1239](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1239) (Daniel Espendiller)
* Cleanup non relevant old PhpStorm versions and add "2018.2.5" test env (Daniel Espendiller)
* Named arguments should be clickable [#1240](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1240) (Daniel Espendiller)

## 0.16.167
* Do not set until constraint so EAP users can still use the plugin

## 0.16.166
* Add 2018.3 to build matrix (Cedric Ziel)
* Inline Callback (Cedric Ziel)
* Fixed Symfony installer version selection to use https (Thomas Schulz)
* Move back to JBList to support legacy platform (Cedric Ziel)
* Move write action out of AWT event for PhpServiceArgumentIntention (Cedric Ziel)
* Adjust resource path for bundle file creation (Cedric Ziel)
* Remove unnecessary null check (Cedric Ziel)
* Update PHP plugin version to 182.3684.42 in 2018.2 build (Cedric Ziel)
* Adapt to changed YAML Psi to find the first KeyValue mapping (Cedric Ziel)
* Fix whitespace issues in fixtures (Cedric Ziel)
* Update build environment to 2018.2 stable (Cedric Ziel)
* Add YAML const GoTo Target for Symfony 3.2+ style constants (Cedric Ziel)
* Add inspection and quick fix for fuzzy service class names (Cedric Ziel)
* Remove unused imports (Cedric Ziel)
* Use adequate casing for sentence (Cedric Ziel)
* Drop unnecessary condition (Cedric Ziel)
* When able to, detect Symfony 4 "public" directory (Cedric Ziel)
* Small cleanups (Cedric Ziel)
* Fix Yaml Inspection for deprecated structure (Cedric Ziel)
* Update intelli gradle plugin to 0.3.3 (Cedric Ziel)
* Add 2018.2 to build matrix (Cedric Ziel)
* Migrate Project structure to use gradle ([#1164](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1164)) (Cedric Ziel)
* Fix travis 2017.3.x build (Daniel Espendiller)
* Fix anchor/querystring order (Massimiliano Arione)
* Fix link to asset function (Massimiliano Arione)

## 0.16.165
* Prevent duplicate same targets in yaml targets eg for class navigation
* Refactoring bundle loading, replacing HashMaps with ArrayList for non unique bundle project names

## 0.16.164
* Fix decorates linemarker is added twice because of XML ending tag
* \[DIC\] Add parent service linemarker definition [#1131](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1131)
* Drop support for PhpStorm 2017.2.x releases

## 0.15.163 / 0.16.163
* Fix assets reading and provide explicit resolving for asset files instead of rescanning them to improve performance and drop massive \`opendirectoryd\` cpu time [#809](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/809) [#1118](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1118)
* Support iterator in twig loop completion [#1097](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1097) [#1035](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1035)
* Migrate LineMarker target to leaf elements to fix performance warning / error [#1122](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1122)
* \[Security\] support voter attributes in is\_granted and has\_role security annotation [#892](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/892)
* Visit parent blocks for Twig variables [#1035](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1035)
* Add support for PhpClass references in ParamConverter::class annotation property
* \[0.16\] Support Twig lexer changes in PhpStorm 2017.3.2 [#1123](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1123) [#1125](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1125)

## 0.15.162
* Refactored Twig api in preparation for plugin split
* Add Twig block name indexer to improve performance [#1091](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1091)
* Rely on index for all block name relevant file visiting [#1091](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1091)
* Improve Twig types support for inline type declaration {# @var Class variable #} and {# @var variable Class #} [#1035](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1035)

## 0.15.161
* Allow path navigation for Twig templates in all php related navigation handler [#1076](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1076)
* Fix translation auto-complete not working in Symfony Flex directory structure [#1096](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1096)
* Refactoring form component to support more usages like extension navigation and self inheritance [#1098](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1098) [#695](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/695)
* [Form] Calling setDefault for data class does not associates form with binded class [#1048](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1048)

## 0.15.160
* Support dotenv type cast syntax [#1080](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1080)
* Support finalized Symfony 3.4 ControllerTrait shortcuts
* Provide add better "app" folder detection and add more tests for
* Support absolute path in Twig templates; optimize path resolving
* Add _fragment route parameter as always available [#1086](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1086) [#1071](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1071) @cedricziel

## 0.15.159
* Provide completion for named key parameter in xml files [#1052](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1052)
* Reimplement Twig template name resolving to improve performance for large projects
* Improve performance for instance check and drop very beginning Symfony2InterfacesUtil class
* Allow multiple targets for routing name resolving
* Add directory navigation for Twig templates [#1076](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1076)
* Merge twig and yaml goto / navigation handler into one extension

## 0.15.158
* \[Shopware #meetnext\] Add service argument updater via PHP intention
* \[SymfonyLive\] Add support for indexed and named arguments [#998](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/998)
* Improve support for class as id services in xml metadata
* Fix NPE for call to TranslationUtil.getTranslationLookupElementsOnDomain [#1067](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1067)
* Allow array function like array_merge variables in template rendering variables completion [#1052](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1052)
* Use Annotation plugin for template name DocBlock extraction and support "template" property
* Support includes in template rendering variables and fix function calls was detected as variable [#1052](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1052)

## 0.14.157 / 0.15.157
* Support services suggestions, argument inspection and service completion prioritization for service class as id attribute in xml files
* Symfony 3.4 support "improved the overriding of templates" [#1043](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1043); all template are know normalized inside index process
* Symfony 3.4 config provider supports "improved the overriding of templates" features [#1043](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1043)
* Empty namespace in Twig settings must be `__main__` to reflect internals
* Add completion for variables of template rendering in PHP; supporting function parameter and annotation methods [#1052](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1052)

## 0.14.156
* Fix binary incompatibility with 2017.2.x and upcoming 2017.3 [#991](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/991) [#1002](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1002) [#1040](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1040)
* Add Twig TokenParserInterface namespace for getTag extraction
* Add navigation for twig token parser tags
* Symfony 3.4 basic support of simpler injection of tagged services [#1039](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1039)
* Symfony 3.4 completion for PHP-based configuration for services [#1041](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1041)
* Optimize instance checks includes support for traits

## 0.14.155
* Fix Doc Tag completion in class constructors [#1024](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1024)
* Drop deprecated ScalarIndexExtension usage in index process
* Allow usage of new Twig 2.0 function and Twig 3.0 namespaces in Extension parser [#893](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/893) [#962](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/962)
* Fix StackOverflowError in FormOptionsUtil for collecting default values [#1026](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1026)
* Fix wrong instance of check in DocHashtagReference [#1024](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1024)
* Add support for Symfony flex template path structure [#922](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/922)
* Add support for Symfony flex: templates, configurations, yml -> yaml extension change, %kernel.project_dir% for Twig config path resolve [#922](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/922)
* Plugin auto configuration should be valid on vendor/symfony existence, drop project specification path validation
* Add support for Twig globals via configuration Twig => globals [#558](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/558) [#904](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/904)
* Add generator for Twig translations [#506](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/506)
* Provide translation shortcut insert handler for common text html elements in Twig file

## 0.14.154
* Drop several workarounds and deprecated usages for old PhpStorm versions
* Migrate xml service instance annotator to inspection and prevent possible memory leaks
* Remove configuration for all type provider og plugins settings
* Migrate yaml parameter and class annotator into inspections
* Migrate all yaml annotator to inspection like service instance check in constructor and calls
* Drop for all annotator configuration of plugin settings
* Fix global route loader was not care about annotations
* Remove annotation route indexer and merge into main router indexer [#648](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/648)
* API: Simplify usage of key index process
* Drop Twig annotator and move to inspections: routing, translations, templates and assets
* Introduce Twig interpolated and concat checks for string values in inspections
* Symfony 3.4: Add support for "controller" keyword for configuring routes controllers [#1023](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1023)
* Fix "trans" auto-complete not working in embed with parameters [#1012](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1012)

## 0.14.153
* Fix service generator was using wrong settings property

## 0.14.152
* Move yaml and php service missing annotator into an inspection to fix missing services in `Container::get` calls should be exposed as an inspection [#997](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/997)
* Fix wrongly lowercase inspection in service name as class name [#1011](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1011)
* Fix template extraction for methods with ending "Action" to fix non-existing template complaining in @Template() annotation [#999](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/999) [#1000](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1000)
* Dropping old deprecated single url generator settings
* Adopt default routing files path to Symfony > 2.7.20 [#1014](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1014) @Koc
* Add configuration switch for Symfony 3.3 service id as class attribute in service generator [#1016](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1016) and dropping class as parameter resolving in class attribute
* Support Symfony 3.4 controller route names prefix in annotations [#1017](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1017)
* Migrate "missing translation" annotator to inspections
* Provide support for translation domains in PHP references; like variables or constant
* Optimize translation folder detection "Resources/translations/" => "/translations/" [#1010](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/1010)

## 0.14.151
* Adopt default container/routing files path to Symfony Flex [#993](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/993) @Koc
* Fix xml id reference for Symfony 3.3 class shortcut was matched on all xml tags with id attribute
* Provide service completion and references for xml alias attribute [#996](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/996)
* Twig path directory separator must be "\" to be clickable [#995](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/995)

## 0.14.150
* Fix wrong cast in service builder class scope extraction [#989](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/989)
* Support Symfony 3.3 class shortcut in yaml and xml method tag scope

## 0.14.149
* Detect Twig templates when using `__invoke` controllers [#980](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/980); drop some redundant template annotation code
* Add @Template __invoke support for annotation template creation quickfix and in Twig template controller resolver [#980](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/980)
* Fix "Navigate to action" on controller with __invoke doesn't work [#986](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/986)
* Create template for missing template is really weird [#795](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/795); replace custom dialog with more commonly JBPopupFactory
* Provide better scope detection for Service Generator, add psr-0 class structure detection fixes: "Create Service" in project tree does nothing [#978](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/978)
* Add lazy decorated service resolving to fix Argument 'decorated' of ServiceUtil.getLineMarkerForDecoratedServiceId must not be null [#982](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/982)
* Add calls support for service usage indexer [#890](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/890)
* Add support for Twig 2 hassers for the attribute operator [#964](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/964)
* Add class completion for yaml service key to support Symfony 3.3 shortcut feature [#987](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/987)
* Fix empty string indexing of Doctrine repository class; "THashSet contract violation - 25 reports & coming" [#985](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/985)
* Use slash for controller subfolder which are PHP namespaces instead of filesystem style to fix autocompletion for controller names in routing files is broken for namespaced classes [#961](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/961)

## 0.14.148
* Symfony 3.3 Fully support autowire in defaults of XML [#966](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/966)
* Symfony 3.3 Arguments are not autocompleted when using the FQCN as id [#968](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/968) [#966](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/966)
* Symfony 4.0 Support array Kernel::getKernelParameters and array_replace [#973](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/973)
* Symfony 3.3 Provide class name completion in xml service ids [#967](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/967)
* Symfony 3.3 Goto definition does not support services with FQCN xml ids shortcuts [#952](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/952)

## 0.14.147
* Fix recursive string value resolve on class fields
* Dropping custom caret overlay listener and all extension points in favor of core parameter hints
* Symfony 3.3 Support classes in global namespace for id shortcut [#952](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/952)
* Symfony 3.3 Argument hint must support yaml id shortcut [#958](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/958)
* Symfony 3.3 Arguments not detected for type and argument index inspection in yaml id shortcut [#959](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/959)
* Symfony 3.3 Invalid lower case inspection for service id as class name [#960](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/960)
* Autocompletion for known tag names in Definition::addTag, clearTag, hasTag [#955](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/955)
* Add Twig print block support for linemarker and navigation

## 0.14.146
* Reworked compiled container parser to support aliases extraction more safety for Symfony 3.3 private service debugger [#618](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/618) [#943](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/943)
* Fix type cast error in voter role extraction [#941](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/941)
* Fix npe in service call visiting of yaml files [#942](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/942)
* Fix when using a classname as service id in a routing.yml plugin is mistakenly reporting the method as of missing in Symfony 3.3 [#940](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/940)
* The inner service of a decorator is always private [#908](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/908)
* Add Twig variable collector for parameter of a given macro scope
* Provide extension point a Twig variable collector
* Dropping ContainerInterface::get usage service linemarker
* Service indexer should known "_defaults" values of Symfony 3.3 dic component [#947](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/947)
* Add parameter parser for Kernel::getKernelParameters [#950](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/950)
* Add "controller.service_arguments" and new yaml service keys completion of Symfony 3.3
* Service argument inspection must respect _default configuration [#948](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/948)
* Service generator for YAML files extract indent from file scope [#533](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/533) [#374](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/374) [#362](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/362) [#736](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/736)
* Action generator menu should also display service generator for yaml files and reduce visibility for valid file scope

## 0.14.145
* Reduce blacklist of all file indexes; only ending with "Test" blocks processing now [#897](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/897)
* Fix translation domain name extraction on empty filename [#927](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/927)
* Provide ternary and better string value detection for form getParent and getExtendedType
* Globally support string value resolve for class constant

## 0.14.144
* Migrate MethodSignatureTypeProvider to PhpTypeProvider3; prevent cross plugin issue [#926](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/926) [#792](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/792)

## 0.14.143
* Add Twig form_theme indexer and provide targets in linemarker [#920](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/920)
* ContainerInterface::get doesn't resolve on multiple parameter; drop parameter length check [#916](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/916)
* Move PHP Annotation plugin `de.espend.idea.php.annotation` from soft to hard dependency to drop duplicate code usages [#448](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/448)
* Use new PhpTypeProvider3 and replace deprecated usages in Container::get, ObjectManager::get, EventDispatcherInterface::dispatch [#792](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/792)
* Refactoring of Twig macro logic, provide tests, use indexer [#924](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/924)
* Add completion for Twig macros after DOT element for "import as" function [#924](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/924)
* Drop regex from Twig set variable collector
* Migrate Doctrine type provider to PhpTypeProvider3 implementation [#792](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/792)
* Refactoring of ObjectManager::findBy* references and support more possible repository usages [#925](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/925) [#898](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/898)

## 0.14.142
* Implement environment variables references for %env(*) on .env and Docker files [#910](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/910)
* Add Twig trans and transchoice tag support for translation keys [#459](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/459)

## 0.14.141
* Parameter Hints must not be provided if plugin is not enabled [#896](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/896)
* Fix cache folder detection in project auto configuration process [#810](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/810) @20uf
* Add navigate to Twig "include" file references [#889](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/889) and use lazy value provider for better performance [#809](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/809)
* Support of resname attribute in xlf trans-unit tag [#913](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/913)
* Fix pattern for Yaml method "calls" [#755](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/755) and implement named services support
* Provide parameter type hint for xml and yaml "call" tags

## 0.14.140
* Add class navigation for named yaml service keys of Symfony 3.3 [#902](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/902)
* Add Parameter Hints for YAML and XML service arguments and dropping caret text overlay [#896](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/896)

## 0.13.139
* Support granted strings in $attributes parameter of VoterInterface:vote foreach and in_array
* Fix no description for an intention [#891](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/891)
* Sort service instance suggest on project usage [#890](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/890)
* Note: Last PhpStorm 2016.3.x release

## 0.13.138
* Use intention for Twig key creation, drop redundant warnings [#443](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/443)
* Fix roles completing/goto not working with array call [#886](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/886)
* Add support for translations placeholder in Twig and PHP [#631](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/631) [#528](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/528)
* Fix routing path parameter completion in Twig; use newly literal lexer element and drop regular expression for route name extractio
* Ignore interpolated strings in Twig "path" / "trans" annotator and inspections [#884](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/884)
* Fix incorrect resolving of Routes with too many underscores on new __invoke controller action in annotations [#881](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/881)
* Drop regular expression for trans filter in Twig, trust and use lexer [#877](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/877), [#814](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/814), [#716](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/716)
* Support absolute urls in profiler [#880](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/880)

## 0.13.137
* Fix npe in route indexing [#874](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/874)
* Fix parameter autocomplete double percentages [#871](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/871)
* Add support for isGranted in Twig and php on security.yaml, Voter::voteOnAttribute and Voter::supports [#431](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/431)
* Add Twig filters autocompletion for filter tag [#878](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/878)
* Replace hasReferencesInSearchScope which too slow for Twig variable extraction [#859](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/859) [#809](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/809)
* Support much more Twig render template pattern like array_merge for controller variables extraction
* Fix possible npe in container annotator because of empty xml tag value
* Wrong xml service instances should be more visible so move from weak to warning highlight
* Migrate Twig translation annotator to inspection; drop possible memory leaks with a popover bridge [#832](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/832)
* Add a persistent Twig translation annotator to create keys in all known domains [#443](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/443)

## 0.13.136
* Support SensioFrameworkExtraBundle @Route annotation for indexer [#828](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/828)
* Add else and elseif to Twig references tag whitelist; fix autocompletion on constant twig macro [#869](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/869)
* Implement support for PSR-11 containers [#867](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/867)
* Add inspection for xml and yaml constants in dic container
* Replace deprecated api usages

## 0.13.135
* Add global template navigation for xml strings and provide template name completion for "template" attributes [#803](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/803)
* Linemarker collectors are not instance safe, load lazy values internally [#846](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/846)
* Support class constant in QueryBuilder::from(Entity::class) [#824](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/824)
* Make yaml service id visitor case insensitive for class names [#847](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/847)
* Fix "Missing Parameter" warning for parameters set via environment variables [#852](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/852)
* Add twig test navigation [#860](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/860)
* Fix Twig missing asset if variable in declaration [#854](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/854)
* Use getBlockPrefix instead of override deprecated getName [#812](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/812) @Koc
* Optimize Twig clickable function pattern [#850](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/850)
* Add brace insert handler for Twig functions with string parameter detection [#864](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/864)
* Add support for class constant in Doctrine repositoryClass on annotation metadata [#857](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/857)
* Dropping weak route name annotator for php files
* Fix possible npe in variable type extraction [#822](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/822)

## 0.13.134
* Fix ConfigLineMarkerProvider.getTreeSignatures must not return null [#846](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/846)
* Support yaml "tags" shortcut syntax for service container [#849](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/849)
* Support optional class for named services [#847](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/847)
* Support new yaml factory syntax [#841](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/841)
* Fix Doctrine autocomplete not working for getRepository(Entity::class) [#824](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/824)
* Save last user selected service generator output format and reuse this value on dialog init [#829](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/829)

## 0.12.133 / 0.13.133
* PhpStorm 2016.3.1: Make compatible with new Deployment API @Leneshka-jb [#826](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/826)
* Code cleanup and Java8 language migrations

## 0.12.132
* Support more OptionsResolver options method parameter for references [#821](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/821)
* Add decorates linemarker for yaml and xml container files
* Service ids should be autocompleted for decorates [#834](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/834)
* Add PhpStorm 2016.3 / 2016.3.1 travis environment

## 0.12.131
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

## 0.12.130
* Add indexer for template usages in annotations [#773](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/773)
* Add scope for template index to reduce variable extraction and improve performance [#800](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/800)
* Template usages now also support function scope

## 0.12.129
* Fix navigation for bundle files on linux based system, increase path limit for child path iteration [#803](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/803)

## 0.12.128
* Decouple Twig namespace loading and provide more default namespace which work without a compiled container [#784](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/784) [#654](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/654)
* Add recursive and directory limit for per Twig path visitor [#800](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/800)
* Add icon provider for Twig template files for extends and implementations
* Dropping PhpStorm8 type class constant api workaround

## 0.12.127
* Profiler should support http urls as data source [#798](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/798)
* Profiler in now configurable in plugins settings [#798](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/798)
* Fix app_dev.php urls in profiler [#540](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/540), [#522](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/522)
* Add xml completion, navigation and linemarker for Doctrine 2.5 "Embeddables" [#471](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/471)

## 0.12.126
* Fix empty PSI elements should not be passed to createDescriptor in container case sensitivity inspection [#788](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/788)
* Support public property for form field mapping and dropping custom Doctrine field mapping its part of PropertyAccess component [#786](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/786)
* Fix "Cannot resolve symbol" for factory service regression and drop deprecated getVariant references for factory method completion [#791](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/791)
* Add linemarker provider for decorated services with lazy definition navigation
* Replace timer for caret listener with executor and future pattern [#785](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/785)
* Add linemarker for config tree builder root definition in [security,config]*.yml files and provide navigation for key itself [#793](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/793)
* Fix subscriber method creation type hint class was not imported and fix possible memory leak because of PsiElement references

## 0.12.125
* Dont index translations files without domain prefix
* Add twig path configuration parser of yaml files [#654](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/654)
* Support xml factory method and class tag [#778](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/778)
* Api migration for upcoming PhpStorm 2016.3 eap [#782](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/782)
* Smarter default namespace detection for default Domain of translations extraction dialog for injected html [#776](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/776)
* Add support for "twig.paths" as "add path" Twig namespaces [#654](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/654)

## 0.12.124
* Fix form "csrf_protection" was not found because of Symfony 3.0 interface drop
* Add static "FormType" fallback and visit method "setDefaultOptions", "configureOptions" for extension key
* Support translation_domain and default keys for form OptionsResolver implementation
* Rename "Symfony Installer" to "Symfony" in new project dialog
* Use IntelliJ DialogWrapper for dialog boxes of file templates
* Add service id completion for xml attribute value on class attribute
* Add completion for service id arguments without type attribute but valid service parent

## 0.12.123
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

## 0.12.122
* Service generator should close on escape key event
* Fix nullable condition on service container builder [#754](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/754)
* Fix yaml does not autocomplete route host option [#756](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/756)
* Settings for the plugin may be better placed inside the PHP group, like other frameworks [#735](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/735)
* EAP: Fix nullable value index for container parameter [#737](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/737)
* Fix possible memory leaks in settings because of project reference
* Add navigation for yaml constant "!php/const:" syntax
* Internally: Dropped all container service source, just one collection now

## 0.12.121
* Add support for decorator inner services [#510](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/510)
* Fix NPE exception in RouteHelper [#750](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/750)
* Fix NullPointerException in FormFieldResolver [#747](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/747)
* Add navigation for controller annotation [#748](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/748)
* Service parent key completion should only be valid inside service scope [#744](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/744)

## 0.12.120
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

## 0.12.119
* Add extension points to allow service collecting for external plugins
* Add extension point to locate service declaration in file
* Move default services from static file to collector

## 0.12.118
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

## 0.12.117
* Use popover for xml container tag suggestion
* Add class name completion for service generator dialog
* Service generator can now directly insert yaml services
* Some yaml ascii char dont need to be escaped, fix inspection for them and reduce deprecated warning to weak notification [#693](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/693)
* Migrate yaml argument creation and update callback to new lexer
* Add service completion suggestion / highlights for service arguments

## 0.12.116
* Migrate yaml routing controller navigation feature
* Migrate yaml config completion
* Migrate yaml sequence item usages, to fix wrong parameter resolving in call and arguments keys [#710](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/710)
* PhpUse#getOriginal is deprecated, use #getFQN instead @artspb
* Use yaml core utils to generate keys for translation, also support nested keys again [#708](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/708), [#711](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/711)

## 0.11.115 / 0.12.115
* Migrate our yaml features to new yaml plugin and support PhpStorm 2016.1 [#626](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/626)
* Provide additional text for yaml route keys completion
* Add quick fix for wrong service instance [#566](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/566); Use popup overlay for suggestion
* Respect formatting for generated service definitions [#374](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/374)
* Activate xml service generator insert button
* Add deprecated inspection for route and container settings in yaml and xml files
* Add Symfony 2.8 / 3.1 YAML deprecations inspections [#693](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/693), [#601](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/601)
* Fix definition created by "Generate Symfony2 service" is invalid because yaml deprecations [#638](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/638), [#693](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/693)

## 0.11.114
* Reduce access of read thread in webDeployment jobs [#694](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/694)
* Replace custom "instanceof" implementation with core isConvertibleFrom
* Fixing yaml class instance checks for single quote strings
* Increase testing coverage for mainly used yaml related features

## 0.11.113
* Decouple all webDeployment dependencies to extensions points and make all related feature optional [#688](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/688)
* Move remote container files parsing to main service factory, this simulates a local filesystem behavior
* Move plugin settings under "Languages and Frameworks" section [#690](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/690)
* Add Symfony 2 and 3 default routing paths to new implementation
* Add service suggestion intention for yaml and xml container files
* Provide service name suggestion quickfix for class instance check of xml and yaml container arguments
* Add XLIFF 2.0 support [#692](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/692)
* Add some more yaml service keys completion for newly added Symfony features

## 0.11.112
* Extracting webDeployment plugin deps into external file, this resolves crashes for disabled "Remote Hosts Access" plugin [#686](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/686)

## 0.11.111
* Add twig variable type inspection
* Add translation support for .xliff extension [#684](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/684)
* Add support for multiple routes; deprecates single usage [#138](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/138)
* Add controller test template [#584](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/584)
* Add inspection for deprecated twig variable usage; last level only
* Experimental: Add support for webDeployment plugin (Remote Host); supports external container and routing files on a "Default Server"
* Experimental: Extend "Remote Host" context menu with action to download configured remote files
* Experimental: Background task to download external files

## 0.11.110
* Add controller provider for PHP Toolbox
* Add description to PhpTyes @var syntax and allow multiline doc comments [#439](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/439)
* Add @see doc tag support for twig. supports: relative files, controller names, twig files, classes and methods syntax [#439](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/439)
* Add Symfony 3 controller getParameter shortcut support; migrate container getParameter registrar for supporting all proxy methods and navigation [#680](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/680)
* Create template for controller action on annotation should prioritize html files [#681](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/681)
* Migrate template create annotator to twig namespaces handling to not only support bundle files
* Add twig namespace extension point and provide json file for twig namespace configuration "ide-twig.json" see "Twig Settings" for example

## 0.11.109
* Fix autocomplete route name in php and twig not working since Symfony 2.8 [#669](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/669)
* Implement more annotation controller route naming strategies [#673](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/673)
* Add Doctrine model PHP Toolbox provider

## 0.11.108
* Try to fix "unable to get stub builder", looks like input filter is true always in helper class [#630](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/630), [#617](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/617)
* Implement PHP Toolbox providers: services, parameter, routes, templates, translation domains
* Fix autocomplete and goto is missing for service ids in DefinitionDecorator [#667](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/667)

## 0.11.107
* Implement twig block name completion workaround; need to strip block tag content on prefixmatcher [#563](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/563), [#390](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/390), [#460](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/460), WI-24362</li>
* Update yaml service template to match Symfony best practices [#657](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/657) @Ma27
* Add array syntax whitelist for twig "trans" domain extraction and support "transchoice" variable in regex [#662](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/662)
* Update travis test matrix dont allow Java8 and PhpStorm10 failing
* Autowire services must not inspect constructor arguments [#664](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/664)
* Synchronized clearing of CaretTextOverlayListener timer to prevent npe [#642](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/642)
* "Method References" and "Type Provider" are deprecated by now and will replaced by Plugin "PHP Toolbox"

## 0.11.106
* Check null before calling getFormTypeClassOnParameter in FormUtil [#650](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/650)
* Support form getParent syntax of Symfony 2.8 / 3.0 [#651](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/651)
* Dropping service alias "form.type" and "form.type_extension" form sources using interfaces instead
* Add path support, class prefix routes and auto naming for route annotation indexer
* Add new form extension visitor to reuse type visitor and support for nested ExtendedType form; resolves [#623](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/623) [#651](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/651)

## 0.11.105
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

## 0.11.104
* Replace deprecated eap "PhpType#add" collection signature with string iteration [#611](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/611), [#622](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/622), [#627](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/627)
* Globally provide references for xml "resources" attributes with Bundle and relative path syntax

## 0.11.103
* All service definitions now indexed as json
* Support service alias for weak services [#391](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/391)
* Add deprecated service inspection [#608](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/608)
* Migrate doctrine metadata index to json and fix npe [#610](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/610)

## 0.11.102
* Support command names inside constant and property strings
* Add autowire attribute to blacklist for service argument inspection [#616](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/616) and add "autowire" and "deprecated" yaml completion
* Add file resource index and add include line marker for routing definition
* Use lazy line marker for class service definitions
* Add route pattern/path provider for Symfony symbol search

## 0.11.101
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

## 0.11.100</h2>
* Add blank fix for empty doctrine repository index value [#609](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/609)

## 0.11.99
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

## 0.11.98
* Full references support for console helpers [#243](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/243)
* Add Doctrine couchdb support; merged into overall odm manager to reuse mongodb implementation
* Doctrine getRepository now returns self instance on an unknown class
* Fix plugin breaks the context menu in the Project view [#575](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/575) thx @steinarer, [#525](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/525)
* Recursively find bundle context for all related action

## 0.11.97
* Class constant signature fixed in PhpStorm9; provide another workaround for supporting both api levels [#541](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/541)
* Event dispatcher should return event class instance [#570](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/570)
* Catch npe issue with plugin enabled check, for global twig navigation [#574](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/574)
* Add "resource" file references for current directory scope [#517](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/517)
* Add assets completion for "absolute_url" [#550](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/550)
* Refactoring and fixing assets handling in PhpStorm9 [#551](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/551)
* Fix invalid inspection on container expressions in yaml files and add LocalInspection testing asserts [#585](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/585)
* Add Travis PhpStorm8, 9 and eap environment switches

## 0.11.96
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

## 0.11.95
* Add Doctrine simple_array and json_array for yaml files, on direct interface parsing [#555](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/555)
* Cache: Implement service definition cache layer, invalidates on global psi change [#350](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/350)
* Cache: Implement twig template name cache on psi change invalidation
* Cache: Refactoring TwigExtensionParser and introduce cache
* Cache: Add metadata cache for routing component
* Add PhpClass collector for "kernel.event_listener" events that are defined in xml and yaml [#531](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/531)
* Collect type hints for methods of getSubscribedEvents [#531](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/531), [#529](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/529)
* Implement support of "kernel.event_listener" events in completion, navigation and method creation argument type hints

## 0.11.94
* Remove postfix completion because its a PhpStorm9 core feature [#389](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/389)
* Improvement template name resolving for overwrites: support parent bundle and app resources; overwrite template linemarker [#437](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/437)
* Add Travis CI infrastructure thx to @Sorien [#536](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/536), [#534](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/534)
* Whitelist Twig_Environment parameter completion for template name

## 0.11.93
* Add "kernel.event_subscriber" to known tags and provide some more user feedback in error case [#511](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/511)
* Add _self support for twig macros [#419](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/419)
* Fix newline issue in controller template [#509](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/509)
* Add project generator for symfony installer and demo application [#475](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/475)

## 0.11.92
* Optimize service name generator and provide custom javascript strategy for it [#362](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/362)
* Add navigation form options of setRequired, setOptional, setDefined and refactor form options to visitor strategy [#502](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/502)
* Remove double Controller in classname [#507](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/507)
* Optimize form ui handling of service generator, prepare "insert" button, add generator action [#362](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/362)
* Use ContainerAwareCommand for command template

## 0.11.91
* Add support for doctrine xml metadata [#319](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/319)
* Add support for conditional twig extends tags, and replace regular match with pattern style
* Provide twig completion for html form action attribute [#497](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/497)
* Twig template file create quickfix should use PsiManager to support eg vcs [#498](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/498)
* Support for query_builder in entity form field type [#426](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/426)
* Fix npe in doctrine querybuilder chain processor [#495](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/495)
* Fix multiple resolve issues in php type provider

## 0.11.90
* Add CompilerPass intention and generator action for Bundle context [#484](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/484)
* Add support for new configureOptions in replacement for deprecated setDefaultOptions [#491](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/491), [#486](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/486), [#490](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/490)
* Add more bundle related file templates in NewGroup
* Fix "Missing argument" in services.yml doesn't keep track of factory methods [#492](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/492)

## 0.11.89
* Add yaml service arguments and tags intention / quickfixes [#470](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/470)
* Add xml tag intention and reuse tagged class interface list also for service generator [#470](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/470)
* Add method psi collector to support parent methods of a command [#454](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/454)
* Overall "Controllers as Services" optimize like navigation, related files, ... [#428](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/428)
* Use more stable PsiElements to find twig trans_default_domain domain name instead of regular expressions [#476](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/476)
* Fix multiResolve issue in method instance checks to resolve issue [#477](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/477) in multiple project command classes
* Fix wrong inspection for FormTypeExtensions tags [#483](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/483)
* Fix npe in index route arguments [#482](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/482)
* Fix warning for optional xml arguments [#485](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/485)

## 0.11.88
* Add console "getArgument" and "getOption" references [#454](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/454)
* "%" char in xml arguments now is a valid completion event [#461](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/461)
* Initial "Missing Argument" xml service inspection and quickfix [#470](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/470)
* Some performance improvements in xml and yaml service resolving

## 0.11.87
* Add completion for twig tags of Twig_TokenParserInterface::getTag implementations [#457](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/457)
* Add trans / transchoice twig tag 'from' support [#459](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/459)
* Add completion for Twig_SimpleTest extension in twig files after IS token
* Add twig operator completion in IF tags
* Fix pattern of twig trans_default_domain tag and use translation index for domain completion
* Fix several issues in twig array completion [#463](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/463)

## 0.11.86
* Support new setFactory syntax in yaml and xml [#436](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/436)
* Add service generator in class context of "Generator Popover" and intention in arguments
* Add twig assets completion for img src tags [#438](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/438)
* Add some more yaml service key completion
* Add method support for twig "for" statements [#208](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/208)
* Fix instance annotator in yaml psi pattern arguments on single quote string, after pattern api changes
* Fix completion for twig inline array doc block pattern
* Fix insertHandler for trailing backslash in twig doc var completion
* Note: implemented testing infrastructure [#405](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/405)

## 0.11.85
* Fix npe in custom assets resolving [#427](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/427)

## 0.11.84
* Fixing npe in tagged class inspections [#425](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/425)
* Add function parameter generator for "kernel.event_listener" on method create quickfix [#424](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/424)
* Add support for getSubscribedEvents inside method create quickfix [#424](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/424)
* Add support for custom assets [#353](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/353)
* Add static event parameter hint list for method create quickfix

## 0.11.83
* Add inspection for tagged services to validate corresponding interfaces or extends instances
* Add "Method Create" quickfix for xml files
* Add navigation, quickfix and inspections for methods inside tag statements [#422](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/422)
* Fix non unix eol error in template files [#421](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/421)

## 0.11.82
* Add method create quickfix for yaml files [#415](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/415)
* Remove weak service warning [#399](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/399)

## 0.11.81
* Fix multiresolve issues eg in AbstractManagerRegistry::getRepository [#403](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/403)

## 0.11.80
* Add missing route inspection with method creation quickfix
* Add deprecated inspection warning for service classes [#375](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/375)
* Support static string methods in twig filter and respect needs_context, needs_environment options in completion [#401](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/401) [#314](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/314)
* Allow more valid chars in annotation route index process [#400](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/400)
* Removes newly added leading backslash on phpstorm8 in class inserts [#402](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/402)
* Fix npe case in twig block goto [#397](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/397)

## 0.11.79
* Refactoring routing handling and prepare multiple route files [#138](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/138)
* Smarter route name resolve on indexed names [#392](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/392), [#376](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/376), [#365](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/365)
* Add doctrine 2.5 cache methods for class / repository completion [#203](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/203)
* Fixing IndexNotReadyException and "Read access is allowed" for eap changes [#370](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/370), [#383](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/383)

## 0.11.78
* Add twig embed tag indexer
* Support "include()" function and "embed" tag in twig variable collector
* Experimental: Add postfix completion [#389](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/389)
* Add more possible twig variables syntax from php files
* Add navigation for twig var doc
* Fix error on non unique class name completion in xml, yaml and twig [#387](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/387)
* Remove grouping for code folding, to make strings independent from each other

## 0.10.77 / 0.11.77
* Add weak routes in controller action related popover
* Add index for twig file php usage in render* methods and add variable collector
* Fix for new yaml SCALAR_STRING / SCALAR_DSTRING lexer changes in service instance annotator
* Fix max depth check in getTwigChildList [#360](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/360)
* Fix possible recursive calls in twig variable includes [#360](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/360)
* Note: last version for PhpStorm7!

## 0.10.76 / 0.11.76
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

## 0.10.75 / 0.11.75
* Add twig constants navigation, completion and folding [#327](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/327)
* Add references for array methods inside EventSubscriberInterface returns
* Add detection for "kernel.event_subscriber" tag on service builder [#352](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/352)
* Add indexer and references for xliff translations
* Quickfix for missing template will generate "block" and "extends" on directory context
* Better completion for class names in yaml and xml [#337](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/337)
* Fix twig missing translation domain pattern on nested filters [#255](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/255)
* Fix out of range exception in querybuilder parameter completion [#371](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/371)

## 0.10.74 / 0.11.74
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

## 0.10.73 / 0.11.73
* Fix npe in container parameter completion [#351](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/351)
* Add route requirements and options completion for yaml files

## 0.10.72 / 0.11.72
* Replace Form array options references with goto provider for performance improvements
* Support service container in library paths [#347](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/347)
* Use indexer for service parameter references to support weak file

## 0.10.71 / 0.11.71
* Fix whitespace pattern in twig function pattern [#340](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/340)
* Fixed typo in service generator "tags" should be "tag" on xml files [#338](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/338)
* Add extension point for controller actions related files
* Add extension point for GotoCompletionRegistrar
* Replace PsiReference for form type with GotoCompletionRegistrar [#313](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/313)

## 0.10.70 / 0.11.70
* Add linemarker for doctrine targetEntity relations
* Add doctrine query expr parameter completion
* Add support for querybuilder "from" index parameter [#322](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/322)
* Add completion for doctrine querybuilder alias in "createQueryBuilder" and "from" parameter
* Fix template file resolving for twig "app" resources

## 0.10.69 / 0.11.69
* Reworked twig template name resolving, for massive performance improvements [#321](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/321)
* Fix possible npe in TagReference inside php [#331](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/331)
* Hide first parameter in tail completion of twig extensions if its a Twig_Environment type hint [#314](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/314)
* Support twig file bundle overwrite in app folder [#275](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/275)
* Add reference provider for twig "block" function [#266](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/266)
* Provide "form" fallback on unknown from type and support nested strings [#325](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/325)
* Whitelist ".mongodb.yml" for controller related files
* 0.11: Use NavigationUtil for popups to fix eap api changes [#329](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/329)

## 0.10.68 / 0.11.68
* Provide weak form extension option completion [#317](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/317)
* Speedup form option completion [#318](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/318)
* Add new custom abstract reference replacements for deprecated getVariants [#313](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/313)
* Add weak doctrine namespaces on bundle names [#316](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/316)
* Add twig macro statement scope resolve for variables [#315](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/315)
* Add some missing retina icons [#312](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/312)

## 0.10.67 / 0.11.67
* Add array completion for constraints constructor [#304](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/304)
* Add support for twig.extension and form.type_extension in service generator [#308](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/308)
* Add bundle controller path to resource completion whitelist [#307](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/307)
* Map entity class with orm.yml file as linemarker [#309](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/309)
* Add current namespace resolving for yaml targetEntity [#305](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/305)
* Add class linemarker for yaml entities
* Add doctrine entity column names as lookup tail text in querybuilder completion

## 0.10.66 / 0.11.66
* Add weak tag references for xml and yaml container files
* 0.11.x: build against eap to resolve StringPattern#oneOf issues [#299](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/299)
* 0.11.x: reflect renaming of GotoRelatedFileAction [#297](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/297)

## 0.10.65
* Allow window path style in twig template names [#296](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/296)
* Add service indexer for tags in xml and yaml container files [#282](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/282)
* Add weak form types on new service tag indexer [#282](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/282)

## 0.10.64
* Add completion for repositoryClass in yaml
* Add completion for mappedBy and inversedBy in yaml
* Add referencedColumnName references for yaml and annotations
* Completely remove static doctrine yaml mapping list and use annotations fields
* Fix annotation targetEntity condition
* Prettify form field completion
* (Pls be careful on next PhpStorm 8 eap update!)

## 0.10.63
* Add completion for form alias tag in xml and yaml container files
* Support for yaml sequences in arguments instance annotator
* Service creator adds form alias as tag where possible [#281](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/281)
* Fix typo inside querybuilder resolver for oneToOne relations

## 0.10.62
* Add support for routes in xml files
* Provide twig context variables for include statements
* Fix some whitespace documents issue in yaml files

## 0.10.61
* Add support for doctrine id orm mapping of yaml files
* Add support for yaml CompoundValues inside routes action linemarker [#289](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/289)
* Fix that yaml files starting with whitespace not indexed for routes and services files
* Fix cast error on php array variables of twig types provider [#290](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/290)

## 0.10.60
* Fix translation annotator to not highlight compiled elements [#262](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/262)
* Fix non reload of translations which are outside PhpStorm index [#262](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/262)
* Add per translation file change indicator [#262](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/262)
* Cache twig file linemaker per file change request
* Add linemaker for routes in yaml
* Add duplicate key inspection for container files of yaml and xml
* Add duplicate route name inspection for yaml file

## 0.10.59
* Add extensions for type and reference provider
* Add instance check annotator for service classes of xml arguments
* Add goto for parameter definition inside yaml and xml
* Refactoring of xml service container references to provide many improvements in completion and navigation
* Remove regular expressions from Twig_Extensions parser and use internal lexer to support more use cases
* Add tail text for all Twig extensions and improve navigation

## 0.10.58
* Add completion for yaml config root keys
* Fix npe in config completion [#284](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/284)

## 0.10.57
* Add yaml key completion for config / security files on "config:dump-reference"
* Add completion for QueryBuilder:set
* Make Twig translation key extractor compatible with PhpStorm8 and allow undo [#213](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/213)

## 0.10.56
* Add twig translation extraction action [#213](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/213)
* Fix data_class in form types should autocomplete any class [#280](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/280)
* Add completion for QueryBuilder:(*)where

## 0.10.55
* Finally(?) fix NullPointerException on index values [#277](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/277), [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238)
* Optimize Doctrine QueryBuilder chaining method collector to resolve methods and also fix some errors [#278](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/278), [#274](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/274)
* Reimplementation of Twig @Template goto on PHP Annotations extension [#276](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/276)
* Migrate Route annotator to inspections [#273](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/273)
* Typo fix to support Doctrine OneToOne relations

## 0.10.54
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

## 0.10.53
* Fix slow index on large files [#261](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/261)
* Fix weak route annotation goto

## 0.10.52
* Globally use weak service and route index [#261](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/261)
* Add new weak annotator for routes and services
* Add route name indexer for annotation
* Add custom index keys processor for filter them in project context
* Add extension point to load custom doctrine model classes
* Fix annotate blank string values
* Remove duplicate from type completion [#260](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/260)

## 0.10.51
* Add twig macro name indexer
* Add macro include/from indexer and add implements linemarker
* Add custom "Symfony2 Symbol" search (Navigate > Symfony2 Symbol) in replacement for toolwindow [#229](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/229) (pls report possible keyboard shortcuts :) )
* Add twig macro and service index to symbol search
* Allow null keys in all index related stuff to temporary fix [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238)
* Strip quoted values inside yaml container indexer

## 0.10.50
* Add twig include indexer
* Add twig linemarker for includes

## 0.10.49
* Add translation key and domain indexer
* Rewrite and refactoring of all translation related stuff
* Make translations available without a compiled file on indexer as weak references
* Improvements in multiline values and quote key files for translation keys
* Rename parameter indexer key name to force a refresh, pls report npe directly to [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238) with your environment data if still occur

## 0.10.48
* Improvements in repositoryClass detection of doctrine annotations eg namespaces
* Add typename for repository "find*" lookup elements
* Add support for annotations based models inside "find*" repository calls
* Add extension point for container file loading
* Add "Interface" and "ClassInterface" to type provider [#254](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/254)
* Activate $option key references inside FormTypeInterface, because of working api now [#162](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/162)
* Refactoring of container related linemarkers to fix some npe (api break?) [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238)

## 0.10.47
* Add support for scss assets [#251](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/251)
* Migrate custom method references provider to variable resolver to support recursive calls
* Add references provider for console HelperSet [#243](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/243)

## 0.10.46
* Add goto for twig "parent" function [#246](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/246)
* Readd parameter class service annotator [#242](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/242)
* Dont use statusbar in phpstorm < 7.1 is not supported [#235](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/235)
* Make several services thread safe and implement npe fixes [#237](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/237), [#238](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/238)
* Dont fire twig type completion inside string values

## 0.10.45
* Some fixes for phpstorm 7.1.2

## 0.10.44
* Close profiler feature and merge into prod
* Add profiler statusbar widget
* Provide collector for mail, route, controller, template for profiler
* Attach all profiler collector to statusbar widget and provide suitable click targets

## 0.10.43
* Add basic form field support in twig types
* Add twig completion for "form.vars"
* Add ManagerRegistry:getManagerForClass reference provider [#231](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/231)
* Add support for twig form_theme [#232](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/232)
* Add function to twig type whitelist
* Fix some npe in yaml [#227](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/227)

## 0.10.42
* Add twig template folding and strip "Bundle"
* Add twig implements and overwrites block linemarker and provide custom popover [#75](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/75)
* Add basic implementation of enum like completion behavior eg Response::setStatusCode, Request::getMethod
* Add doctrine related files to controller method popup
* Use folding names in related file popup where suitable

## 0.10.41
* Add code folding provider for php with support for route, template and repository
* Add code folding provider for twig path and url function
* Add settings for all code folding provider (default=true)
* Add overwrite linemarker for twig blocks [#75](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/75)
* Add yaml static service config completion (class, arguments, ... )
* Readd twig completion workaround for filters (hell!)
* Fix error on class name with trailing backslash on yaml annotator
* Migrate template references, to resolve [#46](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/46) fully

## 0.10.40
* Add support for "Navigate > Related Files" (Ctrl+Alt+Home) inside controller action [#191](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/191)
* Rename plugin settings key to more unique name "Symfony2PluginSettings" [#209](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/209) [#122](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/122)
* Fix accidently removed UrlGeneratorInterface::generate and EntityManager::getReference
* Fix npe and cme in container index [#207](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/207), [#211](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/211) (use "File > Invalidate Cache", if issue still occur)

## 0.10.39
* Add support of php shortcut methods for repository, route and service references [#46](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/46)
* Add blank filter for service stub indexes [#207](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/207)

## 0.10.38
* Add parameter references for doctrine findOneBy/findBy, on yaml config
* Add goto model config inside getRepository
* Add type resolver for events name
* Fix missing @ in yaml service builder
* Fix npe in container index [#206](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/206)

## 0.10.37
* Add Doctrine MongoDB repository resolver  [#205](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/205)
* Add autopopup for string completion values
* Add support for more form methods  [#162](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/162)
* Add reference provider for form "options" keys [#162](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/162) limited by WI-21563
* Add templates for yaml, xml service files and controller
* Service builder is accessible inside project browser context menu of php files
* Fix for missing vendor libs since phpstorm 7.1 [#180](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/180)

## 0.10.36
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

## 0.10.35
* Add new method reference provider Parameter [#196](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/196)
* Add FormFactoryInterface::createForm option keys support
* Add Symbol and File contributor "Navigate > Symbol / File" [#189](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/189)
* Support upcoming "Search Everywhere" of PhpStorm 7.1 [#189](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/189)
* Support optional service reference syntax in yaml [#194](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/194)
* Support twig 1.15 "source" function [#190](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/190)
* Translation annotator check global translation file before fallback to yaml parser [#195](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/195)

## 0.10.34
* Add popover line marker to controller method, showing related files like templates and routes
* Add custom insert handle to not add double "@" on resource paths [#185](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/185)
* Add more twig template name normalizer and fix npe [#186](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/186)
* Prevent add empty and testing service to index
* Fix template annotations pattern are not compatible with phpstorm7 [#184](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/184)
* Fix yaml parameter annotator warnings on concatenate strings [#188](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/188)
* Fix parameter case-sensitivity issues [#179](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/179)
* Move repository to Haehnchen/idea-php-symfony2-plugin

## 0.10.33
* Add reference provider for FormInterface::get/has
* Add more twig template name normalizer [#182](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/182)
* Improve twig completion type lookup names

## 0.10.32
* Service container supports "field" elements eg properties and class constants [#151](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/151)
* Better template name detection on non common usage and performance improvements
* Add new method references provider for translation key with possible domain filter  [#155](https://github.com/Haehnchen/idea-php-symfony2-plugin/issues/155)
* Implement "twig extends" indexer for upcoming features

## 0.10.31
* Add raw yaml routes parser inside index process to provide line marker for controller actions (limited by RUBY-13914)
* Add new method reference provider ClassInterface
* Add controller line marker for twig file, if a matching file exists
* Xml method reference provider support class parameters eg "calls"
* Twig types support "is" as property shortcut

## 0.10.30
* Add support for twig globals in twig variable types
* Remove twig extension test classes from parser index
* Fix twig file scope variable collector

## 0.10.29
* Add controller variable collector for twig
* Add more twig variables pattern
* Add support for array variables in twig
* Improvement for completion and insert handler of twig variable
* Fix some npe and other exception

## 0.10.28
* Update twig macro pattern to support new twig elements
* Add twig macro alias support
* Add twig variable method resolver for goto provider
* Fix twig route path parameter pattern

## 0.10.27
* Add support for route parameter in php and twig
* Add twig variable type detection with goto and completion
* Add parser for twig globals defined as service and text in container file
* Add twig variable detection on inline doc block with several scopes
* Provide some logs for external file loaders like container. (Help -> Show Log ...)
* Remove deprecated twig workarounds
* Provide native route parser, to get all available route information
* Disable twig block name completion, because its blocked now see WI-20266

## 0.9.26 / 0.10.26
* Add completion, goto and line marker for FormTypeInterface:getParent
* Fix FormBuilderInterface:create signature check
* Last version which support PhpStorm 6

## 0.9.25 / 0.10.25
* Translation key and domain annotator for php and twig with yaml key creation quick fix
* Hack to support twig filter completion on char type event (see blocker) and goto
* Add yaml and xml service indexer
* Provide a service definition line marker for classes, based on service index
* Some more form builder completions

## 0.9.24 / 0.10.24
* Provide settings for service line marker and disable it on default

## 0.9.23 / 0.10.23
* Provide a service line marker
* Provide goto for class service definition (click on class name) if available in any suitable yaml or xml file
* Optimize twig assets wildcard detection and goto filter
* 0.10.23: Migrate javascripts and stylesheets to be compatible with twig plugin

## 0.9.22
* Add annotator for php instances inside yaml "calls" and "arguments" services
* Add annotator for method names of yaml "calls"
* Fix twig function insert handler insert double braces

## 0.9.21
* Support EventDispatcher calls inside php dispatcher and subscriber
* Improvements of Event and Tag completion / goto in all languages
* Provide global template goto in yaml
* Improvements in xml to reflected features of previous release
* Support locale routing of I18nRoutingBundle

## 0.9.20
* Mass improvements in php Container Builder (setAlias, Definition, Reference, Alias, findTaggedServiceIds)
* Provide goto for tagged container classes in php and yaml
* Support php template files
* Add ui for custom signature type providers
* Improvements in class doc hash provider and add new one #Interface

## 0.9.19
* Many improvements in template detection
* Support for translation_domain inside OptionsResolverInterface:setDefaults
* Hash tag docblocks are now searched on parent methods not only in current file
* New provider for form options

## 0.9.18
* Directly goto into form options definition not only to method
* Add form child name (underscore method) support on form builder resolve from setDefaultOptions:data_class
* Resolve parent calls inside setDefaultOptions eg for getting base form options
* Fix completion option on incomplete array definition (array key)
* Add php type resolve on form type parameter to not only support form types aliases

## 0.9.17
* Refactor of FormTypes reference contributor to provide goto and custom provider
* Provide form extension and default option array key completion / goto inside FormBuilder calls

## 0.9.16
* Improve twig extension parser to support goto and icons
* Provide domain goto and completion for twig trans_default_domain tag
* Add factory_method tag support inside yaml
* "Create Template" annotator is now also available in php and twig render calls

## 0.9.15
* Implement method parameter completion / goto on custom signatures
* Provide method parameter completion / goto on docblock hashtag
* Update help page for new features

## 0.9.14
* Fix for Settings saving
* Support PhpStorm EAP 7 build 130.1293
* Types for getRepository calls dont need backreferences anymore

## 0.9.13
* Add multi container support
* Some improvements for Twig namespace ui
* Settings ui cleanups and improvements
* Implement help page with reStructuredText and Sphinx, available on GitHub
* Assets annotator support wildcard folder

## 0.9.12
* Rework of XML Pattern to not fire on HTML
* Add local Parameter parser for Yaml
* Add local Parameter and Service parser for XML
* Fix all unsecured MethodReference casting
* Make Symfony "web" and "app" folder configurable in Settings form
* Introduce a Twig ui to manage template namespace (beta)

## 0.9.11
* Fix icon issue in PhpStorm 7 EAP
* Support translation and entity goto / completion in FormTypes arrays
* Quickfix to not fire plugin completion in HTML content since it also interpreted as XML

## 0.9.10
* Support Controller:forward in php
* Resolve repositoryClass on yaml or annotation config
* Support transchoice in php and twig
* Use trans_default_domain as fallback translation domain in twig
* Improvements in twig import, set and macro completion / goto
* Controller goto and completion for twig controller function

## 0.9.9
* Fix for parameter completion in yaml
* Provide global template goto in quoted strings of php and twig files
* Support completion and types for ObjectManager::find calls
* Implement twig extension parser to support function and filter completion (need JetBrains fix for full support WI-19022)
* Reduce build limit to make plugin installable on IntelliJ IDEA 12.1.4

## 0.9.8
* Activate doctrine entity PhpTypes on default
* Implement basic event and tag autocomplete in yaml and xml on known container elements
* Add Service method calls autocomplete in yaml and xml
* Implement a current file scope service parser for yaml, so private services are detected
* Add autocomplete for macro imports on "from" tag in twig

## 0.9.7
* Drop outdated PhpTypeProvider which were removed by PhpStorm 6.0.3
* Support new PhpTypeProvider2 to resolve ide freeze
* Fix for twig addPath paths
* Fix for twig template pattern, so include function is supported again
* Some smaller pattern fixes in yaml and php

## 0.9.6
* Add search panel (left sidebar) to find internal known Symfony components and go to them
* Fix assets "web" reader on Linux
* Filter yaml parameter annotator on token values
* Add icons for all known symfony2 components

## 0.9.5
* Add controller services support for go to and autocomplete
* Support strict=false syntax in yaml
* Fix for NullPointerException of plugin enabled check and routing indexing
* Plugin default state is now "Disabled" per project
* Get registered extra twig templates path on addPath of container
* Fix for YamlKeyFinder which provides better matching for translation go to

## 0.9.4
* Provide a global plugin state toggle per project
* Notice: Default plugin state will be "Disabled" in next version
* Provide go to controller method of routing names
* Autocomplete for _controller in yaml
* Support Yaml value with quote value
* Autocomplete and go to for routing resources in yaml
* Add translation go to for translation key in yaml and php, for yaml files
* Yaml Annotator for parameter, service and class
* Many PsiElement Pattern fixes

## 0.9.3
* Add Annotator which mark unknown route, template, service, assets
* Settings form can disable every Annotator, if its not suitable in environment
* Some autocomplete and pattern matches fixes and optimization
* Add autocomplete for class, factory-service, factory-class in yaml and xml
* Add notice for missing route and container file on project startup

## 0.9.2
* Autocomplete for twig blocks
* Go to for extended twig block
* Some twig translation fixes
* Yaml: Php class completion for service parameter ending with .class
* Yaml: Php class completion list service class parameter

## 0.9.1
* Temporary PhpTypes cache which reduce ide freeze (until fixed on JetBrains side)
* Add PhpTypes cache lifetime settings
* Add some more Annotation support
* Add Annotator and Action to create twig file directly on @Template clicking
* Autocomplete for FormTypes in FormBuilder
* Autocomplete of classes in yaml and xml
* Autocomplete for translation in trans twig and translate php
* Optimize twig templates searching, which sometimes generated outdated listing
* Auto use import of some supported Annotation

## 0.9.0
* Support app level twig templates (::layout.html.twig)
* Ability to disable php types providers in the settings (if you eccounter freezes when autocompleting classes etc)
* Support bundles assets (@AcmeDemoBundle/Resources/xxx)
* Add {% javascripts and {% stylesheets assets autocompletion
* Add assets go to file

## 0.8.1
* Should improve performance and fix some issues with use statements

## 0.8.0
* Autocomplete twig files in @Template annotations
* Go to twig file on @Template annotation
* Autocomplete container parameters in php/xml/yaml
* Autocomplete doctrine getRepository argument
* Go to entity class on getRepository argument
* Detect getRepository() result type
* Detect EntityRepository::find/findOneBy/findAll/findBy result type

## 0.7.1
* Add assets autocompletion in twig asset() calls

## 0.7.0
* Fix the fix about ide freezes
* Add auto completion inside doctrine's .orm.yml config files
* Add @ORM\\ annotations auto completion inside docblocks
* Add services auto completion inside yaml files
* Add services auto completion inside services.xml files
* Add class go to definition inside services.xml files

## 0.6.2
* Should fix ide freezes with class autocompletion (use XXX).

## 0.6.1
* Service aliases support
* Resolve services classes case insensitively

## 0.6.0
* Autocomplete route name in php and twig
* Should fix IDE freezes and StackoverflowException etc :)
* Performance improvment
* No more proxy method detection, the plugin has to know them (for example Controller::get)
* Smarter detection of functions call in twig (ie: {% set var = render('<caret>') %} should work)

## 0.5.3
* Fix a bug on windows when using an absolute path for the container.xml

## 0.5.2
* The plugin settings for the container.xml path, has now a file chooser, and allow paths outside the project

## 0.5.1
* Support {% embed autocomplete in twig

## 0.5.0
* Use a symfony2 icon instead of class icon in services autocomplete :)
* You can now click on templates name in twig to go to file (awesome!)
* "{{ include(" now autocomplete the template name

## 0.4.0
* Autocomplete template name in render() calls
* Clickable template name in render() calls
* Autocomplete template name in twig templates

## 0.3.3
* Better description, and integrate the changelog into the plugin

## 0.3.2
* Services id completion popup also show the class on the right

## 0.3.1
* Fix small cache issue

## 0.3.0
* Services id are now clickable (go to class definition), and autocompletable (CTRL+SPACE).
* Should support all ContainerInterface::get proxies as long as the id is the first argument
  (Previously, only direct calls to ContainerInterface::get or Controller::get)

## 0.2.1
* Fixed required idea build

## 0.2.0
* The `appDevDebugProjectContainer.xml` path can now be configured in the project settings.

## 0.1.0
* Detect ContainerInterface::get result type
