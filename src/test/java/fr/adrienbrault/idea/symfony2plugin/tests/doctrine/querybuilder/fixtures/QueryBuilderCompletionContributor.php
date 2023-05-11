<?php

namespace Doctrine\ORM
{
    class QueryBuilder
    {
        public function andWhere() {}
        public function join() {}
    }

    class EntityRepository
    {
        /**
         * @return QueryBuilder
         */
        public function createQueryBuilder($alias, $indexBy = null)
        {
        }
    }
}


namespace Doctrine\Bundle\DoctrineBundle\Repository
{
    use Doctrine\ORM\EntityRepository;

    class ServiceEntityRepository extends EntityRepository
    {
    }
}

namespace App
{
    class Entity
    {
    }
}
