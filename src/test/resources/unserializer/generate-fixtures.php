<?php

declare(strict_types=1);

// Generates the PHP serialize() byte streams used by PhpUnserializerCompatibilityTest.
// Run this script after changing a fixture and commit the regenerated files.

enum NeutralState
{
    case Ready;
}

final class NeutralNode
{
    public string $label = 'node';
    public ?NeutralNode $self = null;
}

final class NeutralVisibility
{
    public string $publicValue = 'public';
    protected string $protectedValue = 'protected';
    private string $privateValue = 'private';
}

final class NeutralIntegerProperties
{
    /** @return array<int|string, string> */
    public function __serialize(): array
    {
        return [7 => 'seven', 'name' => 'value'];
    }

    /** @param array<int|string, string> $data */
    public function __unserialize(array $data): void
    {
    }
}

final class NeutralLegacy implements Serializable
{
    public function serialize(): string
    {
        return "opaque;\0}\"payload";
    }

    public function unserialize(string $data): void
    {
    }
}

$directory = __DIR__ . '/generated';
if (!is_dir($directory) && !mkdir($directory, 0777, true) && !is_dir($directory)) {
    throw new RuntimeException('Unable to create fixture directory');
}

/** @param mixed $value */
function write_fixture(string $name, mixed $value): void
{
    global $directory;

    $serialized = serialize($value);
    $roundTrip = unserialize($serialized, ['allowed_classes' => true]);
    if ($roundTrip === false && $serialized !== 'b:0;') {
        throw new RuntimeException("PHP rejected generated fixture: {$name}");
    }
    if (file_put_contents("{$directory}/{$name}.ser", $serialized) !== strlen($serialized)) {
        throw new RuntimeException("Unable to write fixture: {$name}");
    }
}

$node = new NeutralNode();
$node->self = $node;

$first = new NeutralNode();
$first->label = 'first';
$second = new NeutralNode();
$second->label = 'second';

$shared = 'shared';

write_fixture('all-tags', [
    'null' => null,
    'false' => false,
    'true' => true,
    'integer' => -42,
    'float' => 1.25e20,
    'infinity' => INF,
    'negative_infinity' => -INF,
    'not_a_number' => NAN,
    'binary' => "utf8-ä\0\xff;}",
    'array' => [7 => 'seven', 'name' => 'value'],
    'object' => new NeutralVisibility(),
    'enum' => NeutralState::Ready,
    'custom' => new NeutralLegacy(),
]);
write_fixture('object-cycle', $node);
write_fixture('aliases', [&$shared, &$shared]);
write_fixture('reference-order', [$first, $first, $second, $second]);
write_fixture('enum-reference', [NeutralState::Ready, NeutralState::Ready]);
write_fixture('custom-followed', [new NeutralLegacy(), 42]);
write_fixture('visibility', new NeutralVisibility());
write_fixture('integer-properties', new NeutralIntegerProperties());

fwrite(STDOUT, 'Generated neutral fixtures with PHP ' . PHP_VERSION . PHP_EOL);
