package converter.comparator.context;

import org.junit.jupiter.api.Test;
import org.omono.converter.comparator.context.IdentifierQuotePair;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierQuotePairTest {
    
    @Test
    void testSameCharQuote() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('"');
        
        assertEquals('"', pair.getStart());
        assertEquals('"', pair.getEnd());
    }
    
    @Test
    void testDifferentCharQuote() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('[', ']');
        
        assertEquals('[', pair.getStart());
        assertEquals(']', pair.getEnd());
    }
    
    @Test
    void testMatches_SameChar() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('"');
        
        assertTrue(pair.matches("\"name\""));
        assertTrue(pair.matches("\"user_name\""));
        assertFalse(pair.matches("name"));
        assertFalse(pair.matches("`name`"));
        assertFalse(pair.matches("'name'"));
    }
    
    @Test
    void testMatches_DifferentChar() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('[', ']');
        
        assertTrue(pair.matches("[name]"));
        assertTrue(pair.matches("[user_name]"));
        assertFalse(pair.matches("name"));
        assertFalse(pair.matches("\"name\""));
    }
    
    @Test
    void testMatches_Backtick() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('`');
        
        assertTrue(pair.matches("`name`"));
        assertFalse(pair.matches("\"name\""));
    }
    
    @Test
    void testMatches_TooShort() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('"');
        
        assertFalse(pair.matches(""));
        assertFalse(pair.matches("\""));
        assertFalse(pair.matches("a"));
    }
    
    @Test
    void testMatches_Null() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('"');
        
        assertFalse(pair.matches(null));
    }
    
    @Test
    void testStrip_Matched() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('"');
        
        assertEquals("name", pair.strip("\"name\""));
        assertEquals("user_name", pair.strip("\"user_name\""));
    }
    
    @Test
    void testStrip_NotMatched() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('"');
        
        assertEquals("name", pair.strip("name"));
        assertEquals("`name`", pair.strip("`name`"));
    }
    
    @Test
    void testStrip_DifferentChar() {
        IdentifierQuotePair pair = IdentifierQuotePair.of('[', ']');
        
        assertEquals("name", pair.strip("[name]"));
        assertEquals("\"name\"", pair.strip("\"name\""));
    }
    
    @Test
    void testEquals() {
        IdentifierQuotePair pair1 = IdentifierQuotePair.of('"');
        IdentifierQuotePair pair2 = IdentifierQuotePair.of('"');
        IdentifierQuotePair pair3 = IdentifierQuotePair.of('`');
        IdentifierQuotePair pair4 = IdentifierQuotePair.of('[', ']');
        
        assertEquals(pair1, pair2);
        assertNotEquals(pair1, pair3);
        assertNotEquals(pair1, pair4);
    }
    
    @Test
    void testHashCode() {
        IdentifierQuotePair pair1 = IdentifierQuotePair.of('"');
        IdentifierQuotePair pair2 = IdentifierQuotePair.of('"');
        
        assertEquals(pair1.hashCode(), pair2.hashCode());
    }
    
    @Test
    void testToString() {
        assertEquals("'\"'", IdentifierQuotePair.of('"').toString());
        assertEquals("'`'", IdentifierQuotePair.of('`').toString());
        assertEquals("'['..']'", IdentifierQuotePair.of('[', ']').toString());
    }
}
