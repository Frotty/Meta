package de.fatox.meta.injection

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * String-based [qualifier][Qualifier].
 *
 *
 * Example usage:
 *
 * <pre>
 * public class Car {
 * &#064;Inject **@Named("driver")** Seat driverSeat;
 * &#064;Inject **@Named("passenger")** Seat passengerSeat;
 * ...
 * }</pre>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class Named(
    /** The name.  */
    val value: String = ""
)