<?php

namespace {
    interface MyTraversable {}
    interface MyDateTimeInterface {}

    class MyDateTime implements MyDateTimeInterface {}
    interface MyIterator extends MyTraversable {}
    interface MyIteratorAggregate extends MyTraversable {}


    abstract class MyTaggedClass extends \MyDateTime implements \MyIterator { }
    abstract class MyTaggedClassFoo implements \MyIteratorAggregate { }

    abstract class MyTaggedClassExtends extends MyTaggedClass{ }
}