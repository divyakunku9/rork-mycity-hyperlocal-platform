package com.sheharsaathi.app.data

/**
 * The in-app help assistant ("Saathi"). Everything runs on the phone: questions are matched
 * against the answers below, so it works offline and nothing the user types leaves the device.
 */
enum class HelpTopic(val label: String) {
    START("Getting started"),
    REPORT("Reporting issues"),
    CIVIC("Civic Hub"),
    EVENTS("Events"),
    NEWS("News & offers"),
    TRAVEL("Getting around"),
    ACCOUNT("Account & privacy"),
    TROUBLE("Something's not working"),
}

/** A shortcut shown under an answer; [route] is a navigation route in AppNavigation. */
data class HelpLink(val label: String, val route: String)

data class Faq(
    val id: String,
    val topic: HelpTopic,
    val question: String,
    val answer: String,
    /** Words and short phrases people use when they mean this question. */
    val keywords: List<String>,
    val link: HelpLink? = null,
)

/** One bubble in the help chat. */
data class HelpMessage(
    val id: Int,
    val fromUser: Boolean,
    val text: String,
    val link: HelpLink? = null,
    val suggestions: List<Faq> = emptyList(),
    val showTopics: Boolean = false,
)

data class HelpReply(
    val text: String,
    val faq: Faq? = null,
    val suggestions: List<Faq> = emptyList(),
    val showTopics: Boolean = false,
)

object HelpFaqs {
    val all: List<Faq> = listOf(
        // ---------------------------------------------------------------- Getting started
        Faq(
            "what-is", HelpTopic.START, "What is Shehar Saathi?",
            "Shehar Saathi is your city companion. In one app you can:\n" +
                "• Explore what's around you on the map\n" +
                "• Read city news and local offers\n" +
                "• Report civic problems like potholes or broken streetlights\n" +
                "• Find and host events\n" +
                "• Reach civic services for your ward\n" +
                "• Plan trips by metro, bus and auto, and check the weather",
            listOf("what is shehar saathi", "what is this app", "what does this app do", "about", "purpose", "features", "use for", "kya hai"),
        ),
        Faq(
            "cities", HelpTopic.START, "Which cities does the app cover?",
            "You can pick from 9 cities: Hyderabad, Bengaluru, Mumbai, Delhi, Chennai, Kolkata, Pune, Kochi and Vijayawada.\n\n" +
                "Hyderabad is the first city with live data (real reports, events, news and ward details). " +
                "The other cities show sample content for now while we get them ready.",
            listOf("cities", "city", "which city", "which cities", "supported", "available", "cover", "hyderabad", "bengaluru", "bangalore", "mumbai", "delhi", "chennai", "kolkata", "pune", "kochi", "vijayawada", "sample", "demo data", "live"),
        ),
        Faq(
            "change-city", HelpTopic.START, "How do I change my city?",
            "Open your Profile and tap the city card (it says \"Change\"). Pick a new city and the map, transit, " +
                "civic services, events and weather all switch to it.",
            listOf("change city", "switch city", "another city", "different city", "moved", "wrong city", "select city"),
        ),
        Faq(
            "change-location", HelpTopic.START, "How do I set or change my location?",
            "Tap the location name at the top of Explore. You can use your current GPS location, search for an area, " +
                "street or full address, or long-press the map to drop a pin on the exact spot.",
            listOf("location", "change location", "set location", "area", "address", "gps", "pin", "where i am", "wrong location", "my area", "locality"),
            HelpLink("Open Explore", "explore"),
        ),
        Faq(
            "tabs", HelpTopic.START, "What are the five tabs at the bottom?",
            "• Explore: the map and what's near you\n" +
                "• News Hub: city news, bulletins and offers\n" +
                "• Report: flag a civic problem\n" +
                "• Events: things to do, and host your own\n" +
                "• Civic Hub: your ward, service links and your filed issues\n\n" +
                "Your Profile and the weather open from the top of the Explore screen.",
            listOf("tabs", "menu", "navigate", "navigation", "bottom bar", "where is", "find", "screens", "sections"),
        ),
        Faq(
            "free", HelpTopic.START, "Is Shehar Saathi free?",
            "Yes. The app is free to use and there is no platform fee on event bookings. " +
                "Some events have a ticket price set by the host, which you pay at the venue.",
            listOf("free", "cost", "price", "charge", "charges", "fee", "fees", "paid", "subscription", "money", "paisa"),
        ),

        // ---------------------------------------------------------------- Reporting
        Faq(
            "report-how", HelpTopic.REPORT, "How do I report a civic issue?",
            "1. Open the Report tab.\n" +
                "2. Snap a live photo of the problem. It's stamped with your GPS position so crews can find the spot.\n" +
                "3. Choose a category and add a short description (you can also speak it).\n" +
                "4. Submit. It's routed to the right authority automatically.\n\n" +
                "Neighbours can confirm your report to raise its priority.",
            listOf("report", "complaint", "complain", "issue", "problem", "pothole", "garbage", "streetlight", "street light", "drain", "sewage", "water leak", "file", "raise", "submit", "shikayat", "flag"),
            HelpLink("Open Report", "report"),
        ),
        Faq(
            "report-track", HelpTopic.REPORT, "How do I track my report?",
            "Go to Civic Hub and tap \"My filed issues\". Each report shows its current status. " +
                "\"Waiting for the authority to pick this up\" means it has been sent but not yet taken up.",
            listOf("track", "status", "my report", "my reports", "my issue", "my issues", "filed", "progress", "update", "resolved", "pending", "what happened", "follow up"),
            HelpLink("My filed issues", "reports?mine=true"),
        ),
        Faq(
            "report-confirm", HelpTopic.REPORT, "What does \"I see this too\" do?",
            "If you've seen the same problem, open the report and tap \"I see this too\". " +
                "Every confirmation from a resident raises the report's priority, so popular issues stand out to the authority.",
            listOf("i see this too", "confirm", "upvote", "vote", "support", "same problem", "priority", "like"),
            HelpLink("Community reports", "reports?mine=false"),
        ),
        Faq(
            "report-photo", HelpTopic.REPORT, "Why does a report need a live photo?",
            "A live photo proves the problem is real and current, and its GPS stamp tells crews exactly where to go. " +
                "If GPS isn't available, the photo is tagged with the spot you chose on the map instead.",
            listOf("photo", "picture", "camera", "live photo", "image", "gallery", "upload", "gps stamp", "why photo"),
        ),
        Faq(
            "report-time", HelpTopic.REPORT, "How long does it take to fix an issue?",
            "That depends on the authority responsible and the type of problem, so we can't promise a time. " +
                "You can follow the status under \"My filed issues\", and more confirmations from neighbours help it get noticed. " +
                "For anything urgent or dangerous, also call the helpline in Civic Hub.",
            listOf("how long", "time", "days", "when fixed", "fix", "delay", "slow", "no action", "not fixed", "still there"),
            HelpLink("Open Civic Hub", "civic"),
        ),

        // ---------------------------------------------------------------- Civic Hub
        Faq(
            "ward-find", HelpTopic.CIVIC, "How do I find my ward and corporator?",
            "Open Civic Hub. Share your location and it shows your ward number and who represents it. " +
                "You can also tap \"Choose my ward from the list\" and search by ward number or area name.\n\n" +
                "Near a ward boundary the nearest ward is shown, so double-check with \"Change ward\". " +
                "Ward details are available for Hyderabad right now.",
            listOf("ward", "corporator", "councillor", "councilor", "representative", "who runs", "my ward", "ward number", "zone", "circle", "change ward", "mla"),
            HelpLink("Open Civic Hub", "civic"),
        ),
        Faq(
            "services", HelpTopic.CIVIC, "How do I pay property tax or water and power bills?",
            "Civic Hub has service tiles that open the official websites: property tax goes to the corporation that runs your ward, " +
                "while water and power bills go to the state boards.\n\n" +
                "Payments happen on the official site, not inside Shehar Saathi. Those sites can be slow at busy times, so try again later if one doesn't load.",
            listOf("property tax", "tax", "bill", "bills", "water bill", "electricity", "power", "current bill", "pay", "trade licence", "trade license", "licence", "certificate", "birth certificate", "services", "portal", "official"),
            HelpLink("Open Civic Hub", "civic"),
        ),
        Faq(
            "helplines", HelpTopic.CIVIC, "Where are the helpline numbers?",
            "Civic Hub lists helplines for your ward, and many service tiles have a call or WhatsApp button. " +
                "In an emergency always call 112 first.",
            listOf("helpline", "phone number", "call", "contact number", "emergency", "whatsapp", "toll free", "112", "police", "ambulance", "fire"),
            HelpLink("Open Civic Hub", "civic"),
        ),
        Faq(
            "bulk-waste", HelpTopic.CIVIC, "How do I get debris or bulk waste picked up?",
            "In Civic Hub, tap \"Bulk Waste\". In Hyderabad it shows the authorised plants you can call or WhatsApp to book a paid pickup " +
                "for construction and demolition debris. Open dumping is fined, so please use an authorised plant.",
            listOf("bulk waste", "debris", "waste", "pickup", "construction waste", "rubble", "dumping", "malba", "furniture"),
            HelpLink("Open Civic Hub", "civic"),
        ),

        // ---------------------------------------------------------------- Events
        Faq(
            "events-find", HelpTopic.EVENTS, "How do I find events near me?",
            "Open the Events tab. Search by name, or filter by day (like \"This weekend\") and category. " +
                "Explore also shows events happening near your location.",
            listOf("events", "event", "find events", "things to do", "weekend", "concert", "gig", "meetup", "workshop", "happening", "near me", "today"),
            HelpLink("Open Events", "events"),
        ),
        Faq(
            "events-book", HelpTopic.EVENTS, "How do I book or RSVP for an event?",
            "Open the event, choose how many passes you need and tap \"RSVP Free\" or \"Book\". " +
                "Your pass and booking ID are saved on that event's page.\n\n" +
                "For paid events you pay at the venue gate and show your Shehar Saathi pass. The app doesn't take payments.",
            listOf("book", "booking", "rsvp", "ticket", "tickets", "pass", "passes", "register", "join", "attend", "booking id", "payment", "pay online", "upi", "refund", "cancel booking"),
            HelpLink("Open Events", "events"),
        ),
        Faq(
            "events-host", HelpTopic.EVENTS, "How do I host or add my own event?",
            "In the Events tab, tap \"Add Event\". Add a title, category, date and time, search the venue on the map, " +
                "and set it as free or add a ticket price. Your event then appears for people nearby and is marked \"Hosted by you\".",
            listOf("host", "add event", "create event", "my event", "organise", "organize", "list event", "post event", "publish"),
            HelpLink("Open Events", "events"),
        ),
        Faq(
            "saved", HelpTopic.EVENTS, "Where are my saved events, news and places?",
            "Tap the bookmark on any event, news story or place you open on the map to save it. " +
                "Everything you've saved is listed in your Profile under \"Saved\". Tap an item there to open it, " +
                "or tap its bookmark to remove it.\n\n" +
                "Saved places, city headlines and listings from ticketing sites are kept on this phone.",
            listOf("saved", "save", "bookmark", "bookmarks", "bookmarked", "favourite", "favorite", "saved places", "saved news", "wishlist"),
        ),

        // ---------------------------------------------------------------- News
        Faq(
            "news", HelpTopic.NEWS, "What's in the News Hub?",
            "News Hub has two parts:\n" +
                "• City news: headlines from local news sites. Tap a story to read it at the source.\n" +
                "• Community: civic bulletins, business offers and ward wins from your area.\n\n" +
                "You can endorse and save stories you find useful.",
            listOf("news", "news hub", "headlines", "stories", "bulletin", "bulletins", "community", "ward wins", "endorse", "articles", "khabar"),
            HelpLink("Open News Hub", "news"),
        ),
        Faq(
            "offers", HelpTopic.NEWS, "How do I use a business offer?",
            "In News Hub, open Community and choose \"Business Offers\". Tap \"Claim Offer\" to get the code, " +
                "copy it and show or use it at the business. Offer terms are set by the business.",
            listOf("offer", "offers", "coupon", "discount", "deal", "deals", "claim", "promo", "code", "business offers"),
            HelpLink("Open News Hub", "news"),
        ),

        // ---------------------------------------------------------------- Getting around
        Faq(
            "plan-trip", HelpTopic.TRAVEL, "How do I plan a trip?",
            "From Explore, open the trip planner. Choose your start and destination, then compare routes by metro, bus, auto and walking. " +
                "Turn on \"Rain-safe\" or \"Step-free\" if you need covered or accessible routes.\n\n" +
                "Start a route to get step-by-step guidance; the app can buzz you one stop before yours.",
            listOf("plan", "trip", "route", "directions", "travel", "commute", "journey", "how to reach", "go to", "navigate to", "rain safe", "step free", "wheelchair", "accessible", "planner"),
            HelpLink("Open trip planner", "plan"),
        ),
        Faq(
            "transit", HelpTopic.TRAVEL, "Where can I see metro and bus timings and fares?",
            "Open the live transit board from Explore. It shows departures for each corridor, service notices, " +
                "and metro, bus and auto fares at a glance.\n\n" +
                "Times are estimates from published timetables and fares are approximate, so check the operator for live updates.",
            listOf("metro", "bus", "mmts", "train", "timings", "timing", "timetable", "schedule", "fare", "fares", "auto fare", "transit", "departures", "next train", "rtc"),
            HelpLink("Open transit board", "transit"),
        ),
        Faq(
            "weather", HelpTopic.TRAVEL, "Where is the weather and air quality?",
            "Tap the weather chip at the top of Explore. You'll see the current conditions, hourly and daily forecast, " +
                "UV, visibility and air quality for your location. Forecast data comes from Open-Meteo.",
            listOf("weather", "rain", "temperature", "forecast", "aqi", "air quality", "pollution", "uv", "humidity", "mausam"),
            HelpLink("Open weather", "weather"),
        ),
        Faq(
            "map-search", HelpTopic.TRAVEL, "How do I search for a place or address on the map?",
            "Use the search bar on Explore. You can type a street, landmark, shop, colony or a full address with a door number. " +
                "If the exact door isn't on the map, choose the area and long-press the map to drop a pin on the spot.",
            listOf("search", "map", "place", "landmark", "door number", "house number", "find address", "shop", "colony", "drop pin", "long press"),
            HelpLink("Open Explore", "explore"),
        ),

        // ---------------------------------------------------------------- Account & privacy
        Faq(
            "sign-in", HelpTopic.ACCOUNT, "How does sign-in work?",
            "You sign in with your mobile number or email. We send a 6-digit one-time code, you enter it, and you're in. " +
                "There's no password to remember.",
            listOf("sign in", "signin", "login", "log in", "otp", "code", "password", "account", "register", "sign up", "signup", "create account"),
        ),
        Faq(
            "otp-missing", HelpTopic.TROUBLE, "I didn't get my sign-in code",
            "Try these in order:\n" +
                "1. Check the number or email you typed. Mobile numbers must be 10 digits starting with 6 to 9.\n" +
                "2. Wait for the timer, then tap \"Resend code\".\n" +
                "3. For email, look in your spam or promotions folder.\n" +
                "4. If the screen shows a demo code, just tap it to fill it in.\n\n" +
                "If a code says it didn't match, request a new one and use only the latest code.",
            listOf("otp not", "no otp", "code not", "didn't get", "didnt get", "not received", "not receiving", "no code", "resend", "sms", "wrong code", "invalid code", "code didn't match", "cant login", "can't login", "cannot login", "unable to login"),
        ),
        Faq(
            "edit-profile", HelpTopic.ACCOUNT, "How do I edit my name, photo or contact details?",
            "Open your Profile. Tap your photo to take a new one or choose from your gallery. " +
                "Edit your name, mobile, email or ward under \"Your details\" and tap \"Save changes\".\n\n" +
                "The mobile number or email you signed in with can't be changed there, because it's your login.",
            listOf("edit profile", "profile", "name", "change name", "photo", "profile picture", "dp", "avatar", "email", "mobile", "phone", "number", "change number", "update details", "can't change"),
        ),
        Faq(
            "notifications", HelpTopic.ACCOUNT, "How do I turn notifications on or off?",
            "In your Profile, under \"Notifications\", there are three switches: Civic Alerts (your reports and ward works), " +
                "Local Events (new events near you) and Emergency Broadcasts (floods, outages and safety alerts). " +
                "We suggest keeping Emergency Broadcasts on.",
            listOf("notification", "notifications", "alerts", "alert", "mute", "turn off", "stop", "reminders", "push", "emergency broadcasts"),
        ),
        Faq(
            "privacy", HelpTopic.ACCOUNT, "What happens to my location and data?",
            "Your location is used to show what's near you, find your ward and stamp report photos so crews can find the spot. " +
                "Reports you file and events you host are shared with the community so neighbours and authorities can act on them.\n\n" +
                "Questions you type in this help chat stay on your phone.",
            listOf("privacy", "data", "safe", "secure", "security", "personal", "share", "tracking", "track me", "who can see", "location permission", "permission", "permissions"),
        ),
        Faq(
            "sign-out", HelpTopic.ACCOUNT, "How do I sign out?",
            "Scroll to the bottom of your Profile and tap \"Sign out\". You'll need a new one-time code to sign back in.",
            listOf("sign out", "signout", "log out", "logout", "exit", "switch account"),
        ),
        Faq(
            "delete-account", HelpTopic.ACCOUNT, "Can I delete my account?",
            "There isn't a delete-account button in the app yet. You can sign out at any time from the bottom of your Profile, " +
                "and you can remove your photo and clear optional details under \"Your details\".",
            listOf("delete account", "remove account", "deactivate", "close account", "erase", "delete my data", "delete"),
        ),

        // ---------------------------------------------------------------- Troubleshooting
        Faq(
            "location-off", HelpTopic.TROUBLE, "The app can't find my location",
            "1. Turn on Location (GPS) in your phone's quick settings.\n" +
                "2. In phone Settings > Apps > Shehar Saathi > Permissions, allow Location.\n" +
                "3. Step outdoors or near a window and try again.\n\n" +
                "You can always set your place by hand: tap the location name at the top of Explore and search or drop a pin.",
            listOf("location not working", "gps not", "can't find location", "location off", "wrong place", "no gps", "location denied", "couldn't get", "location error", "not detecting"),
            HelpLink("Open Explore", "explore"),
        ),
        Faq(
            "not-loading", HelpTopic.TROUBLE, "Something isn't loading",
            "1. Check your internet connection (mobile data or Wi-Fi).\n" +
                "2. Go back and open the screen again, or switch tabs to refresh.\n" +
                "3. Close the app fully and reopen it.\n\n" +
                "Official government websites opened from Civic Hub can be slow or busy at times. That's on their side, so try again a little later.",
            listOf("not loading", "loading", "blank", "empty", "stuck", "slow", "error", "crash", "crashing", "not working", "bug", "hang", "frozen", "no internet", "offline", "nothing shows", "white screen", "doesn't open"),
        ),
        Faq(
            "empty-area", HelpTopic.TROUBLE, "Why do I see few or no events, reports or news near me?",
            "Lists show what's close to your chosen location, so quieter areas have less. Try:\n" +
                "• \"See all\" on any Explore row for the whole city\n" +
                "• Checking your location at the top of Explore\n" +
                "• Being the first: file a report or host an event in your area",
            listOf("nothing near", "no events", "no reports", "no news", "empty list", "few", "not showing", "missing", "nothing here"),
            HelpLink("Open Explore", "explore"),
        ),
        Faq(
            "language", HelpTopic.START, "Is the app available in Telugu or Hindi?",
            "The app is in English for now. More languages, including Telugu and Hindi, are something we'd like to add.",
            listOf("language", "telugu", "hindi", "urdu", "english", "translate", "regional", "bhasha"),
        ),
    )

    fun byTopic(topic: HelpTopic): List<Faq> = all.filter { it.topic == topic }

    /** The questions offered when the chat opens. */
    val starters: List<Faq> get() = listOf("report-how", "ward-find", "events-book", "otp-missing").mapNotNull { id -> all.firstOrNull { it.id == id } }
}

object HelpBot {
    const val NAME = "Saathi"
    private const val MAX_QUESTION = 200

    private val stopWords = setOf(
        "a", "an", "the", "is", "are", "am", "was", "be", "to", "of", "in", "on", "at", "for", "and", "or", "it", "its", "this", "that",
        "i", "me", "my", "we", "our", "you", "your", "do", "does", "did", "can", "could", "will", "would", "should", "how", "what", "where",
        "when", "why", "which", "who", "with", "from", "by", "as", "if", "so", "have", "has", "had", "get", "got", "want", "need", "please",
        "pls", "tell", "show", "help", "there", "shehar", "saathi", "app", "any", "some", "not", "no", "t", "s", "m", "im", "u", "ka", "ki", "ke", "hai", "kaise", "kya",
    )

    /** Everyday words mapped to the word the answers use. Keys and values are already stemmed. */
    private val synonyms = mapOf(
        "complaint" to "report", "complain" to "report", "grievance" to "report", "shikayat" to "report",
        "login" to "sign", "signin" to "sign", "logout" to "signout",
        "otp" to "code", "passcode" to "code",
        "ticket" to "book", "rsvp" to "book", "reserve" to "book",
        "councillor" to "corporator", "councilor" to "corporator",
        "electricity" to "power", "current" to "power",
        "picture" to "photo", "pic" to "photo", "image" to "photo",
        "coupon" to "offer", "discount" to "offer", "deal" to "offer",
        "timing" to "timetable", "schedule" to "timetable",
        "alert" to "notification",
        "bangalore" to "bengaluru",
    )

    private fun normalise(text: String): String =
        text.lowercase().replace('’', '\'').replace(Regex("[^a-z0-9' ]"), " ").replace(Regex("\\s+"), " ").trim()

    private fun stem(word: String): String {
        val w = word.replace("'", "")
        return when {
            w.length > 5 && w.endsWith("ing") -> w.dropLast(3)
            w.length > 4 && w.endsWith("ies") -> w.dropLast(3) + "y"
            w.length > 3 && w.endsWith("s") && !w.endsWith("ss") -> w.dropLast(1)
            else -> w
        }
    }

    internal fun tokens(text: String): List<String> =
        normalise(text).split(' ').asSequence().filter { it.isNotBlank() }.map { stem(it) }.map { synonyms[it] ?: it }
            .filter { it.isNotBlank() && it !in stopWords }.toList()

    private class Indexed(val faq: Faq, val keywordTokens: Set<String>, val questionTokens: Set<String>, val phrases: List<String>)

    private val index: List<Indexed> by lazy {
        HelpFaqs.all.map { f ->
            Indexed(
                faq = f,
                keywordTokens = f.keywords.flatMap { tokens(it) }.toSet(),
                questionTokens = tokens(f.question).toSet(),
                phrases = f.keywords.map { normalise(it) }.filter { it.contains(' ') },
            )
        }
    }

    private fun score(entry: Indexed, query: String, queryTokens: Set<String>): Int {
        var s = 0
        for (t in queryTokens) {
            if (t in entry.keywordTokens) s += 3
            if (t in entry.questionTokens) s += 1
        }
        for (p in entry.phrases) if (query.contains(p)) s += 4
        return s
    }

    /** FAQs ranked for a question, best first, with their scores. */
    internal fun rank(question: String): List<Pair<Faq, Int>> {
        val query = normalise(question.take(MAX_QUESTION))
        val queryTokens = tokens(query).toSet()
        if (query.isEmpty()) return emptyList()
        return index.map { it.faq to score(it, query, queryTokens) }.filter { it.second > 0 }.sortedByDescending { it.second }
    }

    private val greetings = setOf("hi", "hii", "hello", "hey", "namaste", "namaskar", "hola", "yo", "good", "morning", "evening", "afternoon")
    private val thanks = setOf("thank", "thanks", "thx", "ty", "thankyou", "dhanyavad", "shukriya", "great", "ok", "okay", "cool", "nice", "got")

    fun welcome(firstName: String): HelpReply = HelpReply(
        text = (if (firstName.isBlank()) "Hi!" else "Hi $firstName!") +
            " I'm $NAME, your guide to Shehar Saathi. Ask me anything about the app, or pick a topic below.",
        suggestions = HelpFaqs.starters,
        showTopics = true,
    )

    fun topic(topic: HelpTopic): HelpReply = HelpReply(
        text = "Here's what people usually ask about ${topic.label.lowercase()}. Tap one:",
        suggestions = HelpFaqs.byTopic(topic),
    )

    fun answerFor(faq: Faq): HelpReply = HelpReply(
        text = faq.answer,
        faq = faq,
        suggestions = HelpFaqs.byTopic(faq.topic).filter { it.id != faq.id }.take(2),
    )

    fun reply(question: String): HelpReply {
        val words = normalise(question.take(MAX_QUESTION)).split(' ').filter { it.isNotBlank() }.map { stem(it) }
        if (words.isEmpty()) return HelpReply("Type a question and I'll do my best to help.", showTopics = true)

        val ranked = rank(question)
        val best = ranked.firstOrNull()

        // Small talk only counts when the message isn't also a real question.
        if (best == null || best.second < 3) {
            if (words.size <= 4 && words.any { it in greetings }) {
                return HelpReply("Hello! What would you like to know about Shehar Saathi?", suggestions = HelpFaqs.starters, showTopics = true)
            }
            if (words.size <= 5 && words.any { it in thanks }) {
                return HelpReply("Happy to help! Ask me anything else, any time.", showTopics = true)
            }
        }

        if (best == null) {
            return HelpReply(
                "Sorry, I don't have an answer for that yet. I can help with questions about using Shehar Saathi. Try different words, or pick a topic:",
                showTopics = true,
            )
        }

        val runnerUp = ranked.getOrNull(1)?.second ?: 0
        val confident = best.second >= 6 || (best.second >= 3 && best.second > runnerUp)
        if (confident) {
            // Only offer follow-ups that are close matches; otherwise stay within the same topic.
            val related = ranked.drop(1).filter { it.second * 2 >= best.second }.take(2).map { it.first }
                .ifEmpty { HelpFaqs.byTopic(best.first.topic).filter { it.id != best.first.id }.take(2) }
            return HelpReply(best.first.answer, faq = best.first, suggestions = related)
        }
        return HelpReply(
            "I want to be sure I answer the right thing. Did you mean one of these?",
            suggestions = ranked.take(3).map { it.first },
        )
    }
}
