package org.gbm.filter;

import java.util.Date;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Person {

    String name;

    Date dateOfBirth;

    EYE_COLOR eyeColor;

    Double heightInCm;

    Integer shoeSize;
}
