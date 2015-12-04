<?php

namespace Symfony\Component\Form {
    interface FormBuilderInterface {
        public function add();
    }
}

namespace Foo\Form {
    class Bar {
        public function configureOptions($resolver)
        {
            $resolver->setDefaults([
                'configure_options' => null,
            ]);
        }

        public function getName() {
            return 'foo';
        }
    }
}