<?php

namespace {
    abstract class MyTaggedClass extends \DateTime implements \Iterator { }
    abstract class MyTaggedClassFoo implements \IteratorAggregate { }

    abstract class MyTaggedClassExtends extends MyTaggedClass{ }
}