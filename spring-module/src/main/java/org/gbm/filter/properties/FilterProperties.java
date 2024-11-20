package org.gbm.filter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ConfigurationProperties("filter")
@Data
@Lazy
public class FilterProperties {
    String dateFormat = "yyyy-MM-dd hh:mm";
}
