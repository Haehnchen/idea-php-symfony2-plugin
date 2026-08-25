<?php

declare(strict_types=1);

namespace Symfony\Component\VarDumper\Cloner {
    /** Minimal Stub required by the serialized test data. */
    final class Stub
    {
        public const TYPE_REF = 1;
        public const TYPE_ARRAY = 3;

        public int $type = self::TYPE_REF;
        public string|int|null $class = '';
        public mixed $value = null;
        public int $cut = 0;
        public int $handle = 0;
        public int $refCount = 0;
        public int $position = 0;
        public array $attr = [];

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

    /** Encodes arrays into the position table consumed by Data::getValue(true). */
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
            if (!is_array($value) || [] === $value) {
                return $value;
            }

            $position = count($this->data);
            $this->data[$position] = [];
            foreach ($value as $key => $child) {
                $this->data[$position][$key] = $this->encode($child);
            }

            return [(array_is_list($value) ? 2 : 1) => $position];
        }
    }
}

namespace Symfony\Component\Translation\DataCollector {
    use Symfony\Component\VarDumper\Cloner\Data;

    final class TranslationDataCollector
    {
        public function __construct(protected Data $data)
        {
        }
    }
}

namespace {
    use Symfony\Component\Translation\DataCollector\TranslationDataCollector;
    use Symfony\Component\VarDumper\Cloner\Data;

    // Generates neutral Symfony translation profiler test data.
    $messages = [
        'defined-welcome' => [
            'locale' => 'en',
            'fallbackLocale' => null,
            'domain' => 'messages',
            'id' => 'welcome.title',
            'translation' => 'Welcome',
            'parameters' => [],
            'state' => 0,
            'transChoiceNumber' => null,
            'count' => 3,
        ],
        'missing-checkout' => [
            'locale' => 'en',
            'fallbackLocale' => null,
            'domain' => 'checkout',
            'id' => 'checkout.missing_title',
            'translation' => 'checkout.missing_title',
            'parameters' => [['%name%' => 'Example']],
            'state' => 1,
            'transChoiceNumber' => null,
            'count' => 2,
        ],
        'fallback-account' => [
            'locale' => 'en',
            'fallbackLocale' => 'fr',
            'domain' => 'messages',
            'id' => 'account.title',
            'translation' => 'Compte',
            'parameters' => [],
            'state' => 2,
            'transChoiceNumber' => null,
            'count' => 1,
        ],
        'defined-items' => [
            'locale' => 'en',
            'fallbackLocale' => null,
            'domain' => 'messages',
            'id' => 'cart.items',
            'translation' => '{0} Empty|{1} One item|]1,Inf] %count% items',
            'parameters' => [['%count%' => 4]],
            'state' => 0,
            'transChoiceNumber' => 4,
            'count' => 1,
        ],
        'missing-csv' => [
            'locale' => 'en',
            'fallbackLocale' => null,
            'domain' => 'messages',
            'id' => "missing, \"quoted\"\nkey",
            'translation' => "Preview, \"quoted\"\nline",
            'parameters' => [],
            'state' => 1,
            'transChoiceNumber' => null,
            'count' => 1,
        ],
    ];

    $collector = new TranslationDataCollector(new Data([
        'locale' => 'en',
        'fallback_locales' => ['fr', 'de'],
        'global_parameters' => [],
        0 => 2,
        1 => 2,
        2 => 1,
        'messages' => $messages,
    ]));
    $profile = [
        'token' => '7a1a7e',
        'parent' => null,
        'children' => [],
        'data' => ['translation' => $collector],
        'ip' => '127.0.0.1',
        'method' => 'GET',
        'url' => 'http://example.test/translations',
        'time' => 1_723_557_600,
        'status_code' => 200,
    ];

    $serialized = serialize($profile);
    $roundTrip = unserialize($serialized, ['allowed_classes' => true]);
    if (!is_array($roundTrip) || !isset($roundTrip['data']['translation'])) {
        throw new RuntimeException('PHP rejected the generated profiler fixture');
    }

    $directory = __DIR__ . '/generated';
    if (!is_dir($directory) && !mkdir($directory, 0777, true) && !is_dir($directory)) {
        throw new RuntimeException('Unable to create fixture directory');
    }

    $compressed = gzencode($serialized, 3);
    $target = $directory . '/symfony-profiler-translation.gz';
    if ($compressed === false || file_put_contents($target, $compressed) !== strlen($compressed)) {
        throw new RuntimeException('Unable to write profiler fixture');
    }

    fwrite(STDOUT, 'Generated neutral Symfony translation profiler fixture with PHP ' . PHP_VERSION . PHP_EOL);
}
