package io.github.thebusybiscuit.slimefun4.api.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TestApiAnnotations {

    @Test
    void testApiAnnotationContract() {
        assertAnnotationContract(SlimefunAPI.class);
    }

    @Test
    void testInternalAnnotationContract() {
        assertAnnotationContract(SlimefunInternal.class);
    }

    private void assertAnnotationContract(Class<?> annotationType) {
        Retention retention = annotationType.getAnnotation(Retention.class);
        Target target = annotationType.getAnnotation(Target.class);

        assertEquals(RetentionPolicy.CLASS, retention.value());
        assertTrue(Arrays.asList(target.value()).contains(ElementType.TYPE));
        assertTrue(Arrays.asList(target.value()).contains(ElementType.METHOD));
    }
}
