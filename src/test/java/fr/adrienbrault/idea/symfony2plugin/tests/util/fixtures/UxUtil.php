<?php

namespace App
{
    use Symfony\UX\TwigComponent\Attribute\AsTwigComponent;
    use Symfony\UX\TwigComponent\Attribute\ExposeInTemplate;

    #[AsTwigComponent]
    class Alert
    {
        #[ExposeInTemplate]
        private string $message; // available as `{{ message }}` in the template

        #[ExposeInTemplate('alert_type')]
        private string $type = 'success'; // available as `{{ alert_type }}` in the template

        #[ExposeInTemplate(name: 'ico', getter: 'fetchIcon')]
        private string $icon = 'ico-warning'; // available as `{{ ico }}` in the template using `fetchIcon()` as the getter

        private string $notPublicField = 'test';

        /**
         * Required to access $this->message
         */
        public function getMessage(): string
        {
            return $this->message;
        }

        /**
         * Required to access $this->type
         */
        public function getType(): string
        {
            return $this->type;
        }

        /**
         * Required to access $this->icon
         */
        public function fetchIcon(): string
        {
            return $this->icon;
        }

        #[ExposeInTemplate]
        public function getActions(): array // available as `{{ actions }}` in the template
        {
        }

        #[ExposeInTemplate('dismissable')]
        public function canBeDismissed(): bool // available as `{{ dismissable }}` in the template
        {
        }

        private function notPrivateMethod(): array {}
        public function notExposedPublicMethod(): array {}
    }

}
