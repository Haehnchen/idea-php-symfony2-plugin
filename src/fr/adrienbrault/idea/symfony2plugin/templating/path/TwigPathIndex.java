package fr.adrienbrault.idea.symfony2plugin.templating.path;

import java.util.ArrayList;

public class TwigPathIndex {

    final public static String MAIN = "__main__";

    private ArrayList<TwigPath> twigPaths = new ArrayList<TwigPath>();

    public TwigPathIndex addPath(TwigPath twigPath) {
      this.twigPaths.add(twigPath);
      return this;
    }

    public ArrayList<TwigPath> getTwigPaths() {
        return twigPaths;
    }

    public ArrayList<TwigPath> getNamespacePaths(String namespace) {
        ArrayList<TwigPath> twigPaths = new ArrayList<TwigPath>();
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
