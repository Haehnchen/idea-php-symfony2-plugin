<?php

namespace Instance\Of {
    class Foo extends Cool implements Bar{}
    interface Bar extends Apple {}
    class Car extends Foo implements Apple{}
    class Cool{}

    interface Apple{}
}
