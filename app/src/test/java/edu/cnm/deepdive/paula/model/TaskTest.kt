package edu.cnm.deepdive.paula.model

import edu.cnm.deepdive.paula.model.entity.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit test suite verifying Task entity construction, state transitions, and defaults.
 */
class TaskTest {

    @Test
    fun newTask_hasDefaultValues() {
        val task = Task(
            userId = 1L,
            title = "Fold floral quilt"
        )

        assertEquals(0L, task.id)
        assertEquals(1L, task.userId)
        assertEquals("Fold floral quilt", task.title)
        assertFalse(task.isCompleted)
        assertEquals(1, task.priority, "Default priority must be medium (1)")
        assertNull(task.notes)
        assertNull(task.dueDate)
        assertNull(task.completedAt)
        assertNull(task.photoUri)
        assertNotNull(task.createdAt)
    }

    @Test
    fun task_withPhotoUri_preservesUri() {
        val uri = "content://edu.cnm.deepdive.paula.fileprovider/external_images/proof_123.jpg"
        val task = Task(
            userId = 1L,
            title = "Clean laundry space",
            photoUri = uri
        )

        assertEquals(uri, task.photoUri)
    }

    @Test
    fun task_toggleComplete_updatesTimestamps() {
        val task = Task(
            userId = 1L,
            title = "Organize hall closet"
        )

        val completionTime = Instant.now()
        val completedTask = task.copy(
            isCompleted = true,
            completedAt = completionTime
        )

        assertTrue(completedTask.isCompleted)
        assertEquals(completionTime, completedTask.completedAt)

        val reopenedTask = completedTask.copy(
            isCompleted = false,
            completedAt = null
        )

        assertFalse(reopenedTask.isCompleted)
        assertNull(reopenedTask.completedAt)
    }

    @Test
    fun task_priorityLevels() {
        val lowTask = Task(userId = 1L, title = "Low Task", priority = 0)
        val medTask = Task(userId = 1L, title = "Med Task", priority = 1)
        val highTask = Task(userId = 1L, title = "High Task", priority = 2)

        assertEquals(0, lowTask.priority)
        assertEquals(1, medTask.priority)
        assertEquals(2, highTask.priority)
    }
}
