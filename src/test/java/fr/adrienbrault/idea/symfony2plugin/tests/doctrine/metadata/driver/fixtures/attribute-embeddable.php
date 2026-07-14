<?php

namespace ORM\Attributes;

use Doctrine\ORM\Mapping as ORM;
use ORM\Foobar\Status;

#[ORM\Embeddable]
class Address
{
    #[ORM\Column(name: "city_name", type: "string")]
    private string $city;

    #[ORM\Column(type: "string", enumType: Status::class)]
    private Status $status;
}
