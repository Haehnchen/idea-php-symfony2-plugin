<?php

namespace Doctrine\ORM
{
    class QueryBuilder
    {
        public function select() {}
        public function addSelect() {}
        public function where() {}
        public function andWhere() {}
        public function join($join, $alias, $conditionType = null, $condition = null, $indexBy = null) {}
        public function innerJoin($join, $alias, $conditionType = null, $condition = null, $indexBy = null) {}
        public function leftJoin($join, $alias, $conditionType = null, $condition = null, $indexBy = null) {}

        public function from($from, $alias, $indexBy = null) {}

        public function groupBy($groupBy) {}

        public function addGroupBy($groupBy) {}
        public function setParameter($key, $value, $type = null) {}
        public function setParameters($parameters) {}
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
        private $id;
    }
}
