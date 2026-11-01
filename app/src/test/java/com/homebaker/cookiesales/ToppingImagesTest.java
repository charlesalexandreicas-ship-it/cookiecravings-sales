package com.homebaker.cookiesales;

import org.junit.Test;
import static org.junit.Assert.*;

public class ToppingImagesTest {
    @Test public void containsRequestedToppingsButNoPlainChoice() {
        assertArrayEquals(new String[]{"Chocolate","Marshmallows","S’mores","Cadbury","O-Puff Marshmallow","Oreos","Cream-O"},ToppingImages.LABELS);
        assertEquals(ToppingImages.KEYS.length,ToppingImages.LABELS.length);
        for(String key:ToppingImages.KEYS){assertTrue(ToppingImages.accepts(key));assertFalse("plain".equals(key));assertNotEquals(0,ToppingImages.drawable(key));}
    }
    @Test public void preservesLegacyPlainWithoutCallingItChocolate() {
        assertTrue(ToppingImages.accepts("plain"));
        assertEquals("Choose topping",ToppingImages.label("plain"));
        assertEquals(0,ToppingImages.drawable("plain"));
        assertFalse(ToppingImages.accepts("unknown"));assertFalse(ToppingImages.accepts(null));
        assertEquals(0,ToppingImages.drawable("unknown"));
        for(int i=0;i<ToppingImages.KEYS.length;i++)assertEquals(ToppingImages.LABELS[i],ToppingImages.label(ToppingImages.KEYS[i]));
    }
}
