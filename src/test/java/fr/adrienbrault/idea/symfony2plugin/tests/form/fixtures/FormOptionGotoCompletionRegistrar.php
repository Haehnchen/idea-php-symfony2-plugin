<?php
const GLOBAL_CONST_OPTION = 'global_const_option';
define('GLOBAL_CONST_DEFINE', 'global_const_define');
namespace Foo\Form {

    class Bar {
        const CLASS_CONST_OPTION = 'class_const_option';
        public function configureOptions($resolver)
        {
            $resolver->setDefaults([
                'configure_options' => null,
                self::CLASS_CONST_OPTION => null,
                GLOBAL_CONST_OPTION => null,
                GLOBAL_CONST_DEFINE => null,
            ]);
        }

        public function getName() {
            return 'foo';
        }
    }
}