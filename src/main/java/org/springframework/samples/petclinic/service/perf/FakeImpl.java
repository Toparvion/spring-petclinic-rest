package org.springframework.samples.petclinic.service.perf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker for methods whose implementation just simulates some real logic for the sake of example or study case
 * @author Vladimir Plizga
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface FakeImpl {

    String value();
}
