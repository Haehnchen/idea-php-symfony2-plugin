<?php

namespace App
{
    class Foobar {}
    class Foobar2 {}
    class Foobar3 {}
    class Foobar4 {}

    class PostController
    {
        public function __invoke()
        {
            $s->deserialize('foo', Foobar::class, 'json');
            $s->deserialize('foo', 'App\Foobar2', 'json');
            $s->deserialize('foo', '\App\Foobar3[]', 'json');
            $s->deserialize('foo', Foobar4::class . '[]', 'json');
        }
    }
}