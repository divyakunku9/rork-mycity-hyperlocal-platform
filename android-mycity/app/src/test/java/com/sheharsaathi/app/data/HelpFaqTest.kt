package com.sheharsaathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpFaqTest {
    private fun answered(question: String) = HelpBot.reply(question).faq?.id

    @Test fun everydayQuestionsGetTheRightAnswer() {
        val expected = mapOf(
            "how do i report a pothole" to "report-how",
            "garbage not collected in my street" to "report-how",
            "track my complaint" to "report-track",
            "i see this too" to "report-confirm",
            "how do i say a pothole is already fixed" to "report-confirm",
            "the problem is getting worse" to "report-confirm",
            "who is my corporator" to "ward-find",
            "HOW DO I CHANGE MY WARD?" to "ward-find",
            "pay property tax" to "services",
            "book tickets for an event" to "events-book",
            "how to host an event" to "events-host",
            "claim offer" to "offers",
            "metro timings" to "transit",
            "plan a trip to charminar" to "plan-trip",
            "air quality" to "weather",
            "otp not received" to "otp-missing",
            "change profile photo" to "edit-profile",
            "turn off notifications" to "notifications",
            "is my data safe" to "privacy",
            "sign out" to "sign-out",
            "delete my account" to "delete-account",
            "app not loading" to "not-loading",
            "location not working" to "location-off",
            "change city" to "change-city",
            "is it free" to "free",
            "which cities" to "cities",
            "what is shehar saathi" to "what-is",
        )
        expected.forEach { (question, id) -> assertEquals(question, id, answered(question)) }
    }

    @Test fun unclearQuestionsOfferChoicesInsteadOfGuessing() {
        val r = HelpBot.reply("how to login")
        assertNull(r.faq)
        assertTrue(r.suggestions.any { it.id == "sign-in" })
    }

    @Test fun unrelatedQuestionsAdmitItAndOfferTopics() {
        val r = HelpBot.reply("what's the cricket score")
        assertNull(r.faq)
        assertTrue(r.showTopics)
        assertTrue(r.suggestions.isEmpty())
    }

    @Test fun greetingsAndThanksAreNotTreatedAsQuestions() {
        assertTrue(HelpBot.reply("hi").showTopics)
        assertNull(HelpBot.reply("hello there").faq)
        assertTrue(HelpBot.reply("ok thank you").text.startsWith("Happy to help"))
        assertTrue(HelpBot.reply("   ").showTopics)
    }

    @Test fun aGreetingWithARealQuestionStillGetsAnswered() {
        assertEquals("report-how", answered("hi, how do I report a pothole"))
    }

    @Test fun everyFaqIsReachableAndWellFormed() {
        assertEquals(HelpFaqs.all.size, HelpFaqs.all.map { it.id }.toSet().size)
        HelpFaqs.all.forEach { f ->
            assertTrue(f.id, f.answer.isNotBlank() && f.keywords.isNotEmpty())
            // Asking the question exactly as written must lead to its own answer.
            assertEquals(f.question, f.id, answered(f.question))
        }
        HelpTopic.values().forEach { assertTrue(it.label, HelpFaqs.byTopic(it).isNotEmpty()) }
        assertEquals(4, HelpFaqs.starters.size)
    }

    @Test fun shortcutsPointAtRealScreens() {
        val routes = setOf("explore", "news", "report", "events", "civic", "plan", "transit", "weather", "reports?mine=true", "reports?mine=false")
        HelpFaqs.all.mapNotNull { it.link }.forEach { assertTrue(it.route, it.route in routes) }
    }
}
