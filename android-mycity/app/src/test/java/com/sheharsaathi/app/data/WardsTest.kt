package com.sheharsaathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WardsTest {
    private val wards = listOf(
        Ward(231, "HITEC City", "CMC", "Kukatpally", "Madhapur", 17.4474, 78.3762),
        Ward(232, "Madhapur", "CMC", "Kukatpally", "Madhapur", 17.4483, 78.3915),
        Ward(234, "Kondapur", "CMC", "Serilingampally", "Serilingampally", 17.4600, 78.3570),
        Ward(252, "KPHB Colony", "CMC", "Kukatpally", "Kukatpally", 17.4935, 78.3990),
        Ward(165, "Gandhi Nagar", "GHMC", "Secunderabad", "Kavadiguda", 17.4060, 78.4920),
        Ward(268, "Ramachandrapuram (RC Puram)", "CMC", "Serilingampally", "Ameenpur", 17.4930, 78.3000),
        Ward(101, "Ghansi Bazar", "GHMC", "Charminar", "Charminar", 17.3616, 78.4747),
    )
    private fun place(name: String, area: String, lat: Double, lng: Double) = Place("p", name, area, lat, lng, PlaceKind.CURRENT)

    @Test fun areaNameWinsOverTheNearestCentre() {
        // Closer to Madhapur's centre, but the address says HITEC City.
        val m = WardFinder.find(place("Cyber Towers", "HITEC City, Hyderabad", 17.4480, 78.3870), wards)!!
        assertEquals(231, m.ward.no)
        assertTrue(m.byName)
        assertEquals(WardSource.LOCATION, m.source)
    }

    @Test fun oldOsmWardNumbersAreIgnored() {
        val m = WardFinder.find(place("Shop", "Ward 107 Madhapur, Hyderabad", 17.4483, 78.3915), wards)!!
        assertEquals(232, m.ward.no)
    }

    @Test fun spellingsAndColonySuffixMatch() {
        assertEquals(231, WardFinder.find(place("x", "Hitech City", 17.447, 78.376), wards)!!.ward.no)
        assertEquals(252, WardFinder.find(place("x", "KPHB, Kukatpally", 17.494, 78.399), wards)!!.ward.no)
        assertEquals(268, WardFinder.find(place("x", "RC Puram", 17.493, 78.300), wards)!!.ward.no)
    }

    @Test fun aSameNamedPlaceFarAwayIsNotTheWard() {
        // A "Gandhi Nagar" next to Kondapur isn't ward 165 in Secunderabad.
        val m = WardFinder.find(place("x", "Gandhi Nagar, Kondapur", 17.4605, 78.3575), wards)!!
        assertEquals(234, m.ward.no)
        assertFalse(m.byName && m.ward.no == 165)
    }

    @Test fun nearestCentreWhenNoNameMatches() {
        val m = WardFinder.find(place("Some lane", "Unknown Layout", 17.3620, 78.4740), wards)!!
        assertEquals(101, m.ward.no)
        assertFalse(m.byName)
    }

    @Test fun roughCentresOnlyCountForNameMatches() {
        val rough = wards + Ward(98, "Pathargatti", "GHMC", "Charminar", "Charminar", 17.3618, 78.4745, approx = true)
        // Nearest is the rough Pathargatti point, but it isn't used for "nearest ward".
        assertEquals(101, WardFinder.find(place("x", "Some lane", 17.3618, 78.4745), rough)!!.ward.no)
        // By name it still matches.
        assertEquals(98, WardFinder.find(place("x", "Pathargatti, Hyderabad", 17.3618, 78.4745), rough)!!.ward.no)
    }

    @Test fun outsideTheCorporationsIsNull() {
        assertNull(WardFinder.find(place("Warangal", "Warangal", 17.97, 79.59), wards))
    }

    @Test fun searchByNumberNameAndCircle() {
        assertEquals(listOf(231, 232, 234), WardFinder.search("23", wards).map { it.no })
        assertEquals(listOf(231), WardFinder.search("Ward 231", wards).map { it.no })
        assertEquals(234, WardFinder.search("konda", wards).first().no)
        assertEquals(setOf(231, 232), WardFinder.search("Madhapur", wards).map { it.no }.toSet())
        assertEquals(wards.size, WardFinder.search("", wards).size)
    }
}
