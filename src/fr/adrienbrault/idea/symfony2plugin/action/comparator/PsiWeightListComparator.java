package fr.adrienbrault.idea.symfony2plugin.action.comparator;

import fr.adrienbrault.idea.symfony2plugin.action.dict.TranslationFileModel;

import java.util.Comparator;

/**
* Created by daniel on 17.05.14.
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
