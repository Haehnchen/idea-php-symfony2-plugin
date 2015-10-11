package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dic.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.*;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.DoctrineMetadataFileStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataUtil {

    private static DoctrineMappingDriverInterface[] MAPPING_DRIVERS = new DoctrineMappingDriverInterface[] {
        new DoctrineXmlMappingDriver(),
        new DoctrineYamlMappingDriver(),
        new DoctrinePhpMappingDriver(),
    };

    public static Collection<LookupElement> getObjectRepositoryLookupElements(@NotNull Project project) {

        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for(PhpClass phpClass: PhpIndex.getInstance(project).getAllSubclasses("\\Doctrine\\Common\\Persistence\\ObjectRepository")) {
            String presentableFQN = phpClass.getPresentableFQN();
            if(presentableFQN == null) {
                continue;
            }

            lookupElements.add(
                LookupElementBuilder.create(phpClass.getName()).withTypeText(phpClass.getPresentableFQN(), true).withIcon(phpClass.getIcon())
            );
        }

        return lookupElements;
    }

    /**
     * Try to find repository class on models scope on its metadata definition
     */
    @Nullable
    public static PhpClass getClassRepository(final @NotNull Project project, final @NotNull String className) {

        final PhpClass[] phpClass = {null};
        for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, className, GlobalSearchScope.allScope(project))) {

            FileBasedIndex.getInstance().processValues(DoctrineMetadataFileStubIndex.KEY, className, virtualFile, new FileBasedIndex.ValueProcessor<String>() {
                @Override
                public boolean process(VirtualFile virtualFile, String s) {
                    if(phpClass[0] != null) {
                        return true;
                    }

                    phpClass[0] = PhpElementsUtil.getClassInsideNamespaceScope(project, className, s);
                    return true;
                }
            }, GlobalSearchScope.allScope(project));

            if(phpClass[0] != null) {
                return phpClass[0];
            }
        }

        return phpClass[0];
    }

    @NotNull
    public static Collection<VirtualFile> findMetadataFiles(@NotNull Project project, @NotNull String className) {

        final Collection<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();

        FileBasedIndex.getInstance().getFilesWithKey(DoctrineMetadataFileStubIndex.KEY, new HashSet<String>(Collections.singletonList(className)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {
                virtualFiles.add(virtualFile);
                return true;
            }
        }, GlobalSearchScope.allScope(project));

        return virtualFiles;
    }

    @Nullable
    public static DoctrineMetadataModel getModelFields(@NotNull Project project, @NotNull String className) {

        Collection<DoctrineModelField> fields = new ArrayList<DoctrineModelField>();

        for (VirtualFile file : findMetadataFiles(project, className)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if(psiFile == null) {
                continue;
            }

            DoctrineMappingDriverArguments arguments = new DoctrineMappingDriverArguments(project, psiFile, className);
            for (DoctrineMappingDriverInterface mappingDriver : MAPPING_DRIVERS) {
                DoctrineMetadataModel metadata = mappingDriver.getMetadata(arguments);
                if(metadata != null) {
                    return metadata;
                }
            }
        }

        if(fields.size() == 0) {
            return null;
        }

        return new DoctrineMetadataModel(fields);
    }
}
