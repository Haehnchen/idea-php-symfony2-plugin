<?php

namespace Symfony\Component\Form {
    interface FormTypeInterface {}
    interface FormBuilderInterface { public function add(); }
    class FormView {}
    interface FormInterface { public function createView(): FormView; }
}

namespace Symfony\Component\Form\Extension\Core\Type {
    use Symfony\Component\Form\FormTypeInterface;

    class TextType implements FormTypeInterface {}
}

namespace Symfony\Bundle\FrameworkBundle\Controller {
    use Symfony\Component\Form\FormInterface;

    abstract class AbstractController {
        public function createForm($type): FormInterface {}
        public function render($template, array $parameters = []) {}
    }
}

namespace App\Form {
    use Symfony\Component\Form\Extension\Core\Type\TextType;
    use Symfony\Component\Form\FormBuilderInterface;
    use Symfony\Component\Form\FormTypeInterface;

    class ProductType implements FormTypeInterface {
        public function buildForm(FormBuilderInterface $builder, array $options) {
            $builder->add('title', TextType::class);
        }
    }
}

namespace App\Controller {
    use App\Form\ProductType;
    use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;

    class FormCompletionController extends AbstractController {
        public function completion() {
            $form = $this->createForm(ProductType::class);

            return $this->render('form/completion.html.twig', ['form' => $form->createView()]);
        }

        public function generator() {
            $form = $this->createForm(ProductType::class);

            return $this->render('form/generator.html.twig', ['form' => $form->createView()]);
        }
    }
}
