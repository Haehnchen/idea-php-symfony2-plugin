package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpAccessVariableInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpInstruction;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.Parameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ContainerBuilderCall;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import one.util.streamex.StreamEx;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerBuilderStubIndex
 */
public class ContainerBuilderStubIndex extends FileBasedIndexExtension<String, ContainerBuilderCall> {

    public static final ID<String, ContainerBuilderCall> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.container_builder");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private final static ObjectStreamDataExternalizer<ContainerBuilderCall> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    private static int MAX_FILE_BYTE_SIZE = 2621440;

    private static final Set<String> SET = new HashSet<>() {{
        add("Symfony\\Component\\DependencyInjection\\Container");
        add("Symfony\\Component\\DependencyInjection\\ContainerBuilder");
        add("Symfony\\Component\\DependencyInjection\\TaggedContainerInterface");
    }};

    private static final Set<String> METHODS = new HashSet<>() {{
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

            Map<String, ContainerBuilderCall> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!(psiFile instanceof PhpFile) ||
                !Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) ||
                !isValidForIndex(inputData, psiFile)
                ){

                return map;
            }

            StreamEx.of(PhpPsiUtil.findAllClasses((PhpFile) psiFile))
                .flatMap(clazz -> StreamEx.of(clazz.getOwnMethods()))
                .forEach(method -> processMethod(method, map));
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
        return 2;
    }

    private static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        // is Test file in path name
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), ProjectUtil.getProjectDir(inputData.getProject()), '/');
        if(relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Tests/") || relativePath.contains("/Fixture/") || relativePath.contains("/Fixtures/"))) {
            return false;
        }

        // dont index files larger then files; use 5 MB here
        if(inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
            return false;
        }

        return true;
    }

    private void processMethod(@NotNull Method method, @NotNull Map<String, ContainerBuilderCall> map) {
        Set<CharSequence> containerParameters = StreamEx.of(method.getParameters())
            .filter(ContainerBuilderStubIndex::isContainerParam)
            .map(Parameter::getNameCS)
            .toSet();
        if (containerParameters.isEmpty()) return;
        MyInstructionProcessor processor = new MyInstructionProcessor(map, containerParameters);
        for (PhpInstruction instruction : method.getControlFlow().getInstructions()) {
            instruction.process(processor);
        }
    }

    private static boolean isContainerParam(@NotNull Parameter parameter) {
        String parameterType = parameter.getDeclaredType().toString();
        return SET.contains(StringUtils.stripStart(parameterType, "\\"));
    }

    private static class MyInstructionProcessor extends PhpInstructionProcessor {
        @NotNull
        private final Map<String, ContainerBuilderCall> map;
        @NotNull
        private final Set<CharSequence> containerParameters;

        MyInstructionProcessor(@NotNull Map<String, ContainerBuilderCall> map,
                               @NotNull Set<CharSequence> containerParameters) {
            this.map = map;
            this.containerParameters = containerParameters;
        }
        
        @Override
        public boolean processAccessVariableInstruction(PhpAccessVariableInstruction instruction) {
            if (instruction.getAccess().isWrite() || instruction.getAccess().isWriteRef() ||
                    !containerParameters.contains(instruction.getVariableName())) return true;
            
            MethodReference methodReference = 
                    ObjectUtils.tryCast(instruction.getAnchor().getParent(), MethodReference.class);
            if (methodReference == null || !METHODS.contains(methodReference.getName())) return true;
            
            String value = PhpElementsUtil.getFirstArgumentStringValue(methodReference);
            if (value == null) return true;
            
            String methodName = methodReference.getName();
            map.computeIfAbsent(methodName, name -> new ContainerBuilderCall());
            map.get(methodName).addParameter(value);
            
            return true;
        }
    }
}
