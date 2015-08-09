<?php

namespace Symfony\Component\EventDispatcher {
    interface EventSubscriberInterface
    {
        public static function getSubscribedEvents();
    }
}

namespace {

    use Foo\Bar;
    use Symfony\Component\EventDispatcher\EventSubscriberInterface;

    class TestDateTimeEventSubscriber implements EventSubscriberInterface
    {
        public static function getSubscribedEvents()
        {
            return array(
                'pre.foo' => 'preFoo',
                'kernel.request' => 'FOO',
            );
        }

        public function preFoo(\DateTime $d) {}
    }
}
