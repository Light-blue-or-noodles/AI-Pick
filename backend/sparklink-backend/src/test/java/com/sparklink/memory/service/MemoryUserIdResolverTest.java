package com.sparklink.memory.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryUserIdResolverTest {

    @Test
    void resolve_withUserId_returnsPrefixedIdentityKey() {
        assertEquals("sparklink:user:10086", MemoryUserIdResolver.resolve(10086L));
    }

    @Test
    void resolve_withNullUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MemoryUserIdResolver.resolve(null));
    }
}
