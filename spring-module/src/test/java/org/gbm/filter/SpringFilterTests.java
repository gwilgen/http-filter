package org.gbm.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = Application.class)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
public class SpringFilterTests {

    @Autowired
    private MockMvc mvc;
    @Autowired
    PeopleRepository repository;

    static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void bootstrap() throws Exception {
        repository.save(Person.builder()
                .name("Andrew")
                .dateOfBirth(dateFormat.parse("1970/01/01"))
                .heightInCm(183.2)
                .shoeSize(42)
                .build());
        repository.save(Person.builder()
                .name("Bob")
                .dateOfBirth(dateFormat.parse("1975/01/01"))
                .heightInCm(175.6)
                .shoeSize(40)
                .build());
        repository.save(Person.builder()
                .name("Carol")
                .dateOfBirth(dateFormat.parse("1980/01/01"))
                .heightInCm(169.0)
                .shoeSize(36)
                .build());
        repository.save(Person.builder()
                .name("David")
                .dateOfBirth(dateFormat.parse("1985/01/01"))
                .heightInCm(190.1)
                .shoeSize(45)
                .build());
    }

    @AfterEach
    public void clear() {
        repository.deleteAll();
    }

    protected String getQuery(String filter) {
        return "/list?filter=" + URLEncoder.encode(filter, Charset.defaultCharset()).trim();
    }

    protected ResultActions perform(String filter) throws Exception {
        return mvc.perform(get(getQuery(filter), Charset.defaultCharset())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    protected List<String> getNames(String filter) throws Exception {
        String result = perform(filter).andReturn().getResponse().getContentAsString();
        List<?> list = objectMapper.readValue(result, List.class);
        return list.stream().map(obj -> {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).map(str -> {
            try {
                return objectMapper.readValue(str, Person.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).map(Person::getName).toList();
    }

    @Test
    public void findCarolByName() throws Exception {
        perform("""
             name eq "Carol"
         """)
                .andExpect(jsonPath("$[0].name").value("Carol"));
    }

    @Test
    public void listTallerThan180() throws Exception {
        Assertions.assertEquals(List.of("Andrew", "David"), getNames("""
                heightInCm gt 180.2
                """));
    }

    @Test
    public void listTallerThan180Inversed() throws Exception {
        Assertions.assertEquals(List.of("Andrew", "David"), getNames("""
                180.2 lt heightInCm
                """));
    }

    @Test
    public void listWithAnd() throws Exception {
        Assertions.assertEquals(List.of("Bob"), getNames("""
                shoeSize gt 38 and "o" in name
                """));
    }

    @Test
    public void listWithOr() throws Exception {
        /**
         * Does not work due to Jackson's Date serialization. The process lacks of introspection to Jackson annotations (it could use a DateFormatter)
         */
        Assertions.assertEquals(Arrays.asList("Andrew", "Bob", "David"), getNames("""
                shoeSize gt 42 or dateOfBirth lt "1980"
                """));
    }

}