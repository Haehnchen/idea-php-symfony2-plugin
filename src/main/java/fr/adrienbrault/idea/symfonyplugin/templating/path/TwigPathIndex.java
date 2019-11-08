package fr.adrienbrault.idea.symfonyplugin.templating.path;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathIndex {
    @NotNull
    private List<TwigPath> twigPaths = new ArrayList<>();

    void addPath(@NotNull TwigPath twigPath) {
      twigPaths.add(twigPath);
    }

    @NotNull
    List<TwigPath> getTwigPaths() {
        return twigPaths;
    }

    public List<TwigPath> getNamespacePaths(String namespace) {
        List<TwigPath> twigPaths = new ArrayList<>();

        for(TwigPath twigPath: this.getTwigPaths()) {
            if(twigPath.getNamespace().equals(namespace)) {
                twigPaths.add(twigPath);
            }
        }

        return twigPaths;
    }
}
