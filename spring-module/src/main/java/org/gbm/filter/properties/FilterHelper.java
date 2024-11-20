package org.gbm.filter.properties;

import jakarta.annotation.PostConstruct;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Data
@Lazy
public class FilterHelper {
    final FilterProperties properties;
    DateFormat dateFormat;

    @PostConstruct
    public void setDateFormat() {
        dateFormat = new SimpleDateFormat(properties.getDateFormat());
    }
}
