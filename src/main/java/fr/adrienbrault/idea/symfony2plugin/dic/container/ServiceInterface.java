package fr.adrienbrault.idea.symfony2plugin.dic.container;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceInterface {

    @NotNull
    String getId();

    @Nullable
    String getClassName();

    boolean isLazy();

    boolean isAbstract();

    boolean isAutowire();

    boolean isDeprecated();

    boolean isPublic();

    @Nullable
    String getAlias();

    @Nullable
    String getParent();

    @Nullable
    String getDecorates();

    @Nullable
    String getDecorationInnerName();

    @Nullable
    String getResource();

    @Nullable
    String getExclude();

    @NotNull
    Collection<String> getTags();
}
