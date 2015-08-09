<?php

namespace Foo {
    class Bar {
        const BAR = 'post.foo';
    }
}

namespace Symfony\Component\EventDispatcher {
    interface EventSubscriberInterface
    {
        public static function getSubscribedEvents();
    }
}

namespace {

    use Foo\Bar;
    use Symfony\Component\EventDispatcher\EventSubscriberInterface;

    class TestEventSubscriber implements EventSubscriberInterface
    {
        public static function getSubscribedEvents()
        {
            return array(
                'pre.foo' => 'preFoo',
                Bar::BAR => 'postFoo'
            );
        }

        public function preFoo() {}
        public function postFoo() {}
    }
}
