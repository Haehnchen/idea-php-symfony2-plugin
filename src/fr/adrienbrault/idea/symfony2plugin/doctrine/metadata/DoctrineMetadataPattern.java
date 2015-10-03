package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.patterns.XmlPatterns;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataPattern {

    /**
     * <doctrine-mapping|doctrine-*-mapping>
     *   <entity name="Class\Name"/>
     * </doctrine-mapping>
     *
     * <doctrine-mapping|doctrine-*-mapping>
     *   <document name="Class\Name"/>
     * </doctrine-mapping>
     */
    public static XmlAttributeValuePattern getXmlModelClass() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("name")
                .withParent(XmlPatterns
                    .xmlTag().withName(PlatformPatterns.string().oneOf("document", "entity"))
                    .withParent(XmlPatterns
                        .xmlTag().withName(PlatformPatterns.string().matches("doctrine-[\\w+-]*-*mapping"))
                    )
                )
            );
    }

    /**
     * <doctrine-mapping|doctrine-*-mapping>
     *   <document repository-class="Class\Name"/>
     *   <entity repository-class="Class\Name"/>
     * </doctrine-mapping>
     */
    public static XmlAttributeValuePattern getXmlRepositoryClass() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("repository-class")
                .withParent(XmlPatterns
                    .xmlTag().withName(PlatformPatterns.string().oneOf("document", "entity"))
                    .withParent(XmlPatterns
                        .xmlTag().withName(PlatformPatterns.string().matches("doctrine-[\\w+-]*-*mapping"))
                    )
                )
            );
    }
}
