<?php

namespace Instance\Of {
    //duplicates
    class Car implements \DateTime{}
    class Foo implements \DateTime{}


    class Foo extends Cool implements Bar{}
    interface Bar {}
    class Car extends Foo{}
    class Cool{}

    //duplicates
    class Car implements Apple{}
    class Foo implements Apple{}

    interface Apple{}
}
