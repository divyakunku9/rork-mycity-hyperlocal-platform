package com.sheharsaathi.app.data

/** The rules for public responses to a civic report. No Android here, so it is unit tested. */
object IssueResponses {

    /**
     * The responses this person can give. The reporter can't confirm their own report, and once a
     * report is marked resolved the question is only whether it really is fixed.
     */
    fun choices(issue: CivicIssue): List<IssueResponse> =
        if (issue.isMine || issue.status == IssueStatus.RESOLVED) IssueResponse.entries.filter { it != IssueResponse.SEEN }
        else IssueResponse.entries.toList()

    /**
     * What tapping [tapped] means when the person's current response is [current]:
     * tapping the response you already gave takes it back.
     */
    fun next(current: IssueResponse?, tapped: IssueResponse): IssueResponse? = if (current == tapped) null else tapped

    /**
     * The report as it looks once this person's response changes from [old] to [new]. Shown at once,
     * before the server replies with the exact totals.
     */
    fun apply(issue: CivicIssue, old: IssueResponse?, new: IssueResponse?): CivicIssue {
        if (old == new) return issue
        var counts = issue.responses
        if (old != null) counts = counts.plus(old, -1)
        if (new != null) counts = counts.plus(new, 1)
        // Confirmations set a report's priority: everyone but the reporter who says the problem is there.
        val confirming = if (issue.isMine) 0 else (if (new?.confirms == true) 1 else 0) - (if (old?.confirms == true) 1 else 0)
        return issue.copy(
            responses = counts,
            upvotes = (issue.upvotes + confirming).coerceAtLeast(0),
            lastResponse = new ?: issue.lastResponse,
            lastResponseAgo = if (new != null) "Just now" else issue.lastResponseAgo,
        )
    }

    enum class Tone { WARNING, GOOD }

    data class Headline(val text: String, val tone: Tone)

    private fun residents(n: Int) = if (n == 1) "1 resident says" else "$n residents say"

    /** The one thing worth calling out about what residents are saying, if anything. */
    fun headline(issue: CivicIssue): Headline? {
        val c = issue.responses
        return when {
            issue.status == IssueStatus.RESOLVED && c.notFixed > 0 ->
                Headline("Marked resolved, but ${residents(c.notFixed)} it isn't fixed.", Tone.WARNING)
            issue.status == IssueStatus.RESOLVED && c.fixed > 0 ->
                Headline("${residents(c.fixed)} it's fixed.", Tone.GOOD)
            c.fixed > 0 && c.fixed >= c.notFixed ->
                Headline("${residents(c.fixed)} this is fixed. Waiting for the authority to confirm.", Tone.GOOD)
            c.worse > 0 ->
                Headline("${residents(c.worse)} it's getting worse.", Tone.WARNING)
            else -> null
        }
    }

    /** "3 residents", "1 resident" or a nudge when nobody has said it yet. */
    fun countLabel(n: Int): String = when (n) {
        0 -> "No one yet"
        1 -> "1 resident"
        else -> "$n residents"
    }
}
