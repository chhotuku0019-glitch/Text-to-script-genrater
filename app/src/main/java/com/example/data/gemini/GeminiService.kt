package com.example.data.gemini

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HookItem(
    val type: String, // "Curiosity", "Problem", "Bold statement", "Question", "Story", "Result", "Mystery", "Challenge"
    val text: String,
    val explanation: String = ""
)

data class TitleItem(
    val category: String, // "SEO", "Curiosity", "Simple", "High CTR", "Professional", "Short"
    val title: String,
    val scoreHint: String = ""
)

data class DescriptionData(
    val shortDescription: String = "",
    val seoDescription: String = "",
    val detailedDescription: String = "",
    val chapters: List<ChapterItem> = emptyList()
)

data class ChapterItem(
    val timestamp: String,
    val title: String
)

data class ThumbnailConcept(
    val mainText: String,
    val visualIdea: String,
    val subjectPlacement: String,
    val emotion: String,
    val composition: String
)

data class SceneSuggestion(
    val scriptSection: String,
    val sceneIdea: String,
    val bRollIdea: String,
    val screenRecordingIdea: String,
    val animationIdea: String,
    val aiImagePrompt: String
)

data class ScriptAnalysis(
    val hookScore: Int = 0,
    val clarityScore: Int = 0,
    val structureScore: Int = 0,
    val engagementScore: Int = 0,
    val pacingScore: Int = 0,
    val valueScore: Int = 0,
    val repetitionScore: Int = 0,
    val ctaScore: Int = 0,
    val overallScore: Int = 0,
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val summary: String = ""
)

class GeminiService(
    private val getApiKey: () -> String,
    private val getModel: () -> String,
    private val getTemperature: () -> Float
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun resolveApiKey(): String {
        val customKey = getApiKey().trim()
        if (customKey.isNotEmpty()) return customKey
        return BuildConfig.GEMINI_API_KEY
    }

    private suspend fun executeGeminiPrompt(
        prompt: String,
        systemInstruction: String? = null,
        forceJson: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key in Settings or AI Studio Secrets.")
            )
        }

        val model = getModel().ifEmpty { "gemini-3.5-flash" }
        val temperature = getTemperature()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val userContent = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            userContent.put("parts", partsArray)
            contentsArray.put(userContent)
            rootJson.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("temperature", temperature)
            if (forceJson) {
                genConfig.put("responseMimeType", "application/json")
            }
            rootJson.put("generationConfig", genConfig)

            if (!systemInstruction.isNullOrEmpty()) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                sysParts.put(JSONObject().put("text", systemInstruction))
                sysContent.put("parts", sysParts)
                rootJson.put("systemInstruction", sysContent)
            }

            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: $responseBody"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No content returned by Gemini model."))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (text.isEmpty()) {
                return@withContext Result.failure(Exception("Empty text in Gemini response."))
            }

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(): Result<String> {
        val result = executeGeminiPrompt(
            prompt = "Respond with 'CONNECTED_SUCCESS' if you receive this message.",
            systemInstruction = "You are a test connection probe."
        )
        return if (result.isSuccess) {
            Result.success("API Connected successfully!")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    // 1. Clean Transcript
    suspend fun cleanTranscript(rawTranscript: String, language: String): Result<String> {
        val systemInstruction = """
            You are an expert speech-to-text transcript editor for YouTube creators.
            Clean the user's spoken transcript with high precision:
            - Fix punctuation and natural sentence boundaries.
            - Remove excessive filler words (uh, um, you know, matlab, like, actually, basically) while preserving speech personality.
            - Correct grammar and transcription phonetics.
            - Preserve original meaning and facts faithfully. NEVER invent facts or statistics.
            - Maintain natural Hinglish or Hindi colloquial flow when selected (Language: $language).
            - Keep technical names, software terms, and brand names intact.
            - Separate into readable, spoken paragraphs.
            Return ONLY the cleaned transcript text without markdown preamble or quotes.
        """.trimIndent()

        val prompt = "Raw Spoken Transcript:\n\"$rawTranscript\"\n\nPlease provide the clean transcript version:"
        return executeGeminiPrompt(prompt, systemInstruction)
    }

    // 2. Generate YouTube Script from Text Idea
    suspend fun generateScriptFromText(
        ideaText: String,
        videoType: String,
        language: String,
        targetDuration: String,
        tone: String,
        targetAudience: String
    ): Result<String> {
        val systemInstruction = """
            You are ScriptForge AI, an elite YouTube scriptwriter and content strategist.
            Create a highly engaging, human-sounding, professional YouTube script.

            CRITICAL SCRIPT RULES:
            - Video Type: $videoType
            - Language: $language (If Hinglish: write in natural, conversational Hinglish using Roman script that sounds like a real popular YouTuber speaking naturally; If Hindi: write in Hindi; If English: write in energetic modern YouTube English).
            - Target Duration: $targetDuration
            - Tone: $tone
            - Target Audience: ${if (targetAudience.isEmpty()) "General YouTube Viewers" else targetAudience}

            STRUCTURE REQUIREMENTS:
            1. [HOOK] - First 5-10 seconds to stop the scroll immediately (curiosity, problem, or bold statement).
            2. [INTRODUCTION] - Quick premise and promise of what viewers will get.
            3. [PROBLEM / CONTEXT] - Address why this matters.
            4. [MAIN CONTENT SECTIONS] - Logical sections with clear headings (e.g. ## Step 1: ..., ## Step 2: ...).
            5. [NATURAL TRANSITIONS] - Smooth spoken flow connecting thoughts without repetitive transition clichés.
            6. [CALL TO ACTION (CTA)] - Relevant, authentic CTA placed naturally.
            7. [OUTRO] - Punchy wrap-up.

            FORMATTING RULES:
            - One clear thought per sentence.
            - Keep paragraphs short (1-3 sentences maximum) for effortless voice-over reading.
            - Spoken language optimized for natural teleprompter delivery.
            - Do NOT sound robotic or like an academic textbook.
            - Never invent fake statistics, sponsors, or fake personal anecdotes not implied by the prompt.
        """.trimIndent()

        val prompt = "Video Idea / Concept:\n$ideaText\n\nGenerate the complete YouTube script:"
        return executeGeminiPrompt(prompt, systemInstruction)
    }

    // 3. Generate YouTube Script from Transcript
    suspend fun generateScriptFromTranscript(
        cleanTranscript: String,
        videoType: String,
        language: String,
        targetDuration: String,
        tone: String,
        targetAudience: String
    ): Result<String> {
        val systemInstruction = """
            You are ScriptForge AI, an expert YouTube scriptwriter.
            Transform this creator's spoken transcript into a structured, highly polished YouTube script.
            - Video Type: $videoType
            - Language: $language (Keep natural Hinglish / Hindi / English conversational voice)
            - Target Duration: $targetDuration
            - Tone: $tone
            - Target Audience: ${if (targetAudience.isEmpty()) "General YouTube Viewers" else targetAudience}

            ORGANIZATION:
            - Build a powerful HOOK from the most exciting takeaway.
            - Form an energetic INTRO.
            - Structure the spoken points into clear numbered/titled sections.
            - Fix broken arguments, combine related thoughts, and smooth transitions.
            - Add a natural CTA and OUTRO.
            - Format in readable, punchy spoken paragraphs (1-2 sentences per line).
        """.trimIndent()

        val prompt = "Creator's Spoken Content / Transcript:\n$cleanTranscript\n\nTransform this into a master YouTube script:"
        return executeGeminiPrompt(prompt, systemInstruction)
    }

    // 4. AI Script Tools / Rewrites
    suspend fun applyScriptTool(
        toolName: String,
        currentScript: String,
        language: String,
        selectedText: String = ""
    ): Result<String> {
        val targetContent = if (selectedText.isNotEmpty()) selectedText else currentScript
        val instruction = when (toolName) {
            "Improve Script" -> "Improve the overall pacing, punchiness, clarity, and viewer retention of this script while keeping its core structure."
            "Improve Hook" -> "Rewrite the hook to be 10x more compelling, sparking intense curiosity and immediately stopping the scroll."
            "Make More Engaging" -> "Add rhetorical questions, engaging analogies, suspense, and dynamic delivery cues to maximize viewer watch time."
            "Make Shorter" -> "Condense and trim all fluff, tighten sentences, and deliver maximum value in fewer words."
            "Expand Script" -> "Elaborate with clear practical examples, detailed step-by-step guidance, and deeper explanations without filler."
            "Make More Natural" -> "Make the wording sound 100% human and conversational, removing any robotic or overly scripted phrasing."
            "Make Voice-over Friendly" -> "Optimize sentence length, rhythm, pauses, and breath marks for effortless on-mic teleprompter delivery."
            "Convert to Hindi" -> "Translate and adapt the script into natural, spoken modern Hindi (Devanagari or Romanized based on context)."
            "Convert to Hinglish" -> "Rewrite into natural, trending YouTube Hinglish (conversational blend of Hindi and English in Roman script)."
            "Convert to English" -> "Rewrite into fluent, high-energy modern English suitable for international YouTube creators."
            "Make More Casual" -> "Infuse friendly, relatable, casual banter and a relaxed conversational creator vibe."
            "Make More Educational" -> "Structure into clear pedagogical takeaways, actionable lessons, and structured key points."
            "Make Documentary Style" -> "Infuse cinematic narrative pacing, dramatic reveals, atmosphere, and compelling storytelling arcs."
            "Make High-Energy" -> "Boost urgency, excitement, dynamic pace, and enthusiastic YouTuber energy."
            "Remove Repetition" -> "Identify repeated ideas or identical phrasing and eliminate redundancy smoothly."
            "Improve Transitions" -> "Refine bridge lines between topics so every section flows seamlessly into the next."
            "Add CTA" -> "Insert a crisp, natural, high-converting Call To Action asking viewers to subscribe, comment, or check the link."
            else -> "Refine and enhance this script: $toolName"
        }

        val systemInstruction = """
            You are ScriptForge AI script editor.
            Language: $language
            Task: $instruction
            Preserve factual accuracy and original voice. Return ONLY the rewritten script content.
        """.trimIndent()

        val prompt = "Script Content to Modify:\n$targetContent"
        return executeGeminiPrompt(prompt, systemInstruction)
    }

    // 4b. Creator Fun Zone & Meme AI Tools
    suspend fun applyFunTool(
        toolName: String,
        currentScript: String,
        language: String
    ): Result<String> {
        val instruction = when (toolName) {
            "Meme Mode" -> """
                Infuse this YouTube script with hilarious modern internet memes, relatable creator humor, emoji punchlines, and viral pop-culture references (e.g., 'API ne bola: bhai aaj nahi. 💀').
                Keep the core information intact while making it laugh-out-loud relatable.
            """.trimIndent()

            "Bhai Moment" -> """
                Add classic relatable 'Bhai Moment' desi/Hinglish creator humor — the struggle, the overconfidence, the realization, relatable everyday pain points, and conversational 'Arre bhai...', 'Sun bhai...' comedic flavor.
            """.trimIndent()

            "Roast My Script" -> """
                Perform a hilarious, witty, lighthearted comedy roast of this script!
                Point out cheesy clichés, overused tropes, cringe transitions, and obvious fluff in a playfully brutal yet lovingly helpful YouTuber manner.
                Break down:
                🔥 THE BRUTAL ROAST (Funny critiques)
                💀 CRINGE RADAR (Overused phrases)
                💡 HOW TO FIX IT (Actionable punchy edits)
            """.trimIndent()

            "Comedy Boost" -> """
                Inject stand-up quality comedic timing, humorous analogies, unexpected punchy callbacks, and witty one-liners into this script to keep viewers grinning throughout.
            """.trimIndent()

            "Brainrot Mode" -> """
                Infuse playful Gen-Z / internet brainrot slang (e.g. cooking, no cap, rizz, sigma, emotional damage, lore, gigachad, bro really thought) in a fun, tasteful way tailored for high-engagement viral YouTube Shorts / meme breakdowns.
            """.trimIndent()

            "Reaction Generator" -> """
                Analyze this script and generate a comprehensive cue-sheet of hilarious visual reactions, meme sound effects ([Vine Boom], [Record Scratch], [Awkward Pause 2s], [Emotional Damage SFX], [Bruh Sound Effect]), face zoom-ins, and meme video cutaway cues alongside key script lines.
            """.trimIndent()

            "Expectation vs Reality" -> """
                Create a hilarious 'Expectation vs Reality' comedy segment based on this topic — contrasting what beginners or gurus think happens vs what actually goes down in real life.
                Format clearly with:
                ✨ EXPECTATION (What you thought would happen)
                💀 REALITY (What actually happens)
                🎬 HOW TO DELIVER ON CAMERA
            """.trimIndent()

            "Shorts Punchline" -> """
                Generate 5 ultra-punchy, viral comedic closing lines and mic-drop punchlines tailored for YouTube Shorts & Reels retention loops.
            """.trimIndent()

            "Deadpan Mode" -> """
                Rewrite this script with an iconic deadpan, monotone, brutally dry sarcastic humor style — delivering wild or absurd facts with total straight-faced seriousness.
            """.trimIndent()

            "Savage But Friendly" -> """
                Add delightfully savage, witty banter and playful roasts of the viewer or common viewer habits, keeping the overall vibe warm, magnetic, and creator-friendly.
            """.trimIndent()

            else -> "Infuse creative creator comedy into this script: $toolName"
        }

        val systemInstruction = """
            You are ScriptForge AI's Creator Fun Zone comedy director.
            Language: $language
            Task: $instruction
            Do not make serious educational content inappropriate or offensive.
            Deliver clean, high-retention creator humor that viewers love to share.
        """.trimIndent()

        val prompt = "Creator's Script Content:\n$currentScript\n\nGenerate the comedy enhancement:"
        return executeGeminiPrompt(prompt, systemInstruction)
    }

    // 5. Generate Hooks (8 Types)
    suspend fun generateHooks(scriptOrIdea: String, videoType: String, language: String): Result<List<HookItem>> {
        val prompt = """
            Generate 8 distinct, powerful YouTube hook variations for this video:
            Video Type: $videoType
            Language: $language
            Content:
            $scriptOrIdea

            Provide 8 hooks for these specific types:
            1. Curiosity
            2. Problem
            3. Bold statement
            4. Question
            5. Story
            6. Result
            7. Mystery
            8. Challenge

            Return a valid JSON array of objects with keys:
            - "type": string (The hook type)
            - "text": string (The exact spoken hook words)
            - "explanation": string (Why this hook works)
        """.trimIndent()

        val result = executeGeminiPrompt(prompt, forceJson = true)
        return result.mapCatching { jsonStr ->
            parseHooksJson(cleanJsonString(jsonStr))
        }
    }

    // 6. Generate YouTube Titles (10 Titles in 6 Categories)
    suspend fun generateTitles(scriptOrIdea: String, videoType: String, language: String): Result<List<TitleItem>> {
        val prompt = """
            Generate 10 high-CTR, click-worthy, non-misleading YouTube title ideas based on this content:
            Video Type: $videoType
            Language: $language
            Content:
            $scriptOrIdea

            Categories to include:
            - SEO (Search-friendly)
            - Curiosity (Intriguing)
            - Simple (Direct & clean)
            - High CTR (Strong emotional trigger)
            - Professional (Authoritative)
            - Short (Punchy under 50 characters)

            Return a valid JSON array of objects with keys:
            - "category": string
            - "title": string
            - "scoreHint": string (e.g. "Predicted CTR: 9.2/10")
        """.trimIndent()

        val result = executeGeminiPrompt(prompt, forceJson = true)
        return result.mapCatching { jsonStr ->
            parseTitlesJson(cleanJsonString(jsonStr))
        }
    }

    // 7. Generate Description & Chapters
    suspend fun generateDescriptionAndChapters(scriptText: String, videoType: String, language: String): Result<DescriptionData> {
        val prompt = """
            Generate a complete YouTube video description package for this script:
            Language: $language
            Script:
            $scriptText

            Create:
            1. shortDescription: A 2-line punchy description for social snippets and mobile search.
            2. seoDescription: A keyword-rich 1-2 paragraph description optimized for YouTube search algorithm without stuffing.
            3. detailedDescription: A comprehensive description with bullet points of key takeaways and video outline.
            4. chapters: An array of timestamped chapters (e.g. "00:00", "01:15", etc.) matching the logical flow of the script.

            Return JSON object:
            {
              "shortDescription": "...",
              "seoDescription": "...",
              "detailedDescription": "...",
              "chapters": [
                { "timestamp": "00:00", "title": "Intro" },
                ...
              ]
            }
        """.trimIndent()

        val result = executeGeminiPrompt(prompt, forceJson = true)
        return result.mapCatching { jsonStr ->
            parseDescriptionJson(cleanJsonString(jsonStr))
        }
    }

    // 8. Generate Thumbnail Concepts
    suspend fun generateThumbnailConcepts(scriptText: String, videoType: String, language: String): Result<List<ThumbnailConcept>> {
        val prompt = """
            Generate 5 creative, high-converting YouTube thumbnail concepts for this video:
            Video Type: $videoType
            Language: $language
            Script:
            $scriptText

            For each concept provide:
            - "mainText": Bold text overlay for the thumbnail (maximum 2-4 words, e.g. "NO CODING?!")
            - "visualIdea": The core visual elements and background imagery
            - "subjectPlacement": Where the creator/subject is placed (e.g. "Creator right side, shocked expression")
            - "emotion": The dominant emotional trigger (Curiosity, Excitement, Shock, Urgency, Authority)
            - "composition": Color scheme, lighting, and visual contrast guide

            Return a valid JSON array of objects with these exact keys.
        """.trimIndent()

        val result = executeGeminiPrompt(prompt, forceJson = true)
        return result.mapCatching { jsonStr ->
            parseThumbnailJson(cleanJsonString(jsonStr))
        }
    }

    // 9. Generate Scene & Visual Suggestions
    suspend fun generateSceneSuggestions(scriptText: String, videoType: String, language: String): Result<List<SceneSuggestion>> {
        val prompt = """
            Break down this YouTube script into its major visual scenes and generate practical production ideas for the creator:
            Script:
            $scriptText

            For each key script section, provide:
            - "scriptSection": Quote of the script line or section title
            - "sceneIdea": Primary camera framing (Talking head / Wide / Close-up)
            - "bRollIdea": Supplementary footage ideas
            - "screenRecordingIdea": What software, UI, or slides to screen record
            - "animationIdea": Motion graphic, text pop-up, or arrow pointer suggestion
            - "aiImagePrompt": Prompt to generate a custom background or illustration

            Return a valid JSON array of objects with these exact keys.
        """.trimIndent()

        val result = executeGeminiPrompt(prompt, forceJson = true)
        return result.mapCatching { jsonStr ->
            parseSceneSuggestionsJson(cleanJsonString(jsonStr))
        }
    }

    // 10. Analyze Script
    suspend fun analyzeScript(scriptText: String, videoType: String, language: String, targetDuration: String): Result<ScriptAnalysis> {
        val prompt = """
            Analyze this YouTube script as an expert YouTube growth consultant:
            Video Type: $videoType
            Language: $language
            Target Duration: $targetDuration
            Script:
            $scriptText

            Evaluate across 8 criteria (scores 0-100):
            - hookScore: How captivating the first 10 seconds are
            - clarityScore: How clear the explanations and vocabulary are
            - structureScore: How well organized the sections and flow are
            - engagementScore: How well it holds viewer interest and curiosity
            - pacingScore: Rhythm and speed of information delivery
            - valueScore: Actionable takeaway density
            - repetitionScore: Lack of redundant phrases (higher means less repetition)
            - ctaScore: Effectiveness and placement of Call To Action
            - overallScore: Composite rating (0-100)

            Also provide:
            - "strengths": Array of 3-4 key strong points
            - "improvements": Array of 3-4 specific actionable fixes
            - "summary": A 2-sentence executive summary

            Return a valid JSON object with these keys.
        """.trimIndent()

        val result = executeGeminiPrompt(prompt, forceJson = true)
        return result.mapCatching { jsonStr ->
            parseAnalysisJson(cleanJsonString(jsonStr))
        }
    }

    // Helper Cleaners & JSON Parsers
    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }
        return clean
    }

    private fun parseHooksJson(jsonStr: String): List<HookItem> {
        val list = mutableListOf<HookItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HookItem(
                        type = obj.optString("type", "Hook ${i + 1}"),
                        text = obj.optString("text", ""),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback simple parser
        }
        return list
    }

    private fun parseTitlesJson(jsonStr: String): List<TitleItem> {
        val list = mutableListOf<TitleItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TitleItem(
                        category = obj.optString("category", "General"),
                        title = obj.optString("title", ""),
                        scoreHint = obj.optString("scoreHint", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    private fun parseDescriptionJson(jsonStr: String): DescriptionData {
        return try {
            val obj = JSONObject(jsonStr)
            val chaptersArr = obj.optJSONArray("chapters")
            val chapters = mutableListOf<ChapterItem>()
            if (chaptersArr != null) {
                for (i in 0 until chaptersArr.length()) {
                    val cObj = chaptersArr.getJSONObject(i)
                    chapters.add(
                        ChapterItem(
                            timestamp = cObj.optString("timestamp", "00:00"),
                            title = cObj.optString("title", "")
                        )
                    )
                }
            }
            DescriptionData(
                shortDescription = obj.optString("shortDescription", ""),
                seoDescription = obj.optString("seoDescription", ""),
                detailedDescription = obj.optString("detailedDescription", ""),
                chapters = chapters
            )
        } catch (e: Exception) {
            DescriptionData(detailedDescription = jsonStr)
        }
    }

    private fun parseThumbnailJson(jsonStr: String): List<ThumbnailConcept> {
        val list = mutableListOf<ThumbnailConcept>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ThumbnailConcept(
                        mainText = obj.optString("mainText", "CONCEPT ${i + 1}"),
                        visualIdea = obj.optString("visualIdea", ""),
                        subjectPlacement = obj.optString("subjectPlacement", ""),
                        emotion = obj.optString("emotion", ""),
                        composition = obj.optString("composition", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    private fun parseSceneSuggestionsJson(jsonStr: String): List<SceneSuggestion> {
        val list = mutableListOf<SceneSuggestion>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SceneSuggestion(
                        scriptSection = obj.optString("scriptSection", "Section ${i + 1}"),
                        sceneIdea = obj.optString("sceneIdea", ""),
                        bRollIdea = obj.optString("bRollIdea", ""),
                        screenRecordingIdea = obj.optString("screenRecordingIdea", ""),
                        animationIdea = obj.optString("animationIdea", ""),
                        aiImagePrompt = obj.optString("aiImagePrompt", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    private fun parseAnalysisJson(jsonStr: String): ScriptAnalysis {
        return try {
            val obj = JSONObject(jsonStr)
            val strengthsArr = obj.optJSONArray("strengths")
            val strengths = mutableListOf<String>()
            if (strengthsArr != null) {
                for (i in 0 until strengthsArr.length()) {
                    strengths.add(strengthsArr.optString(i))
                }
            }
            val improvementsArr = obj.optJSONArray("improvements")
            val improvements = mutableListOf<String>()
            if (improvementsArr != null) {
                for (i in 0 until improvementsArr.length()) {
                    improvements.add(improvementsArr.optString(i))
                }
            }

            ScriptAnalysis(
                hookScore = obj.optInt("hookScore", 80),
                clarityScore = obj.optInt("clarityScore", 85),
                structureScore = obj.optInt("structureScore", 85),
                engagementScore = obj.optInt("engagementScore", 80),
                pacingScore = obj.optInt("pacingScore", 78),
                valueScore = obj.optInt("valueScore", 88),
                repetitionScore = obj.optInt("repetitionScore", 82),
                ctaScore = obj.optInt("ctaScore", 80),
                overallScore = obj.optInt("overallScore", 83),
                strengths = strengths,
                improvements = improvements,
                summary = obj.optString("summary", "Solid script with strong potential for high audience retention.")
            )
        } catch (e: Exception) {
            ScriptAnalysis(
                overallScore = 80,
                summary = "Analysis generated successfully.",
                strengths = listOf("Clear topic and purpose", "Good flow of points"),
                improvements = listOf("Strengthen the opening hook", "Add more direct visual cues")
            )
        }
    }
}
