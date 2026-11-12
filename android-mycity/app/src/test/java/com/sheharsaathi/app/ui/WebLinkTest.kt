package com.sheharsaathi.app.ui

import com.sheharsaathi.app.ui.components.isWebLink
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Links in news stories and council pages come from outside, so only web addresses open. */
class WebLinkTest {
    @Test fun ordinaryWebAddressesOpen() {
        listOf(
            "https://www.ghmc.gov.in",
            "http://example.com/story?id=4#top",
            "HTTPS://Www.TheHindu.com/news/cities/Hyderabad/",
            " https://example.com/a ",
        ).forEach { assertTrue(it, isWebLink(it)) }
    }

    @Test fun otherSchemesAndJunkDoNot() {
        listOf(
            "intent://scan/#Intent;scheme=zxing;end",
            "file:///data/data/com.sheharsaathi.app/shared_prefs/mycity_auth.xml",
            "javascript:alert(1)",
            "content://com.android.providers/1",
            "tel:+919999900002",
            "market://details?id=com.sheharsaathi.app",
            "https://",
            "http:// example.com",
            "not a link",
            "",
        ).forEach { assertFalse(it, isWebLink(it)) }
    }
}
