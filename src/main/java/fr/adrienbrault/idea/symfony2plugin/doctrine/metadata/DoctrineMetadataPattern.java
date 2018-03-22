package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataPattern {

    @RegExp
    public static final String DOCTRINE_MAPPING = "doctrine-[\\w+-]*-*mapping";

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
                    .xmlTag().withName(PlatformPatterns.string().oneOf("document", "entity", "embedded-document", "embedded"))
                    .withParent(XmlPatterns
                        .xmlTag().withName(PlatformPatterns.string().matches(DOCTRINE_MAPPING))
                    )
                )
            );
    }

    public static XmlAttributeValuePattern getEmbeddableNameClassPattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("name")
                .withParent(XmlPatterns
                    .xmlTag().withName("embeddable")
                    .withParent(XmlPatterns
                        .xmlTag().withName(PlatformPatterns.string().matches(DOCTRINE_MAPPING))
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
                        .xmlTag().withName(PlatformPatterns.string().matches(DOCTRINE_MAPPING))
                    )
                )
            );
    }

    /**
     * <reference-one target-document="Foo"/>
     * <reference-many target-document="Foo"/>
     * <embed-many target-document="Foo"/>
     * <embed-one target-document="Foo"/>
     */
    public static XmlAttributeValuePattern getXmlTargetDocumentClass() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("target-document")
                .withParent(XmlPatterns
                    .xmlTag().withName(PlatformPatterns.string().oneOf("reference-one", "reference-many", "embed-many", "embed-one"))
                    .withParent(
                        XmlPatterns.xmlTag().withName(XmlPatterns.string().oneOf("embedded-document", "embedded", "document")).withParent(
                            XmlPatterns.xmlTag().withName(PlatformPatterns.string().matches(DOCTRINE_MAPPING))
                        )
                    )
                )
            );
    }

    /**
     * <one-to-one target-entity="Foo">
     * <one-to-many target-entity="Foo">
     * <many-to-one target-entity="Foo">
     * <many-to-many target-entity="Foo">
     */
    public static XmlAttributeValuePattern getXmlTargetEntityClass() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("target-entity")
                .withParent(XmlPatterns
                    .xmlTag().withName(PlatformPatterns.string().oneOf("one-to-one", "one-to-many", "many-to-one", "many-to-many"))
                    .withParent(
                        XmlPatterns.xmlTag().withName("entity").withParent(
                            XmlPatterns.xmlTag().withName(PlatformPatterns.string().matches(DOCTRINE_MAPPING))
                        )
                    )
                )
            );
    }

    /**
     * <doctrine-mapping|doctrine-*-mapping>
     *   <field type="Class\Name"/>
     * </doctrine-mapping>
     */
    public static XmlAttributeValuePattern getFieldType() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("type")
                .withParent(XmlPatterns
                    .xmlTag().withName("field")
                    .withParent(
                        XmlPatterns.xmlTag().withName(XmlPatterns.string().oneOf("entity", "document", "embedded-document", "embedded")).withParent(
                            XmlPatterns.xmlTag().withName(PlatformPatterns.string().matches(DOCTRINE_MAPPING))
                        )
                    )
                )
            );
    }

    /**
     * <doctrine-mapping|doctrine-*-mapping>
     *   <document><field name="Foo"/></document>
     *   <document><id name="Foo"/></document>
     * </doctrine-mapping>
     */
    public static XmlAttributeValuePattern getFieldName() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("name")
                .withParent(XmlPatterns
                    .xmlTag().withName(XmlPatterns.string().oneOf("field", "id"))
                    .withParent(
                        XmlPatterns.xmlTag().withName(XmlPatterns.string().oneOf("entity", "document", "embedded-document", "embedded")).withParent(
                            XmlPatterns.xmlTag().withName(PlatformPatterns.string().matches(DOCTRINE_MAPPING))
                        )
                    )
                )
            );
    }

    /**
     * <doctrine-mapping|doctrine-*-mapping>
     *   <document><embed-one field="Foo"/></document>
     * </doctrine-mapping>
     */
    public static XmlAttributeValuePattern getFieldNameRelation() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("field")
                .withParent(XmlPatterns
                    .xmlTag().withName(XmlPatterns.string().oneOf("embed-one", "embed-many", "reference-one", "reference-many", "one-to-one", "one-to-many", "many-to-one", "many-to-many"))
                    .withParent(
                        XmlPatterns.xmlTag().withName(XmlPatterns.string().oneOf("entity", "document", "embedded-document", "embedded")).withParent(
                            XmlPatterns.xmlTag().withName(PlatformPatterns.string().matches(DOCTRINE_MAPPING))
                        )
                    )
                )
            );
    }

    /**
     * fields:
     *  i<caret>d: []
     *
     * embedOne:
     *  add<caret>ress: []
     */
    public static PsiElementPattern.Capture<PsiElement> getYamlFieldName() {
        return PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
            .withParent(
                PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                    PlatformPatterns.psiElement(YAMLCompoundValue.class).withParent(
                        PlatformPatterns.psiElement(YAMLKeyValue.class).withName(PlatformPatterns.string().oneOf(
                            "id", "fields", "embedOne", "embedMany", "referenceOne", "referenceMany", "oneToOne", "oneToMany", "manyToOne", "manyToMany"
                        ))
                    )
                )
            );
    }
}
