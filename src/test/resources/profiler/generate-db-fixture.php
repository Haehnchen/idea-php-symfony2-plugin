<?php

declare(strict_types=1);

namespace Doctrine\Bundle\DoctrineBundle\DataCollector {
    final class DoctrineDataCollector
    {
        /** @param array<string, mixed> $data */
        public function __construct(protected array $data)
        {
        }
    }
}

namespace {
    use Doctrine\Bundle\DoctrineBundle\DataCollector\DoctrineDataCollector;

    // Generates the query-bearing Symfony profile used by SymfonyProfilerDatabaseConsumerTest.
    // Run this script after changing its data and commit the regenerated GZIP file.

    // Mirrors the shape of a real Doctrine trace while keeping paths, application classes, and
    // application method names generic. The complete trace is intentional so renderers can test
    // their own filtering without weakening the consumer fixture.
    $selectBacktrace = [
        [
            'file' => '/app/vendor/doctrine/orm/src/Query/Exec/FinalizedSelectExecutor.php',
            'line' => 30,
            'function' => 'executeQuery',
            'class' => 'Doctrine\\DBAL\\Connection',
            'type' => '->',
        ],
        [
            'file' => '/app/vendor/doctrine/orm/src/Query.php',
            'line' => 300,
            'function' => 'execute',
            'class' => 'Doctrine\\ORM\\Query\\Exec\\FinalizedSelectExecutor',
            'type' => '->',
        ],
        [
            'file' => '/app/vendor/doctrine/orm/src/AbstractQuery.php',
            'line' => 900,
            'function' => '_doExecute',
            'class' => 'Doctrine\\ORM\\Query',
            'type' => '->',
        ],
        [
            'file' => '/app/src/Repository/ExampleRepository.php',
            'line' => 40,
            'function' => 'getOneOrNullResult',
            'class' => 'Doctrine\\ORM\\AbstractQuery',
            'type' => '->',
        ],
        [
            'file' => '/app/src/Service/ExampleService.php',
            'line' => 30,
            'function' => 'findOne',
            'class' => 'App\\Repository\\ExampleRepository',
            'type' => '->',
        ],
        [
            'file' => '/app/src/Controller/ExampleController.php',
            'line' => 20,
            'function' => 'load',
            'class' => 'App\\Service\\ExampleService',
            'type' => '->',
        ],
        [
            'file' => '/app/vendor/symfony/http-kernel/HttpKernel.php',
            'line' => 160,
            'function' => 'show',
            'class' => 'App\\Controller\\ExampleController',
            'type' => '->',
        ],
        [
            'file' => '/app/vendor/symfony/http-kernel/HttpKernel.php',
            'line' => 80,
            'function' => 'handleRaw',
            'class' => 'Symfony\\Component\\HttpKernel\\HttpKernel',
            'type' => '->',
        ],
        [
            'file' => '/app/vendor/symfony/http-kernel/Kernel.php',
            'line' => 190,
            'function' => 'handle',
            'class' => 'Symfony\\Component\\HttpKernel\\HttpKernel',
            'type' => '->',
        ],
        [
            'file' => '/app/vendor/symfony/runtime/Runner/Symfony/HttpKernelRunner.php',
            'line' => 35,
            'function' => 'handle',
            'class' => 'Symfony\\Component\\HttpKernel\\Kernel',
            'type' => '->',
        ],
        [
            'file' => '/app/vendor/autoload_runtime.php',
            'line' => 30,
            'function' => 'run',
            'class' => 'Symfony\\Component\\Runtime\\Runner\\Symfony\\HttpKernelRunner',
            'type' => '->',
        ],
        [
            'file' => '/app/public/index.php',
            'line' => 5,
            'function' => 'require_once',
        ],
    ];

    $updateBacktrace = $selectBacktrace;
    $updateBacktrace[0] = [
        'file' => '/app/vendor/doctrine/orm/src/Persisters/Entity/BasicEntityPersister.php',
        'line' => 500,
        'function' => 'executeStatement',
        'class' => 'Doctrine\\DBAL\\Connection',
        'type' => '->',
    ];
    $updateBacktrace[1] = [
        'file' => '/app/vendor/doctrine/orm/src/UnitOfWork.php',
        'line' => 1100,
        'function' => 'update',
        'class' => 'Doctrine\\ORM\\Persisters\\Entity\\BasicEntityPersister',
        'type' => '->',
    ];
    $updateBacktrace[2] = [
        'file' => '/app/vendor/doctrine/orm/src/EntityManager.php',
        'line' => 250,
        'function' => 'commit',
        'class' => 'Doctrine\\ORM\\UnitOfWork',
        'type' => '->',
    ];
    $updateBacktrace[3] = [
        'file' => '/app/src/Repository/ExampleRepository.php',
        'line' => 60,
        'function' => 'flush',
        'class' => 'Doctrine\\ORM\\EntityManager',
        'type' => '->',
    ];
    $updateBacktrace[4] = [
        'file' => '/app/src/Service/ExampleService.php',
        'line' => 50,
        'function' => 'updateOne',
        'class' => 'App\\Repository\\ExampleRepository',
        'type' => '->',
    ];
    $updateBacktrace[5] = [
        'file' => '/app/src/Controller/ExampleController.php',
        'line' => 35,
        'function' => 'save',
        'class' => 'App\\Service\\ExampleService',
        'type' => '->',
    ];
    $updateBacktrace[6] = [
        'file' => '/app/vendor/symfony/http-kernel/HttpKernel.php',
        'line' => 160,
        'function' => 'update',
        'class' => 'App\\Controller\\ExampleController',
        'type' => '->',
    ];

    $collector = new DoctrineDataCollector([
        'queries' => [
            'default' => [
                [
                    'executionMS' => 0.004,
                    'sql' => 'SELECT * FROM users WHERE id = ?',
                    'params' => [42],
                    'types' => [],
                    'backtrace' => $selectBacktrace,
                ],
                [
                    'executionMS' => 0.006,
                    'sql' => 'SELECT * FROM users WHERE id = ?',
                    'params' => [43],
                    'types' => [],
                    'backtrace' => $selectBacktrace,
                ],
                [
                    'executionMS' => 0.00234,
                    'sql' => 'UPDATE users SET last_seen = ? WHERE id = ?',
                    'params' => ['2026-08-13', 42],
                    'types' => [],
                    'backtrace' => $updateBacktrace,
                ],
            ],
        ],
        'connections' => [
            'default' => 'doctrine.dbal.default_connection',
            'analytics' => 'doctrine.dbal.analytics_connection',
        ],
        'managers' => [],
        'entities' => [],
        'errors' => [],
        'caches' => [],
    ]);

    $profile = [
        'token' => 'profile01',
        'parent' => null,
        'children' => [],
        'data' => ['db' => $collector],
        'ip' => '127.0.0.1',
        'method' => 'GET',
        'url' => 'http://example.test/orders/42',
        'time' => 1723557600,
        'status_code' => 200,
    ];

    $serialized = serialize($profile);
    $roundTrip = unserialize($serialized, ['allowed_classes' => true]);
    if (!is_array($roundTrip) || !isset($roundTrip['data']['db'])) {
        throw new RuntimeException('PHP rejected the generated profiler fixture');
    }

    $directory = __DIR__ . '/generated';
    if (!is_dir($directory) && !mkdir($directory, 0777, true) && !is_dir($directory)) {
        throw new RuntimeException('Unable to create fixture directory');
    }

    $compressed = gzencode($serialized, 3);
    if ($compressed === false || file_put_contents($directory . '/symfony-profiler-db.gz', $compressed) !== strlen($compressed)) {
        throw new RuntimeException('Unable to write profiler fixture');
    }

    fwrite(STDOUT, 'Generated Symfony profiler fixture with PHP ' . PHP_VERSION . PHP_EOL);
}
