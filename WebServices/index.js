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

const PORT = process.env.PORT || 3000;

require("dotenv").config();
const { GoogleGenAI } = require("@google/genai");
const multer = require("multer");

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
const GEMINI_MODEL = process.env.GEMINI_MODEL || "gemini-1.5-flash";
const upload = multer({ storage: multer.memoryStorage() });

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
      profilePicturePath,
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
      profilePicturePath,
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
    const { userId, name, category, subtitle, startTime, endTime } = req.body;
    const habit = await Habit.create({
      userId,
      name,
      category: category || "Focus",
      subtitle: subtitle || "",
      startTime,
      endTime,
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
    } = req.body;
    await habit.update({
      name,
      category,
      subtitle,
      isCompleted,
      streak,
      startTime,
      endTime,
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
app.post("/api/nutrition", async (req, res) => {
  try {
    const { userId, food_name, calories, image_url } = req.body;
    const nutritionLog = await NutritionLog.create({
      userId,
      food_name,
      calories,
      image_url,
      consumed_at: Date.now(),
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

    const prompt = `Analyze the food in this image. Estimate the standard portion size and total calories. 
    Return ONLY a valid JSON object in this exact format without any additional text or markdown formatting: 
    {"food_name": "Food Name", "calories": 1500}`;

    const result = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: [prompt, image],
    });
    const responseText = result.text.trim();

    const cleanJson = responseText.replace(/```json|```/g, "").trim();
    const parsedData = JSON.parse(cleanJson);

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
    const { userId, message } = req.body;

    // 1. Fetch User data to get the existing chatSummary
    const user = await User.findByPk(userId);
    if (!user) return res.status(404).json({ error: "User not found" });

    // 2. Fetch daily metrics (Calories & Sleep)
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const todayNutrition = await NutritionLog.findAll({ where: { userId } });
    const caloriesToday = todayNutrition
      .filter((log) => log.consumed_at >= today.getTime())
      .reduce((sum, log) => sum + log.calories, 0);

    const lastSleep = await SleepLog.findOne({
      where: { userId },
      order: [["date", "DESC"]],
    });

    let sleepDuration = "No data yet";
    if (lastSleep && lastSleep.startTime && lastSleep.endTime) {
      const hours =
        (lastSleep.endTime - lastSleep.startTime) / (1000 * 60 * 60);
      sleepDuration = `${hours.toFixed(1)} hours`;
    }

    // 3. Fetch recent unsummarized chat history (max 3 months old)
    const threeMonthsAgo = Date.now() - 90 * 24 * 60 * 60 * 1000;
    const recentChats = await ChatLog.findAll({
      where: {
        userId,
        isSummarized: false,
        createdAt: { [Op.gte]: threeMonthsAgo }, // Strictly limit to last 3 months
      },
      order: [["createdAt", "ASC"]],
    });

    // Format recent chats for the prompt
    let recentChatContext = "";
    if (recentChats.length > 0) {
      recentChatContext =
        "RECENT CONVERSATION HISTORY:\n" +
        recentChats.map((c) => `${c.sender}: ${c.message}`).join("\n");
    }

    // 4. Construct the Agentic System Prompt
    const systemPrompt = `You are a Digital Wellness Assistant. Your SOLE purpose is to help users maintain their health.
    STRICT RULES:
    1. You MUST ONLY answer questions regarding basic health, nutrition, light exercise, and sleep.
    2. Use very polite, empathetic, and simple English. Avoid complex medical jargon.
    3. You are not a doctor. Advise them to consult a real doctor if they mention severe symptoms.
    4. Use the provided context to personalize your advice.
    
    DAILY METRICS CONTEXT:
    - Calories consumed today: ${caloriesToday} kcal
    - Last recorded sleep: ${sleepDuration}

    LONG-TERM USER SUMMARY:
    ${user.chatSummary || "No previous summary."}

    ${recentChatContext}
    
    USER: "${message}"`;

    // 5. Generate AI Response
    const result = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: systemPrompt,
    });
    const aiReply = result.text;

    // 6. Save the new messages to the database
    await ChatLog.bulkCreate([
      { userId, sender: "USER", message: message, createdAt: Date.now() },
      { userId, sender: "AI", message: aiReply, createdAt: Date.now() },
    ]);

    // 7. Trigger Background Summarization if memory gets too long (e.g., > 10 messages)
    // We add +2 because we just added the new user message and AI reply
    if (recentChats.length + 2 >= 10) {
      // Fetch them again to include the two we just inserted
      const chatsToSummarize = await ChatLog.findAll({
        where: { userId, isSummarized: false },
      });

      // Execute asynchronously (do not await) so the Android app gets the reply instantly
      updateChatSummary(userId, user.chatSummary, chatsToSummarize);
    }

    return res.status(200).json({ reply: aiReply });
  } catch (err) {
    console.error("Chatbot Error:", err);
    return res
      .status(500)
      .json({ error: "Virtual assistant is currently unavailable." });
  }
});

async function updateChatSummary(userId, currentSummary, unsummarizedChats) {
  try {
    const chatTranscript = unsummarizedChats
      .map((c) => `${c.sender}: ${c.message}`)
      .join("\n");

    const summarizerPrompt = `
        You are an AI memory manager for a health app. 
        Update the following user profile summary using the new conversation transcript. 
        Focus ONLY on extracting long-term health facts, goals, and user preferences. Drop irrelevant chit-chat. Keep it concise.
        
        CURRENT SUMMARY:
        ${currentSummary || "No summary yet."}
        
        NEW TRANSCRIPT:
        ${chatTranscript}
        
        Output ONLY the updated plain text summary, nothing else.`;

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
