<?php

namespace Symfony\Component\Form
{
    interface FormBuilderInterface
    {
        public function add($child, $type = null, array $options = []);
        public function create($name, $type = null, array $options = []);
    }

    interface FormInterface
    {
        public function add($child, $type = null, array $options = []);
    }

    interface FormFactoryInterface
    {
        public function create($type = null, $data = null, array $options = []);
        public function createBuilder($type = null, $data = null, array $options = []);
        public function createNamedBuilder($name, $type = null, $data = null, array $options = []);
        public function createNamed($name, $type = null, $data = null, array $options = []);
    }
}

namespace Symfony\Component\Form\Attribute
{
    #[\Attribute(\Attribute::TARGET_CLASS)]
    class AsFormType {}
}

namespace Symfony\Bundle\FrameworkBundle\Controller
{
    class AbstractController
    {
        public function createForm($type, $data = null, array $options = []) {}
    }
}

namespace App\Form
{
    use Symfony\Component\Form\Attribute\AsFormType;

    #[AsFormType]
    abstract class AbstractAccountData {}

    #[AsFormType]
    class AccountData extends AbstractAccountData
    {
        public function getName() { return 'ignored_dto_alias'; }
        public function getParent() { return 'ignored_dto_parent'; }
    }

    class UnattributedData extends AccountData {}
}

namespace Symfony\Component\Form
{
    interface FormTypeInterface
    {
    }
}

namespace Symfony\Component\Form\Extension\Core\Type
{
    use Symfony\Component\Form\FormTypeInterface;

    class HiddenType implements FormTypeInterface
    {
    }
}


