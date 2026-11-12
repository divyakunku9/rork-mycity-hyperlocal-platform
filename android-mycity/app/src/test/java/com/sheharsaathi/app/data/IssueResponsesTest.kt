package com.sheharsaathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueResponsesTest {
    private fun issue(
        status: IssueStatus = IssueStatus.REPORTED, mine: Boolean = false, upvotes: Int = 0,
        responses: ResponseCounts = ResponseCounts(),
    ) = CivicIssue(
        id = "i1", title = "Pothole reported", category = IssueCategory.POTHOLE, location = "Madhapur", description = "Deep pothole",
        authorities = listOf("@GHMCOnline"), urgency = Urgency.MEDIUM, status = status, upvotes = upvotes,
        reportedAgo = "2h ago", isMine = mine, lat = 17.44, lng = 78.39, responses = responses,
    )

    @Test fun aNeighbourCanGiveAnyOfTheFourResponses() {
        assertEquals(IssueResponse.entries.toList(), IssueResponses.choices(issue()))
    }

    @Test fun theReporterCannotConfirmTheirOwnReportButCanSayHowItIsNow() {
        val choices = IssueResponses.choices(issue(mine = true))
        assertFalse(IssueResponse.SEEN in choices)
        assertEquals(listOf(IssueResponse.STILL_THERE, IssueResponse.WORSE, IssueResponse.FIXED), choices)
    }

    @Test fun onAResolvedReportTheQuestionIsWhetherItIsReallyFixed() {
        assertFalse(IssueResponse.SEEN in IssueResponses.choices(issue(status = IssueStatus.RESOLVED)))
    }

    @Test fun tappingYourOwnResponseAgainTakesItBack() {
        assertEquals(IssueResponse.WORSE, IssueResponses.next(null, IssueResponse.WORSE))
        assertEquals(IssueResponse.FIXED, IssueResponses.next(IssueResponse.WORSE, IssueResponse.FIXED))
        assertNull(IssueResponses.next(IssueResponse.FIXED, IssueResponse.FIXED))
    }

    @Test fun aFirstResponseIsCountedAndRaisesPriority() {
        val after = IssueResponses.apply(issue(upvotes = 3, responses = ResponseCounts(seen = 3)), null, IssueResponse.SEEN)
        assertEquals(4, after.responses.seen)
        assertEquals(4, after.upvotes)
        assertEquals(IssueResponse.SEEN, after.lastResponse)
    }

    @Test fun changingAResponseMovesTheCountAndKeepsPriorityWhileTheProblemIsThere() {
        val before = issue(upvotes = 4, responses = ResponseCounts(seen = 4))
        val worse = IssueResponses.apply(before, IssueResponse.SEEN, IssueResponse.WORSE)
        assertEquals(ResponseCounts(seen = 3, worse = 1), worse.responses)
        assertEquals(4, worse.upvotes)
    }

    @Test fun sayingItIsFixedStopsCountingTowardsPriority() {
        val before = issue(upvotes = 4, responses = ResponseCounts(seen = 4))
        val fixed = IssueResponses.apply(before, IssueResponse.SEEN, IssueResponse.FIXED)
        assertEquals(ResponseCounts(seen = 3, fixed = 1), fixed.responses)
        assertEquals(3, fixed.upvotes)
    }

    @Test fun takingAResponseBackUndoesIt() {
        val before = issue(upvotes = 1, responses = ResponseCounts(stillThere = 1))
        val after = IssueResponses.apply(before, IssueResponse.STILL_THERE, null)
        assertEquals(ResponseCounts(), after.responses)
        assertEquals(0, after.upvotes)
    }

    @Test fun theReportersOwnResponseIsShownButDoesNotRaiseTheirOwnPriority() {
        val after = IssueResponses.apply(issue(mine = true, upvotes = 2, responses = ResponseCounts(seen = 2)), null, IssueResponse.WORSE)
        assertEquals(1, after.responses.worse)
        assertEquals(2, after.upvotes)
    }

    @Test fun applyingAndUndoingReturnsTheSameTotals() {
        val before = issue(upvotes = 5, responses = ResponseCounts(seen = 3, stillThere = 1, worse = 1, fixed = 2))
        val there = IssueResponses.apply(before, IssueResponse.STILL_THERE, IssueResponse.FIXED)
        val back = IssueResponses.apply(there, IssueResponse.FIXED, IssueResponse.STILL_THERE)
        assertEquals(before.responses, back.responses)
        assertEquals(before.upvotes, back.upvotes)
        assertEquals(before, IssueResponses.apply(before, IssueResponse.SEEN, IssueResponse.SEEN))
    }

    @Test fun countsNeverGoBelowZero() {
        val after = IssueResponses.apply(issue(), IssueResponse.WORSE, null)
        assertEquals(0, after.responses.worse)
        assertEquals(0, after.upvotes)
    }

    @Test fun aResolvedReportThatResidentsSayIsNotFixedIsCalledOut() {
        val h = IssueResponses.headline(issue(status = IssueStatus.RESOLVED, responses = ResponseCounts(stillThere = 2, worse = 1, fixed = 5)))!!
        assertEquals(IssueResponses.Tone.WARNING, h.tone)
        assertEquals("Marked resolved, but 3 residents say it isn't fixed.", h.text)
    }

    @Test fun residentsSayingAnOpenReportIsFixedIsCalledOut() {
        val h = IssueResponses.headline(issue(responses = ResponseCounts(seen = 4, fixed = 1)))!!
        assertEquals(IssueResponses.Tone.GOOD, h.tone)
        assertEquals("1 resident says this is fixed. Waiting for the authority to confirm.", h.text)
    }

    @Test fun moreSayingNotFixedThanFixedShowsTheWorseningInstead() {
        val h = IssueResponses.headline(issue(responses = ResponseCounts(stillThere = 1, worse = 2, fixed = 1)))!!
        assertEquals("2 residents say it's getting worse.", h.text)
        assertNull(IssueResponses.headline(issue(responses = ResponseCounts(seen = 9, stillThere = 2))))
    }

    @Test fun responseKeysMatchTheDatabase() {
        assertEquals(listOf("seen", "still_there", "worse", "fixed"), IssueResponse.entries.map { it.key })
        assertEquals(IssueResponse.STILL_THERE, IssueResponse.of("still_there"))
        assertNull(IssueResponse.of("something_else"))
        assertNull(IssueResponse.of(null))
        assertTrue(IssueResponse.WORSE.confirms)
        assertFalse(IssueResponse.FIXED.confirms)
    }

    @Test fun countLabels() {
        assertEquals("No one yet", IssueResponses.countLabel(0))
        assertEquals("1 resident", IssueResponses.countLabel(1))
        assertEquals("12 residents", IssueResponses.countLabel(12))
    }
}
