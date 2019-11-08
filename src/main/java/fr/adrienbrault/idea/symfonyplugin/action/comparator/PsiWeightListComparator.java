package fr.adrienbrault.idea.symfonyplugin.action.comparator;

import fr.adrienbrault.idea.symfonyplugin.action.dict.TranslationFileModel;

import java.util.Comparator;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiWeightListComparator implements Comparator<TranslationFileModel> {
    @Override
    public int compare(TranslationFileModel o1, TranslationFileModel o2) {
        if(o1.getWeight() > o2.getWeight()) {
            return -1;
        }

        if(o1.getWeight() < o2.getWeight()) {
            return 1;
        }

        String relativePathO1 = o1.getRelativePath();
        String relativePathO2 = o2.getRelativePath();
        if(relativePathO1 == null || relativePathO2 == null) {
            return 0;
        }

        return relativePathO1.compareTo(relativePathO2);
    }
}
