const {
  sequelize,
  User,
  Habit,
  SleepLog,
  NutritionLog,
  ChatLog,
} = require("./db");

async function seedDatabase() {
  try {
    console.log("Syncing database (dropping existing tables)...");
    await sequelize.sync({ force: true });
    console.log("Database synced.");

    // 1. Create a default User with short credentials
    console.log("Seeding User...");
    const user = await User.create({
      username: "a",
      password: "a",
      fullName: "John Doe",
      height: 175.0,
      weight: 70.0,
      birthDate: Date.now() - 25 * 365 * 24 * 60 * 60 * 1000, // 25 years old
      bloodType: "O+",
      conditions: "None",
      emergencyContactName: "Jane Doe",
      emergencyContactPhone: "1234567890",
    });

    const userId = user.id;

    // Time generation helpers
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;
    const thirtyDaysAgo = now - 30 * oneDayMs;

    // 2. Seed Sleep Logs (1 per day for the last 30 days)
    console.log("Seeding Sleep Logs...");
    const sleepLogs = [];
    for (let i = 0; i < 30; i++) {
      const date = thirtyDaysAgo + i * oneDayMs;
      // Randomize sleep start time between 22:00 and 01:00
      const startOffsetMs = Math.floor(Math.random() * 3 * 60 * 60 * 1000) - 2 * 60 * 60 * 1000; 
      // Sleep duration between 6 to 9 hours
      const sleepDurationMs = Math.floor(Math.random() * 3 * 60 * 60 * 1000) + 6 * 60 * 60 * 1000;
      
      const startTime = date + startOffsetMs;
      const endTime = startTime + sleepDurationMs;
      
      // Calculate quality based on duration (better duration = higher quality roughly)
      const hours = sleepDurationMs / (60 * 60 * 1000);
      let quality = 3;
      if (hours > 7.5) quality = 5;
      else if (hours > 6.5) quality = 4;
      else if (hours < 5) quality = 2;
      
      // Add random variation to quality
      if (Math.random() > 0.8) quality = Math.max(1, Math.min(5, quality + (Math.random() > 0.5 ? 1 : -1)));

      sleepLogs.push({
        userId,
        startTime,
        endTime,
        quality,
        date,
      });
    }
    await SleepLog.bulkCreate(sleepLogs);

    // 3. Seed Habits
    console.log("Seeding Habits...");
    const habits = [];
    
    // Reminders offsets (milliseconds from midnight)
    const eatReminders = [8 * 3600 * 1000, 13 * 3600 * 1000, 19 * 3600 * 1000]; // 8am, 1pm, 7pm
    const drinkReminders = [9 * 3600 * 1000, 11 * 3600 * 1000, 14 * 3600 * 1000, 16 * 3600 * 1000, 20 * 3600 * 1000]; // 9am, 11am, 2pm, 4pm, 8pm
    const exerciseReminders = [7 * 3600 * 1000, 17 * 3600 * 1000]; // 7am, 5pm

    const habitData = [
      { name: "Eat Healthy", category: "Nutrition", subtitle: "General healthy meals", reminders: eatReminders },
      { name: "Drink Water", category: "Nutrition", subtitle: "Stay hydrated", reminders: drinkReminders },
      { name: "Exercise (Walking)", category: "Fitness", subtitle: "General healthy walking", reminders: exerciseReminders }
    ];
    
    for (const data of habitData) {
      // Create absolute timestamps for reminders today, which Android uses to extract hour/minute
      const todayStart = new Date().setHours(0,0,0,0);
      const absoluteReminders = data.reminders.map(offset => todayStart + offset);

      habits.push({
        userId,
        name: data.name,
        category: data.category,
        subtitle: data.subtitle,
        isCompleted: Math.random() > 0.5,
        streak: Math.floor(Math.random() * 15),
        startTime: now - 30 * oneDayMs, // Started 30 days ago
        endTime: now + 30 * oneDayMs,   // Ends in 30 days
        createdAt: now - 30 * oneDayMs,
        reminders: absoluteReminders, // Multiple reminders
      });
    }
    await Habit.bulkCreate(habits);

    // 4. Seed Nutrition Logs (3 meals a day for the last 30 days)
    console.log("Seeding Nutrition Logs...");
    const nutritionLogs = [];
    const mealTypes = ["breakfast", "lunch", "dinner", "snack"];
    const foods = {
      breakfast: [ { name: "Oatmeal", calories: 150 }, { name: "Eggs & Toast", calories: 350 }, { name: "Pancakes", calories: 450 } ],
      lunch: [ { name: "Chicken Salad", calories: 400 }, { name: "Burger", calories: 600 }, { name: "Pasta", calories: 500 } ],
      dinner: [ { name: "Steak & Veggies", calories: 700 }, { name: "Salmon", calories: 550 }, { name: "Soup", calories: 300 } ],
      snack: [ { name: "Apple", calories: 95 }, { name: "Protein Bar", calories: 200 }, { name: "Almonds", calories: 160 } ]
    };

    for (let i = 0; i < 30; i++) {
      const baseDate = thirtyDaysAgo + i * oneDayMs;
      
      // 3 to 4 meals per day
      const mealsToday = Math.random() > 0.5 ? 4 : 3;
      
      for(let j = 0; j < mealsToday; j++) {
        const type = mealTypes[j];
        const foodOptions = foods[type];
        const selectedFood = foodOptions[Math.floor(Math.random() * foodOptions.length)];
        
        // Random time during the day depending on meal type
        let timeOffsetHours;
        if(type === 'breakfast') timeOffsetHours = 7 + Math.random() * 2;
        else if(type === 'lunch') timeOffsetHours = 12 + Math.random() * 2;
        else if(type === 'dinner') timeOffsetHours = 18 + Math.random() * 2;
        else timeOffsetHours = 15 + Math.random() * 2; // snack
        
        nutritionLogs.push({
          userId,
          food_name: selectedFood.name,
          calories: selectedFood.calories,
          consumed_at: baseDate + timeOffsetHours * 60 * 60 * 1000,
          meal_type: type
        });
      }
    }
    await NutritionLog.bulkCreate(nutritionLogs);

    // 5. Seed Chat Logs
    console.log("Seeding Chat Logs...");
    const chatLogs = [
      { userId, sender: "USER", message: "Hello! I want to improve my sleep.", createdAt: now - 5 * oneDayMs },
      { userId, sender: "AI", message: "Hi! I can help with that. To start, how many hours do you usually sleep?", createdAt: now - 5 * oneDayMs + 10000 },
      { userId, sender: "USER", message: "About 5 hours, I feel tired.", createdAt: now - 5 * oneDayMs + 20000 },
      { userId, sender: "AI", message: "5 hours is a bit short. Try setting a consistent bedtime. Let's aim for 7 hours.", createdAt: now - 5 * oneDayMs + 30000 },
    ];
    await ChatLog.bulkCreate(chatLogs);

    console.log("Seeding completed successfully!");
    console.log("You can now login with Username: 'a' and Password: 'a'");
    process.exit(0);
  } catch (error) {
    console.error("Error during seeding:", error);
    process.exit(1);
  }
}

seedDatabase();