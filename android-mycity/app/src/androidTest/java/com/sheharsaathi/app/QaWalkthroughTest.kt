package com.sheharsaathi.app

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Walks through the app the way a resident would, against the live backend, using the
 * Supabase test number (no SMS is sent). Saves a screenshot after every step plus a
 * report of which steps passed. A failed step is recorded and the walk carries on.
 *
 * Results: /sdcard/Android/data/com.sheharsaathi.app/files/qa
 */
@RunWith(AndroidJUnit4::class)
class QaWalkthroughTest {

    @get:Rule(order = 0)
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val outDir: File by lazy {
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa").apply { deleteRecursively(); mkdirs() }
    }
    private val report = StringBuilder()
    private var shotNo = 0
    private var failures = 0

    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(1500) // the emulator's screen capture lags the app by a frame or two
        runCatching {
            val bmp: Bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
            shotNo++
            File(outDir, "%02d-%s.png".format(shotNo, name)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
        }
    }

    private fun step(name: String, block: () -> Unit) {
        val t0 = System.currentTimeMillis()
        try {
            block()
            report.append("PASS  $name (${System.currentTimeMillis() - t0} ms)\n")
        } catch (t: Throwable) {
            failures++
            report.append("FAIL  $name: ${t.javaClass.simpleName}: ${t.message?.lineSequence()?.take(6)?.joinToString(" | ")}\n")
            shot("FAIL-" + name.lowercase().replace(Regex("[^a-z0-9]+"), "-").take(40))
        }
        File(outDir, "report.txt").writeText(report.toString())
    }

    private fun exists(m: SemanticsMatcher) =
        rule.onAllNodes(m, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
            rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(m: SemanticsMatcher, seconds: Long = 20) =
        rule.waitUntil(seconds * 1000) { exists(m) }

    private fun waitText(text: String, seconds: Long = 20) = waitFor(hasText(text, substring = true), seconds)

    private fun node(m: SemanticsMatcher): SemanticsNodeInteraction =
        if (rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty()) rule.onAllNodes(m)[0]
        else rule.onAllNodes(m, useUnmergedTree = true)[0]

    private fun clickText(text: String) = node(hasText(text, substring = true)).performClick()
    private fun tab(label: String) = node(hasText(label)).performClick()
    private fun back() { instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); Thread.sleep(700) }
    private val precisionTag = hasText("Exact door").or(hasText("Building")).or(hasText("Street")).or(hasText("Area"))

    /** Types an address into map search, screenshots the results, opens the top one and screenshots the map. */
    private val mapSearchPill = SemanticsMatcher("click label 'Search the map'") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Search the map" }

    /** The Explore panel can be pulled up over the map; drag it back down until the map's search button shows. */
    private fun showMap() {
        repeat(4) {
            if (exists(mapSearchPill)) return
            rule.onRoot().performTouchInput { swipe(Offset(centerX, height * 0.45f), Offset(centerX, height * 0.95f), 400) }
            rule.waitForIdle(); Thread.sleep(500)
        }
    }

    private fun searchAddress(label: String, address: String) {
        backToExplore()
        showMap()
        node(mapSearchPill).performClick()
        waitText("Find on map", 10)
        node(hasSetTextAction()).performTextClearance()
        node(hasSetTextAction()).performTextInput(address)
        waitFor(precisionTag, 30)
        Thread.sleep(1500) // let the step-back searches finish so the list is final
        shot("addr-$label")
        rule.onAllNodes(precisionTag)[0].performClick()
        waitText("Navigate", 10)
        Thread.sleep(2500) // map flies to the spot and loads tiles
        shot("addr-$label-map")
        back()
    }

    /** Presses back until the Explore screen is showing (the planner's keyboard can eat one press). */
    private fun backToExplore() {
        repeat(4) { if (exists(hasText("ransit nearby", substring = true))) return; back(); rule.waitForIdle() }
    }
    private fun scrollDown() = rule.onRoot().performTouchInput { swipeUp(durationMillis = 400) }
    private fun scrollTop() = repeat(4) { rule.onRoot().performTouchInput { swipeDown(durationMillis = 250) } }

    private val openProfile = SemanticsMatcher("click label 'Open profile'") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open profile"
    }

    /** The location picker can pop up if GPS puts us outside the city; close it so it doesn't block taps. */
    private fun dismissLocationPicker() {
        if (exists(hasText("Pick your area", substring = true)) || exists(hasText("Set as my location", substring = true))) back()
    }

    @Test
    fun walkthrough() {
        step("Splash shows") { shot("splash") }

        step("Sign-in screen") {
            waitText("Sign in to Shehar Saathi", 15)
            shot("sign-in")
        }
        step("Enter name and test number, send code") {
            node(hasSetTextAction() and hasText("Your name", substring = true)).performTextInput("QA Tester")
            node(hasSetTextAction() and hasText("Mobile number", substring = true)).performTextInput("9999900002")
            shot("sign-in-filled")
            clickText("Send OTP")
            waitText("Enter the 6-digit code", 30)
            shot("otp")
        }
        step("Wrong code is rejected") {
            node(hasContentDescription("One-time code, 6 digits")).performTextInput("000000")
            waitText("expired", 20)
            shot("otp-wrong-code")
        }
        step("Right code signs in") {
            node(hasContentDescription("One-time code, 6 digits")).performTextInput("135790")
            waitText("where do you live", 30)
            shot("choose-city")
        }
        step("Pick Hyderabad") {
            node(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("Hyderabad", substring = true))
            node(hasContentDescription("Hyderabad", substring = true)).performClick()
            shot("hyderabad-selected")
            clickText("Continue to Hyderabad")
            waitText("Explore", 20)
            Thread.sleep(6000) // live content, weather, map tiles
            dismissLocationPicker()
            shot("explore")
        }
        step("Explore scrolls") {
            scrollDown(); shot("explore-scroll-1")
            scrollDown(); shot("explore-scroll-2")
            scrollDown(); shot("explore-scroll-3")
            scrollTop()
        }
        step("Explore rows: near you, civic, events, news") {
            tab("Explore"); Thread.sleep(1000); dismissLocationPicker()
            waitFor(hasContentDescription("Feed ", substring = true), 30)
            // Find the sheet's list afresh each time: it's rebuilt as rows fill in.
            fun scrollTo(m: SemanticsMatcher) =
                rule.onAllNodes(hasScrollToNodeAction().and(hasAnyDescendant(hasText("Civic issues"))))[0].performScrollToNode(m)
            scrollTo(hasText("Happening near", substring = true)); Thread.sleep(1500); shot("explore-near")
            scrollTo(hasText("Civic issues")); Thread.sleep(1200); shot("explore-civic")
            scrollTo(hasText("Events")); Thread.sleep(1500); shot("explore-events")
            scrollTo(hasText("News")); Thread.sleep(1500); shot("explore-news")
            scrollTo(hasText("Happening near", substring = true)); Thread.sleep(800)
        }
        step("Happening near: See all") {
            node(hasContentDescription("See all: Happening near", substring = true)).performClick()
            waitText("Within 5 km of you", 10); Thread.sleep(1500); shot("nearby-events")
            node(hasText("Civic (", substring = true)).performClick(); Thread.sleep(1000); shot("nearby-civic")
            node(hasText("News (", substring = true)).performClick(); Thread.sleep(1000); shot("nearby-news")
            back(); Thread.sleep(1000)   // one press: Near page → Explore (never risk leaving the app)
        }
        step("Weather screen from the header chip") {
            tab("Explore"); Thread.sleep(1000); dismissLocationPicker()
            val chip = SemanticsMatcher("click label 'Open weather'") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open weather" }
            waitFor(chip, 20)
            node(chip).performClick()
            try {
                waitText("Next 24 hours", 30)
                shot("weather")
                check(exists(hasText("Air quality", substring = true, ignoreCase = true))) { "no air quality" }
                scrollDown(); shot("weather-scroll-1")
                // The daily list sits below the tall air-quality card; scroll straight to it.
                rule.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasText("day forecast", substring = true, ignoreCase = true))
                shot("weather-daily")
                rule.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasText("Sunrise"))
                shot("weather-details")
            } finally { backToExplore() }
        }
        step("Map search: full address with door number") {
            tab("Explore"); Thread.sleep(1000); dismissLocationPicker()
            showMap()
            node(mapSearchPill).performClick()
            waitText("Find on map", 10)
            node(hasSetTextAction()).performTextInput("8-2-293/82/A, Road No 36, Jubilee Hills")
            waitFor(hasText("Exact door", substring = true).or(hasText("Building")).or(hasText("Street")), 30)
            shot("map-search-address")
            node(hasText("Jubilee Hills Road No 36", substring = true)).performClick()
            waitText("Navigate", 10); shot("map-search-picked")
            back()
        }
        // More door numbers: four that exist in the map data, two that don't (to check the honest fallback).
        listOf(
            "banjara" to "6-3-596/23/1, Venkata Ramana Colony, Banjara Hills",
            "jubilee-rd2" to "8-2-120/76/5, Road No 2, Jubilee Hills",
            "begumpet" to "1-10-176, Begumpet Main Road, Begumpet",
            "nallakunta" to "2-1-565/6/A, Nallakunta Main Road",
            "dilsukhnagar" to "H.No 16-11-740/5, Dilsukhnagar",
            "flat-kondapur" to "Flat 302, Aparna Towers, Kondapur",
        ).forEach { (label, address) -> step("Map search: $address") { searchAddress(label, address) } }
        step("News Hub: city headlines load") {
            tab("News Hub")
            waitFor(hasContentDescription("Story:", substring = true), 30)
            shot("news"); scrollDown(); shot("news-scroll"); scrollTop()
        }
        step("News topics filter") {
            node(hasText("Crime & Safety")).performClick()
            rule.waitForIdle(); Thread.sleep(1500)
            check(!exists(hasText("For you").and(SemanticsMatcher("selected") { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Selected) == true }))) { "topic chip didn't select" }
            check(exists(hasText("Crime & Safety", substring = true))) { "no crime stories shown" }
            shot("news-topic")
            node(hasText("For you")).performClick(); Thread.sleep(500)
        }
        step("News community tab") {
            node(hasText("Community")).performClick()
            waitText("Civic Bulletins", 10)
            Thread.sleep(1500); shot("news-community")
            node(hasText("City news")).performClick(); Thread.sleep(500)
        }
        step("Events: listings from ticketing sites") {
            tab("Events")
            waitFor(hasContentDescription("Listed event:", substring = true), 30)
            shot("events"); scrollDown(); shot("events-scroll"); scrollTop()
        }
        step("Events: listing details and book link") {
            node(hasContentDescription("Listed event:", substring = true)).performClick()
            waitFor(hasText("Booking, payment and refunds are handled by", substring = true), 10)
            shot("event-listing-sheet")
            check(exists(hasText(" on District", substring = true)) || exists(hasText(" on Eventbrite", substring = true)) ||
                exists(hasText(" on Meetup", substring = true)) || exists(hasText(" on AllEvents", substring = true))) { "no book link" }
            back()
        }
        step("Events: topic and day filters") {
            if (exists(hasText("Music"))) { node(hasText("Music")).performClick(); Thread.sleep(800) }
            node(hasText("This weekend")).performClick(); Thread.sleep(800); shot("events-filtered")
            node(hasText("Any day")).performClick(); if (exists(hasText("All"))) node(hasText("All")).performClick()
        }
        step("Events: Community tab with Add Event") {
            node(hasText("Community")).performClick(); Thread.sleep(1200)
            waitText("Add Event", 10); shot("events-community")
        }
        step("Add Event sheet") {
            scrollTop()
            clickText("Add Event")
            Thread.sleep(1500); shot("add-event")
            // The sheet is a second window, so swipe inside it rather than on the main screen.
            rule.onAllNodes(isRoot()).onLast().performTouchInput { swipeUp(durationMillis = 400) }
            shot("add-event-scroll")
            back()
        }
        step("Civic Hub tab") {
            tab("Civic Hub"); Thread.sleep(2500); shot("civic")
            scrollDown(); shot("civic-scroll-1"); scrollDown(); shot("civic-scroll-2"); scrollDown(); shot("civic-scroll-3")
            scrollTop()
        }
        step("Report tab") { tab("Report"); Thread.sleep(2000); shot("report"); scrollDown(); shot("report-scroll") }
        step("Report: too-short description is blocked") {
            node(hasText("Submit Report")).performScrollTo().performClick()
            Thread.sleep(800)
            waitText("Add at least a short sentence", 5)
            shot("report-validation")
        }
        step("Report: submit a pothole report") {
            node(hasSetTextAction()).performScrollTo().performTextInput("QA test report: large pothole near the bus stop, please ignore.")
            node(hasText("Submit Report")).performScrollTo().performClick()
            waitText("Pothole reported", 20)
            Thread.sleep(4000) // upload + insert
            shot("issue-detail")
            back()
        }
        step("My filed issues list") {
            tab("Civic Hub"); Thread.sleep(1500)
            node(hasText("My filed issues")).performScrollTo().performClick()
            Thread.sleep(2000); shot("my-reports")
            back()
        }
        step("Explore: planner") {
            tab("Explore"); Thread.sleep(1500); dismissLocationPicker()
            node(hasText("ransit nearby", substring = true)).performScrollTo().performClick()
            Thread.sleep(1500)
            clickText("Plan trip")
            Thread.sleep(2000); shot("planner")
            backToExplore()
        }
        step("Live transit board") {
            tab("Explore"); Thread.sleep(1000)
            node(hasText("ransit nearby", substring = true)).performScrollTo().performClick()
            Thread.sleep(2000); shot("transit-board"); scrollDown(); shot("transit-board-scroll")
            backToExplore()
        }
        step("Profile") {
            tab("Explore"); Thread.sleep(1000)
            node(openProfile).performClick()
            waitText("Sign out", 10)
            shot("profile"); scrollDown(); shot("profile-scroll")
            back()
        }
        step("Screen recreate keeps the session and the new report") {
            rule.activityRule.scenario.recreate()
            Thread.sleep(5000)
            dismissLocationPicker()
            shot("after-recreate")
            tab("Civic Hub"); Thread.sleep(3000)
            node(hasText("My filed issues")).performScrollTo().performClick()
            waitText("Pothole reported", 15)
            shot("my-reports-after-recreate")
        }

        step("Clean up: delete the test report from the live backend") {
            // The walkthrough runs against production, so it removes what it created.
            val sb = com.sheharsaathi.app.data.backend.Supabase(instrumentation.targetContext)
            val uid = sb.userId ?: error("not signed in")
            val r = kotlinx.coroutines.runBlocking { sb.delete("civic_issues", "reporter_id" to "eq.$uid") }
            if (r is com.sheharsaathi.app.data.backend.Res.Err) error("delete failed: ${r.message}")
        }

        File(outDir, "report.txt").writeText(report.toString() + "\n$failures step(s) failed\n")
        if (failures > 0) throw AssertionError("$failures walkthrough step(s) failed:\n$report")
    }
}
