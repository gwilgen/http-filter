package org.gbm.filter;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SpringController {

    FilterComponent filterComponent;
    PeopleRepository repository;

    @RequestMapping("list")
    public List<Person> list(Pageable pageable) {
        return repository
                .findAll(filterComponent.toQuerydslPredicate(QPerson.class), pageable)
                .getContent();
    }
}
