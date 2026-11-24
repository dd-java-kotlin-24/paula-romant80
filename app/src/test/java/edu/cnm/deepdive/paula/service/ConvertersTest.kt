package edu.cnm.deepdive.paula.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import java.time.LocalDate

/**
 * Unit test suite verifying Room database TypeConverters for Instant and LocalDate.
 */
class ConvertersTest {

    private lateinit var converters: Converters

    @BeforeEach
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun instantToEpochMilli_roundTrip() {
        val now = Instant.now()
        val epochMilli = converters.fromInstant(now)
        val reconstructed = converters.toInstant(epochMilli)
        assertEquals(now.toEpochMilli(), reconstructed?.toEpochMilli())
    }

    @Test
    fun nullInstant_returnsNull() {
        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
    }

    @Test
    fun localDateToEpochDay_roundTrip() {
        val today = LocalDate.now()
        val epochDay = converters.fromLocalDate(today)
        val reconstructed = converters.toLocalDate(epochDay)
        assertEquals(today, reconstructed)
    }

    @Test
    fun nullLocalDate_returnsNull() {
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
    }

    @ParameterizedTest
    @ValueSource(longs = [0L, 1000L, 18262L, 20000L, -500L])
    fun parameterizedEpochDay_convertsAccurately(epochDay: Long) {
        val date = converters.toLocalDate(epochDay)
        val backToDay = converters.fromLocalDate(date)
        assertEquals(epochDay, backToDay)
    }

    @ParameterizedTest
    @ValueSource(longs = [0L, 1700000000000L, 1732468800000L, 2000000000000L])
    fun parameterizedEpochMilli_convertsAccurately(epochMilli: Long) {
        val instant = converters.toInstant(epochMilli)
        val backToMilli = converters.fromInstant(instant)
        assertEquals(epochMilli, backToMilli)
    }
}
