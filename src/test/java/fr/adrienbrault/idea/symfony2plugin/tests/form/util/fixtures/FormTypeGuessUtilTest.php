<?php

namespace Symfony\Component\Form {
    interface FormBuilderInterface {
        public function add($child, $type = null, array $options = []);
        public function create($name, $type = null, array $options = []);
    }
}

namespace Symfony\Component\Form\Extension\Core\Type {
    class EmailType {}
    class TextType {}
}

namespace App\Form {
    use DateTimeImmutable as Moment;
    use Symfony\Component\Form\Attribute\FormField;
    use Symfony\Component\Form\Extension\Core\Type\EmailType;
    use Symfony\Component\Form\Extension\Core\Type\TextType;
    use Symfony\Component\Form\FormBuilderInterface;

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

        public function configureOptions($resolver) {
            $resolver->setDefaults(['data_class' => EventData::class]);
        }
    }
}
