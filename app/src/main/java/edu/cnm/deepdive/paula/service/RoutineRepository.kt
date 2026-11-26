/*
 *  Copyright 2026 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.paula.service

import edu.cnm.deepdive.paula.model.dao.RoutineDao
import edu.cnm.deepdive.paula.model.dao.RoutineLogDao
import edu.cnm.deepdive.paula.model.entity.Routine
import edu.cnm.deepdive.paula.model.entity.RoutineLog
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Service repository providing access to recurring habit routines, completion logging,
 * and streak tracking logic.
 */
@Singleton
class RoutineRepository @Inject constructor(
    private val routineDao: RoutineDao,
    private val routineLogDao: RoutineLogDao
) {

    fun get(routineId: Long): Flow<Routine?> =
        routineDao.select(routineId)

    fun getAll(): Flow<List<Routine>> =
        routineDao.selectAll()

    fun getByUser(userId: Long): Flow<List<Routine>> =
        routineDao.selectByUser(userId)

    fun getActiveByUser(userId: Long): Flow<List<Routine>> =
        routineDao.selectActiveByUser(userId)

    fun getByCategory(categoryId: Long): Flow<List<Routine>> =
        routineDao.selectByCategory(categoryId)

    suspend fun findById(routineId: Long): Routine? =
        routineDao.findById(routineId)

    suspend fun save(routine: Routine): Routine {
        return if (routine.id == 0L) {
            val id = routineDao.insert(routine)
            routine.copy(id = id)
        } else {
            routineDao.update(routine)
            routine
        }
    }

    suspend fun remove(routine: Routine): Int =
        routineDao.delete(routine)

    fun getLogs(routineId: Long): Flow<List<RoutineLog>> =
        routineLogDao.selectForRoutine(routineId)

    fun getCompletionCount(routineId: Long): Flow<Int> =
        routineLogDao.getCompletionCount(routineId)

    suspend fun recordCompletion(
        routine: Routine,
        completedDate: LocalDate = LocalDate.now(),
        notes: String? = null,
        photoUri: String? = null
    ): Routine {
        val existingLog = routineLogDao.findByRoutineAndDate(routine.id, completedDate)
        if (existingLog != null) {
            return routine
        }

        val log = RoutineLog(
            routineId = routine.id,
            completedDate = completedDate,
            completedAt = Instant.now(),
            notes = notes,
            photoUri = photoUri
        )
        routineLogDao.insert(log)

        val (newStreak, newBestStreak) = calculateNextStreak(
            frequency = routine.frequency,
            currentStreak = routine.currentStreak,
            bestStreak = routine.bestStreak,
            lastCompletedDate = routine.lastCompletedDate,
            completedDate = completedDate
        )
        val updated = routine.copy(
            currentStreak = newStreak,
            bestStreak = newBestStreak,
            lastCompletedDate = completedDate
        )
        routineDao.update(updated)
        return updated
    }

    suspend fun removeCompletion(
        routine: Routine,
        completedDate: LocalDate = LocalDate.now()
    ): Routine {
        val existingLog = routineLogDao.findByRoutineAndDate(routine.id, completedDate)
        if (existingLog != null) {
            routineLogDao.delete(existingLog)
            val newStreak = (routine.currentStreak - 1).coerceAtLeast(0)
            val previousLog = routineLogDao.findLatestForRoutine(routine.id)
            val updated = routine.copy(
                currentStreak = newStreak,
                lastCompletedDate = if (newStreak > 0) previousLog?.completedDate else null
            )
            routineDao.update(updated)
            return updated
        }
        return routine
    }

    companion object {
        fun calculateNextStreak(
            frequency: String,
            currentStreak: Int,
            bestStreak: Int,
            lastCompletedDate: LocalDate?,
            completedDate: LocalDate
        ): Pair<Int, Int> {
            val newStreak = when {
                lastCompletedDate == completedDate -> currentStreak
                lastCompletedDate != null && isConsecutiveCompletion(frequency, lastCompletedDate, completedDate) ->
                    currentStreak + 1
                else -> 1
            }
            return Pair(newStreak, maxOf(bestStreak, newStreak))
        }

        fun parseScheduledDays(frequency: String): Set<DayOfWeek> {
            val upper = frequency.uppercase().trim()
            return when {
                upper == "DAILY" -> DayOfWeek.values().toSet()
                upper == "WEEKDAYS" -> setOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                )
                upper == "WEEKENDS" -> setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                else -> {
                    val days = mutableSetOf<DayOfWeek>()
                    val tokens = upper.split(",", " ", ";", "/").map { it.trim() }
                    for (token in tokens) {
                        when {
                            token.startsWith("MON") -> days.add(DayOfWeek.MONDAY)
                            token.startsWith("TUE") -> days.add(DayOfWeek.TUESDAY)
                            token.startsWith("WED") -> days.add(DayOfWeek.WEDNESDAY)
                            token.startsWith("THU") -> days.add(DayOfWeek.THURSDAY)
                            token.startsWith("FRI") -> days.add(DayOfWeek.FRIDAY)
                            token.startsWith("SAT") -> days.add(DayOfWeek.SATURDAY)
                            token.startsWith("SUN") -> days.add(DayOfWeek.SUNDAY)
                        }
                    }
                    days
                }
            }
        }

        private fun isConsecutiveCompletion(
            frequency: String,
            lastDate: LocalDate,
            currentDate: LocalDate
        ): Boolean {
            if (!lastDate.isBefore(currentDate)) return false
            val scheduled = parseScheduledDays(frequency)
            if (scheduled.isEmpty()) {
                return lastDate == currentDate.minusDays(1)
            }
            var cursor = currentDate.minusDays(1)
            while (!scheduled.contains(cursor.dayOfWeek) && cursor.isAfter(lastDate)) {
                cursor = cursor.minusDays(1)
            }
            return lastDate == cursor || lastDate == currentDate.minusDays(1)
        }
    }

}
