package de.fatox.meta.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * String-based {@linkplain Qualifier qualifier}.
 *
 * <p>Example usage:
 *
 * <pre>
 *   public class Car {
 *     &#064;Inject <b>@Named("driver")</b> Seat driverSeat;
 *     &#064;Inject <b>@Named("passenger")</b> Seat passengerSeat;
 *     ...
 *   }</pre>
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface Named {

    /** The name. */
    String value() default "";
}