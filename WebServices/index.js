const express = require("express");
const { sequelize, User, Habit, SleepLog, NutritionLog } = require("./db");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

const port = 3000;

// ==========================================
// 1. ROUTES UNTUK AUTH (REGISTER & LOGIN)
// ==========================================
app.post("/api/users/register", async (req, res) => {
  try {
    const { username, password, fullName } = req.body;

    if (!username || !password) {
      return res.status(400).json({ error: "Username dan password wajib diisi" });
    }

    // Cek apakah username sudah ada
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
      return res.status(400).json({ error: "Username dan password wajib diisi" });
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
      fullName, height, weight, birthDate, bloodType,
      conditions, emergencyContactName, emergencyContactPhone,
      profilePicturePath
    } = req.body;

    await user.update({
      fullName, height, weight, birthDate, bloodType,
      conditions, emergencyContactName, emergencyContactPhone,
      profilePicturePath
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

    const { name, category, subtitle, isCompleted, streak, startTime, endTime } = req.body;
    await habit.update({ name, category, subtitle, isCompleted, streak, startTime, endTime });
    return res.status(200).json(habit);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.delete("/api/habits/:id", async (req, res) => {
  try {
    // Soft delete
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
// START SERVER
// ==========================================
sequelize.sync({ alter: true }).then(() => {
  console.log("Database siap (synced with alter).");
  app.listen(port, function () {
    console.log(`Server berjalan di port ${port}...`);
  });
}).catch((err) => {
  console.error("Gagal menyambungkan ke database:", err.message);
});
