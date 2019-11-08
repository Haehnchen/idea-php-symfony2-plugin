<?php

namespace Symfony\Component\EventDispatcher {
    class Event {
        public function stopPropagation() {}
    }

    class EventDispatcherInterface {
        public function dispatch($name, $event) {}
    }
}

namespace {
    use Symfony\Component\EventDispatcher\Event;

    class FooEvent extends Event {
        public function onFoo() {}
    }
}



