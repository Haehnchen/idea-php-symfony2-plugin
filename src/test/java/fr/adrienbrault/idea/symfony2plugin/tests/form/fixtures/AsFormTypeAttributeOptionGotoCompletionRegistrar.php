<?php

namespace Symfony\Component\Form {
    interface FormTypeInterface {}
    interface FormTypeExtensionInterface {}
}

namespace Symfony\Component\Form\Attribute {
    #[\Attribute(\Attribute::TARGET_CLASS)]
    class AsFormType {
        public function __construct(public array $options = []) {}
    }
}

namespace Symfony\Component\Form\Extension\Core\Type {
    class FormType implements \Symfony\Component\Form\FormTypeInterface {
        public function configureOptions($resolver) {
            $resolver->setDefaults([
                'label' => null,
                'required' => true,
                'data_class' => null,
            ]);
        }
    }
}

namespace App {
    use Symfony\Component\Form\Attribute\AsFormType;
    use Symfony\Component\Form\FormTypeExtensionInterface;
    use Symfony\Component\Form\Extension\Core\Type\FormType;

    #[AsFormType(options: ['ancestor_option' => null])]
    abstract class ParentAccountData {}

    class IntermediateAccountData extends ParentAccountData {
        public function configureOptions($resolver) {
            $resolver->setDefaults(['unused_method_option' => null]);
        }
    }

    class CoreExtension implements FormTypeExtensionInterface {
        public static function getExtendedTypes() {
            return [FormType::class];
        }

        public function configureOptions($resolver) {
            $resolver->setDefaults(['core_extension' => null]);
        }
    }

    class AccountExtension implements FormTypeExtensionInterface {
        public static function getExtendedTypes() {
            return [AccountData::class];
        }

        public function configureOptions($resolver) {
            $resolver->setDefaults(['account_extension' => null]);
        }
    }

    class ParentExtension implements FormTypeExtensionInterface {
        public static function getExtendedTypes() {
            return [ParentAccountData::class];
        }

        public function configureOptions($resolver) {
            $resolver->setDefaults(['parent_extension' => null]);
        }
    }

    class UnattributedExtension implements FormTypeExtensionInterface {
        public static function getExtendedTypes() {
            return [IntermediateAccountData::class];
        }

        public function configureOptions($resolver) {
            $resolver->setDefaults(['unattributed_extension' => null]);
        }
    }
}
