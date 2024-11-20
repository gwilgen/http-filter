package org.gbm.filter;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class SpringController {

    FilterComponent filterComponent;
    PeopleRepository repository;

    @RequestMapping("list")
    public List<Person> list(Pageable pageable) {
        return repository.findAll(filterComponent.toQuerydslPredicate(QPerson.class), pageable).getContent();
    }

}
