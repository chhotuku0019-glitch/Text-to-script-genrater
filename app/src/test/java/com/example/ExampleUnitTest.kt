package com.example

import com.example.data.repository.ProjectRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun wordCountCalculation_isCorrect() {
        val emptyText = ""
        val sampleScript = "Hey everyone! Welcome back to my YouTube channel. Today we're learning AI."
        assertEquals(0, ProjectRepository.calculateWordCount(emptyText))
        assertEquals(11, ProjectRepository.calculateWordCount(sampleScript))
    }
}
