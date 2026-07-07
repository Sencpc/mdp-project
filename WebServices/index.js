const express = require("express");
const { User, Habit, HabitLog, SleepLog, NutritionLog } = require("./db"); // Import model yang baru

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

const port = 3000;

// 1. ROUTES UNTUK USERS
app.post("/api/users", async (req, res) => {
  try {
    const { name, email, daily_calorie_target, daily_sleep_target } = req.body;
    const user = await User.create({
      name,
      email,
      daily_calorie_target,
      daily_sleep_target,
    });
    return res.status(201).json(user);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.get("/api/users/:id", async (req, res) => {
  try {
    const user = await User.findByPk(req.params.id, {
      include: [Habit, SleepLog, NutritionLog],
    });
    if (!user) return res.status(404).send("User not found");
    return res.status(200).json(user);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.put("/api/users/:id", async (req, res) => {
  try {
    const { name, daily_calorie_target, daily_sleep_target } = req.body;
    const user = await User.findByPk(req.params.id);
    if (!user) return res.status(404).send("User not found");

    await user.update({ name, daily_calorie_target, daily_sleep_target });
    return res.status(200).json(user);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// 2. ROUTES UNTUK HABITS & HABIT LOGS
app.post("/api/habits", async (req, res) => {
  try {
    const { user_id, title, description, frequency } = req.body;
    const habit = await Habit.create({
      user_id,
      title,
      description,
      frequency,
    });
    return res.status(201).json(habit);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.put("/api/habits/:id", async (req, res) => {
  try {
    const { title, description, frequency, is_active } = req.body;
    const habit = await Habit.findByPk(req.params.id);
    if (!habit) return res.status(404).send("Habit not found");

    await habit.update({ title, description, frequency, is_active });
    return res.status(200).json(habit);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.post("/api/habits/log", async (req, res) => {
  try {
    const { habit_id, completed_date, status } = req.body;
    const log = await HabitLog.create({ habit_id, completed_date, status });
    return res.status(201).json(log);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.delete("/api/habits/:id", async (req, res) => {
  try {
    const deleted = await Habit.destroy({ where: { id: req.params.id } });
    if (!deleted) return res.status(404).send("Habit not found");
    return res.status(204).send();
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// 3. ROUTES UNTUK SLEEP CYCLE LOGGER
app.post("/api/sleep", async (req, res) => {
  try {
    const { user_id, sleep_start, sleep_end, quality_rating, notes } = req.body;
    const sleepLog = await SleepLog.create({
      user_id,
      sleep_start,
      sleep_end,
      quality_rating,
      notes,
    });
    return res.status(201).json(sleepLog);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.get("/api/sleep/user/:userId", async (req, res) => {
  try {
    const logs = await SleepLog.findAll({
      where: { user_id: req.params.userId },
    });
    return res.status(200).json(logs);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// 4. ROUTES UNTUK CALORIE / NUTRITION SCANNER
app.post("/api/nutrition", async (req, res) => {
  try {
    const { user_id, food_name, calories, image_url } = req.body;
    // Data ini dikirim dari Android setelah AI memprediksi kalori
    const nutritionLog = await NutritionLog.create({
      user_id,
      food_name,
      calories,
      image_url,
    });
    return res.status(201).json(nutritionLog);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.get("/api/nutrition/user/:userId", async (req, res) => {
  try {
    const logs = await NutritionLog.findAll({
      where: { user_id: req.params.userId },
    });
    return res.status(200).json(logs);
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.listen(port, function () {
  console.log(`Listening on port ${port}...`);
});
