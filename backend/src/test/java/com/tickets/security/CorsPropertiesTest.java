package com.tickets.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void acceptsASingleExplicitOrigin() {
        CorsProperties properties = new CorsProperties(" http://localhost:5173 ");

        assertEquals("http://localhost:5173", properties.allowedOrigin());
    }

    @Test
    void rejectsWildcardOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new CorsProperties("*"));
        assertThrows(IllegalArgumentException.class, () -> new CorsProperties(" * "));
    }

    @Test
    void rejectsWildcardInOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new CorsProperties("https://*.example.com"));
    }

    @Test
    void rejectsMultipleOrigins() {
        assertThrows(IllegalArgumentException.class,
                () -> new CorsProperties("http://localhost:5173,https://tickets.example.com"));
    }
}
