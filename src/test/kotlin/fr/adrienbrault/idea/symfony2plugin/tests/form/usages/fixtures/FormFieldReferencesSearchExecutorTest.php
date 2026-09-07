<?php
namespace Symfony\Component\Form {
    interface FormBuilderInterface {
        /** @return FormBuilderInterface */
        public function add($name, $type = null, array $options = []);
        /** @return FormBuilderInterface */
        public function create($name, $type = null, array $options = []);
        /** @return FormBuilderInterface */
        public function get($name);
    }
}
namespace Symfony\Component\OptionsResolver {
    class OptionsResolver {
        public function setDefaults(array $options) {}
        public function setDefault($name, $value) {}
    }
    interface OptionsResolverInterface {
        public function setDefaults(array $options);
    }
}
