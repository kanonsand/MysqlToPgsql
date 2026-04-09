package org.example.server.aop;

import org.omono.converter.mybatis.ConversionControl;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that skips SQL conversion for methods annotated with @SkipConversion.
 * 
 * Automatically registered as a Spring bean via @Component.
 */
@Aspect
@Component
public class SkipConversionAspect {
    
    /**
     * Around advice that skips conversion for annotated methods.
     */
    @Around("@annotation(skipConversion)")
    public Object aroundSkipConversion(ProceedingJoinPoint pjp, SkipConversion skipConversion) throws Throwable {
        ConversionControl.beginSkip();
        try {
            return pjp.proceed();
        } finally {
            ConversionControl.endSkip();
        }
    }
}
