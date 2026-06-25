package com.example.randomgallery.android.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlConfigTest {

    @Test
    fun `sanitize trims and keeps trailing slash`() {
        assertEquals(
            "http://192.168.1.7:8086/",
            BaseUrlConfig.sanitize("  http://192.168.1.7:8086  ")
        )
    }

    @Test
    fun `sanitize rejects unsupported schemes`() {
        assertNull(BaseUrlConfig.sanitize("ftp://192.168.1.7:8086"))
    }

    @Test
    fun `resolve uses fallback when custom url is blank`() {
        assertEquals(
            "http://10.0.2.2:8086/",
            BaseUrlConfig.resolve(custom = "   ", fallback = "http://10.0.2.2:8086/")
        )
    }
}
