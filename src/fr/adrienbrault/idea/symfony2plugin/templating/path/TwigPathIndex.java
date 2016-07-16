package fr.adrienbrault.idea.symfony2plugin.templating.path;

import java.util.ArrayList;
import java.util.List;

public class TwigPathIndex {

    final public static String MAIN = "__main__";

    private List<TwigPath> twigPaths = new ArrayList<>();

    public TwigPathIndex addPath(TwigPath twigPath) {
      this.twigPaths.add(twigPath);
      return this;
    }

    synchronized public List<TwigPath> getTwigPaths() {
        return twigPaths;
    }

    public List<TwigPath> getNamespacePaths(String namespace) {
        ArrayList<TwigPath> twigPaths = new ArrayList<>();
        for(TwigPath twigPath: this.getTwigPaths()) {
            if(twigPath.getNamespace().equals(namespace)) {
                twigPaths.add(twigPath);
            }
        }
        return twigPaths;
    }

    public enum NamespaceType {
        BUNDLE, ADD_PATH
    }

}
