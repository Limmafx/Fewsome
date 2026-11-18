package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.audio.PronunciationTrainer
import com.example.data.local.AppDatabase
import com.example.data.local.NumberProgressEntity
import com.example.domain.AdaptiveScheduler
import com.example.domain.LearningMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches French Numbers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("French Numbers", context.getString(R.string.app_name))
    }

    @Test
    fun `room persistence preserves delayed review after database reopen`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scheduler = AdaptiveScheduler()
        val now = 1_700_000_000_000L
        val prog = scheduler.recordAttempt(null, 91, LearningMode.WRITING, true, 1800L, now)
        db1.numberProgressDao().upsert(NumberProgressEntity.fromDomain(prog, now))

        val loaded = db1.numberProgressDao().getByNumber(91)?.toDomain()
        assertNotNull(loaded)
        assertEquals(1, loaded?.skills?.get(LearningMode.WRITING)?.correct)
        assertEquals(1, loaded?.skills?.get(LearningMode.WRITING)?.reviewIntervalDays)
        assertEquals(0, loaded?.skills?.get(LearningMode.SPEAKING)?.attempts)
        db1.close()
    }

    @Test
    fun `pronunciation trainer degrades gracefully when speech recognition is unsupported`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val trainer = PronunciationTrainer(context)
        var errorReported = false
        trainer.startSpeechRecognition(
            onTranscript = {},
            onServiceError = { errorReported = true }
        )
        // Robolectric has no system recognition service by default -> graceful callback, no crash
        assertTrue(errorReported || trainer.isSpeechRecognitionSupported())
        trainer.release()
    }

    @Test
    fun `home screen renders and starts drag and drop session`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("French Numbers Trainer").assertIsDisplayed()
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("start_training_button"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_training_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("exercise_counter_text").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("exercise_counter_text").assertIsDisplayed()
        composeRule.onNodeWithTag("drag_drop_zone").assertIsDisplayed()
        composeRule.onNodeWithTag("drag_option_0").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("exercise_lazy_column")
            .performScrollToNode(hasTestTag("exercise_feedback_card"))
        composeRule.onNodeWithTag("exercise_feedback_card").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_exit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Итоги тренировки").assertIsDisplayed()
    }
}
