package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.json.JsonFileType;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonValue;
import com.intellij.lang.ecmascript6.psi.ES6FromClause;
import com.intellij.lang.ecmascript6.psi.ES6ImportDeclaration;
import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.TypeScriptFileType;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.ecmal4.JSReferenceListMember;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StimulusController;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Index for Stimulus controllers in JavaScript/TypeScript files.
 *
 * Indexes JavaScript classes that extend Controller from '@hotwired/stimulus'.
 * Controller names are derived from the filename (e.g., hello_controller.js -> hello).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StimulusControllerStubIndex extends FileBasedIndexExtension<String, StimulusController> {
    public static final ID<String, StimulusController> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.stimulus_controller_index");
    private static final ObjectStreamDataExternalizer<StimulusController> EXTERNALIZER = new ObjectStreamDataExternalizer<>();
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @Override
    public @NotNull ID<String, StimulusController> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, StimulusController, FileContent> getIndexer() {
        return inputData -> {
            Map<String, StimulusController> map = new HashMap<>();

            // Handle controllers.json files
            if (inputData.getPsiFile() instanceof JsonFile jsonFile) {
                return indexControllersJson(jsonFile);
            }

            // Handle JavaScript/TypeScript controller files
            if (!(inputData.getPsiFile() instanceof JSFile jsFile)) {
                return map;
            }

            // Two independent indexing mechanisms:

            // 1. app.register() calls in bootstrap/app files (e.g., app.register('my-controller', MyController))
            collectRegisteredControllers(jsFile, map);

            // 2. Standard controller files (*_controller.js, *-controller.ts)
            if (hasControllerImportFromStimulus(jsFile) && hasClassExtendingController(jsFile)) {
                String controllerName = extractControllerName(inputData.getFile());
                if (controllerName != null) {
                    String finalControllerName = buildNamespaceFromPath(inputData.getFile(), controllerName);
                    map.put(finalControllerName, new StimulusController(finalControllerName));
                }
            }

            return map;
        };
    }

    /**
     * Walk the AST to find import declarations from '@hotwired/stimulus' or 'stimulus'
     * that import 'Controller'
     */
    private boolean hasControllerImportFromStimulus(@NotNull JSFile jsFile) {
        boolean[] hasImport = {false};

        jsFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (hasImport[0]) {
                    return;
                }

                if (element instanceof ES6ImportDeclaration importDecl) {
                    ES6FromClause fromClause = importDecl.getFromClause();
                    if (fromClause != null) {
                        String referenceText = fromClause.getReferenceText();
                        if (referenceText != null) {
                            referenceText = stripQuotes(referenceText);
                            // Check for @hotwired/stimulus or stimulus module
                            if ("@hotwired/stimulus".equals(referenceText) || "stimulus".equals(referenceText)) {
                                // Check if Controller is in the import specifiers
                                String importText = importDecl.getText();
                                if (importText != null && importText.contains("Controller")) {
                                    hasImport[0] = true;
                                    return;
                                }
                            }
                        }
                    }
                }

                super.visitElement(element);
            }
        });

        return hasImport[0];
    }

    /**
     * Find the variable name that holds the result of startStimulusApp() call.
     * Pattern: const app = startStimulusApp();
     *
     * @return The variable name (e.g., "app") or null if not found
     */
    @Nullable
    private String findStimulusAppVariable(@NotNull JSFile jsFile) {
        String[] appVarName = {null};

        jsFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (appVarName[0] != null) {
                    return;
                }

                // Look for: const app = startStimulusApp()
                if (element instanceof JSVariable variable) {
                    JSExpression initializer = variable.getInitializer();
                    if (initializer instanceof JSCallExpression callExpr) {
                        JSExpression methodExpr = callExpr.getMethodExpression();
                        if (methodExpr instanceof JSReferenceExpression refExpr) {
                            if ("startStimulusApp".equals(refExpr.getReferenceName())) {
                                appVarName[0] = variable.getName();
                                return;
                            }
                        }
                    }
                }

                super.visitElement(element);
            }
        });

        return appVarName[0];
    }

    /**
     * Collect controller names from app.register('name', Controller) calls.
     * Only collects from the variable that holds startStimulusApp() result.
     *
     * Pattern: const app = startStimulusApp(); app.register('controller_name', SomeController)
     */
    private void collectRegisteredControllers(@NotNull JSFile jsFile, @NotNull Map<String, StimulusController> map) {
        String appVarName = findStimulusAppVariable(jsFile);
        if (appVarName == null) {
            return;
        }

        jsFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof JSCallExpression callExpr) {
                    JSExpression methodExpr = callExpr.getMethodExpression();
                    if (methodExpr instanceof JSReferenceExpression refExpr) {
                        // Check if it's a .register() call
                        if ("register".equals(refExpr.getReferenceName())) {
                            // Check if it's called on the stimulus app variable
                            JSExpression qualifier = refExpr.getQualifier();
                            if (qualifier instanceof JSReferenceExpression qualifierRef) {
                                if (appVarName.equals(qualifierRef.getReferenceName())) {
                                    JSExpression[] arguments = callExpr.getArguments();
                                    if (arguments.length >= 2 && arguments[0] instanceof JSLiteralExpression literal) {
                                        if (literal.isQuotedLiteral()) {
                                            String controllerName = literal.getStringValue();
                                            if (controllerName != null && !controllerName.isEmpty()) {
                                                map.put(controllerName, new StimulusController(controllerName));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                super.visitElement(element);
            }
        });
    }

    /**
     * Walk the AST to find class declarations that extend Controller
     */
    private boolean hasClassExtendingController(@NotNull JSFile jsFile) {
        boolean[] hasClass = {false};

        jsFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (hasClass[0]) {
                    return;
                }

                if (element instanceof JSClass jsClass) {
                    // Get the extends list and check the text
                    var extendsList = jsClass.getExtendsList();
                    if (extendsList != null) {
                        for (JSReferenceListMember member : extendsList.getMembers()) {
                            @Nullable String extendsText = member.getReferenceText();
                            if (extendsText != null && "Controller".equals(extendsText.trim())) {
                                hasClass[0] = true;
                                return;
                            }
                        }
                    }
                }

                super.visitElement(element);
            }
        });

        return hasClass[0];
    }

    /**
     * Extract the Stimulus controller name from the filename.
     * Convention: hello_controller.js -> hello
     *             my_component_controller.js -> my-component
     */
    @Nullable
    private String extractControllerName(@NotNull VirtualFile file) {
        String fileName = file.getNameWithoutExtension();

        // Remove _controller or -controller suffix
        if (fileName.endsWith("_controller")) {
            fileName = fileName.substring(0, fileName.length() - "_controller".length());
        } else if (fileName.endsWith("-controller")) {
            fileName = fileName.substring(0, fileName.length() - "-controller".length());
        } else {
            // Not a standard Stimulus controller filename
            return null;
        }

        // Convert underscores to dashes (Stimulus convention)
        return fileName.replace("_", "-");
    }

    /**
     * Strip surrounding quotes (single or double) from a string.
     */
    @NotNull
    private String stripQuotes(@NotNull String text) {
        if (text.length() >= 2) {
            char first = text.charAt(0);
            char last = text.charAt(text.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return text.substring(1, text.length() - 1);
            }
        }
        return text;
    }

    /**
     * Build namespace from parent folder path (max 3 levels, stopping at "controllers" folder).
     * Uses getParent() to navigate up the directory tree.
     *
     * @param file The controller file
     * @param controllerName The base controller name (e.g., "list")
     * @return Full controller name with namespace (e.g., "users--list")
     */
    @NotNull
    private String buildNamespaceFromPath(@NotNull VirtualFile file, @NotNull String controllerName) {
        StringBuilder namespace = new StringBuilder();
        VirtualFile parent = file.getParent();

        for (int level = 0; level < 3 && parent != null; level++) {
            String parentName = parent.getName();

            // Stop when we reach the "controllers" folder
            if ("controllers".equals(parentName)) {
                break;
            }

            // Prepend this folder to namespace
            if (!namespace.isEmpty()) {
                namespace.insert(0, "--");
            }

            namespace.insert(0, parentName.replace("_", "-"));
            parent = parent.getParent();
        }

        // Combine namespace and controller name with double-dash separator
        if (!namespace.isEmpty()) {
            return namespace + "--" + controllerName;
        }

        return controllerName;
    }

    /**
     * Index controllers from controllers.json file.
     * Expected structure:
     * {
     *   "controllers": {
     *     "@symfony/ux-chartjs": {
     *       "chart": {
     *         "enabled": true,
     *         "fetch": "eager"
     *       }
     *     }
     *   }
     * }
     *
     * This will extract controller names like "@symfony/ux-chartjs/chart"
     */
    @NotNull
    private Map<String, StimulusController> indexControllersJson(@NotNull JsonFile jsonFile) {
        Map<String, StimulusController> map = new HashMap<>();

        JsonObject topLevelObject = getTopLevelObject(jsonFile);
        if (topLevelObject == null) {
            return map;
        }

        JsonProperty controllersProperty = topLevelObject.findProperty("controllers");
        if (controllersProperty == null) {
            return map;
        }

        JsonValue controllersValue = controllersProperty.getValue();
        if (!(controllersValue instanceof JsonObject controllersObject)) {
            return map;
        }

        // Iterate through packages (e.g., "@symfony/ux-chartjs")
        for (JsonProperty packageProperty : getPropertyList(controllersObject)) {
            JsonValue packageValue = packageProperty.getValue();
            if (!(packageValue instanceof JsonObject packageObject)) {
                continue;
            }

            String packageName = packageProperty.getName();

            // Iterate through controllers in this package (e.g., "chart")
            for (JsonProperty controllerProperty : getPropertyList(packageObject)) {
                String controllerName = controllerProperty.getName();
                JsonValue controllerValue = controllerProperty.getValue();

                if (!(controllerValue instanceof JsonObject controllerConfig)) {
                    continue;
                }

                // Check if controller is enabled
                JsonProperty enabledProperty = controllerConfig.findProperty("enabled");
                if (enabledProperty != null) {
                    JsonValue enabledValue = enabledProperty.getValue();
                    if (enabledValue != null && "false".equals(enabledValue.getText())) {
                        continue;
                    }
                }

                // Build the full controller name: "@symfony/ux-chartjs/chart"
                String fullControllerName = packageName + "/" + controllerName;
                // Normalize to Stimulus format: "symfony--ux-chartjs--chart"
                String normalizedName = normalizeControllerName(fullControllerName);

                // Store both normalized name (for HTML) and original name (for Twig)
                map.put(normalizedName, new StimulusController(normalizedName, fullControllerName));
            }
        }

        return map;
    }

    /**
     * Get all JsonProperty children from a JsonObject.
     * The PSI API doesn't have a getProperties() method, so we need to iterate through children.
     */
    @NotNull
    private List<JsonProperty> getPropertyList(@NotNull JsonObject jsonObject) {
        List<JsonProperty> properties = new ArrayList<>();
        for (PsiElement child : jsonObject.getChildren()) {
            if (child instanceof JsonProperty property) {
                properties.add(property);
            }
        }
        return properties;
    }

    /**
     * Get the top-level JSON object from a JSON file.
     */
    @Nullable
    private JsonObject getTopLevelObject(@NotNull JsonFile jsonFile) {
        PsiElement[] children = jsonFile.getChildren();
        for (PsiElement child : children) {
            if (child instanceof JsonObject jsonObject) {
                return jsonObject;
            }
        }
        return null;
    }

    /**
     * Normalize a controller name from "@symfony/ux-chartjs/chart" to "symfony--ux-chartjs--chart"
     * Removes leading @, converts / to --.
     */
    @NotNull
    private String normalizeControllerName(@NotNull String name) {
        return name
            .replace("@", "")
            .replace("/", "--");
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public @NotNull DataExternalizer<StimulusController> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 6;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return file -> {
            // Accept controllers.json files
            FileType fileType = file.getFileType();

            if (fileType == JsonFileType.INSTANCE && "controllers.json".equals(file.getName())) {
                return true;
            }

            // Only index JavaScript and TypeScript files
            if (fileType != JavaScriptFileType.INSTANCE && fileType != TypeScriptFileType.INSTANCE) {
                return false;
            }

            String name = file.getNameWithoutExtension();

            // Accept controller files (hello_controller.js, search-controller.ts)
            if (name.endsWith("_controller") || name.endsWith("-controller")) {
                return true;
            }

            // Accept common Stimulus bootstrap files that might contain app.register() calls
            return "bootstrap".equals(name) || "app".equals(name);
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
