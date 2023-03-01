package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigCreateContainer {
    @NotNull
    final private Map<String, Integer> extendHap = new HashMap<>();

    @NotNull
    final private Map<String, Integer> blockHap = new HashMap<>();

    public void addExtend(@NotNull String extend) {
        if(extendHap.containsKey(extend)) {
            extendHap.put(extend, extendHap.get(extend) + 1);
        } else {
            extendHap.put(extend, 1);
        }
    }

    public void addBlock(@NotNull String block) {
        if(blockHap.containsKey(block)) {
            blockHap.put(block, blockHap.get(block) + 1);
        } else {
            blockHap.put(block, 1);
        }
    }

    @Nullable
    public String getExtend() {
        Map<String, Integer> extendsMap = getExtends();
        return extendsMap.size() > 0 ? extendsMap.keySet().iterator().next() : null;
    }

    @NotNull
    private Map<String, Integer> getExtends() {
        return sortByValue(extendHap);
    }

    @NotNull
    private Map<String, Integer> getBlocks() {
        return sortByValue(blockHap);
    }

    @NotNull
    public Collection<String> getBlockNames(int limit) {
        List<String> strings = new ArrayList<>(getBlocks().keySet());

        if(strings.size() > limit) {
            strings = strings.subList(0, limit);
        }

        return strings;
    }

    @NotNull
    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map )
    {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Collections.reverse(list);

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put( entry.getKey(), entry.getValue() );
        }

        return result;
    }
}
