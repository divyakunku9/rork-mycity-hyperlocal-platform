package com.sheharsaathi.app

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.swipe
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
        // The button stays in the screen tree even when the panel covers it, so always drag down;
        // if the panel is already down this just pans the map a little.
        repeat(2) {
            rule.onRoot().performTouchInput { swipe(Offset(centerX, height * 0.55f), Offset(centerX, height * 0.95f), 400) }
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
    /** Explore's scrolling list of rows (Happening near, Civic, Events, News). */
    private val exploreRows = hasScrollToNodeAction().and(hasAnyDescendant(hasText("See all", substring = true)))

    private fun backToExplore() {
        // The map's search button is always in Explore's tree, even when the rows below are scrolled away.
        repeat(4) { if (exists(mapSearchPill) || exists(hasText("ransit nearby", substring = true))) return; back(); rule.waitForIdle() }
    }
    private fun scrollDown() = rule.onRoot().performTouchInput { swipeUp(durationMillis = 400) }
    private fun scrollTop() = repeat(4) { rule.onRoot().performTouchInput { swipeDown(durationMillis = 250) } }

    private val openProfile = SemanticsMatcher("click label 'Open profile'") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open profile"
    }

    /** The location picker can pop up if GPS puts us outside the city; close it so it doesn't block taps. */
    private val showOnMap = SemanticsMatcher("click label 'Show on map'") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == "Show on map"
    }
    private var placeSaved = false

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
            // Rows fill in (and the list is rebuilt) while live data arrives, so retry a scroll that lands mid-rebuild.
            fun scrollTo(m: SemanticsMatcher) {
                var last: Throwable? = null
                repeat(4) {
                    try {
                        rule.waitForIdle()
                        rule.onAllNodes(exploreRows)[0].performScrollToNode(m)
                        return
                    } catch (t: AssertionError) { last = t; Thread.sleep(1500) }
                }
                throw last!!
            }
            scrollTo(hasText("Happening near", substring = true)); Thread.sleep(1500); shot("explore-near")
            // The civic row only shows when there are open reports nearby.
            if (exists(hasText("Civic issues near", substring = true))) { scrollTo(hasText("Civic issues near", substring = true)); Thread.sleep(1200); shot("explore-civic") }
            scrollTo(hasText("Events near", substring = true).or(hasText("Events in ", substring = true))); Thread.sleep(1500); shot("explore-events")
            scrollTo(hasText("News near", substring = true).or(hasText("News in ", substring = true))); Thread.sleep(1500); shot("explore-news")
            scrollTo(hasText("Happening near", substring = true)); Thread.sleep(800)
        }
        step("Happening near: See all") {
            if (!exists(hasContentDescription("See all: Happening near", substring = true))) {
                rule.onAllNodes(exploreRows)[0].performScrollToNode(hasText("Happening near", substring = true))
            }
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
        step("Save a place: drop a pin on the map and bookmark it") {
            tab("Explore"); Thread.sleep(1000); dismissLocationPicker()
            showMap()
            // A real long press on an empty part of the map (the system's own gesture, as a finger would do it).
            instrumentation.uiAutomation.executeShellCommand("input swipe 375 1000 375 1000 1500").close()
            waitFor(hasContentDescription("Save place"), 15)
            Thread.sleep(2500) // the pin's address is looked up
            node(hasContentDescription("Save place")).performClick()
            waitFor(hasContentDescription("Remove saved place"), 5)
            placeSaved = true
            shot("map-place-saved")
            back()
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
        step("Save a city news story") {
            waitFor(hasContentDescription("Save story"), 10)
            node(hasContentDescription("Save story")).performScrollTo().performClick()
            waitFor(hasContentDescription("Remove saved story"), 5)
            shot("news-story-saved")
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
        step("Save a listed event") {
            node(hasContentDescription("Save event")).performScrollTo().performClick()
            waitFor(hasContentDescription("Remove saved event"), 5)
            shot("event-saved")
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
        step("Civic Hub: ward from my location") {
            // The emulator's GPS is set to Madhapur.
            waitText("From your location", 30)
            waitText("Ward corporator", 5)
            shot("civic-ward-location")
        }
        step("Civic Hub: choose a ward by hand") {
            node(hasText("Change ward")).performClick()
            waitText("Choose your ward", 10)
            shot("ward-picker")
            node(hasSetTextAction()).performTextInput("Malkajgiri")
            Thread.sleep(800); shot("ward-picker-search")
            node(hasText("Malkajgiri") and !hasSetTextAction()).performClick()
            waitText("You chose this ward", 10)
            waitText("Ward 183", 5)
            shot("civic-ward-chosen")
        }
        step("Civic Hub: trade licence goes to the chosen ward's corporation") {
            node(hasText("Trade License")).performScrollTo().performClick()
            waitText("mmc.telangana.gov.in", 10)
            shot("service-trade-mmc")
            back()
        }
        step("Civic Hub: back to my location's ward") {
            scrollTop()
            node(hasText("Use location")).performClick()
            waitText("From your location", 20)
            shot("civic-ward-location-again")
        }
        step("Civic Hub: service tiles open their options") {
            for ((tile, expect) in listOf(
                "Pay Utility Bills" to "Property tax",
                "Trade License" to "Apply / renew online",
                "Bulk Waste" to "Shamshabad",
                "Tree Fall / Hazard" to "Call HYDRAA",
            )) {
                node(hasText(tile)).performScrollTo().performClick()
                waitText(expect, 10)
                shot("service-" + tile.lowercase().replace(Regex("[^a-z]+"), "-").trim('-'))
                back()
            }
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
        }
        step("Report: public responses (still not fixed, then fixed)") {
          try {
            // It is the tester's own report: they can say how it is now, but not confirm it.
            node(hasText("WHAT RESIDENTS SAY")).performScrollTo()  // section labels are shown in capitals
            check(!exists(hasText("I see this too"))) { "the reporter was offered 'I see this too' on their own report" }
            node(hasText("Still not fixed")).performScrollTo().performClick()
            waitText("Your response: Still not fixed", 10)
            Thread.sleep(2500) // the server's totals arrive
            check(exists(hasText("Only you so far"))) { "the response wasn't counted" }
            shot("issue-response-still-not-fixed")
            // Changing the response moves it.
            node(hasText("It's fixed now")).performScrollTo().performClick()
            waitText("Your response: It's fixed now", 10)
            waitText("1 resident says this is fixed", 10)
            Thread.sleep(2500)
            shot("issue-response-fixed")
            // Tapping it again takes it back.
            node(hasText("It's fixed now")).performScrollTo().performClick()
            rule.waitUntil(10_000) { !exists(hasText("Your response:", substring = true)) }
            Thread.sleep(2500)
            node(hasText("Still not fixed")).performScrollTo().performClick()
            waitText("Your response: Still not fixed", 10)
            Thread.sleep(2500)
            shot("issue-response-final")
          } finally { back() }  // leave the report even if a check fails, so later steps start from the tabs
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
        step("Help chat answers questions") {
            tab("Explore"); Thread.sleep(1000)
            node(openProfile).performClick()
            waitText("Sign out", 10)
            node(hasText("Help & FAQs")).performScrollTo().performClick()
            waitText("Browse by topic", 10)
            shot("help-welcome")
            clickText("How do I report a civic issue?")
            waitText("Open Report", 10)
            shot("help-answer")
            node(hasSetTextAction()).performTextInput("otp not received")
            node(hasContentDescription("Send question")).performClick()
            waitText("Resend code", 10)
            shot("help-typed-question")
            // The keyboard is open, so leave the chat with its back arrow (the system key would only close the keyboard).
            node(hasContentDescription("Back")).performClick(); Thread.sleep(1000)
            waitText("Sign out", 10)
            // Profile reopens scrolled down with its own arrow off screen, so use the system key here.
            back()
            waitFor(hasText("Explore"), 10)
        }
        step("Saved story, event and place show in Profile") {
            tab("Explore"); Thread.sleep(1000)
            node(openProfile).performClick()
            waitText("Sign out", 10)
            node(hasContentDescription("Saved news: 1")).performScrollTo()
            check(exists(hasContentDescription("Remove saved story"))) { "saved story isn't listed" }
            check(exists(hasContentDescription("Remove saved event"))) { "saved event isn't listed" }
            check(!exists(hasContentDescription("Bookmarked events: 0"))) { "event count didn't change" }
            if (placeSaved) check(exists(hasContentDescription("Saved places: 1"))) { "saved place isn't counted" }
            shot("profile-saved")
            // Removing from the Profile takes it off the list and the count.
            node(hasContentDescription("Remove saved story")).performScrollTo().performClick()
            waitFor(hasContentDescription("Saved news: 0"), 5)
            shot("profile-saved-story-removed")
            if (placeSaved) {
                // A saved place opens on the Explore map.
                node(showOnMap).performScrollTo().performClick()
                waitText("Get me there", 10); Thread.sleep(2000)
                shot("saved-place-on-map")
                back()
            } else back()
            waitFor(hasText("Explore"), 10)
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
