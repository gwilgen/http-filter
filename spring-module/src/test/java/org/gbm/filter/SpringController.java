package org.gbm.filter;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SpringController {

    FilterUrlReader filterUrlReader;
    PeopleService service;

    @RequestMapping("list")
    public List<Person> list(Pageable pageable) {
        return service.getPage(filterUrlReader.getFilter(), pageable);
    }
}
