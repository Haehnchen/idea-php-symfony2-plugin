<?php

namespace Doctrine\ORM\Mapping {
    class Entity {}
    class Column {}
    class ManyToOne {}
    class OneToMany {}
}

namespace Doctrine\DBAL\Types {
    class Types {
        public const TIME_IMMUTABLE = 'time_immutable';
    }
}

namespace Symfony\Component\Form {
    interface FormBuilderInterface {
        public function add($child, $type = null, array $options = []);
        public function create($name, $type = null, array $options = []);
    }
}

namespace Symfony\Component\Form\Extension\Core\Type {
    class EmailType {}
    class TextType {}
    class DateTimeType {}
}

namespace App\Form {
    use DateTimeImmutable as Moment;
    use Doctrine\ORM\Mapping as ORM;
    use Doctrine\DBAL\Types\Types;
    use Symfony\Component\Form\Attribute\FormField;
    use Symfony\Component\Form\Extension\Core\Type\EmailType;
    use Symfony\Component\Form\Extension\Core\Type\TextType;
    use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
    use Symfony\Component\Form\FormBuilderInterface;

    #[ORM\Entity]
    class EventData {
        #[FormField]
        public int $capacity;

        #[FormField]
        public float $price;

        #[FormField]
        public bool $isPublic;

        #[FormField]
        public \DateTime $startsAt;

        #[FormField]
        public \DateTimeImmutable $immutableStartsAt;

        #[FormField]
        public \DateTimeInterface $dateTimeInterface;

        #[FormField]
        public \Symfony\Component\Clock\DatePoint $datePoint;

        #[FormField]
        public \DateInterval $duration;

        #[FormField]
        public ?int $nullableCapacity;

        #[FormField]
        public ?Moment $nullableStartsAt;

        #[FormField(EmailType::class)]
        public int $explicitType;

        #[FormField(type: null)]
        public int $guessedType;

        #[FormField(options: ['property_path' => 'capacity'])]
        public string $capacityLabel;

        #[FormField]
        #[ORM\Column(type: 'date')]
        public ?\DateTimeImmutable $day;

        #[FormField]
        #[ORM\Column(type: 'date_immutable')]
        public ?\DateTimeImmutable $immutableDay;

        #[FormField]
        #[ORM\Column(type: 'time')]
        public ?\DateTimeImmutable $startTime;

        #[FormField]
        #[ORM\Column(type: Types::TIME_IMMUTABLE)]
        public ?\DateTimeImmutable $immutableTime;

        #[FormField(DateTimeType::class)]
        #[ORM\Column(type: 'date')]
        public ?\DateTimeImmutable $explicitDate;

        #[FormField]
        #[ORM\Column(type: 'decimal')]
        public string $amount;

        #[FormField]
        #[ORM\Column(type: 'bigint')]
        public string $externalId;

        #[FormField]
        #[ORM\Column(type: 'boolean')]
        public $enabled;

        #[FormField]
        #[ORM\Column(type: 'string')]
        public string $title;

        #[FormField]
        #[ORM\Column(type: 'text')]
        public string $description;

        #[FormField]
        #[ORM\Column(type: 'simple_array')]
        public array $tags;

        #[FormField]
        #[ORM\ManyToOne(targetEntity: EventData::class)]
        public ?EventData $parent;

        #[FormField]
        #[ORM\OneToMany(targetEntity: EventData::class, mappedBy: 'parent')]
        public $children;

        #[FormField]
        #[ORM\Column(type: 'custom_integer')]
        public int $customCapacity;

        #[FormField]
        #[ORM\Column(type: 'json')]
        public array $settings;

        public function __construct(
            #[FormField] public int $promotedCapacity,
        ) {}
    }

    class EventType {
        public function addCapacity(FormBuilderInterface $builder) {
            $builder->add(options: [], child: 'capacity');
        }

        public function createMinutes(FormBuilderInterface $builder) {
            $builder->create(options: ['property_path' => 'capacity'], name: 'minutes');
        }

        public function addExplicitType(FormBuilderInterface $builder) {
            $builder->add('capacity', TextType::class);
        }

        public function createDay(FormBuilderInterface $builder) {
            $builder->create(options: ['property_path' => 'day'], name: 'eventDay');
        }

        public function configureOptions($resolver) {
            $resolver->setDefaults(['data_class' => EventData::class]);
        }
    }
}
