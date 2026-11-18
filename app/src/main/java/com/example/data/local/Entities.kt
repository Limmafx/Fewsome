package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.FrenchNumberEngine
import com.example.domain.LearningMode
import com.example.domain.NumberProgress
import com.example.domain.SkillRecord
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "number_progress")
data class NumberProgressEntity(
    @PrimaryKey val number: Int,
    val skillsJson: String,
    val confusionsJson: String,
    val updatedAt: Long
) {
    fun toDomain(): NumberProgress {
        val skillsObj = runCatching { JSONObject(skillsJson) }.getOrNull() ?: JSONObject()
        val skillMap = LearningMode.entries.associateWith { mode ->
            val obj = skillsObj.optJSONObject(mode.name)
            if (obj == null) {
                SkillRecord()
            } else {
                val timesArr = obj.optJSONArray("times") ?: JSONArray()
                val times = buildList {
                    for (i in 0 until timesArr.length()) add(timesArr.optLong(i))
                }
                SkillRecord(
                    attempts = obj.optInt("attempts", 0),
                    correct = obj.optInt("correct", 0),
                    consecutiveCorrect = obj.optInt("consecutive", 0),
                    lastPracticedAt = obj.optLong("lastAt", -1L).takeIf { it > 0L },
                    nextReviewAt = obj.optLong("nextAt", -1L).takeIf { it > 0L },
                    averageResponseMs = obj.optLong("avgMs", -1L).takeIf { it > 0L },
                    recentResponseTimes = times,
                    reviewIntervalDays = obj.optInt("intervalDays", 0),
                    selfReportedCorrect = obj.optInt("selfReported", 0),
                    asrMatchedCount = obj.optInt("asrMatched", 0)
                )
            }
        }

        val confObj = runCatching { JSONObject(confusionsJson) }.getOrNull() ?: JSONObject()
        val confMap = buildMap<Int, Int> {
            val keys = confObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                k.toIntOrNull()?.let { put(it, confObj.optInt(k, 0)) }
            }
        }

        return NumberProgress(
            number = number,
            categories = FrenchNumberEngine.categoriesFor(number),
            skills = skillMap,
            confusionTargets = confMap
        )
    }

    companion object {
        fun fromDomain(progress: NumberProgress, nowMillis: Long): NumberProgressEntity {
            val skillsObj = JSONObject()
            progress.skills.forEach { (mode, rec) ->
                val obj = JSONObject()
                obj.put("attempts", rec.attempts)
                obj.put("correct", rec.correct)
                obj.put("consecutive", rec.consecutiveCorrect)
                if (rec.lastPracticedAt != null) obj.put("lastAt", rec.lastPracticedAt)
                if (rec.nextReviewAt != null) obj.put("nextAt", rec.nextReviewAt)
                if (rec.averageResponseMs != null) obj.put("avgMs", rec.averageResponseMs)
                obj.put("intervalDays", rec.reviewIntervalDays)
                obj.put("selfReported", rec.selfReportedCorrect)
                obj.put("asrMatched", rec.asrMatchedCount)
                val timesArr = JSONArray()
                rec.recentResponseTimes.forEach { timesArr.put(it) }
                obj.put("times", timesArr)
                skillsObj.put(mode.name, obj)
            }

            val confObj = JSONObject()
            progress.confusionTargets.forEach { (k, v) ->
                confObj.put(k.toString(), v)
            }

            return NumberProgressEntity(
                number = progress.number,
                skillsJson = skillsObj.toString(),
                confusionsJson = confObj.toString(),
                updatedAt = nowMillis
            )
        }
    }
}

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val modeName: String,
    val totalAnswered: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val accuracyPercent: Int,
    val averageResponseMs: Long,
    val isAssessment: Boolean,
    val missedNumbersCsv: String
)

@Entity(tableName = "diagnostic_snapshots")
data class DiagnosticSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val completedCount: Int,
    val recognitionAccuracy: Int?,
    val writingAccuracy: Int?,
    val listeningAccuracy: Int?,
    val averageLatencyMs: Long,
    val recommendedStageNumber: Int,
    val recommendedDifficultyName: String,
    val weakCategoriesCsv: String,
    val planSummaryRu: String
)
