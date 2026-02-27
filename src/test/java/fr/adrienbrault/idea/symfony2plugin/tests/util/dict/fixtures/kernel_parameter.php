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

    // Symfony 6.4/7.x style with short array syntax and + operator for array union
    class ModernKernel extends Kernel
    {
        protected function getKernelParameters()
        {
            $bundles = [];
            $bundlesMetadata = [];

            foreach ($this->bundles as $name => $bundle) {
                $bundles[$name] = $bundle::class;
                $bundlesMetadata[$name] = [
                    'path' => $bundle->getPath(),
                    'namespace' => $bundle->getNamespace(),
                ];
            }

            return [
                'kernel.project_dir' => realpath($this->getProjectDir()) ?: $this->getProjectDir(),
                'kernel.environment' => $this->environment,
                'kernel.runtime_environment' => '%env(default:kernel.environment:APP_RUNTIME_ENV)%',
                'kernel.debug' => $this->debug,
                'kernel.build_dir' => realpath($dir = $this->warmupDir ?: $this->getBuildDir()) ?: $dir,
                'kernel.cache_dir' => realpath($dir = $this->getCacheDir()) ?: $dir,
                'kernel.logs_dir' => realpath($dir = $this->getLogDir()) ?: $dir,
                'kernel.bundles' => $bundles,
                'kernel.bundles_metadata' => $bundlesMetadata,
                'kernel.charset' => $this->getCharset(),
                'kernel.container_class' => $this->getContainerClass(),
            ] + (null !== ($dir = $this->getShareDir()) ? ['kernel.share_dir' => realpath($dir) ?: $dir] : []);
        }
    }
}
