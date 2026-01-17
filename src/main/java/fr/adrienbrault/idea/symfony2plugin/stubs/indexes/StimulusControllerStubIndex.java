package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.lang.ecmascript6.psi.ES6FromClause;
import com.intellij.lang.ecmascript6.psi.ES6ImportDeclaration;
import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.TypeScriptFileType;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.ecmal4.JSReferenceListMember;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiReference;
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

            if (!(inputData.getPsiFile() instanceof JSFile jsFile)) {
                return map;
            }

            String controllerName = extractControllerName(inputData.getFile());
            if (controllerName == null) {
                return map;
            }

            // Check if the file imports Controller from @hotwired/stimulus using AST
            if (!hasControllerImportFromStimulus(jsFile)) {
                return map;
            }

            // Check if the file has a class extending Controller using AST
            if (!hasClassExtendingController(jsFile)) {
                return map;
            }

            // Build namespace from parent folders AFTER validation
            String finalControllerName = buildNamespaceFromPath(inputData.getFile(), controllerName);

            map.put(finalControllerName, new StimulusController(finalControllerName));
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
                        @Nullable PsiReference reference = fromClause.getReference();
                        if (reference != null) {
                            String referenceText = reference.getCanonicalText();
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
        return 1;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return file -> {
            // Only index JavaScript and TypeScript files
            if (file.getFileType() != JavaScriptFileType.INSTANCE && file.getFileType() != TypeScriptFileType.INSTANCE) {
                return false;
            }

            // Only index files ending with _controller or -controller
            String name = file.getNameWithoutExtension();
            return name.endsWith("_controller") || name.endsWith("-controller");
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
