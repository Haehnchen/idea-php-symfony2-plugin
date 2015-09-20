package fr.adrienbrault.idea.symfony2plugin.stubs.util;

import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.ID;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.*;

public class IndexUtil {

    public static void forceReindex() {

        ID<?,?>[] indexIds = new ID<?,?>[] {
            ServicesDefinitionStubIndex.KEY,
            AnnotationRoutesStubIndex.KEY,
            ContainerParameterStubIndex.KEY,
            ServicesTagStubIndex.KEY,
            TwigExtendsStubIndex.KEY,
            TwigIncludeStubIndex.KEY,
            TwigMacroFromStubIndex.KEY,
            TwigMacroFunctionStubIndex.KEY,
            RoutesStubIndex.KEY,
            YamlTranslationStubIndex.KEY,
            DoctrineMetadataFileStubIndex.KEY
        };

        for(ID<?,?> id: indexIds) {
            FileBasedIndexImpl.getInstance().requestRebuild(id);
            FileBasedIndexImpl.getInstance().scheduleRebuild(id, new Throwable());
        }

    }

}
