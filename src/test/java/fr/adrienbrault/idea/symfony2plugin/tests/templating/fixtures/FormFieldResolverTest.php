<?php

namespace App\Form;

use Symfony\Component\Form\Attribute\AsFormType;
use Symfony\Component\Form\Attribute\FormField;

#[AsFormType]
class XmlEventData {
    #[FormField]
    public ?\DateTimeImmutable $day;

    #[FormField]
    public ?\DateTimeImmutable $startsAt;
}
