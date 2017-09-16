package fr.adrienbrault.idea.symfony2plugin.stubs.util;

import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.ID;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IndexUtil {

    public static void forceReindex() {

        ID<?,?>[] indexIds = new ID<?,?>[] {
            ContainerBuilderStubIndex.KEY,
            ContainerParameterStubIndex.KEY,
            DoctrineMetadataFileStubIndex.KEY,
            EventAnnotationStubIndex.KEY,
            FileResourcesIndex.KEY,
            PhpTwigTemplateUsageStubIndex.KEY,
            RoutesStubIndex.KEY,
            ServicesDefinitionStubIndex.KEY,
            ServicesTagStubIndex.KEY,
            TwigExtendsStubIndex.KEY,
            TwigIncludeStubIndex.KEY,
            TwigMacroFunctionStubIndex.KEY,
            TranslationStubIndex.KEY,
        };

        for(ID<?,?> id: indexIds) {
            FileBasedIndexImpl.getInstance().requestRebuild(id);
            FileBasedIndexImpl.getInstance().scheduleRebuild(id, new Throwable());
        }

    }

}
