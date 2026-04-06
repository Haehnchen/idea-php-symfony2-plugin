@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityFieldsCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.ServiceDefinitionCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyCommandCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeOptionsCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestsCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRouteCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyServiceLocatorCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigComponentCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigExtensionCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateUsageCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateVariablesCollector
import kotlinx.coroutines.currentCoroutineContext
import org.apache.commons.lang3.StringUtils

/**
 * Groups Symfony MCP tools so they are exposed to the MCP server as a single toolset.
 */
class SymfonyToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists Symfony routes with URL mappings, controller mappings, file paths, and Twig template usages.

        Use this tool to find routes by route name, controller, partial route path, or any partial request URL.

        Returns CSV format with columns: name,controller,path,filePath,lineNumber,templates
        - name: Route name
        - controller: Full controller class::method reference
        - path: URL path pattern (contains parameters {id})
        - filePath: Relative path to controller file from project root
        - lineNumber: Controller method line number in the file; empty if unresolved
        - templates: Semicolon-separated list of Twig templates used by this controller references via render*() or php attributes

        Example output:
        name,controller,path,filePath,lineNumber,templates
        app_user_list,App\Controller\UserController::list,/users,src/Controller/UserController.php,42,user/list.html.twig;user/_pagination.html.twig
    """)
    suspend fun list_symfony_routes_url_controllers(
        @McpDescription("""Optional route-name filter (partial matching, case-insensitive). Examples: 'admin_dashboard', 'my_car_foo_stuff', 'App\Controller\ClassLikeRoute'""")
        routeName: String? = null,
        @McpDescription("""Optional controller filter (partial matching, case-insensitive). Valid examples: 'FooController', 'FooController::method', 'App\Controller\FooController::method'""")
        controller: String? = null,
        @McpDescription("Optional route-path or request-URL filter (partial matching). Examples: '/edit/{id}', '/edit/12', '/admin/users'")
        urlPath: String? = null,
        @McpDescription("Optional Ant-style glob on the controller file path relative to the project root. Example: 'src/Controller/**/*Admin*.php'")
        fileGlob: String? = null,
    ): String = withSymfonyProject { project ->
        readAction {
            SymfonyRouteCollector(project).collect(routeName, controller, urlPath, fileGlob)
        }
    }

    @McpTool
    @McpDescription("""
        Lists all Symfony console commands available in the project as CSV.

        Returns CSV format with columns: name,className,filePath,options,arguments
        - name: Command name (e.g., cache:clear)
        - className: FQN of implementing class
        - filePath: Relative path from project root
        - options: JSON array of command options with name, shortcut, description, defaultValue
        - arguments: JSON array of command arguments with name, description, defaultValue

        Example output:
        name,className,filePath,options,arguments
        cache:clear,\App\Command\CacheClearCommand,src/Command/CacheClearCommand.php,,
    """)
    suspend fun list_symfony_commands(
        @McpDescription("Optional Ant-style glob on the command class file path relative to the project root. Example: 'src/Command/**/*.php'")
        fileGlob: String? = null,
    ): String = withSymfonyProject { project ->
        readAction {
            SymfonyCommandCollector(project).collect(fileGlob)
        }
    }

    @McpTool
    @McpDescription("""
        Lists all Doctrine ORM entities in the project as CSV.

        Returns CSV format with columns: className,filePath
        - className: FQN of the entity class
        - filePath: Relative path from project root

        Example output:
        className,filePath
        App\Entity\User,src/Entity/User.php
    """)
    suspend fun list_doctrine_entities(): String = withSymfonyProject { project ->
        readAction {
            DoctrineEntityCollector(project).collect()
        }
    }

    @McpTool
    @McpDescription("""
        Lists all fields of a Doctrine entity as CSV.

        Parameters:
        - className: FQN of the entity class (e.g., "App\Entity\User" or "\App\Entity\User")

        Returns CSV format with columns: name,column,type,relation,relationType,enumType,propertyType
        - name: Field/property name
        - column: Database column name
        - type: Doctrine type (string, integer, text, etc.)
        - relation: Related entity class if this is a relation field
        - relationType: Type of relation (OneToOne, OneToMany, ManyToOne, ManyToMany)
        - enumType: FQN of the enum class for enum fields (PHP 8.1+)
        - propertyType: Pipe-separated PHP property types (e.g. "string", "\App\Status|null")

        Example output:
        name,column,type,relation,relationType,enumType,propertyType
        id,id,integer,,,,int
        username,username,string,,,string
        status,status,string,,,\App\Enum\Status,\App\Enum\Status
        orders,orders,,App\Entity\Order,OneToMany,,
    """)
    suspend fun list_doctrine_entity_fields(
        @McpDescription("""Doctrine entity FQN. Valid examples: 'App\Entity\User', '\App\Entity\User'""")
        className: String,
    ): String = withSymfonyProject { project ->
        if (className.isBlank()) {
            mcpFail("className parameter is required.")
        }

        readAction {
            DoctrineEntityFieldsCollector(project).collect(className)
        }
    }

    @McpTool
    @McpDescription("""
        Lists available Twig template extensions for code generation and template assistance.

        Use this to discover:
        - Filters for value transformation ({{ value|filter }})
        - Functions for template logic ({{ function() }})
        - Tests for conditionals ({% if var is test %})
        - Tags for control structures ({% tag %})

        Supports filtering by name (partial match) and type. Use to generate accurate Twig code,
        validate template syntax, or suggest available extensions to developers.

        Returns CSV: extension_type,name,className,methodName,parameters
        Example: filter,upper,\Twig\Extension\CoreExtension,upper,"value,encoding"
    """)
    suspend fun list_twig_extensions(
        @McpDescription("Partial name search (case-insensitive). Examples: 'date', 'url', 'format'")
        search: String? = null,
        @McpDescription("Include filters ({{ value|filter }}). Default: true")
        includeFilters: Boolean = true,
        @McpDescription("Include functions ({{ func() }}). Default: true")
        includeFunctions: Boolean = true,
        @McpDescription("Include tests ({% if value is test %}). Default: true")
        includeTests: Boolean = true,
        @McpDescription("Include tags ({% tag %}). Default: true")
        includeTags: Boolean = true,
    ): String = withSymfonyProject { project ->
        readAction {
            TwigExtensionCollector(project).collect(search, includeFilters, includeFunctions, includeTests, includeTags)
        }
    }

    @McpTool
    @McpDescription("""
        Lists recent Symfony requests from the Profiler as CSV for tracing request handling, controller resolution, and rendered Twig views.

        - hash: Profiler token/hash
        - method: HTTP method (GET, POST, etc.)
        - url: Request URL
        - statusCode: HTTP status code
        - profilerUrl: URL to the profiler page
        - controller: Controller handling the request
        - route: Matched route name
        - entryView: Symfony profiler "Entry View"
        - renderTemplate: Templates indexed from render*()/@Template/#[Template], joined by semicolon
        - renderedTemplates: First 3 Twig templates from the profiler "Rendered Templates" panel, in profiler order, joined by semicolon
        - formTypes: Form types used (pipe-separated if multiple)
    """)
    suspend fun list_profiler_requests(
        @McpDescription("Optional URL partial match, case-insensitive. Example: '/admin'")
        url: String? = null,
        @McpDescription("Optional profiler token partial match, case-insensitive. Example: '18e6b8'")
        hash: String? = null,
        @McpDescription("""Optional controller partial match, case-insensitive. Valid examples: 'UserController', 'UserController::showAction', 'App\Controller\UserController::showAction'""")
        controller: String? = null,
        @McpDescription("Optional route partial match, case-insensitive. Example: 'user_show'")
        route: String? = null,
        @McpDescription("Optional max rows. Default: 25.")
        limit: Int = 25,
    ): String = withSymfonyProject { project ->
        readAction {
            SymfonyProfilerRequestsCollector(project).collect(url, hash, controller, route, limit)
        }
    }

    @McpTool
    @McpDescription("""
        Lists all Symfony form types in the project as CSV.

        Returns CSV format with columns: name,className,filePath
        - name: Form type name/alias
        - className: FQN of the form type class
        - filePath: Relative path from project root

        Example output:
        name,className,filePath
        user,App\Form\UserType,src/Form/UserType.php
    """)
    suspend fun list_symfony_forms(): String = withSymfonyProject { project ->
        readAction {
            SymfonyFormTypeCollector(project).collect()
        }
    }

    @McpTool
    @McpDescription("""
        Lists all options for a Symfony form type as CSV.

        Returns CSV format with columns: name,type,source
        - name: Option name
        - type: Option type (DEFAULT, REQUIRED, DEFINED)
        - source: Source class that defines the option

        Example output:
        name,type,source
        label,DEFAULT,Symfony\Component\Form\Extension\Core\Type\FormType
        required,DEFAULT,Symfony\Component\Form\Extension\Core\Type\FormType
        data,DEFAULT,Symfony\Component\Form\Extension\Core\Type\FormType
    """)
    suspend fun list_symfony_form_options(
        @McpDescription("""Form type name or FQN. Valid examples: 'text', 'App\Form\UserType', 'Symfony\Component\Form\Extension\Core\Type\TextType'""")
        formType: String,
    ): String = withSymfonyProject { project ->
        if (formType.isBlank()) {
            mcpFail("formType parameter is required.")
        }

        readAction {
            SymfonyFormTypeOptionsCollector(project).collect(formType)
        }
    }

    @McpTool
    @McpDescription("""
        Locate in which lineNumber a Symfony service is defined in configuration files by service name or class name.

        Returns CSV format with columns: serviceName,className,filePath,lineNumber
        - serviceName: The service ID/name (from service definition)
        - className: FQN of the service class (if available)
        - filePath: Relative path from project root
        - lineNumber: Line number where the service definition starts (1-indexed)

        IMPORTANT: The lineNumber indicates only the START of the service definition.
        Service definitions are multi-line YAML/XML/PHP blocks. You MUST read a range
        of lines (around the lineNumber, typically 10-20 lines depending on complexity)
        to capture the complete service definition.

        Note: Autowired services (automatically registered by Symfony based on class names)
        do not have explicit definitions in config files.

        Example output:
        serviceName,className,filePath,lineNumber
        app.service.my_service,\App\Service\MyService,config/services.yaml,15
        app.my_service_alias,\App\Service\MyService,config/services.yaml,25
    """)
    suspend fun locate_symfony_service(
        @McpDescription("""Service ID or fully qualified class name. Examples: 'app.service.my_service', 'twig', '\App\Service\MyService'""")
        identifier: String,
    ): String = withSymfonyProject { project ->
        if (identifier.isBlank()) {
            mcpFail("identifier parameter is required.")
        }

        readAction {
            SymfonyServiceLocatorCollector(project).collect(identifier)
        }
    }

    @McpTool
    @McpDescription(
        $$"""
        Generate Symfony service definitions in YAML, XML, Fluent PHP or PHP array format for one or more classes.
        Inspects each class constructor to guess service dependencies from parameter types for explicit wiring.

        Fluent PHP example:
        ```php
        $services->set(\App\EmailService::class)
            ->args([
                service('mailer')
            ]);
        ```

        PHP array example (eg in `App::config()` or `return`):
        ```php
        [
            \App\EmailService::class => [
                'arguments' => [
                    service('mailer')
                ],
            ],
        ];
        ```
    """
    )
    suspend fun generate_symfony_service_definition(
        @McpDescription("""Fully qualified class name for the service, or a comma-separated list of class names. Valid examples: '\App\Service\EmailService', '\App\Service\EmailService,\App\Service\SmsService'""")
        className: String,
        @McpDescription("Output format: 'yaml' (default), 'xml', 'fluent' or 'phparray'")
        format: String = "yaml",
        @McpDescription("If true, uses class name as service ID (default); if false, generates a short ID")
        useClassNameAsId: Boolean = true,
    ): String = withSymfonyProject { project ->
        if (StringUtils.isBlank(className)) {
            mcpFail("className parameter is required.")
        }

        val outputType = when (format.lowercase()) {
            "xml" -> ServiceBuilder.OutputType.XML
            "yaml", "" -> ServiceBuilder.OutputType.Yaml
            "fluent" -> ServiceBuilder.OutputType.Fluent
            "phparray" -> ServiceBuilder.OutputType.PhpArray
            else -> mcpFail("Invalid format: '$format'. Valid values are: 'yaml', 'xml', 'fluent' or 'phparray'")
        }

        readAction {
            ServiceDefinitionCollector(project).collect(className, outputType, useClassNameAsId)
        }
    }

    @McpTool
    @McpDescription("""
        Lists usages of Twig templates. Accepts a partial template name and can optionally constrain resolved template files with a project-relative Ant-style glob.

        CSV columns (values semicolon-separated): template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path
        - template: logical template name (e.g. "home/index.html.twig")
        - controller: PHP methods via render()/renderView()/@Template/#[Template]
        - twig_include: {% include %} / {{ include() }}
        - twig_embed: {% embed %}
        - twig_extends: {% extends %}
        - twig_import: {% import %} / {% from ... import %}
        - twig_use: {% use %}
        - twig_form_theme: {% form_theme %}
        - twig_component: {{ component('X') }} / {% component X %} / <twig:X>
        - route_name: Symfony route name(s) for controller action(s) in the controller column
        - route_path: Symfony route path(s) for controller action(s) in the controller column

        Example:
        template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path
        partials/nav.html.twig,App\Controller\BaseController::index,templates/layouts/base.html.twig,,,,,,,app_base_index,/
        layouts/base.html.twig,,,,,templates/pages/home.html.twig;templates/pages/about.html.twig,,,,
    """)
    suspend fun list_twig_template_usages(
        @McpDescription("Optional partial template name. Example: 'home/index.html.twig'")
        template: String? = null,
        @McpDescription("Optional Ant-style glob on resolved template file paths relative to the project root. Examples: 'templates/home/index.html.twig', 'templates/admin/**/*.html.twig'")
        fileGlob: String? = null,
    ): String = withSymfonyProject { project ->
        val normalizedTemplate = template?.trim()?.takeIf { it.isNotBlank() }
        val normalizedFileGlob = fileGlob?.trim()?.takeIf { it.isNotBlank() }

        if (normalizedTemplate == null && normalizedFileGlob == null) {
            mcpFail("At least one of 'template' or 'fileGlob' must be provided.")
        }

        readAction {
            TwigTemplateUsageCollector(project).collect(normalizedTemplate, normalizedFileGlob)
        }
    }

    @McpTool
    @McpDescription("""
        Lists Symfony UX Twig components and composition metadata.

        Supports partial component-name search (case-insensitive).

        Returns CSV format with columns:
        component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks

        - component_name: canonical component name (e.g. Alert, Admin:Card)
        - template_relative_path: component template path relative to project root
        - component_tag: HTML syntax (e.g. <twig:Alert></twig:Alert>)
        - twig_component_syntax: function syntax (e.g. {{ component('Alert') }})
        - component_print_block_syntax: block print calls for available blocks (e.g. {{ block('footer') }})
        - twig_tag_composition_syntax: Twig tag composition snippet ({% component 'Alert' %}...{% endcomponent %})
        - props: component props exposed by PHP class/template props tag
        - template_blocks: block names provided by component template
    """)
    suspend fun list_twig_components(
        @McpDescription("Optional partial component-name filter (case-insensitive). Examples: 'Alert', 'Admin:Card'")
        search: String? = null,
    ): String = withSymfonyProject { project ->
        readAction {
            TwigComponentCollector(project).collect(search)
        }
    }

    @McpTool
    @McpDescription("""
        Lists all variables available in a Twig template with their PHP types and first-level accessible properties.
        Accepts a logical template name and can optionally filter resolved template files by project-relative path glob.

        CSV columns:
        - variable: Twig variable name
        - type: PHP FQN(s) joined with "|" (may include "[]" suffix for arrays)
        - properties: comma-separated first-level Twig-accessible names (get/is/has shortcut methods + public fields)

        Example:
        variable,type,properties
        user,\App\Entity\User,"id,email,name,roles,createdAt"
        app,\Symfony\Bridge\Twig\AppVariable,"user,request,session,environment,debug,token,flashes"
        products,\App\Entity\Product[],"id,title,price,category,isActive"
    """)
    suspend fun list_twig_template_variables(
        @McpDescription("Logical template name. Example: 'home/index.html.twig'")
        template: String,
        @McpDescription("Optional Ant-style glob on the resolved template file path relative to the project root. Example: 'templates/home/index.html.twig' or 'templates/admin/**/*.html.twig'.")
        fileGlob: String? = null,
    ): String = withSymfonyProject { project ->
        readAction {
            TwigTemplateVariablesCollector(project).collect(template, fileGlob)
        }
    }

    private suspend fun withSymfonyProject(action: suspend (Project) -> String): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        return action(project)
    }
}
