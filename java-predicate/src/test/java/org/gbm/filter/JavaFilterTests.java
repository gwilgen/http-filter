package org.gbm.filter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
public class JavaFilterTests {

    static final List<Person> people = new ArrayList<>();

    static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void populatePeople() {
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        try {
            people.add(Person.builder()
                    .name("Andrew")
                    .dateOfBirth(dateFormat.parse("1970/01/01"))
                    .eyeColor(EYE_COLOR.BLUE)
                    .heightInCm(183.2)
                    .shoeSize(42)
                    .build());
            people.add(Person.builder()
                    .name("Bob")
                    .dateOfBirth(dateFormat.parse("1975/01/01"))
                    .eyeColor(EYE_COLOR.GREEN)
                    .heightInCm(175.6)
                    .shoeSize(40)
                    .build());
            people.add(Person.builder()
                    .name("Carol")
                    .dateOfBirth(dateFormat.parse("1980/01/01"))
                    .eyeColor(EYE_COLOR.BLUE)
                    .heightInCm(169.0)
                    .shoeSize(36)
                    .build());
            people.add(Person.builder()
                    .name("David")
                    .dateOfBirth(dateFormat.parse("1985/01/01"))
                    .eyeColor(EYE_COLOR.BROWN)
                    .heightInCm(190.1)
                    .shoeSize(45)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize test", e);
        }
    }

    protected List<Person> filter(String filter) {
        List<Person> result = people.stream()
                .filter(JavaFilterUtils.asPredicate(FilterCriteria.from(filter))).toList();
        if (result.isEmpty()) {
            log.info("No results found");
        } else {
            result.stream().map(this::toJson).filter(Objects::nonNull).forEach(log::info);
        }
        return result;
    }

    protected String toJson(Person p) {
        try {
            return objectMapper.writeValueAsString(p);
        } catch (Exception e) {
            log.error("Could not print {}", p.getName(), e);
        }
        return null;
    }

    @Test
    public void testBasicCondition() {
        Assertions.assertEquals(
                1, filter("""
                        name eq "David"
                        """)
                        .size());
    }

    @Test
    public void testOperationCondition() {
        Assertions.assertEquals(
                2,
                filter("""
				name eq "David" or heightInCm gt 180.2
				""")
                        .size());
    }
}
