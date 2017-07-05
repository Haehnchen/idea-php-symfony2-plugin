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
                    'kernel.array_merge' => null,
                ),
                null
            );

            return array_merge_recursive(
                array(
                    'kernel.array_merge_recursive' => null,
                ),
                null
            );

            return array_replace(
                [
                    'kernel.array_replace' => null,
                ],
                array(
                    'kernel.array_replace_2' => null,
                )
            );

            return [
                'kernel.array' => null,
            ];
        }
    }
}
