<?php

namespace Symfony\Component\HttpKernel
{
    abstract class Kernel
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
