<?php

namespace Foo
{
    final class BarEvent
    {
        /**
         * @Event
         */
        const PRE_BAR = 'bar.pre_bar';

        /**
         * @Event("My\MyFooEvent")
         */
        const POST_BAR = 'bar.post_bar';
    }
}
