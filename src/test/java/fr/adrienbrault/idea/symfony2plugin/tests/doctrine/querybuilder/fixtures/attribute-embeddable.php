<?php

namespace App;

use Doctrine\ORM\Mapping as ORM;

#[ORM\Embeddable]
class AttributeAddress
{
    #[ORM\Column(type: 'string')]
    private string $city;

    #[ORM\Column(type: 'string')]
    private string $status;
}
