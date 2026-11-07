package com.sheharsaathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherDetailTest {
    @Test fun localTimeLabels() {
        assertEquals("6 PM", WeatherService.hourLabel("2026-11-07T18:00"))
        assertEquals("12 AM", WeatherService.hourLabel("2026-11-08T00:00"))
        assertEquals("12 PM", WeatherService.hourLabel("2026-11-08T12:00"))
        assertEquals("6:17 AM", WeatherService.clock("2026-11-07T06:17"))
        assertEquals("5:42 PM", WeatherService.clock("2026-11-07T17:42"))
        assertEquals("Sun", WeatherService.weekday("2026-11-08"))
        assertEquals("8 Nov", WeatherService.shortDate("2026-11-08"))
    }

    @Test fun windDirections() {
        assertEquals("N", compass(0)); assertEquals("ENE", compass(60)); assertEquals("S", compass(180)); assertEquals("N", compass(355))
    }

    @Test fun indiaScaleFromParticulates() {
        // PM2.5 45 µg/m³ sits halfway through CPCB's 31–60 band → about 75 (Satisfactory)
        val a = AirDetail(146, pm25 = 45.0, pm10 = 47.8, o3 = null, no2 = null, so2 = null, co = null, hours = emptyList())
        assertEquals(75, a.indiaAqi)
        assertEquals("Satisfactory", indiaAqiLabel(75))
        assertEquals("Unhealthy for sensitive groups", usAqiLabel(146))
    }
}
