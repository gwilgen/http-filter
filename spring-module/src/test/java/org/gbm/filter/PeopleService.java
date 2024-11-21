package org.gbm.filter;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PeopleService {

    PeopleRepository repository;
    PredicateFilterFactory predicateFilterFactory;

    List<Person> getPage(String filter, Pageable pageable) {
        return repository
                .findAll(predicateFilterFactory.from(QPerson.class, filter), pageable)
                .getContent();
    }
}
