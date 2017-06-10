<?php

namespace Symfony\Component\HttpKernel
{
    abstract class Kernel
    {
        protected function getKernelParameters()
        {
            return array_merge(
                array(
                    'kernel.root_dir' => null,
                    'kernel.project_dir' => null,
                ),
                null
            );
        }
    }

    class MyFoo extends Kernel
    {
        protected function getKernelParameters()
        {
            return array_merge(
                array(
                    'kernel.foobar' => null,
                ),
                null
            );
        }
    }
}
