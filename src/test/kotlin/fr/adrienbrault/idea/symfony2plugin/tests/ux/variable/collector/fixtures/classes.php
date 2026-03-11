<?php

namespace Symfony\UX\TwigComponent\Attribute
{
    #[\Attribute(\Attribute::TARGET_CLASS)]
    class AsTwigComponent
    {
        public function __construct(
            public ?string $name = null,
            public ?string $template = null,
        ) {}
    }
}

namespace Symfony\UX\TwigComponent
{
    class ComponentAttributes
    {
    }
}

namespace App\Twig\Components
{
    use Symfony\UX\TwigComponent\Attribute\AsTwigComponent;

    #[AsTwigComponent(template: 'components/Alert.html.twig')]
    class Alert
    {
        public string $message = '';
        public int $count = 0;
        private string $secret = 'hidden';
    }
}
