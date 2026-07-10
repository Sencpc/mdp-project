const express = require("express");
const {
  sequelize,
  Op,
  User,
  Habit,
  SleepLog,
  NutritionLog,
  ChatLog,
} = require("./db");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Basic logging middleware
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

const PORT = process.env.PORT || 3000;

require("dotenv").config();
const { GoogleGenAI } = require("@google/genai");
const multer = require("multer");

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
const GEMINI_MODEL = process.env.GEMINI_MODEL || "gemini-flash-latest";
const upload = multer({ storage: multer.memoryStorage() });

// ==========================================
// SATUSEHAT LIVE INTEGRATION CACHE
// ==========================================
const SATUSEHAT_CLIENT_ID =
  process.env.SATUSEHAT_CLIENT_ID ||
  "CXaAyZAAaGAx8szZib7PGmV0BJVqvfKhcFZCBPQcp83KjOw3";
const SATUSEHAT_CLIENT_SECRET =
  process.env.SATUSEHAT_CLIENT_SECRET ||
  "cZKYNGrXMj4bwBfsQXKjXjlYUE8UOOMfGiJkOhnRpGT9DytkxQlF4hWHQfyyqZJn";

let cachedHospitals = null;
let cachedHospitalsTimestamp = 0;

async function getSatuSehatHospitals() {
  const CACHE_DURATION_MS = 60 * 60 * 1000; // 1 hour
  if (
    cachedHospitals &&
    Date.now() - cachedHospitalsTimestamp < CACHE_DURATION_MS
  ) {
    return cachedHospitals;
  }

  try {
    // 1. Get OAuth Token
    const authResponse = await fetch(
      "https://api-satusehat-stg.dto.kemkes.go.id/oauth2/v1/accesstoken",
      {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `client_id=${SATUSEHAT_CLIENT_ID}&client_secret=${SATUSEHAT_CLIENT_SECRET}`,
      },
    );
    const authData = await authResponse.json();
    if (!authData.access_token)
      throw new Error("Failed to get SatuSehat token");

    // 2. Fetch Hospitals (jenis_sarana=104 is Rumah Sakit)
    const msiResponse = await fetch(
      "https://api-satusehat-stg.dto.kemkes.go.id/masterdata/v1/mastersaranaindex/mastersarana?limit=5&page=1&jenis_sarana=104",
      {
        method: "GET",
        headers: { Authorization: `Bearer ${authData.access_token}` },
      },
    );
    const msiData = await msiResponse.json();

    if (msiData && msiData.data && msiData.data.length > 0) {
      cachedHospitals = msiData.data.map((h) => `    - ${h.nama}`).join("\n");
      cachedHospitalsTimestamp = Date.now();
      return cachedHospitals;
    }
  } catch (error) {
    console.error("SatuSehat Live Fetch Error:", error.message);
  }

  // Fallback
  return `    - RSUP Nasional Dr. Cipto Mangunkusumo (RSCM)\n    - RS Pondok Indah\n    - RS Siloam Hospitals`;
}

// ==========================================
// SATUSEHAT STRUCTURED FACILITY API
// ==========================================
let cachedFacilitiesStructured = null;
let cachedFacilitiesStructuredTimestamp = 0;

async function getSatuSehatFacilitiesStructured() {
  const CACHE_DURATION_MS = 60 * 60 * 1000; // 1 hour
  if (
    cachedFacilitiesStructured &&
    Date.now() - cachedFacilitiesStructuredTimestamp < CACHE_DURATION_MS
  ) {
    return cachedFacilitiesStructured;
  }

  // 1. Get OAuth Token (server-side only)
  const authResponse = await fetch(
    "https://api-satusehat-stg.dto.kemkes.go.id/oauth2/v1/accesstoken",
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: `client_id=${SATUSEHAT_CLIENT_ID}&client_secret=${SATUSEHAT_CLIENT_SECRET}`,
    },
  );
  const authData = await authResponse.json();
  if (!authData.access_token)
    throw new Error("Failed to get SatuSehat token");

  const token = authData.access_token;
  let allFacilities = [];

  // 2. Fetch Hospitals (jenis_sarana=104)
  const hospitalRes = await fetch(
    "https://api-satusehat-stg.dto.kemkes.go.id/masterdata/v1/mastersaranaindex/mastersarana?limit=50&page=1&jenis_sarana=104&status_aktif=true",
    { headers: { Authorization: `Bearer ${token}` } },
  );
  const hospitalData = await hospitalRes.json();
  if (hospitalData && hospitalData.data) {
    allFacilities = allFacilities.concat(hospitalData.data);
  }

  // 3. Fetch Clinics (jenis_sarana=103)
  const clinicRes = await fetch(
    "https://api-satusehat-stg.dto.kemkes.go.id/masterdata/v1/mastersaranaindex/mastersarana?limit=50&page=1&jenis_sarana=103&status_aktif=true",
    { headers: { Authorization: `Bearer ${token}` } },
  );
  const clinicData = await clinicRes.json();
  if (clinicData && clinicData.data) {
    allFacilities = allFacilities.concat(clinicData.data);
  }

  cachedFacilitiesStructured = allFacilities;
  cachedFacilitiesStructuredTimestamp = Date.now();
  return allFacilities;
}

// GET /api/facilities — Returns structured SATUSEHAT MSI facility data for Android
app.get("/api/facilities", async (req, res) => {
  try {
    const facilities = await getSatuSehatFacilitiesStructured();
    res.json({ success: true, data: facilities });
  } catch (error) {
    console.error("Facilities API Error:", error.message);
    res.status(500).json({ success: false, message: error.message, data: [] });
  }
});

// ==========================================
// 1. ROUTES UNTUK AUTH (REGISTER & LOGIN)
// ==========================================
app.post("/api/users/register", async (req, res) => {
  try {
    const { username, password, fullName } = req.body;

    if (!username || !password) {
      return res
        .status(400)
        .json({ error: "Username dan password wajib diisi" });
    }

    const existingUser = await User.findOne({ where: { username } });
    if (existingUser) {
      return res.status(409).json({ error: "Username sudah digunakan" });
    }

    const user = await User.create({
      username,
      password,
      fullName: fullName || "",
    });

    // Create template habits for the new user
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;

    const eatReminders = [8 * 3600 * 1000, 13 * 3600 * 1000, 19 * 3600 * 1000];
    const drinkReminders = [
      9 * 3600 * 1000,
      11 * 3600 * 1000,
      14 * 3600 * 1000,
      16 * 3600 * 1000,
      20 * 3600 * 1000,
    ];
    const exerciseReminders = [7 * 3600 * 1000, 17 * 3600 * 1000];

    const habitData = [
      {
        name: "Eat Healthy",
        category: "Nutrition",
        subtitle: "General healthy meals",
        reminders: eatReminders,
      },
      {
        name: "Drink Water",
        category: "Nutrition",
        subtitle: "Stay hydrated",
        reminders: drinkReminders,
      },
      {
        name: "Exercise (Walking)",
        category: "Fitness",
        subtitle: "General healthy walking",
        reminders: exerciseReminders,
      },
    ];

    const habitsToCreate = [];
    for (const data of habitData) {
      const todayStart = new Date().setHours(0, 0, 0, 0);
      const absoluteReminders = data.reminders.map(
        (offset) => todayStart + offset,
      );

      habitsToCreate.push({
        userId: user.id,
        name: data.name,
        category: data.category,
        subtitle: data.subtitle,
        isCompleted: false,
        streak: 0,
        startTime: now,
        endTime: now + oneDayMs, // 24 hour window
        createdAt: now,
        reminders: absoluteReminders,
      });
    }
    await Habit.bulkCreate(habitsToCreate);

    return res.status(201).json({
      id: user.id,
      username: user.username,
      fullName: user.fullName,
    });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.post("/api/users/login", async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res
        .status(400)
        .json({ error: "Username dan password wajib diisi" });
    }

    const user = await User.findOne({ where: { username } });
    if (!user) {
      return res.status(401).json({ error: "Username atau password salah" });
    }

    if (user.password !== password) {
      return res.status(401).json({ error: "Username atau password salah" });
    }

    return res.status(200).json({
      id: user.id,
      username: user.username,
      fullName: user.fullName,
    });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// ==========================================
// 2. ROUTES UNTUK USERS (CRUD)
// ==========================================
app.get("/api/users/:id", async (req, res) => {
  try {
    const user = await User.findByPk(req.params.id, {
      include: [Habit, SleepLog, NutritionLog],
    });
    if (!user) return res.status(404).json({ error: "User not found" });
    return res.status(200).json(user);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.put("/api/users/:id", async (req, res) => {
  try {
    const user = await User.findByPk(req.params.id);
    if (!user) return res.status(404).json({ error: "User not found" });

    const {
      fullName,
      height,
      weight,
      birthDate,
      bloodType,
      conditions,
      emergencyContactName,
      emergencyContactPhone,
    } = req.body;

    await user.update({
      fullName,
      height,
      weight,
      birthDate,
      bloodType,
      conditions,
      emergencyContactName,
      emergencyContactPhone,
    });

    return res.status(200).json(user);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// ==========================================
// 3. ROUTES UNTUK HABITS
// ==========================================
app.post("/api/habits", async (req, res) => {
  try {
    const { userId, name, category, subtitle, startTime, endTime, reminders, useRingtone, useVibration, enableNotification } =
      req.body;
    const habit = await Habit.create({
      userId,
      name,
      category: category || "Focus",
      subtitle: subtitle || "",
      startTime,
      endTime,
      reminders: reminders || [],
      useRingtone: useRingtone !== false, // default true
      useVibration: useVibration !== false, // default true
      enableNotification: enableNotification !== false, // default true
      createdAt: Date.now(),
    });
    return res.status(201).json(habit);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.get("/api/habits/user/:userId", async (req, res) => {
  try {
    const habits = await Habit.findAll({
      where: { userId: req.params.userId, deletedAt: null },
      order: [["createdAt", "DESC"]],
    });
    return res.status(200).json(habits);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.put("/api/habits/:id", async (req, res) => {
  try {
    const habit = await Habit.findByPk(req.params.id);
    if (!habit) return res.status(404).json({ error: "Habit not found" });

    const {
      name,
      category,
      subtitle,
      isCompleted,
      streak,
      startTime,
      endTime,
      reminders,
      useRingtone,
      useVibration,
      enableNotification
    } = req.body;
    await habit.update({
      name,
      category,
      subtitle,
      isCompleted,
      streak,
      startTime,
      endTime,
      reminders: reminders || habit.reminders,
      useRingtone: useRingtone !== undefined ? useRingtone : habit.useRingtone,
      useVibration: useVibration !== undefined ? useVibration : habit.useVibration,
      enableNotification: enableNotification !== undefined ? enableNotification : habit.enableNotification,
    });
    return res.status(200).json(habit);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.delete("/api/habits/:id", async (req, res) => {
  try {
    const habit = await Habit.findByPk(req.params.id);
    if (!habit) return res.status(404).json({ error: "Habit not found" });

    await habit.update({ deletedAt: Date.now() });
    return res.status(200).json({ message: "Habit deleted" });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// ==========================================
// 4. ROUTES UNTUK SLEEP LOGS
// ==========================================
app.post("/api/sleep", async (req, res) => {
  try {
    const { userId, startTime, endTime, quality } = req.body;
    const sleepLog = await SleepLog.create({
      userId,
      startTime,
      endTime,
      quality,
      date: Date.now(),
    });
    return res.status(201).json(sleepLog);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.get("/api/sleep/user/:userId", async (req, res) => {
  try {
    const logs = await SleepLog.findAll({
      where: { userId: req.params.userId },
      order: [["date", "DESC"]],
    });
    return res.status(200).json(logs);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// ==========================================
// 5. ROUTES UNTUK NUTRITION LOGS
// ==========================================
// Helper: auto-classify meal type based on hour of day
function classifyMealType(timestampMs) {
  const hour = new Date(timestampMs).getHours();
  if (hour >= 5 && hour < 11) return "breakfast";
  if (hour >= 11 && hour < 15) return "lunch";
  if (hour >= 17 && hour < 21) return "dinner";
  return "additional";
}

app.post("/api/nutrition", async (req, res) => {
  try {
    const { userId, food_name, calories, image_url, meal_type } = req.body;
    const consumedAt = Date.now();
    const nutritionLog = await NutritionLog.create({
      userId,
      food_name,
      calories,
      image_url,
      consumed_at: consumedAt,
      meal_type: meal_type || classifyMealType(consumedAt),
    });
    return res.status(201).json(nutritionLog);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.get("/api/nutrition/user/:userId", async (req, res) => {
  try {
    const logs = await NutritionLog.findAll({
      where: { userId: req.params.userId },
      order: [["consumed_at", "DESC"]],
    });
    return res.status(200).json(logs);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.put("/api/nutrition/:id", async (req, res) => {
  try {
    const log = await NutritionLog.findByPk(req.params.id);
    if (!log) return res.status(404).json({ error: "Nutrition log not found" });

    const { meal_type } = req.body;
    if (
      !meal_type ||
      !["breakfast", "lunch", "dinner", "additional"].includes(meal_type)
    ) {
      return res.status(400).json({
        error:
          "Invalid meal_type. Must be: breakfast, lunch, dinner, or additional",
      });
    }

    await log.update({ meal_type });
    return res.status(200).json(log);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// ==========================================
// 6. ROUTES FOR AI CALORIE SCANNER (GEMINI 3.0 FLASH)
// ==========================================
app.post("/api/nutrition/analyze", upload.single("image"), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: "Image not found in the request" });
    }

    const image = {
      inlineData: {
        data: req.file.buffer.toString("base64"),
        mimeType: req.file.mimetype,
      },
    };

    const prompt = `You are a strict food/drink recognition and calorie estimation system.

    STEP 1 — VALIDATION (do this FIRST before any analysis):
    - Determine if the image contains a clearly recognizable food item or beverage.
    - REJECT the image by returning the error JSON (see below) if ANY of the following are true:
      • The image does NOT contain food or drink (e.g. a person, animal, object, scenery, text, meme, document).
      • The item is NOT edible or drinkable by humans (e.g. pet food, raw inedible plants, chemicals, soap, candles).
      • The image is too blurry, dark, or obscured to reliably identify the food.
      • The image contains multiple very different dishes that cannot be reasonably grouped into one meal entry.
      • The image is a close-up of a package/label or a menu/poster (no real food visible).
      • The image is a generic stock photo or illustration.

    STEP 2 — ANALYSIS (only if validation passes):
    - Identify the specific food or drink as precisely as possible (e.g. "Nasi Goreng" not just "Fried Rice", "Cappuccino" not just "Coffee").
    - Estimate a realistic single-serving portion size based on visual cues (plate size, cup size, utensils for scale).
    - Estimate total calories for the visible portion. Use standard nutritional databases as your reference. Round to the nearest 5 kcal.
    - If the image shows a meal with multiple components on one plate (e.g. rice + chicken + vegetables), combine them into one entry with a descriptive name.

    STEP 3 — MEAL TYPE CLASSIFICATION:
    - Based on the food/drink identified, classify it into one of these categories:
      • "breakfast" — typical morning foods (cereal, toast, eggs, pancakes, oatmeal, coffee, juice, etc.)
      • "lunch" — typical midday meals (rice dishes, sandwiches, pasta, salads, soups, etc.)
      • "dinner" — typical evening meals (heavier dishes, steak, curry, full course meals, etc.)
      • "additional" — snacks, desserts, standalone drinks, supplements, or anything that doesn't clearly fit a main meal
    - If the food could fit multiple categories, pick the MOST LIKELY one based on common eating patterns.

    RESPONSE FORMAT — Return ONLY a valid JSON object, no markdown, no explanation, no extra text:

    On SUCCESS:
    {"food_name": "Specific Food Name", "calories": 350, "meal_type": "lunch"}

    On REJECTION (validation failed):
    {"food_name": null, "calories": 0, "error": "Brief reason why this was rejected"}`;

    const result = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: [prompt, image],
    });
    const responseText = result.text.trim();

    const cleanJson = responseText.replace(/```json|```/g, "").trim();
    const parsedData = JSON.parse(cleanJson);

    // Server-side guardrail: if the AI flagged it as non-food, return 422
    if (parsedData.error || !parsedData.food_name) {
      return res.status(422).json({
        error:
          parsedData.error ||
          "Could not identify any food or drink in the image.",
      });
    }

    return res.status(200).json(parsedData);
  } catch (err) {
    console.error("AI Analyze Error:", err);
    return res
      .status(500)
      .json({ error: "Failed to process image through AI" });
  }
});

// ==========================================
// 7. ROUTES FOR AI WELLBEING CHATBOT
// ==========================================
app.post("/api/chat", async (req, res) => {
  try {
    const { userId, message, timezone } = req.body;
    console.log(`[Chat API] Start handling chat request for User ${userId}`);
    const startTime = Date.now();

    // 1. Fetch User data to get the existing chatSummary
    const user = await User.findByPk(userId);
    if (!user) return res.status(404).json({ error: "User not found" });

    // 2. Fetch all user data in parallel for efficiency
    const now = Date.now();
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayMs = today.getTime();
    const thirtyDaysAgoMs = todayMs - 30 * 24 * 60 * 60 * 1000;
    const threeMonthsAgo = now - 90 * 24 * 60 * 60 * 1000;

    const timeOpts = { hour: "2-digit", minute: "2-digit" };
    if (timezone) timeOpts.timeZone = timezone;
    const dateOpts = { month: "short", day: "numeric" };
    if (timezone) dateOpts.timeZone = timezone;
    const fmtTime = (ms) =>
      new Date(Number(ms)).toLocaleTimeString("id-ID", timeOpts);
    const fmtDate = (ms) =>
      new Date(Number(ms)).toLocaleDateString("id-ID", dateOpts);

    const [allNutrition, sleepLogs, habits, recentChats] = await Promise.all([
      NutritionLog.findAll({
        where: { userId, consumed_at: { [Op.gte]: thirtyDaysAgoMs } },
        order: [["consumed_at", "DESC"]],
      }),
      SleepLog.findAll({
        where: { userId, date: { [Op.gte]: thirtyDaysAgoMs } },
        order: [["date", "DESC"]],
      }),
      Habit.findAll({
        where: { userId, deletedAt: null },
        order: [["createdAt", "DESC"]],
      }),
      ChatLog.findAll({
        where: {
          userId,
          isSummarized: false,
          createdAt: { [Op.gte]: threeMonthsAgo },
        },
        order: [["createdAt", "ASC"]],
      }),
    ]);

    // --- Build NUTRITION context (full 30 days) ---
    let nutritionContext = "No nutrition data.";
    if (allNutrition.length > 0) {
      nutritionContext =
        "Past 30 Days Meals:\n" +
        allNutrition
          .map(
            (m) =>
              `${fmtDate(m.consumed_at)} [${m.meal_type.toUpperCase()}]: ${m.food_name} (${m.calories}kcal)`,
          )
          .join("\n");
    }

    // --- Build SLEEP context (full 30 days) ---
    let sleepContext = "No sleep data.";
    if (sleepLogs.length > 0) {
      const rows = sleepLogs.map((s) => {
        const hrs = ((s.endTime - s.startTime) / (1000 * 60 * 60)).toFixed(1);
        const q = s.quality ? `Q${s.quality}/5` : "N/A";
        return `${fmtDate(s.date)}: ${hrs}h ${fmtTime(s.startTime)}-${fmtTime(s.endTime)} ${q}`;
      });
      sleepContext = `Past 30 Days Sleep Logs:\n${rows.join("\n")}`;
    }

    // --- Build HABITS context (token-optimized) ---
    let habitsContext = "No habits tracked.";
    if (habits.length > 0) {
      const rows = habits.map((h) => {
        const done = h.isCompleted ? "✓" : "✗";
        const time = `${fmtTime(h.startTime)}-${fmtTime(h.endTime)}`;
        return `${done} ${h.name} [${h.category}] streak:${h.streak} ${time}`;
      });
      const completedCount = habits.filter((h) => h.isCompleted).length;
      habitsContext = `${completedCount}/${habits.length} completed today\n${rows.join("\n")}`;
    }

    // Format recent chats for the prompt
    let recentChatContext = "";
    if (recentChats.length > 0) {
      recentChatContext =
        "RECENT CONVERSATION HISTORY:\n" +
        recentChats.map((c) => `${c.sender}: ${c.message}`).join("\n");
    }

    // Prepare User Demographic Context
    let ageStr = "Unknown";
    if (user.birthDate) {
      const ageDifMs = Date.now() - Number(user.birthDate);
      const ageDate = new Date(ageDifMs);
      ageStr = `${Math.abs(ageDate.getUTCFullYear() - 1970)} years old`;
    }
    const heightStr = user.height ? `${user.height} cm` : "Unknown";
    const weightStr = user.weight ? `${user.weight} kg` : "Unknown";
    const bloodTypeStr = user.bloodType || "Unknown";
    const conditionsStr = user.conditions || "None";

    // 3. Construct the Agentic System Prompt
    const currentTimeStr = timezone
      ? new Date().toLocaleString("id-ID", { timeZone: timezone })
      : new Date().toLocaleString("id-ID", { timeZone: "Asia/Jakarta" });

    // 3.5. Prepare Medical Recommendations Context (SatuSehat & Doctors)
    const liveHospitals = await getSatuSehatHospitals();
    const medicalRecommendationContext = `
    AVAILABLE MEDICAL PROFESSIONALS (You can recommend these specific doctors if the user needs help):
    - General Practice: Dr. Sarah Jenkins, Dr. Kevin Smith, Dr. Amelia Brown, Dr. David Wilson, Dr. Olivia Clark
    - Therapy & Mental Health: Dr. Michael Chen, Dr. Emily White, Dr. Daniel Moore, Dr. Sophia Taylor, Dr. Ethan Scott
    - Nutritionists: Dr. Elena Rodriguez, Dr. Chloe Evans, Dr. Lucas Hall, Dr. Grace Young, Dr. Ryan Adams

    AVAILABLE SATUSEHAT HOSPITALS/FASYANKES (For physical checkups, dynamically fetched):
${liveHospitals}
    `;

    const systemPrompt = `You are a Digital Wellness Assistant. Your SOLE purpose is to help users maintain their health.
    STRICT RULES:
    1. You MUST ONLY answer questions regarding basic health, nutrition, light exercise, and sleep.
    2. Use a friendly, conversational, and empathetic tone in simple English. Avoid complex medical jargon.
    3. You are not a doctor. Advise them to consult a real doctor if they mention severe symptoms.
    4. You MUST format your responses using Markdown. Do NOT use any HTML tags.
    5. Be highly natural and varied in your responses. Do NOT use repetitive, templated greetings or robotic transitions (e.g., avoid saying "It's great that you asked about sleep!" every time). Act like a real, casual human friend but polite enough.
    6. You have access to the user's full health data below. Use it silently for reasoning. Do NOT awkwardly announce their stats (e.g. NEVER say "Since you are 162cm and 24 years old..."). Only mention specific data if directly asked or if it's clinically relevant to give advice.
    7. CRITICAL: NEVER generate code, write scripts, or provide programming assistance. NEVER perform, explain, or assist with any mathematical calculations (no arithmetic, algebra, calculus, etc.). If asked for code or math, firmly but politely refuse and remind the user of your wellness-only purpose.

    USER PROFILE:
    Age:${ageStr} | Height:${heightStr} | Weight:${weightStr} | Blood:${bloodTypeStr}
    Conditions: ${conditionsStr}

    NUTRITION DATA:
    ${nutritionContext}

    SLEEP DATA:
    ${sleepContext}

    HABITS:
    ${habitsContext}

    Current Time: ${currentTimeStr} (${timezone || "UTC"})

    LONG-TERM USER SUMMARY:
    ${user.chatSummary || "No previous summary."}

    MEDICAL RECOMMENDATIONS LIST:
    ${medicalRecommendationContext}

    ${recentChatContext}
    
    USER: "${message}"`;

    // 4. Generate AI Response
    console.log(
      `[Chat API] Calling Gemini API (Elapsed: ${Date.now() - startTime}ms)`,
    );
    const result = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: systemPrompt,
    });
    const aiReplyMarkdown = result.text;
    const aiReply = aiReplyMarkdown.trim();
    console.log(
      `[Chat API] Gemini API responded (Elapsed: ${Date.now() - startTime}ms)`,
    );

    // 5. Save the new messages to the database
    await ChatLog.bulkCreate([
      { userId, sender: "USER", message: message, createdAt: Date.now() },
      { userId, sender: "AI", message: aiReply, createdAt: Date.now() },
    ]);

    // 6. Trigger Background Summarization if memory gets too long (e.g., >= 4 messages)
    // We add +2 because we just added the new user message and AI reply
    if (recentChats.length + 2 >= 4) {
      // Fetch them again to include the two we just inserted
      const chatsToSummarize = await ChatLog.findAll({
        where: { userId, isSummarized: false },
      });

      // Execute asynchronously (do not await) so the Android app gets the reply instantly
      updateChatSummary(userId, user.chatSummary, chatsToSummarize);
    }

    console.log(
      `[Chat API] Sending response to client (Total time: ${Date.now() - startTime}ms)`,
    );
    return res.status(200).json({ reply: aiReply });
  } catch (err) {
    console.error("Chatbot Error:", err);
    return res
      .status(500)
      .json({ error: "Virtual assistant is currently unavailable." });
  }
});

app.post("/api/dashboard/weekly-summary", async (req, res) => {
  try {
    const {
      sleepHours,
      calories,
      screenTimeMinutes,
      habitsCompleted,
      habitsTotal,
    } = req.body;

    const prompt = `You are a friendly health and wellness AI assistant.
    Your task is to write a highly personalized, empathetic, and motivational 1-2 sentence message for the user based on their WEEKLY averages and totals.

    USER'S WEEKLY STATS (AVERAGES/TOTALS):
    - Avg Sleep: ${sleepHours.toFixed(1)} hours/day
    - Avg Calories: ${calories} kcal/day
    - Avg Screen Time: ${screenTimeMinutes} minutes/day
    - Habits Completed: ${habitsCompleted} out of ${habitsTotal} this week

    INSTRUCTIONS:
    1. DO NOT just list the numbers back to the user.
    2. Provide positive reinforcement if they are doing well over the week.
    3. Provide gentle, encouraging advice if they are struggling.
    4. Keep it strictly to 1 or 2 sentences max. Keep it punchy, warm, and natural.`;

    const result = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: prompt,
    });

    const summary = result.text.trim();
    return res.status(200).json({ summary });
  } catch (err) {
    console.error("Dashboard Weekly Summary Error:", err);
    return res.status(500).json({ error: "Failed to generate AI summary." });
  }
});

app.delete("/api/chat/user/:userId/history", async (req, res) => {
  try {
    const { userId } = req.params;
    await ChatLog.destroy({ where: { userId } });
    return res
      .status(200)
      .json({ message: "Chat history cleared successfully." });
  } catch (err) {
    console.error("Clear History Error:", err);
    return res.status(500).json({ error: "Failed to clear chat history." });
  }
});

app.delete("/api/chat/user/:userId/memory", async (req, res) => {
  try {
    const { userId } = req.params;
    await User.update({ chatSummary: "" }, { where: { id: userId } });
    return res
      .status(200)
      .json({ message: "AI memory context reset successfully." });
  } catch (err) {
    console.error("Reset Memory Error:", err);
    return res.status(500).json({ error: "Failed to reset AI memory." });
  }
});

async function updateChatSummary(userId, currentSummary, unsummarizedChats) {
  try {
    const chatTranscript = unsummarizedChats
      .map((c) => `${c.sender}: ${c.message}`)
      .join("\n");

    const summarizerPrompt = `You are an AI memory manager for a health & wellness app. Your job is to maintain a living user profile summary that the wellness chatbot reads for personalized advice.

    INSTRUCTIONS:
    1. Merge the NEW TRANSCRIPT into the CURRENT SUMMARY below.
    2. PRESERVE all existing facts — never drop something just to be shorter. It is better to be slightly longer than to lose a meaningful detail.
    3. ADD any new information from the transcript that falls into the categories below.
    4. Only DROP content that is purely conversational filler with zero long-term value (e.g. "hi", "thanks", "ok").
    5. If new information CONTRADICTS an older fact, replace the old fact with the new one (e.g. weight changed, goal updated).
    6. Write in concise bullet points, grouped by category. Skip empty categories.

    CATEGORIES TO TRACK:
    • HEALTH PROFILE — chronic conditions, allergies, injuries, medications, recent diagnoses, physical limitations
    • BODY & VITALS — weight changes, BMI observations, blood pressure mentions, any self-reported vitals over time
    • GOALS — fitness goals, weight targets, sleep improvement goals, dietary goals, habit-building goals
    • DIET & PREFERENCES — food preferences, dietary restrictions (vegetarian, halal, etc.), disliked foods, eating patterns, fasting habits
    • SLEEP PATTERNS — recurring sleep issues, preferred sleep schedule, insomnia mentions, nap habits
    • EXERCISE & ACTIVITY — workout routines, preferred exercises, activity level, sports
    • EMOTIONAL & MENTAL — stress triggers, anxiety mentions, mood patterns, motivation struggles, mental health notes
    • KEY EVENTS — doctor visits, medical test results, injuries, significant life changes affecting health
    • PERSONAL CONTEXT — relevant lifestyle details (student, night-shift worker, new parent, etc.) that affect health advice

    CURRENT SUMMARY:
    ${currentSummary || "No summary yet."}

    NEW TRANSCRIPT:
    ${chatTranscript}

    Output ONLY the updated summary in the bullet-point format above. No preamble, no explanation.`;

    const result = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: summarizerPrompt,
    });
    const newSummary = result.text.trim();

    // Save new summary to User
    await User.update({ chatSummary: newSummary }, { where: { id: userId } });

    // Mark these specific chats as summarized so they aren't processed again
    const chatIds = unsummarizedChats.map((c) => c.id);
    await ChatLog.update(
      { isSummarized: true },
      { where: { id: { [Op.in]: chatIds } } },
    );

    console.log(`Updated chat summary for User ${userId}`);
  } catch (error) {
    console.error("Background Summarizer Error:", error);
  }
}

// ==========================================
// START SERVER
// ==========================================
sequelize
  .sync({ alter: true })
  .then(() => {
    console.log("Database siap (synced with alter).");
    app.listen(PORT, function () {
      console.log(`Server berjalan di port ${PORT}...`);
    });
  })
  .catch((err) => {
    console.error("Gagal menyambungkan ke database:", err.message);
  });
