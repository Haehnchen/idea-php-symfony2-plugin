# Symfony Endpoints Integration - Agent Implementation Plan

**Target:** Implement `microservices.endpointsProvider` for Symfony Plugin
**Reference:** `ENDPOINTS_INTEGRATION_ANALYSIS.md`
**Inspired by:** Laravel Idea 12.3.0.253

---

## Agent Context

This plan guides an AI agent through implementing the Endpoints Tool Window integration for the Symfony Plugin. The feature exposes Symfony routes in IntelliJ Ultimate's Endpoints tool window.

### Prerequisites

- IntelliJ Ultimate (microservices plugin only available there)
- Existing Symfony Plugin codebase
- Understanding of `RouteHelper.getAllRoutes()` and related infrastructure

---

## Phase 1: Project Structure & Configuration

### Step 1.1: Create Optional Module XML

**File:** `src/main/resources/META-INF/symfony_endpoints.xml`

```xml
<idea-plugin>
    <extensions defaultExtensionNs="com.intellij">
        <microservices.endpointsProvider
            implementation="fr.adrienbrault.idea.symfony2plugin.integrations.endpoints.SymfonyEndpointProvider"/>
    </extensions>
</idea-plugin>
```

### Step 1.2: Register Optional Dependency

**File:** `src/main/resources/META-INF/plugin.xml`

Add after existing `<depends>` entries:

```xml
<!-- Optional: Endpoints tool window integration (Ultimate only) -->
<depends optional="true" config-file="symfony_endpoints.xml">
    com.intellij.microservices.ui
</depends>
```

### Step 1.3: Create Package Structure

```
src/main/java/fr/adrienbrault/idea/symfony2plugin/integrations/
└── endpoints/
    ├── SymfonyEndpointProvider.java
    ├── SymfonyEndpointInfo.java
    └── EndpointFeature.java
```

### Step 1.4: Create Runtime Feature Check

**File:** `src/main/java/fr/adrienbrault/idea/symfony2plugin/integrations/endpoints/EndpointFeature.java`

```java
package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints;

/**
 * Runtime check for Endpoints feature availability.
 * The microservices plugin is only available in IntelliJ Ultimate.
 */
public class EndpointFeature {
    private static final boolean ENDPOINTS_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("com.intellij.microservices.endpoints.EndpointProvider");
            available = true;
        } catch (ClassNotFoundException ignored) {
        }
        ENDPOINTS_AVAILABLE = available;
    }

    public static boolean isEndpointsAvailable() {
        return ENDPOINTS_AVAILABLE;
    }
}
```

---

## Phase 2: Core Implementation

### Step 2.1: Implement SymfonyEndpointProvider

**File:** `src/main/java/fr/adrienbrault/idea/symfony2plugin/integrations/endpoints/SymfonyEndpointProvider.java`

**Requirements:**
1. Implement `EndpointProvider` interface
2. Use `RouteHelper.getAllRoutes(project)` to get routes
3. Filter internal routes (names starting with `_`)
4. Filter routes without valid paths
5. Return `List<SymfonyEndpointInfo>`

**Key Implementation Points:**

```java
package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints;

import com.intellij.microservices.endpoints.EndpointInfo;
import com.intellij.microservices.endpoints.EndpointProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SymfonyEndpointProvider implements EndpointProvider {

    private static final String ID = "symfony.routes";

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Symfony Routes";
    }

    @Override
    public @NotNull Icon getIcon() {
        return Symfony2Icons.SYMFONY;
    }

    @Override
    public boolean isApplicable(@NotNull Project project) {
        Settings settings = Settings.getInstance(project);
        return settings != null && settings.pluginEnabled;
    }

    @Override
    public @NotNull List<EndpointInfo> getEndpoints(@NotNull Project project) {
        List<EndpointInfo> endpoints = new ArrayList<>();

        Map<String, Route> routes = RouteHelper.getAllRoutes(project);

        for (Route route : routes.values()) {
            // Skip internal routes (starting with _)
            if (route.getName().startsWith("_")) {
                continue;
            }

            // Skip routes without a path
            String path = route.getPath();
            if (path == null || path.isEmpty()) {
                continue;
            }

            endpoints.add(new SymfonyEndpointInfo(project, route));
        }

        return endpoints;
    }

    @Override
    public void navigateToSource(@NotNull EndpointInfo endpoint) {
        if (endpoint instanceof SymfonyEndpointInfo symfonyEndpoint) {
            symfonyEndpoint.navigate();
        }
    }
}
```

### Step 2.2: Implement SymfonyEndpointInfo

**File:** `src/main/java/fr/adrienbrault/idea/symfony2plugin/integrations/endpoints/SymfonyEndpointInfo.java`

**Requirements:**
1. Wrap `Route` object
2. Implement `EndpointInfo` interface
3. Provide navigation to controller method

**Key Implementation Points:**

```java
package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints;

import com.intellij.microservices.endpoints.EndpointInfo;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SymfonyEndpointInfo implements EndpointInfo {

    private final @NotNull Project project;
    private final @NotNull Route route;

    public SymfonyEndpointInfo(@NotNull Project project, @NotNull Route route) {
        this.project = project;
        this.route = route;
    }

    @Override
    public @NotNull List<String> getMethods() {
        Collection<String> methods = route.getMethods();

        if (methods.isEmpty()) {
            // Default to all methods if none specified
            return List.of("GET", "POST", "PUT", "DELETE", "PATCH");
        }

        return new ArrayList<>(methods);
    }

    @Override
    public @NotNull String getPath() {
        String path = route.getPath();
        return path != null ? path : "/";
    }

    @Override
    public @Nullable String getHandlerName() {
        return route.getController();
    }

    @Nullable
    @Override
    public String getGroupName() {
        String name = route.getName();

        // Group by route name prefix (e.g., "app_user_show" → "app")
        int underscoreIndex = name.indexOf('_');
        if (underscoreIndex > 0) {
            return name.substring(0, underscoreIndex);
        }

        // Or group by controller namespace
        String controller = route.getController();
        if (controller != null && controller.contains("\\")) {
            String[] parts = controller.split("\\\\");
            if (parts.length > 2) {
                return parts[1];
            }
        }

        return "Routes";
    }

    @Override
    public @Nullable Object getSourceElement() {
        PsiElement[] methods = RouteHelper.getMethods(project, route.getName());
        if (methods.length > 0) {
            return methods[0];
        }
        return null;
    }

    /**
     * Navigate to the controller method.
     */
    public void navigate() {
        PsiElement[] methods = RouteHelper.getMethods(project, route.getName());

        if (methods.length > 0 && methods[0] instanceof Navigatable navigatable) {
            if (navigatable.canNavigate()) {
                navigatable.navigate(true);
            }
        }
    }

    public @NotNull String getRouteName() {
        return route.getName();
    }

    public @NotNull List<String> getPathVariables() {
        return new ArrayList<>(route.getVariables());
    }
}
```

---

## Phase 3: Build & Verify

### Step 3.1: Build the Plugin

```bash
./gradlew clean buildPlugin
```

### Step 3.2: Verify Compilation

Check for:
- No compilation errors
- Optional dependency correctly configured
- Classes compile only when microservices API is available

---

## Phase 4: Testing

### Step 4.1: Create Test Class

**File:** `src/test/java/fr/adrienbrault/idea/symfony2plugin/integrations/endpoints/SymfonyEndpointProviderTest.java`

```java
package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

public class SymfonyEndpointProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testProviderIsApplicableForSymfonyProject() {
        // Create minimal Symfony structure
        myFixture.addFileToProject("symfony.lock", "{}");

        Project project = getProject();
        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();

        assertTrue(provider.isApplicable(project));
    }

    public void testProviderReturnsEndpoints() {
        // Create route configuration
        myFixture.addFileToProject(
            "config/routes.yaml",
            "app_user_show:\n" +
            "  path: /users/{id}\n" +
            "  controller: App\\Controller\\UserController::show\n" +
            "  methods: [GET]"
        );

        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        List<?> endpoints = provider.getEndpoints(getProject());

        assertFalse(endpoints.isEmpty());
    }

    public void testInternalRoutesAreFiltered() {
        myFixture.addFileToProject(
            "config/routes.yaml",
            "_wdt:\n" +
            "  path: /_wdt/{token}\n" +
            "  controller: web_profiler.controller.profiler::toolbarAction\n" +
            "\n" +
            "app_home:\n" +
            "  path: /\n" +
            "  controller: App\\Controller\\HomeController::index"
        );

        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        List<?> endpoints = provider.getEndpoints(getProject());

        // Should only contain app_home, not _wdt
        assertEquals(1, endpoints.stream()
            .filter(e -> ((SymfonyEndpointInfo) e).getPath().startsWith("/_"))
            .count());
    }

    public void testEndpointInfoProperties() {
        myFixture.addFileToProject(
            "config/routes.yaml",
            "api_users_list:\n" +
            "  path: /api/users\n" +
            "  controller: App\\Controller\\Api\\UserController::list\n" +
            "  methods: [GET, POST]"
        );

        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        List<?> endpoints = provider.getEndpoints(getProject());

        SymfonyEndpointInfo endpoint = endpoints.stream()
            .filter(e -> ((SymfonyEndpointInfo) e).getRouteName().equals("api_users_list"))
            .map(e -> (SymfonyEndpointInfo) e)
            .findFirst()
            .orElse(null);

        assertNotNull(endpoint);
        assertEquals("/api/users", endpoint.getPath());
        assertEquals("App\\Controller\\Api\\UserController::list", endpoint.getHandlerName());
        assertTrue(endpoint.getMethods().contains("GET"));
        assertTrue(endpoint.getMethods().contains("POST"));
        assertEquals("api", endpoint.getGroupName());
    }
}
```

### Step 4.2: Run Tests

```bash
./gradlew test --tests "*SymfonyEndpointProvider*"
```

---

## Phase 5: Manual Verification

### Step 5.1: Install in IntelliJ Ultimate

1. Build plugin: `./gradlew buildPlugin`
2. Open IntelliJ Ultimate
3. Install plugin from disk: `build/distributions/symfony2-plugin-*.zip`

### Step 5.2: Test in Symfony Project

1. Open a Symfony project
2. Navigate to: View → Tool Windows → Endpoints
3. Verify:
   - Symfony Routes group appears
   - Routes are listed with correct methods
   - Clicking a route navigates to controller
   - Search/filter works correctly

### Step 5.3: Test Edge Cases

- [ ] Empty route collection
- [ ] Routes without controller
- [ ] Routes with multiple methods
- [ ] Routes with path variables
- [ ] Internal routes (starting with `_`) are hidden

---

## Implementation Checklist

### Files to Create

| File | Status |
|------|--------|
| `META-INF/symfony_endpoints.xml` | [ ] |
| `integrations/endpoints/EndpointFeature.java` | [ ] |
| `integrations/endpoints/SymfonyEndpointProvider.java` | [ ] |
| `integrations/endpoints/SymfonyEndpointInfo.java` | [ ] |
| `test/.../SymfonyEndpointProviderTest.java` | [ ] |

### Files to Modify

| File | Change | Status |
|------|--------|--------|
| `META-INF/plugin.xml` | Add optional dependency | [ ] |

### Verification Steps

- [ ] Plugin compiles without errors
- [ ] Tests pass
- [ ] Plugin works in IntelliJ Ultimate
- [ ] Endpoints appear in tool window
- [ ] Navigation to controllers works
- [ ] Internal routes are filtered

---

## Key Dependencies

### Existing Classes to Use

| Class | Purpose |
|-------|---------|
| `fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper` | Get all routes |
| `fr.adrienbrault.idea.symfony2plugin.routing.Route` | Route model |
| `fr.adrienbrault.idea.symfony2plugin.Settings` | Plugin settings |
| `fr.adrienbrault.idea.symfony2plugin.Symfony2Icons` | Symfony icon |

### IntelliJ API (Ultimate Only)

| Class | Purpose |
|-------|---------|
| `com.intellij.microservices.endpoints.EndpointProvider` | Provider interface |
| `com.intellij.microservices.endpoints.EndpointInfo` | Endpoint data |

---

## Estimated Effort

| Phase | Time |
|-------|------|
| Configuration | 30 min |
| Core Implementation | 2 hours |
| Testing | 1 hour |
| Manual Verification | 30 min |
| **Total** | **~4 hours** |

---

## Success Criteria

1. **Compilation:** Plugin builds successfully
2. **Ultimate Only:** Feature only active in IntelliJ Ultimate
3. **Visibility:** All Symfony routes appear in Endpoints tool window
4. **Navigation:** Clicking an endpoint navigates to the controller
5. **Filtering:** Internal routes are hidden
6. **Grouping:** Routes are organized by prefix/controller
7. **Performance:** No noticeable delay loading routes

---

## Notes for Agent

1. **Read existing code first:** Understand `RouteHelper` and `Route` classes
2. **Check API availability:** Some methods may differ from Laravel implementation
3. **Test on Ultimate:** Feature only works in IntelliJ Ultimate
4. **Reuse existing infrastructure:** Don't duplicate route collection logic
5. **Handle nulls gracefully:** Routes may not have all properties set
