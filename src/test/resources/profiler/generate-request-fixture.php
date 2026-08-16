<?php

declare(strict_types=1);

namespace Symfony\Component\VarDumper\Cloner {
    /** Minimal neutral fixture implementation of Symfony's serialized Stub shape. */
    final class Stub
    {
        public const TYPE_REF = 1;
        public const TYPE_STRING = 2;
        public const TYPE_ARRAY = 3;
        public const TYPE_OBJECT = 4;
        public const TYPE_RESOURCE = 5;
        public const TYPE_SCALAR = 6;

        public int $type = self::TYPE_REF;
        public string|int|null $class = '';
        public mixed $value = null;
        public int $cut = 0;
        public int $handle = 0;
        public int $refCount = 0;
        public int $position = 0;
        public array $attr = [];

        /** Mirrors modern Symfony by serializing only fields that differ from defaults. */
        public function __serialize(): array
        {
            $defaults = [
                'type' => self::TYPE_REF,
                'class' => '',
                'value' => null,
                'cut' => 0,
                'handle' => 0,
                'refCount' => 0,
                'position' => 0,
                'attr' => [],
            ];
            $data = [];
            foreach ($defaults as $name => $default) {
                if ($this->{$name} !== $default) {
                    $data[$name] = $this->{$name};
                }
            }

            return $data;
        }
    }

    /** Encodes arrays and objects into the position table consumed by Data::getValue(true). */
    final class Data
    {
        private array $data = [0 => []];
        private int $position = 0;
        private int|string $key = 0;
        private int $maxDepth = 20;
        private int $maxItemsPerDepth = -1;
        private int $useRefHandles = -1;
        private array $context = [];

        public function __construct(mixed $value)
        {
            $this->data[0][0] = $this->encode($value);
        }

        private function encode(mixed $value): mixed
        {
            if (is_array($value)) {
                if ([] === $value) {
                    return [];
                }

                // Reserve the position before descending so child positions remain stable.
                $position = count($this->data);
                $this->data[$position] = [];
                foreach ($value as $key => $child) {
                    $this->data[$position][$key] = $this->encode($child);
                }

                return [(array_is_list($value) ? 2 : 1) => $position];
            }

            if (is_object($value)) {
                $position = count($this->data);
                $this->data[$position] = [];
                foreach ((array) $value as $key => $child) {
                    $this->data[$position][$key] = $this->encode($child);
                }

                $stub = new Stub();
                $stub->type = Stub::TYPE_OBJECT;
                $stub->class = $value::class;
                $stub->position = $position;

                return $stub;
            }

            return $value;
        }
    }
}

namespace Symfony\Component\HttpKernel\DataCollector {
    use Symfony\Component\VarDumper\Cloner\Data;

    final class RequestDataCollector
    {
        /** @param array<string, mixed> $data */
        public function __construct(protected Data $data)
        {
        }
    }
}

namespace ProfilerFixture {
    final class TraceContext
    {
        public function __construct(
            private string $secretToken,
            protected string $label,
            public array $metadata,
        ) {
        }
    }
}

namespace {
    use ProfilerFixture\TraceContext;
    use Symfony\Component\HttpKernel\DataCollector\RequestDataCollector;
    use Symfony\Component\VarDumper\Cloner\Data;

    // This fixture is intentionally synthetic and contains no names or values from a real project.
    $requestData = [
        'method' => 'POST',
        'format' => 'json',
        'content_type' => 'application/json',
        'status_text' => 'OK',
        'status_code' => 200,
        'request_query' => [
            'page' => '2',
            'api_key' => 'query-secret',
        ],
        'request_request' => [
            'email' => 'user@example.test',
            'password' => 'request-secret',
            'profile' => [
                'display_name' => 'Alice',
                'otp' => 'otp-secret',
            ],
        ],
        'request_files' => [],
        'request_headers' => [
            'content-type' => 'application/json',
            'authorization' => 'Bearer authorization-secret',
            'x-correlation-id' => 'corr-42',
        ],
        'request_server' => [
            'REQUEST_URI' => '/login?api_key=query-secret',
            'QUERY_STRING' => 'api_key=query-secret',
            'APP_ENV' => 'test',
            'APP_SECRET' => 'application-secret',
            'DATABASE_URL' => 'mysql://fixture-user:database-secret@database:3306/example',
            'HTTP_HOST' => 'example.test',
        ],
        'request_cookies' => [
            'PHPSESSID' => 'session-secret',
            'theme' => 'dark',
        ],
        'request_attributes' => [
            '_route' => 'app_login',
            '_controller' => 'Example\\Controller\\LoginController::login',
            'csrf_token' => 'csrf-secret',
        ],
        'route' => 'app_login',
        'response_headers' => [
            'content-type' => 'application/json',
            'set-cookie' => 'response-cookie-secret',
        ],
        'response_cookies' => [
            'session' => 'response-session-secret',
        ],
        'session_metadata' => [
            'Created' => 'Thu, 13 Aug 2026 12:00:00 +0000',
            'Lifetime' => 3600,
        ],
        'session_attributes' => [
            '_security_main' => 'security-session-secret',
            'cart_id' => 'cart-42',
        ],
        'session_usages' => [
            [
                'name' => 'ExampleService:load',
                'file' => '/app/src/Service/ExampleService.php',
                'line' => 21,
                'context' => new TraceContext('object-secret', 'neutral-context', ['safe' => true]),
            ],
        ],
        'stateless_check' => false,
        'flashes' => [],
        'path_info' => '/login',
        'controller' => [
            'class' => 'Example\\Controller\\LoginController',
            'method' => 'login',
            'file' => '/app/src/Controller/LoginController.php',
            'line' => 42,
        ],
        'locale' => 'en',
        'dotenv_vars' => [
            'APP_ENV' => 'test',
            'APP_SECRET' => 'dotenv-secret',
            'MESSENGER_TRANSPORT_DSN' => 'doctrine://transport-secret',
            'FEATURE_FLAG' => '1',
        ],
        'content' => '{"password":"body-secret"}',
        'curlCommand' => 'curl -H "Authorization: Bearer curl-secret" http://example.test/login',
        'redirect' => [
            'token' => 'redirect-secret',
            'route' => 'app_home',
            'method' => 'GET',
            'status_code' => 302,
        ],
        'identifier' => 'app_login',
        'future_context' => [
            'feature' => 'neutral-value',
            'nested' => ['api_token' => 'future-secret'],
        ],
    ];

    $collector = new RequestDataCollector(new Data($requestData));
    $profile = [
        'token' => 'abc123',
        'parent' => null,
        'children' => [],
        'data' => ['request' => $collector],
        'ip' => '127.0.0.1',
        'method' => 'POST',
        'url' => 'http://example.test/login?page=2',
        'time' => 1786622400,
        'status_code' => 200,
    ];

    $serialized = serialize($profile);
    $roundTrip = unserialize($serialized, ['allowed_classes' => true]);
    if (!is_array($roundTrip) || !isset($roundTrip['data']['request'])) {
        throw new RuntimeException('PHP rejected the generated profiler fixture');
    }

    $directory = __DIR__ . '/generated';
    if (!is_dir($directory) && !mkdir($directory, 0777, true) && !is_dir($directory)) {
        throw new RuntimeException('Unable to create fixture directory');
    }

    $compressed = gzencode($serialized, 3);
    $target = $directory . '/symfony-profiler-request.gz';
    if ($compressed === false || file_put_contents($target, $compressed) !== strlen($compressed)) {
        throw new RuntimeException('Unable to write profiler fixture');
    }

    fwrite(STDOUT, 'Generated neutral Symfony request profiler fixture with PHP ' . PHP_VERSION . PHP_EOL);
}
