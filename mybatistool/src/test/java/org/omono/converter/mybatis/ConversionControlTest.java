package org.omono.converter.mybatis;

import org.junit.Test;
import org.omono.converter.mybatis.ConversionControl;

import static org.junit.Assert.*;

/**
 * Tests for ConversionControl ThreadLocal mechanism.
 */
public class ConversionControlTest {
    
    @Test
    public void testShouldSkipDefaultValue() {
        // Default should be false (no skip)
        assertFalse("Should not skip by default", ConversionControl.shouldSkip());
    }
    
    @Test
    public void testBeginEndSkip() {
        assertFalse("Should not skip initially", ConversionControl.shouldSkip());
        
        ConversionControl.beginSkip();
        assertTrue("Should skip after beginSkip", ConversionControl.shouldSkip());
        
        ConversionControl.endSkip();
        assertFalse("Should not skip after endSkip", ConversionControl.shouldSkip());
    }
    
    @Test
    public void testSkipRunnable() {
        assertFalse("Should not skip before", ConversionControl.shouldSkip());
        
        ConversionControl.skip(() -> {
            assertTrue("Should skip inside runnable", ConversionControl.shouldSkip());
        });
        
        assertFalse("Should not skip after", ConversionControl.shouldSkip());
    }
    
    @Test
    public void testSkipSupplier() {
        assertFalse("Should not skip before", ConversionControl.shouldSkip());
        
        String result = ConversionControl.skip(() -> {
            assertTrue("Should skip inside supplier", ConversionControl.shouldSkip());
            return "test";
        });
        
        assertEquals("test", result);
        assertFalse("Should not skip after", ConversionControl.shouldSkip());
    }
    
    @Test
    public void testSkipWithException() {
        assertFalse("Should not skip before", ConversionControl.shouldSkip());
        
        try {
            ConversionControl.skip(() -> {
                assertTrue("Should skip inside", ConversionControl.shouldSkip());
                throw new RuntimeException("Test exception");
            });
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }
        
        // Should still be cleared after exception
        assertFalse("Should not skip after exception", ConversionControl.shouldSkip());
    }
    
    @Test
    public void testNestedSkip() {
        assertEquals(0, ConversionControl.getSkipDepth());
        
        ConversionControl.beginSkip();
        assertEquals(1, ConversionControl.getSkipDepth());
        assertTrue("Should skip at level 1", ConversionControl.shouldSkip());
        
        ConversionControl.beginSkip();
        assertEquals(2, ConversionControl.getSkipDepth());
        assertTrue("Should still skip at level 2", ConversionControl.shouldSkip());
        
        ConversionControl.endSkip();
        assertEquals(1, ConversionControl.getSkipDepth());
        assertTrue("Should still skip after level 2 end", ConversionControl.shouldSkip());
        
        ConversionControl.endSkip();
        assertEquals(0, ConversionControl.getSkipDepth());
        assertFalse("Should not skip after all ends", ConversionControl.shouldSkip());
    }
    
    @Test
    public void testClear() {
        ConversionControl.beginSkip();
        assertTrue("Should skip after beginSkip", ConversionControl.shouldSkip());
        
        ConversionControl.clear();
        assertFalse("Should not skip after clear", ConversionControl.shouldSkip());
    }
    
    @Test
    public void testMultipleThreads() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            ConversionControl.beginSkip();
            assertTrue("Thread 1 should skip", ConversionControl.shouldSkip());
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
            
            assertTrue("Thread 1 should still skip", ConversionControl.shouldSkip());
            ConversionControl.endSkip();
        });
        
        Thread t2 = new Thread(() -> {
            assertFalse("Thread 2 should not skip initially", ConversionControl.shouldSkip());
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // ignore
            }
            
            ConversionControl.beginSkip();
            assertTrue("Thread 2 should skip after beginSkip", ConversionControl.shouldSkip());
            ConversionControl.endSkip();
        });
        
        // Main thread should not be affected
        assertFalse("Main thread should not skip", ConversionControl.shouldSkip());
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        // Main thread still not affected
        assertFalse("Main thread should still not skip", ConversionControl.shouldSkip());
    }
}
