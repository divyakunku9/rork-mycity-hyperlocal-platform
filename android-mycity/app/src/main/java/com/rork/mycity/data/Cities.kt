package com.rork.mycity.data

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rork.mycity.R

enum class HelplineKind { CIVIC, POWER, WATER, POLICE }

data class Helpline(val label: String, val display: String, val dial: String, val kind: HelplineKind)

data class MetroLineInfo(val name: String, val short: String, val color: Long, val from: String, val to: String)

/** Official online service portals for the Civic Hub tiles. */
data class CityPortals(
    val utilityBills: String,
    val tradeLicense: String,
    val bulkWaste: String,
    val treeHazard: String,
)

/** Local names used to write believable sample content for a city. */
data class CityFlavor(
    val food: String,
    val cafe: String,
    val tiffin: String,
    val mall: String,
    val market: String,
    val park: String,
    val techHub: String,
    val artVenue: String,
    val rooftop: String,
    val culturalVenue: String,
    val busyRoad: String,
    val altRoute: String,
)

data class City(
    val id: String,
    val name: String,
    val state: String,
    val tagline: String,
    val centerLat: Double,
    val centerLng: Double,
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
    @DrawableRes val imageRes: Int,
    val municipalShort: String,
    /** Order: municipal body, traffic police, power utility, water utility. */
    val authorities: List<Authority>,
    val helplines: List<Helpline>,
    val portals: CityPortals,
    val metroName: String?,
    val metroLines: List<MetroLineInfo>,
    val busOperator: String,
    val areas: List<Pair<String, Pair<Double, Double>>>,
    val landmarks: List<Place>,
    val flavor: CityFlavor,
) {
    fun contains(lat: Double, lng: Double) = lat in south..north && lng in west..east

    val municipal get() = authorities[0]
    val traffic get() = authorities[1]
    val power get() = authorities[2]
    val water get() = authorities[3]

    val isHyderabad get() = id == "hyderabad"

    fun areaPlaces(): List<Place> = areas.map { (n, ll) ->
        Place("area-${id}-${n.lowercase().replace(' ', '-').replace(".", "")}", n, name, ll.first, ll.second, PlaceKind.LANDMARK)
    }

    /** Default spot until GPS or a manual pick: the first listed neighbourhood. */
    val defaultPlace: Place
        get() = areas.first().let { (n, ll) -> Place("gps", n, name, ll.first, ll.second, PlaceKind.CURRENT) }

    fun distanceToCenterKm(lat: Double, lng: Double) = Transit.distanceKm(lat, lng, centerLat, centerLng)
}

/** The city the whole app is currently showing. Observable from Compose. */
object CurrentCity {
    var city by mutableStateOf(Cities.HYDERABAD)
}

private fun lm(city: String, id: String, name: String, area: String, lat: Double, lng: Double, kind: PlaceKind = PlaceKind.LANDMARK) =
    Place("$city-$id", name, area, lat, lng, kind)

object Cities {

    val HYDERABAD = City(
        id = "hyderabad", name = "Hyderabad", state = "Telangana", tagline = "City of Pearls",
        centerLat = 17.4126, centerLng = 78.4482, south = 17.20, north = 17.62, west = 78.22, east = 78.70,
        imageRes = R.drawable.city_hyderabad, municipalShort = "GHMC",
        authorities = listOf(
            Authority("@GHMCOnline", "Greater Hyderabad Municipal Corporation", true),
            Authority("@HYDTP", "Hyderabad Traffic Police", true),
            Authority("@TGSPDCL", "Telangana Southern Power Distribution Co.", true),
            Authority("@HMWSSBOnline", "Hyderabad Metro Water Supply & Sewerage Board", true),
        ),
        helplines = listOf(
            Helpline("GHMC (040)", "21111111", "04021111111", HelplineKind.CIVIC),
            Helpline("TGSPDCL", "1912", "1912", HelplineKind.POWER),
            Helpline("HMWSSB", "155313", "155313", HelplineKind.WATER),
        ),
        portals = CityPortals(
            utilityBills = "https://tgsouthernpower.org",
            tradeLicense = "https://www.ghmc.gov.in",
            bulkWaste = "https://www.ghmc.gov.in",
            treeHazard = "https://www.ghmc.gov.in",
        ),
        metroName = "Hyderabad Metro",
        metroLines = listOf(
            MetroLineInfo("Red Line", "R", LineColors.RED, "Miyapur", "LB Nagar"),
            MetroLineInfo("Blue Line", "B", LineColors.BLUE, "Raidurg", "Nagole"),
            MetroLineInfo("Green Line", "G", LineColors.GREEN, "JBS Parade Ground", "MG Bus Station"),
        ),
        busOperator = "TGSRTC",
        areas = listOf(
            "Madhapur" to (17.4483 to 78.3915), "HITEC City" to (17.4435 to 78.3772),
            "Gachibowli" to (17.4401 to 78.3489), "Kondapur" to (17.4699 to 78.3578),
            "Financial District" to (17.4146 to 78.3420), "Kokapet" to (17.3964 to 78.3257),
            "Manikonda" to (17.4018 to 78.3866), "Jubilee Hills" to (17.4325 to 78.4073),
            "Banjara Hills" to (17.4156 to 78.4347), "Somajiguda" to (17.4239 to 78.4590),
            "Ameerpet" to (17.4375 to 78.4482), "Begumpet" to (17.4447 to 78.4664),
            "Secunderabad" to (17.4399 to 78.4983), "Kukatpally" to (17.4849 to 78.4138),
            "Miyapur" to (17.4966 to 78.3570), "Kompally" to (17.5367 to 78.4851),
            "Himayatnagar" to (17.4010 to 78.4840), "Abids" to (17.3920 to 78.4760),
            "Charminar" to (17.3616 to 78.4747), "Dilsukhnagar" to (17.3688 to 78.5247),
            "LB Nagar" to (17.3457 to 78.5522), "Uppal" to (17.4018 to 78.5602),
            "Tarnaka" to (17.4281 to 78.5394), "Shamshabad" to (17.2600 to 78.3900),
        ),
        landmarks = emptyList(), // Hyderabad uses the full planner place list in Transit
        flavor = CityFlavor(
            food = "Irani chai, haleem and Osmania biscuits", cafe = "Third Wave Coffee", tiffin = "Chutneys",
            mall = "Inorbit Mall", market = "Lamakaan", park = "KBR National Park", techHub = "T-Hub 2.0",
            artVenue = "Kalakriti Art Gallery", rooftop = "The Moonshine Project", culturalVenue = "Shilpakala Vedika",
            busyRoad = "Biodiversity flyover", altRoute = "Old Mumbai Highway",
        ),
    )

    val BENGALURU = City(
        id = "bengaluru", name = "Bengaluru", state = "Karnataka", tagline = "Garden City",
        centerLat = 12.9716, centerLng = 77.5946, south = 12.78, north = 13.25, west = 77.42, east = 77.82,
        imageRes = R.drawable.city_bengaluru, municipalShort = "BBMP",
        authorities = listOf(
            Authority("@BBMPCOMM", "Bruhat Bengaluru Mahanagara Palike", true),
            Authority("@blrcitytraffic", "Bengaluru Traffic Police", true),
            Authority("@NimmaBESCOM", "Bangalore Electricity Supply Company", true),
            Authority("@BWSSB", "Bangalore Water Supply & Sewerage Board", true),
        ),
        helplines = listOf(
            Helpline("BBMP", "1533", "1533", HelplineKind.CIVIC),
            Helpline("BESCOM", "1912", "1912", HelplineKind.POWER),
            Helpline("BWSSB", "1916", "1916", HelplineKind.WATER),
        ),
        portals = CityPortals(
            utilityBills = "https://bescom.karnataka.gov.in",
            tradeLicense = "https://trade.bbmpgov.in",
            bulkWaste = "https://bbmp.gov.in",
            treeHazard = "https://bbmp.gov.in",
        ),
        metroName = "Namma Metro",
        metroLines = listOf(
            MetroLineInfo("Purple Line", "P", 0xFF7B2D8E, "Whitefield", "Challaghatta"),
            MetroLineInfo("Green Line", "G", LineColors.GREEN, "Madavara", "Silk Institute"),
            MetroLineInfo("Yellow Line", "Y", LineColors.YELLOW, "RV Road", "Bommasandra"),
        ),
        busOperator = "BMTC",
        areas = listOf(
            "Indiranagar" to (12.9784 to 77.6408), "Koramangala" to (12.9352 to 77.6245),
            "MG Road" to (12.9756 to 77.6066), "Whitefield" to (12.9698 to 77.7500),
            "HSR Layout" to (12.9116 to 77.6474), "Jayanagar" to (12.9250 to 77.5938),
            "Malleshwaram" to (13.0035 to 77.5710), "Electronic City" to (12.8452 to 77.6602),
            "Hebbal" to (13.0358 to 77.5970), "Marathahalli" to (12.9569 to 77.7011),
        ),
        landmarks = listOf(
            lm("blr", "cubbon", "Cubbon Park", "Sampangi Rama Nagar", 12.9763, 77.5929),
            lm("blr", "lalbagh", "Lalbagh Botanical Garden", "Mavalli", 12.9507, 77.5848),
            lm("blr", "majestic", "Majestic (Kempegowda Bus Station)", "Gandhi Nagar", 12.9767, 77.5713, PlaceKind.METRO),
            lm("blr", "vidhana", "Vidhana Soudha", "Ambedkar Veedhi", 12.9796, 77.5906),
            lm("blr", "airport", "Kempegowda Int'l Airport", "Devanahalli", 13.1986, 77.7066),
        ),
        flavor = CityFlavor(
            food = "filter coffee, benne dosa and craft beer", cafe = "Blue Tokai", tiffin = "Vidyarthi Bhavan",
            mall = "Phoenix Marketcity", market = "Rangoli Metro Art Center", park = "Cubbon Park", techHub = "NASSCOM CoE",
            artVenue = "Chitrakala Parishath", rooftop = "Skyye", culturalVenue = "Ranga Shankara",
            busyRoad = "Silk Board junction", altRoute = "Hosur Road service lane",
        ),
    )

    val MUMBAI = City(
        id = "mumbai", name = "Mumbai", state = "Maharashtra", tagline = "City of Dreams",
        centerLat = 19.0760, centerLng = 72.8777, south = 18.88, north = 19.30, west = 72.76, east = 73.05,
        imageRes = R.drawable.city_mumbai, municipalShort = "BMC",
        authorities = listOf(
            Authority("@mybmc", "Brihanmumbai Municipal Corporation", true),
            Authority("@MTPHereToHelp", "Mumbai Traffic Police", true),
            Authority("@myBESTElectricity", "BEST Undertaking (Electricity)", true),
            Authority("@mybmcWaterSupply", "BMC Hydraulic Engineering (Water)", true),
        ),
        helplines = listOf(
            Helpline("BMC", "1916", "1916", HelplineKind.CIVIC),
            Helpline("BEST Power", "19123", "19123", HelplineKind.POWER),
            Helpline("Police", "112", "112", HelplineKind.POLICE),
        ),
        portals = CityPortals(
            utilityBills = "https://www.bestundertaking.com",
            tradeLicense = "https://portal.mcgm.gov.in",
            bulkWaste = "https://portal.mcgm.gov.in",
            treeHazard = "https://portal.mcgm.gov.in",
        ),
        metroName = "Mumbai Metro",
        metroLines = listOf(
            MetroLineInfo("Line 1", "1", LineColors.BLUE, "Versova", "Ghatkopar"),
            MetroLineInfo("Line 2A", "2A", LineColors.YELLOW, "Dahisar East", "DN Nagar"),
            MetroLineInfo("Line 3 Aqua", "3", 0xFF00A3C4, "Aarey JVLR", "Cuffe Parade"),
            MetroLineInfo("Line 7", "7", LineColors.RED, "Dahisar East", "Gundavali"),
        ),
        busOperator = "BEST",
        areas = listOf(
            "Bandra West" to (19.0596 to 72.8295), "Andheri East" to (19.1136 to 72.8697),
            "Lower Parel" to (18.9980 to 72.8300), "Colaba" to (18.9067 to 72.8147),
            "Powai" to (19.1176 to 72.9060), "Dadar" to (19.0178 to 72.8478),
            "Juhu" to (19.1075 to 72.8263), "BKC" to (19.0660 to 72.8650),
            "Goregaon" to (19.1663 to 72.8526), "Chembur" to (19.0522 to 72.9005),
        ),
        landmarks = listOf(
            lm("bom", "gateway", "Gateway of India", "Colaba", 18.9220, 72.8347),
            lm("bom", "csmt", "Chhatrapati Shivaji Maharaj Terminus", "Fort", 18.9398, 72.8355, PlaceKind.METRO),
            lm("bom", "marine", "Marine Drive", "Churchgate", 18.9440, 72.8230),
            lm("bom", "juhu", "Juhu Beach", "Juhu", 19.0988, 72.8267),
            lm("bom", "sgnp", "Sanjay Gandhi National Park", "Borivali East", 19.2147, 72.9106),
        ),
        flavor = CityFlavor(
            food = "vada pav, bhel and cutting chai", cafe = "Subko Coffee", tiffin = "Café Madras",
            mall = "Phoenix Palladium", market = "Kala Ghoda", park = "Joggers Park", techHub = "WeWork BKC",
            artVenue = "Jehangir Art Gallery", rooftop = "AER, Four Seasons", culturalVenue = "NCPA",
            busyRoad = "Western Express Highway", altRoute = "SV Road",
        ),
    )

    val DELHI = City(
        id = "delhi", name = "Delhi", state = "Delhi NCT", tagline = "Heart of India",
        centerLat = 28.6139, centerLng = 77.2090, south = 28.40, north = 28.88, west = 76.84, east = 77.35,
        imageRes = R.drawable.city_delhi, municipalShort = "MCD",
        authorities = listOf(
            Authority("@MCD_Delhi", "Municipal Corporation of Delhi", true),
            Authority("@dtptraffic", "Delhi Traffic Police", true),
            Authority("@bsesdelhi", "BSES Delhi (Power)", true),
            Authority("@DelhiJalBoard", "Delhi Jal Board", true),
        ),
        helplines = listOf(
            Helpline("MCD", "155305", "155305", HelplineKind.CIVIC),
            Helpline("BSES", "19123", "19123", HelplineKind.POWER),
            Helpline("Jal Board", "1916", "1916", HelplineKind.WATER),
        ),
        portals = CityPortals(
            utilityBills = "https://www.bsesdelhi.com",
            tradeLicense = "https://mcdonline.nic.in",
            bulkWaste = "https://mcdonline.nic.in",
            treeHazard = "https://mcdonline.nic.in",
        ),
        metroName = "Delhi Metro",
        metroLines = listOf(
            MetroLineInfo("Yellow Line", "Y", LineColors.YELLOW, "Samaypur Badli", "HUDA City Centre"),
            MetroLineInfo("Blue Line", "B", LineColors.BLUE, "Dwarka Sec 21", "Noida Electronic City"),
            MetroLineInfo("Magenta Line", "M", 0xFFB5179E, "Janakpuri West", "Botanical Garden"),
            MetroLineInfo("Pink Line", "P", 0xFFE5689E, "Majlis Park", "Shiv Vihar"),
        ),
        busOperator = "DTC",
        areas = listOf(
            "Connaught Place" to (28.6315 to 77.2167), "Hauz Khas" to (28.5494 to 77.2001),
            "Karol Bagh" to (28.6519 to 77.1909), "Lajpat Nagar" to (28.5677 to 77.2433),
            "Saket" to (28.5245 to 77.2066), "Dwarka" to (28.5921 to 77.0460),
            "Rohini" to (28.7495 to 77.0565), "Chandni Chowk" to (28.6506 to 77.2303),
            "Vasant Kunj" to (28.5200 to 77.1590), "Mayur Vihar" to (28.6090 to 77.2940),
        ),
        landmarks = listOf(
            lm("del", "indiagate", "India Gate", "Rajpath", 28.6129, 77.2295),
            lm("del", "redfort", "Red Fort", "Chandni Chowk", 28.6562, 77.2410),
            lm("del", "qutub", "Qutub Minar", "Mehrauli", 28.5245, 77.1855),
            lm("del", "lodhi", "Lodhi Garden", "Lodhi Estate", 28.5931, 77.2197),
            lm("del", "ndls", "New Delhi Railway Station", "Paharganj", 28.6430, 77.2194, PlaceKind.METRO),
        ),
        flavor = CityFlavor(
            food = "chole bhature, kebabs and kulfi", cafe = "Perch Wine & Coffee", tiffin = "Saravana Bhavan CP",
            mall = "Select Citywalk", market = "Dilli Haat", park = "Lodhi Garden", techHub = "91springboard Okhla",
            artVenue = "Triveni Kala Sangam", rooftop = "Ophelia", culturalVenue = "India Habitat Centre",
            busyRoad = "Ring Road near AIIMS", altRoute = "Aurobindo Marg",
        ),
    )

    val CHENNAI = City(
        id = "chennai", name = "Chennai", state = "Tamil Nadu", tagline = "Gateway to the South",
        centerLat = 13.0827, centerLng = 80.2707, south = 12.82, north = 13.25, west = 80.08, east = 80.33,
        imageRes = R.drawable.city_chennai, municipalShort = "GCC",
        authorities = listOf(
            Authority("@chennaicorp", "Greater Chennai Corporation", true),
            Authority("@ChennaiTraffic", "Greater Chennai Traffic Police", true),
            Authority("@TANGEDCO_Offl", "Tamil Nadu Power Distribution Corporation", true),
            Authority("@CHN_Metro_Water", "Chennai Metro Water (CMWSSB)", true),
        ),
        helplines = listOf(
            Helpline("GCC", "1913", "1913", HelplineKind.CIVIC),
            Helpline("TNPDCL", "94987 94987", "9498794987", HelplineKind.POWER),
            Helpline("Metro Water", "044-45674567", "04445674567", HelplineKind.WATER),
        ),
        portals = CityPortals(
            utilityBills = "https://www.tnebltd.gov.in",
            tradeLicense = "https://chennaicorporation.gov.in",
            bulkWaste = "https://chennaicorporation.gov.in",
            treeHazard = "https://chennaicorporation.gov.in",
        ),
        metroName = "Chennai Metro",
        metroLines = listOf(
            MetroLineInfo("Blue Line", "B", LineColors.BLUE, "Wimco Nagar", "Airport"),
            MetroLineInfo("Green Line", "G", LineColors.GREEN, "Chennai Central", "St. Thomas Mount"),
        ),
        busOperator = "MTC",
        areas = listOf(
            "T. Nagar" to (13.0418 to 80.2341), "Adyar" to (13.0012 to 80.2565),
            "Mylapore" to (13.0368 to 80.2676), "Anna Nagar" to (13.0850 to 80.2101),
            "Velachery" to (12.9815 to 80.2180), "Nungambakkam" to (13.0569 to 80.2425),
            "Thoraipakkam" to (12.9416 to 80.2362), "Besant Nagar" to (13.0003 to 80.2667),
            "Guindy" to (13.0067 to 80.2206), "Egmore" to (13.0732 to 80.2609),
        ),
        landmarks = listOf(
            lm("maa", "marina", "Marina Beach", "Triplicane", 13.0500, 80.2824),
            lm("maa", "kapali", "Kapaleeshwarar Temple", "Mylapore", 13.0339, 80.2696),
            lm("maa", "central", "Chennai Central", "Park Town", 13.0827, 80.2757, PlaceKind.METRO),
            lm("maa", "fort", "Fort St. George", "George Town", 13.0797, 80.2873),
            lm("maa", "elliots", "Elliot's Beach", "Besant Nagar", 12.9986, 80.2717),
        ),
        flavor = CityFlavor(
            food = "filter kaapi, idli-vada and sundal", cafe = "Writer's Cafe", tiffin = "Ratna Cafe",
            mall = "Express Avenue", market = "Pondy Bazaar", park = "Semmozhi Poonga", techHub = "IIT Madras Research Park",
            artVenue = "DakshinaChitra", rooftop = "Sera, The Tapas Bar", culturalVenue = "The Music Academy",
            busyRoad = "Kathipara flyover", altRoute = "Inner Ring Road",
        ),
    )

    val KOLKATA = City(
        id = "kolkata", name = "Kolkata", state = "West Bengal", tagline = "City of Joy",
        centerLat = 22.5726, centerLng = 88.3639, south = 22.40, north = 22.75, west = 88.20, east = 88.52,
        imageRes = R.drawable.city_kolkata, municipalShort = "KMC",
        authorities = listOf(
            Authority("@kmc_kolkata", "Kolkata Municipal Corporation", true),
            Authority("@KolkataTraffic", "Kolkata Traffic Police", true),
            Authority("@CESCLimited", "CESC Limited (Power)", true),
            Authority("@KMCWaterSupply", "KMC Water Supply Department", true),
        ),
        helplines = listOf(
            Helpline("KMC", "1800-345-3375", "18003453375", HelplineKind.CIVIC),
            Helpline("CESC", "1912", "1912", HelplineKind.POWER),
            Helpline("Police", "112", "112", HelplineKind.POLICE),
        ),
        portals = CityPortals(
            utilityBills = "https://www.cesc.co.in",
            tradeLicense = "https://www.kmcgov.in",
            bulkWaste = "https://www.kmcgov.in",
            treeHazard = "https://www.kmcgov.in",
        ),
        metroName = "Kolkata Metro",
        metroLines = listOf(
            MetroLineInfo("Blue Line", "B", LineColors.BLUE, "Dakshineswar", "Kavi Subhash"),
            MetroLineInfo("Green Line", "G", LineColors.GREEN, "Howrah Maidan", "Salt Lake Sector V"),
            MetroLineInfo("Purple Line", "P", 0xFF7B2D8E, "Joka", "Majerhat"),
            MetroLineInfo("Orange Line", "O", 0xFFF08A24, "Kavi Subhash", "Beleghata"),
        ),
        busOperator = "WBTC",
        areas = listOf(
            "Park Street" to (22.5535 to 88.3520), "Salt Lake Sector V" to (22.5726 to 88.4339),
            "Ballygunge" to (22.5276 to 88.3650), "New Town" to (22.5867 to 88.4747),
            "Gariahat" to (22.5180 to 88.3670), "Esplanade" to (22.5646 to 88.3510),
            "Behala" to (22.4986 to 88.3100), "Dum Dum" to (22.6230 to 88.4200),
            "Howrah" to (22.5958 to 88.2636), "Shyambazar" to (22.6010 to 88.3720),
        ),
        landmarks = listOf(
            lm("ccu", "victoria", "Victoria Memorial", "Maidan", 22.5448, 88.3426),
            lm("ccu", "howrahbridge", "Howrah Bridge", "Howrah", 22.5851, 88.3468),
            lm("ccu", "kalighat", "Kalighat Temple", "Kalighat", 22.5203, 88.3426),
            lm("ccu", "eden", "Eden Gardens", "BBD Bagh", 22.5646, 88.3433, PlaceKind.VENUE),
            lm("ccu", "collegest", "College Street", "Boipara", 22.5745, 88.3630),
        ),
        flavor = CityFlavor(
            food = "kathi rolls, puchka and mishti doi", cafe = "Sienna Store & Café", tiffin = "Flurys",
            mall = "Quest Mall", market = "New Market", park = "Rabindra Sarobar", techHub = "Webel IT Park",
            artVenue = "Academy of Fine Arts", rooftop = "Sky Bar, Park Street", culturalVenue = "Rabindra Sadan",
            busyRoad = "AJC Bose Road flyover", altRoute = "EM Bypass",
        ),
    )

    val PUNE = City(
        id = "pune", name = "Pune", state = "Maharashtra", tagline = "Queen of the Deccan",
        centerLat = 18.5204, centerLng = 73.8567, south = 18.40, north = 18.68, west = 73.68, east = 74.02,
        imageRes = R.drawable.city_pune, municipalShort = "PMC",
        authorities = listOf(
            Authority("@PMCPune", "Pune Municipal Corporation", true),
            Authority("@PuneCityTraffic", "Pune City Traffic Police", true),
            Authority("@MSEDCL", "Maharashtra State Electricity Distribution Co.", true),
            Authority("@PMCWaterSupply", "PMC Water Supply Department", true),
        ),
        helplines = listOf(
            Helpline("PMC", "1800-103-0222", "18001030222", HelplineKind.CIVIC),
            Helpline("MSEDCL", "1912", "1912", HelplineKind.POWER),
            Helpline("Police", "112", "112", HelplineKind.POLICE),
        ),
        portals = CityPortals(
            utilityBills = "https://wss.mahadiscom.in",
            tradeLicense = "https://www.pmc.gov.in",
            bulkWaste = "https://www.pmc.gov.in",
            treeHazard = "https://www.pmc.gov.in",
        ),
        metroName = "Pune Metro",
        metroLines = listOf(
            MetroLineInfo("Purple Line", "P", 0xFF7B2D8E, "PCMC", "Swargate"),
            MetroLineInfo("Aqua Line", "A", 0xFF00A3C4, "Vanaz", "Ramwadi"),
        ),
        busOperator = "PMPML",
        areas = listOf(
            "Koregaon Park" to (18.5362 to 73.8930), "Kothrud" to (18.5074 to 73.8077),
            "Hinjewadi" to (18.5913 to 73.7389), "Viman Nagar" to (18.5679 to 73.9143),
            "Baner" to (18.5590 to 73.7868), "Shivajinagar" to (18.5308 to 73.8475),
            "Camp" to (18.5150 to 73.8780), "Hadapsar" to (18.5089 to 73.9260),
            "Aundh" to (18.5580 to 73.8075), "Deccan Gymkhana" to (18.5167 to 73.8410),
        ),
        landmarks = listOf(
            lm("pnq", "shaniwar", "Shaniwar Wada", "Kasba Peth", 18.5195, 73.8553),
            lm("pnq", "agakhan", "Aga Khan Palace", "Kalyani Nagar", 18.5524, 73.9015),
            lm("pnq", "station", "Pune Junction", "Agarkar Nagar", 18.5286, 73.8743, PlaceKind.METRO),
            lm("pnq", "dagdusheth", "Dagdusheth Ganpati Temple", "Budhwar Peth", 18.5164, 73.8561),
            lm("pnq", "fcroad", "FC Road", "Shivajinagar", 18.5236, 73.8410),
        ),
        flavor = CityFlavor(
            food = "misal pav, Bun maska and mastani", cafe = "Kimaya Café", tiffin = "Vaishali",
            mall = "Phoenix Marketcity Viman Nagar", market = "Tulshibaug", park = "Empress Garden", techHub = "Rajiv Gandhi Infotech Park",
            artVenue = "Pu La Deshpande Garden Gallery", rooftop = "High Spirits", culturalVenue = "Bal Gandharva Rang Mandir",
            busyRoad = "University Circle", altRoute = "Senapati Bapat Road",
        ),
    )

    val KOCHI = City(
        id = "kochi", name = "Kochi", state = "Kerala", tagline = "Queen of the Arabian Sea",
        centerLat = 9.9816, centerLng = 76.2999, south = 9.85, north = 10.12, west = 76.17, east = 76.42,
        imageRes = R.drawable.city_kochi, municipalShort = "Kochi Corporation",
        authorities = listOf(
            Authority("@KochiCorp", "Kochi Municipal Corporation", true),
            Authority("@KochiCityPolice", "Kochi City Traffic Police", true),
            Authority("@KSEBLtd", "Kerala State Electricity Board", true),
            Authority("@KWAOfficial", "Kerala Water Authority", true),
        ),
        helplines = listOf(
            Helpline("KSEB", "1912", "1912", HelplineKind.POWER),
            Helpline("Water Auth.", "1916", "1916", HelplineKind.WATER),
            Helpline("Police", "112", "112", HelplineKind.POLICE),
        ),
        portals = CityPortals(
            utilityBills = "https://wss.kseb.in",
            tradeLicense = "https://kochicorporation.lsgkerala.gov.in",
            bulkWaste = "https://kochicorporation.lsgkerala.gov.in",
            treeHazard = "https://kochicorporation.lsgkerala.gov.in",
        ),
        metroName = "Kochi Metro",
        metroLines = listOf(
            MetroLineInfo("Kochi Metro", "K", 0xFF00A99D, "Aluva", "Tripunithura"),
            MetroLineInfo("Water Metro", "W", LineColors.BLUE, "High Court", "Vypin"),
        ),
        busOperator = "KSRTC",
        areas = listOf(
            "Ernakulam MG Road" to (9.9710 to 76.2847), "Fort Kochi" to (9.9658 to 76.2421),
            "Kakkanad" to (10.0159 to 76.3419), "Edappally" to (10.0261 to 76.3083),
            "Vyttila" to (9.9674 to 76.3200), "Kaloor" to (9.9989 to 76.2950),
            "Panampilly Nagar" to (9.9580 to 76.2950), "Mattancherry" to (9.9580 to 76.2590),
            "Marine Drive" to (9.9816 to 76.2770),
        ),
        landmarks = listOf(
            lm("cok", "nets", "Chinese Fishing Nets", "Fort Kochi", 9.9683, 76.2420),
            lm("cok", "lulu", "Lulu Mall", "Edappally", 10.0271, 76.3080, PlaceKind.VENUE),
            lm("cok", "jewtown", "Jew Town", "Mattancherry", 9.9572, 76.2597),
            lm("cok", "junction", "Ernakulam Junction", "South", 9.9690, 76.2910, PlaceKind.METRO),
            lm("cok", "infopark", "Infopark", "Kakkanad", 10.0105, 76.3647),
        ),
        flavor = CityFlavor(
            food = "appam, karimeen pollichathu and tender coconut", cafe = "Kashi Art Café", tiffin = "Kayees Rahmathulla",
            mall = "Lulu Mall", market = "Broadway Market", park = "Subhash Bose Park", techHub = "Kerala Startup Mission",
            artVenue = "Aspinwall House", rooftop = "Seagull Rooftop", culturalVenue = "J.T. Pac",
            busyRoad = "Vyttila hub", altRoute = "Seaport–Airport Road",
        ),
    )

    val VIJAYAWADA = City(
        id = "vijayawada", name = "Vijayawada", state = "Andhra Pradesh", tagline = "City of Victory",
        centerLat = 16.5062, centerLng = 80.6480, south = 16.42, north = 16.60, west = 80.54, east = 80.76,
        imageRes = R.drawable.city_vijayawada, municipalShort = "VMC",
        authorities = listOf(
            Authority("@VMCVijayawada", "Vijayawada Municipal Corporation", true),
            Authority("@VjaCityPolice", "Vijayawada City Traffic Police", true),
            Authority("@APCPDCL", "AP Central Power Distribution Corporation", true),
            Authority("@VMCWater", "VMC Water Supply Wing", true),
        ),
        helplines = listOf(
            Helpline("APCPDCL", "1912", "1912", HelplineKind.POWER),
            Helpline("AP Grievance", "1100", "1100", HelplineKind.CIVIC),
            Helpline("Police", "112", "112", HelplineKind.POLICE),
        ),
        portals = CityPortals(
            utilityBills = "https://apcpdcl.in",
            tradeLicense = "https://cdma.ap.gov.in",
            bulkWaste = "https://cdma.ap.gov.in",
            treeHazard = "https://cdma.ap.gov.in",
        ),
        metroName = null,
        metroLines = emptyList(),
        busOperator = "APSRTC",
        areas = listOf(
            "Benz Circle" to (16.4996 to 80.6566), "Governorpet" to (16.5110 to 80.6260),
            "Labbipet" to (16.5030 to 80.6400), "Patamata" to (16.4930 to 80.6670),
            "Gunadala" to (16.5200 to 80.6600), "Bhavanipuram" to (16.5310 to 80.5980),
            "Moghalrajpuram" to (16.5080 to 80.6420), "Auto Nagar" to (16.4890 to 80.6890),
            "One Town" to (16.5180 to 80.6100), "Poranki" to (16.4740 to 80.7040),
        ),
        landmarks = listOf(
            lm("vga", "durga", "Kanaka Durga Temple", "Indrakeeladri", 16.5175, 80.6066),
            lm("vga", "barrage", "Prakasam Barrage", "Krishna River", 16.5068, 80.6040),
            lm("vga", "junction", "Vijayawada Junction", "Railway Colony", 16.5180, 80.6200, PlaceKind.METRO),
            lm("vga", "bhavani", "Bhavani Island", "Krishna River", 16.5250, 80.5860),
            lm("vga", "pnbs", "Pandit Nehru Bus Station", "Krishnalanka", 16.5090, 80.6170, PlaceKind.METRO),
        ),
        flavor = CityFlavor(
            food = "pesarattu, Andhra meals and Bandar laddu", cafe = "Coffee Cup", tiffin = "Babai Hotel",
            mall = "PVP Square", market = "Besant Road", park = "Rajiv Gandhi Park", techHub = "Medha Towers",
            artVenue = "Victoria Jubilee Museum", rooftop = "The Gateway Rooftop", culturalVenue = "Tummalapalli Kalakshetram",
            busyRoad = "Benz Circle flyover", altRoute = "Eluru Road",
        ),
    )

    val all = listOf(HYDERABAD, BENGALURU, MUMBAI, DELHI, CHENNAI, KOLKATA, PUNE, KOCHI, VIJAYAWADA)

    fun byId(id: String?) = all.firstOrNull { it.id == id }

    fun nearest(lat: Double, lng: Double): City =
        all.firstOrNull { it.contains(lat, lng) } ?: all.minBy { it.distanceToCenterKm(lat, lng) }
}
