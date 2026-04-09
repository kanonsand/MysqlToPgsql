package org.example.server.aop;

import java.lang.annotation.*;

/**
 * Annotation to skip SQL conversion for annotated methods.
 * 
 * Use this annotation on methods that should execute SQL without conversion.
 * Works with SkipConversionAspect.
 * 
 * Example:
 * <pre>
 * &#64;SkipConversion
 * public void executeRawQuery() {
 *     // SQL will not be converted
 *     jdbcTemplate.execute("SELECT * FROM mysql_table");
 * }
 * 
 * &#64;SkipConversion
 * public List&lt;User&gt; findUsersRaw() {
 *     // MyBatis query will not be converted
 *     return userMapper.findUsersRaw();
 * }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipConversion {
    /**
     * Optional reason for skipping (for documentation).
     */
    String value() default "";
}
