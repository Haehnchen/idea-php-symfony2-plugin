<?php

namespace
{
    class MyDateTime
    {
    }
}

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

namespace My {
   class MyFooEvent {} 
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
                Bar::BAR => 'postFoo',
                'pre.foo1' => ['onStoreOrder', 0],
                'pre.foo2' => [
                    ['onKernelResponseMid', 10],
                    ['onKernelResponsePre', 20],
                ],
                'pre.foo3' => [],
                'pre.foo4' => null,
            );
        }

        public function preFoo() {}
        public function postFoo() {}
    }
}
