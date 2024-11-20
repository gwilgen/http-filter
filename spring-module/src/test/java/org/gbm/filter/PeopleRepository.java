package org.gbm.filter;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
public interface PeopleRepository extends JpaRepository<Person, Integer>, QuerydslPredicateExecutor<Person> {
}
