<?php

class Foo
{
    const FOO = 'foo1_4_const';

    public function foo1()
    {
        return 'foo1_1';

        return 'foo1_2';

        return 'foo1_3';

        return self::FOO;

        return Foo::class;
    }

    public function foo2()
    {
        if (true) {
            return 'foo2_1';
        }


        if (true) {
            return 'foo2_2';
            if (true) {
                return 'foo2_3';
                if (true) {
                    return 'foo2_4';
                }
            }
        }

        return 'foo2_x';
    }

    public function foo3()
    {
        switch (true) {
            case 0:
                return 'foo3_1';
            default:
                return 'foo3_2';
        }

        return 'foo3_3';
    }
}