package com.sheharsaathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressSearchTest {
    @Test fun ghmcDoorNumberAndStreet() {
        val a = AddressSearch.parse("8-2-293/82/A, Road No. 36, Jubilee Hills, Hyderabad")
        assertEquals("8-2-293/82/A", a.door)
        assertEquals(listOf("Road No 36", "Jubilee Hills"), a.parts)
        assertEquals("Jubilee Hills", a.locality)
    }

    @Test fun flatAndPlotPrefixesArePeeledOff() {
        val f = AddressSearch.parse("Flat 302, Aparna Towers, Kondapur")
        assertEquals(listOf("Flat 302"), f.units)
        assertEquals(listOf("Aparna Towers", "Kondapur"), f.parts)
        val p = AddressSearch.parse("Plot No. 45, Kavuri Hills Phase 2, Madhapur, Hyderabad 500081")
        assertEquals(listOf("Plot No 45"), p.units)
        assertEquals(listOf("Kavuri Hills Phase 2", "Madhapur"), p.parts)   // city and pin code dropped
        assertNull(p.door)
    }

    @Test fun landmarkDirections() {
        val a = AddressSearch.parse("lane opposite ratnadeep, kukatpally")
        assertEquals("ratnadeep", a.landmark)
        assertEquals("kukatpally", a.locality)
    }

    @Test fun doorNumberMatching() {
        assertEquals(1.0, AddressSearch.doorMatch("8-2-293/82/A", "8-2-293/82/A/1125"), 0.0)
        assertEquals(0.5, AddressSearch.doorMatch("1-98/9/2", "1-98/90/25"), 0.0)
        assertEquals(0.0, AddressSearch.doorMatch("1-98/9/2", null), 0.0)
    }

    @Test fun abbreviations() {
        assertEquals("Road No 12 banjara hills", AddressSearch.tidy("rd no. 12 banjara hills"))
    }
}
