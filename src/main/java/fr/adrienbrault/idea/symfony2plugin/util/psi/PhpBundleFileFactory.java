package fr.adrienbrault.idea.symfony2plugin.util.psi;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpCodeEditUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.refactoring.PhpAliasImporter;
import com.jetbrains.php.refactoring.PhpNameUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpBundleFileFactory {

    @Nullable
    public static PsiElement invokeCreateCompilerPass(@NotNull PhpClass bundleClass, @Nullable Editor editor) {
        String className = Messages.showInputDialog("Class name for CompilerPass (no namespace needed): ", "New File", Symfony2Icons.SYMFONY);
        if(StringUtils.isBlank(className)) {
            return null;
        }

        if(!PhpNameUtil.isValidClassName(className)) {
            Messages.showMessageDialog(bundleClass.getProject(), "Invalid class name", "Error", Symfony2Icons.SYMFONY);
        }

        try {
            return PhpBundleFileFactory.createCompilerPass(bundleClass, className);
        } catch (Exception e) {
            if(editor != null) {
                IdeHelper.showErrorHintIfAvailable(editor, "Error:" + e.getMessage());
            } else {
                JOptionPane.showMessageDialog(null, "Error:" + e.getMessage());
            }
        }

        return null;
    }

    @NotNull
    public static PsiElement createBundleFile(@NotNull PhpClass bundleClass, @NotNull String template, @NotNull String className, Map<String, String> vars) throws Exception {

        VirtualFile directory = bundleClass.getContainingFile().getContainingDirectory().getVirtualFile();
        if(fileExists(directory, new String[] {className})) {
            throw new Exception("File already exists");
        }

        String COMPILER_TEMPLATE = "/fileTemplates/" + template + ".php";
        String fileTemplateContent = getFileTemplateContent(COMPILER_TEMPLATE);
        if(fileTemplateContent == null) {
            throw new Exception("Template content error");
        }

        String[] split = className.split("\\\\");

        String ns = bundleClass.getNamespaceName();
        String join = StringUtils.join(Arrays.copyOf(split, split.length - 1), "/");

        vars.put("ns", (ns.startsWith("\\") ? ns.substring(1) : ns) + join.replace("/", "\\"));
        vars.put("class", split[split.length - 1]);
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            fileTemplateContent = fileTemplateContent.replace("{{ " + entry.getKey() + " }}", entry.getValue());
        }

        VirtualFile compilerDirectory = getAndCreateDirectory(directory, join);
        if(compilerDirectory == null) {
            throw new Exception("Directory creation failed");
        }

        Project project = bundleClass.getProject();
        PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(split[split.length - 1] + ".php", PhpFileType.INSTANCE, fileTemplateContent);
        CodeStyleManager.getInstance(project).reformat(fileFromText);
        return PsiDirectoryFactory.getInstance(project).createDirectory(compilerDirectory).add(fileFromText);
    }

    @NotNull
    public static PsiElement createCompilerPass(@NotNull PhpClass bundleClass, @NotNull String className) throws Exception {

        VirtualFile directory = bundleClass.getContainingFile().getContainingDirectory().getVirtualFile();
        if(fileExists(directory, className)) {
            throw new Exception("File already exists");
        }

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(bundleClass);
        if(scopeForUseOperator == null) {
            throw new Exception("No 'use' scope found");
        }

        VirtualFile compilerDirectory = getAndCreateCompilerDirectory(directory);
        if(compilerDirectory == null) {
            throw new Exception("Directory creation failed");
        }

        Project project = bundleClass.getProject();

        if(bundleClass.findOwnMethodByName("build") == null) {

            insertUseIfNecessary(scopeForUseOperator, "\\Symfony\\Component\\DependencyInjection\\ContainerBuilder");

            Method method = PhpPsiElementFactory.createMethod(project, "" +
                    "public function build(ContainerBuilder $container)\n" +
                    "    {\n" +
                    "        parent::build($container);\n" +
                    "    }"
            );

            PhpCodeEditUtil.insertClassMember(bundleClass, method);
        }

        Method buildMethod = bundleClass.findOwnMethodByName("build");
        if(buildMethod == null) {
            throw new Exception("No 'build' method found");
        }

        String relativePath = VfsUtil.getRelativePath(compilerDirectory, directory);
        if(relativePath == null) {
            throw new Exception("path error");
        }

        MethodReference methodReference = PhpPsiElementFactory.createMethodReference(project, "$container->addCompilerPass(new " + className + "());");

        String ns = bundleClass.getNamespaceName() + relativePath.replace("/", "\\");
        String nsClass = ns + "\\" + className;

        insertUseIfNecessary(scopeForUseOperator, nsClass);

        GroupStatement groupStatement = PhpPsiUtil.getChildByCondition(buildMethod, GroupStatement.INSTANCEOF);
        if(groupStatement != null) {
            PsiElement semicolon = methodReference.getNextSibling();
            groupStatement.addRangeBefore(methodReference, semicolon, groupStatement.getLastChild());
        }

        String COMPILER_TEMPLATE = "/fileTemplates/compiler_pass.php";
        String fileTemplateContent = getFileTemplateContent(COMPILER_TEMPLATE);
        if(fileTemplateContent == null) {
            throw new Exception("Template content error");
        }

        String replace = fileTemplateContent.replace("{{ ns }}", ns.startsWith("\\") ? ns.substring(1) : ns).replace("{{ class }}", className);
        PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(className + ".php", PhpFileType.INSTANCE, replace);
        CodeStyleManager.getInstance(project).reformat(fileFromText);

        return PsiDirectoryFactory.getInstance(project).createDirectory(compilerDirectory).add(fileFromText);
    }

    @Nullable
    public static PhpClass getPhpClassForCreateCompilerScope(@Nullable PhpClass phpClass) {

        if(phpClass == null) {
            return null;
        }

        if(!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\HttpKernel\\Bundle\\BundleInterface")) {
            return null;
        }

        return phpClass;
    }

    @Nullable
    public static PhpClass getPhpClassForCreateCompilerScope(@NotNull Editor editor, @Nullable PsiFile file) {

        if(file == null || !(file instanceof PhpFile)) {
            return null;
        }

        return getPhpClassForCreateCompilerScope(PhpCodeEditUtil.findClassAtCaret(editor, file));
    }

    private static void insertUseIfNecessary(PhpPsiElement scopeForUseOperator, String nsClass) {
        if(!PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).values().contains(nsClass)) {
            PhpAliasImporter.insertUseStatement(nsClass, scopeForUseOperator);
        }
    }

    private static boolean fileExists(@NotNull VirtualFile bundleDir, @NotNull String className) {
        return
            VfsUtil.findRelativeFile(bundleDir, "DependencyInjection", "Compiler", className + ".php") != null ||
            VfsUtil.findRelativeFile(bundleDir, "DependencyInjection", "CompilerPass", className + ".php") != null;
    }

    private static boolean fileExists(@NotNull VirtualFile bundleDir, @NotNull String... fqnClassName) {

        for (String s : fqnClassName) {
            String[] split = s.split("/");
            split[split.length - 1] += ".php";

            if(VfsUtil.findRelativeFile(bundleDir, split) != null) {
                return true;
            }
        }

        return false;
    }
    @Nullable
    private static VirtualFile getAndCreateDirectory(@NotNull VirtualFile directory, @NotNull String relativePath) {

        try {
            return VfsUtil.createDirectoryIfMissing(directory, relativePath);
        } catch (IOException ignored) {
        }

        return null;
    }

    @Nullable
    private static VirtualFile getAndCreateCompilerDirectory(@NotNull VirtualFile directory) {

        VirtualFile relativeFile = VfsUtil.findRelativeFile(directory, "DependencyInjection", "Compiler");
        if(relativeFile != null) {
            return relativeFile;
        }

        relativeFile = VfsUtil.findRelativeFile(directory, "DependencyInjection", "CompilerPass");
        if(relativeFile != null) {
            return relativeFile;
        }

        try {
            return VfsUtil.createDirectoryIfMissing(directory, "DependencyInjection/Compiler");
        } catch (IOException ignored) {
        }

        return null;
    }

    @Nullable
    private static String getFileTemplateContent(@NotNull String filename) {
        try {
            // replace on windows, just for secure reasons
            return StreamUtil.readText(PhpBundleFileFactory.class.getResourceAsStream(filename), "UTF-8").replace("\r\n", "\n");
        } catch (IOException e) {
            return null;
        }
    }

}
