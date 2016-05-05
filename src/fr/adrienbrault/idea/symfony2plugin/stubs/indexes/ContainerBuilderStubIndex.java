package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.HashSet;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ContainerBuilderCall;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;


public class ContainerBuilderStubIndex extends FileBasedIndexExtension<String, ContainerBuilderCall> {

    public static final ID<String, ContainerBuilderCall> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.container_builder");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private final static ObjectStreamDataExternalizer<ContainerBuilderCall> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    private static final Set<String> SET = new HashSet<String>() {{
        add("Symfony\\Component\\DependencyInjection\\Container");
        add("Symfony\\Component\\DependencyInjection\\ContainerBuilder");
        add("Symfony\\Component\\DependencyInjection\\TaggedContainerInterface");
    }};

    private static final Set<String> METHODS = new HashSet<String>() {{
        add("findTaggedServiceIds");
        add("setDefinition");
        add("setParameter");
        add("setAlias");
        add("register");
    }};

    @NotNull
    @Override
    public DataIndexer<String, ContainerBuilderCall, FileContent> getIndexer() {

        return inputData -> {

            Map<String, ContainerBuilderCall> map = new THashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!(psiFile instanceof PhpFile)) {
                return map;
            }

            psiFile.accept(new MyPsiRecursiveElementWalkingVisitor(map));

            return map;
        };
    }

    @NotNull
    @Override
    public ID<String, ContainerBuilderCall> getName() {
        return KEY;
    }


    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    public DataExternalizer<ContainerBuilderCall> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return virtualFile -> virtualFile.getFileType() == PhpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    private class MyPsiRecursiveElementWalkingVisitor extends PsiRecursiveElementVisitor {

        private final Map<String, ContainerBuilderCall> map;

        MyPsiRecursiveElementWalkingVisitor(@NotNull Map<String, ContainerBuilderCall> map) {
            this.map = map;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(!(element instanceof Parameter)) {
                super.visitElement(element);
                return;
            }

            ClassReference classReference = ObjectUtils.tryCast(element.getFirstChild(), ClassReference.class);
            if(classReference == null) {
                return;
            }

            String fqn = StringUtils.stripStart(classReference.getFQN(), "\\");
            if(!SET.contains(fqn)) {
                return;
            }

            Parameter parentOfType = PsiTreeUtil.getParentOfType(classReference, Parameter.class);
            if(parentOfType == null) {
                return;
            }

            final String name = parentOfType.getName();

            Method method = PsiTreeUtil.getParentOfType(classReference, Method.class);
            if(method == null) {
                return;
            }

            method.accept(new MyMethodVariableVisitor(name, map));

            super.visitElement(element);
        }
    }

    private class MyMethodVariableVisitor extends PsiRecursiveElementVisitor {

        @NotNull
        private final String name;

        @NotNull
        private final Map<String, ContainerBuilderCall> result;

        MyMethodVariableVisitor(@NotNull String name, @NotNull Map<String, ContainerBuilderCall> result) {
            this.name = name;
            this.result = result;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(!(element instanceof Variable) || !name.equals(((Variable) element).getName())) {
                super.visitElement(element);
                return;
            }

            MethodReference methodReference = ObjectUtils.tryCast(element.getParent(), MethodReference.class);
            if(methodReference == null || !METHODS.contains(methodReference.getName())) {
                super.visitElement(element);
                return;
            }

            String value = Symfony2InterfacesUtil.getFirstArgumentStringValue(methodReference);
            if(value == null) {
                super.visitElement(element);
                return;
            }

            String methodName = methodReference.getName();
            if(!result.containsKey(methodName)) {
                result.put(methodName, new ContainerBuilderCall());
            }

            result.get(methodName).addParameter(value);

            super.visitElement(element);
        }
    }
}
