package com.sheharsaathi.app.data

/** One thing the user can do from a Civic Hub tile: open a site, call, or WhatsApp. */
data class CivicAction(
    val title: String,
    val sub: String,
    val kind: Kind,
    /** A URL, a number to dial, or a WhatsApp number with country code. */
    val target: String,
) {
    enum class Kind { WEB, CALL, WHATSAPP }
}

data class CivicService(val title: String, val intro: String, val actions: List<CivicAction>)

/**
 * The four Civic Hub tiles for Hyderabad. Since Feb 2026 the city has three corporations, so
 * property tax and trade licences go to whichever one runs the user's ward.
 * Links are checked by .github/workflows/civic-data.yml (qa/civic_links.txt).
 */
object HyderabadServices {
    const val GHMC_SITE = "https://www.ghmc.gov.in"
    const val GHMC_TRADE = "https://ghmc.gov.in/Trade.aspx"
    const val CMC_SITE = "https://cmc.telangana.gov.in"
    const val MMC_SITE = "https://mmc.telangana.gov.in"
    const val POWER = "https://tgsouthernpower.org"
    const val WATER = "https://www.hyderabadwater.gov.in"

    private fun site(corp: String?) = when (corp) { "CMC" -> CMC_SITE; "MMC" -> MMC_SITE; else -> GHMC_SITE }
    private fun corpName(corp: String?) = when (corp) { "CMC" -> "Cyberabad corporation"; "MMC" -> "Malkajgiri corporation"; else -> "GHMC" }
    private fun host(url: String) = url.removePrefix("https://").removePrefix("www.").substringBefore("/")

    fun bills(corp: String?) = CivicService(
        "Pay utility bills",
        "Property tax goes to the corporation that runs your ward; power and water bills go to the state boards.",
        listOf(
            CivicAction("Property tax", "${corpName(corp)} · ${host(site(corp))}", CivicAction.Kind.WEB, site(corp)),
            CivicAction("Electricity bill", "TGSPDCL · tgsouthernpower.org", CivicAction.Kind.WEB, POWER),
            CivicAction("Water bill", "HMWSSB · hyderabadwater.gov.in", CivicAction.Kind.WEB, WATER),
        ),
    )

    fun trade(corp: String?) = CivicService(
        "Trade licence",
        "Apply for or renew a trade licence with the ${corpName(corp)}. You'll log in with a one-time password and can pay online.",
        listOf(
            if (corp == "CMC" || corp == "MMC") CivicAction("Apply / renew online", "${corpName(corp)} · ${host(site(corp))}", CivicAction.Kind.WEB, site(corp))
            else CivicAction("Apply / renew online", "GHMC · ghmc.gov.in/Trade.aspx", CivicAction.Kind.WEB, GHMC_TRADE),
        ),
    )

    val bulkWaste = CivicService(
        "Bulk & debris pickup",
        "Construction and demolition debris must go to an authorised plant; open dumping is fined. Call or WhatsApp a plant to book a paid pickup.",
        listOf(
            CivicAction("Shamshabad & Thumkunta plants", "Toll-free 1800 203 0033", CivicAction.Kind.CALL, "18002030033"),
            CivicAction("WhatsApp Shamshabad & Thumkunta", "73300 00203", CivicAction.Kind.WHATSAPP, "917330000203"),
            CivicAction("Jeedimetla & Fathullaguda plants", "Toll-free 1800 120 1159", CivicAction.Kind.CALL, "18001201159"),
            CivicAction("WhatsApp Jeedimetla & Fathullaguda", "97054 33369", CivicAction.Kind.WHATSAPP, "919705433369"),
        ),
    )

    val treeFall = CivicService(
        "Tree fall / hazard",
        "HYDRAA clears fallen trees and waterlogging across Hyderabad, in all three corporations.",
        listOf(
            CivicAction("Call HYDRAA", "Toll-free 1070", CivicAction.Kind.CALL, "1070"),
            CivicAction("Send photos on WhatsApp", "HYDRAA · 87124 06899", CivicAction.Kind.WHATSAPP, "918712406899"),
            CivicAction("Tree on a power line", "TGSPDCL · 1912", CivicAction.Kind.CALL, "1912"),
        ),
    )
}
