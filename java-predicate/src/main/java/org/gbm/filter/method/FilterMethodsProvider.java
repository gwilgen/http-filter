package org.gbm.filter.method;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

@Slf4j
public class FilterMethodsProvider {

    Map<String, Method> methods = null;

    private static final FilterMethodsProvider INSTANCE = new FilterMethodsProvider();

    private FilterMethodsProvider() {}

    public static Method get(String method) {
        if (null == INSTANCE.methods) {
            try {
                INSTANCE.methods = populateMethods();
                INSTANCE.methods.values().stream()
                        .filter(m -> !Modifier.isStatic(m.getModifiers()))
                        .forEach(m -> log.warn("Method '{}' will be called as a pure function", getName(m)));
            } catch (IOException ioe) {
                throw new RuntimeException("Could not populate filter methods", ioe);
            }
        }
        return INSTANCE.methods.get(method);
    }

    private static String getName(Method m) {
        return m.getName();
    }

    private static Map<String, Method> populateMethods() throws IOException {
        return new Reflections(FilterMethodsProvider.class.getClassLoader())
                .getMethodsAnnotatedWith(FilterMethod.class).stream()
                        .collect(Collectors.toMap(FilterMethodsProvider::getName, m -> m));
    }
}
