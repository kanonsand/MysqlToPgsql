package org.omono.converter.mybatis;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-local context for controlling SQL conversion behavior.
 * 
 * Use cases:
 * 1. Skip conversion for specific method calls
 * 2. Disable conversion temporarily for raw SQL execution
 * 3. AOP integration for annotation-based control
 * 
 * Example usage:
 * <pre>
 * // Skip conversion for a block of code
 * ConversionControl.skip(() -> {
 *     mapper.executeRawSql();
 * });
 * 
 * // Or manually control
 * ConversionControl.beginSkip();
 * try {
 *     mapper.executeRawSql();
 * } finally {
 *     ConversionControl.endSkip();
 * }
 * 
 * // Check if should skip
 * if (ConversionControl.shouldSkip()) {
 *     // ...
 * }
 * </pre>
 * 
 * AOP integration example (Spring):
 * <pre>
 * &#64;Around("@annotation(skipConversion)")
 * public Object aroundSkipConversion(ProceedingJoinPoint pjp, SkipConversion skipConversion) {
 *     ConversionControl.beginSkip();
 *     try {
 *         return pjp.proceed();
 *     } finally {
 *         ConversionControl.endSkip();
 *     }
 * }
 * </pre>
 */
public class ConversionControl {
    
    // Use counter to support nested skip calls
    private static final ThreadLocal<AtomicInteger> SKIP_COUNTER = ThreadLocal.withInitial(() -> new AtomicInteger(0));
    
    /**
     * Execute a runnable with conversion skipped.
     * 
     * @param runnable the code to execute
     */
    public static void skip(Runnable runnable) {
        beginSkip();
        try {
            runnable.run();
        } finally {
            endSkip();
        }
    }
    
    /**
     * Execute a supplier with conversion skipped.
     * 
     * @param supplier the code to execute
     * @return the result
     */
    public static <T> T skip(java.util.function.Supplier<T> supplier) {
        beginSkip();
        try {
            return supplier.get();
        } finally {
            endSkip();
        }
    }
    
    /**
     * Begin skipping conversion for current thread.
     * Must be paired with endSkip() in a finally block.
     * Supports nested calls.
     */
    public static void beginSkip() {
        SKIP_COUNTER.get().incrementAndGet();
    }
    
    /**
     * End skipping conversion for current thread.
     * Should be called in a finally block after beginSkip().
     * Supports nested calls - only clears when counter reaches 0.
     */
    public static void endSkip() {
        AtomicInteger counter = SKIP_COUNTER.get();
        int value = counter.decrementAndGet();
        if (value < 0) {
            // Reset to 0 if somehow went negative
            counter.set(0);
        }
    }
    
    /**
     * Check if conversion should be skipped for current thread.
     * 
     * @return true if conversion should be skipped (counter > 0)
     */
    public static boolean shouldSkip() {
        return SKIP_COUNTER.get().get() > 0;
    }
    
    /**
     * Get the current skip depth (for debugging).
     * 
     * @return the number of beginSkip() calls without matching endSkip()
     */
    public static int getSkipDepth() {
        return SKIP_COUNTER.get().get();
    }
    
    /**
     * Clear any skip flag for current thread.
     * Safe to call even if no flag is set.
     * Use with caution - this breaks the nesting contract.
     */
    public static void clear() {
        SKIP_COUNTER.get().set(0);
    }
}
