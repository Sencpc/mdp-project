const { Sequelize, Model, DataTypes } = require("sequelize");

const DB_NAME = "mdp-project";
const DB_USER = "root";
const DB_PASS = "";
const DB_HOST = "localhost";

const sequelize = new Sequelize(DB_NAME, DB_USER, DB_PASS, {
  host: DB_HOST,
  dialect: "mysql",
  logging: false,
});

// 1. Model: User
class User extends Model {}
User.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    name: {
      type: DataTypes.STRING(100),
      allowNull: false,
    },
    email: {
      type: DataTypes.STRING(150),
      allowNull: false,
      unique: true,
    },
    daily_calorie_target: {
      type: DataTypes.INTEGER,
      defaultValue: 2000,
    },
    daily_sleep_target: {
      type: DataTypes.INTEGER,
      defaultValue: 8,
    },
  },
  {
    sequelize,
    tableName: "users",
    timestamps: true,
    createdAt: "created_at",
    updatedAt: false,
  },
);

// 2. Model: Habit
class Habit extends Model {}
Habit.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    title: {
      type: DataTypes.STRING(150),
      allowNull: false,
    },
    description: {
      type: DataTypes.TEXT,
    },
    frequency: {
      type: DataTypes.STRING(50),
      defaultValue: "DAILY",
    },
    is_active: {
      type: DataTypes.BOOLEAN,
      defaultValue: true,
    },
  },
  {
    sequelize,
    tableName: "habits",
    timestamps: true,
    createdAt: false,
    updatedAt: "last_synced",
  },
);

// 3. Model: HabitLog
class HabitLog extends Model {}
HabitLog.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    completed_date: {
      type: DataTypes.DATEONLY,
      allowNull: false,
    },
    status: {
      type: DataTypes.STRING(20),
      defaultValue: "COMPLETED",
    },
  },
  {
    sequelize,
    tableName: "habit_logs",
    timestamps: false,
  },
);

// 4. Model: SleepLog
class SleepLog extends Model {}
SleepLog.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    sleep_start: {
      type: DataTypes.DATE,
      allowNull: false,
    },
    sleep_end: {
      type: DataTypes.DATE,
      allowNull: false,
    },
    quality_rating: {
      type: DataTypes.INTEGER,
      validate: { min: 1, max: 5 },
    },
    notes: {
      type: DataTypes.TEXT,
    },
  },
  {
    sequelize,
    tableName: "sleep_logs",
    timestamps: false,
  },
);

// 5. Model: NutritionLog
class NutritionLog extends Model {}
NutritionLog.init(
  {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    food_name: {
      type: DataTypes.STRING(150),
      allowNull: false,
    },
    calories: {
      type: DataTypes.INTEGER,
      allowNull: false,
    },
    image_url: {
      type: DataTypes.STRING(255),
    },
    consumed_at: {
      type: DataTypes.DATE,
      defaultValue: DataTypes.NOW,
    },
  },
  {
    sequelize,
    tableName: "nutrition_logs",
    timestamps: false,
  },
);

// ==========================================
// MENDEFINISIKAN RELASI (FOREIGN KEYS)
// ==========================================

// Relasi User ke Habits (1 to Many)
User.hasMany(Habit, { foreignKey: "user_id", onDelete: "CASCADE" });
Habit.belongsTo(User, { foreignKey: "user_id" });

// Relasi User ke SleepLogs (1 to Many)
User.hasMany(SleepLog, { foreignKey: "user_id", onDelete: "CASCADE" });
SleepLog.belongsTo(User, { foreignKey: "user_id" });

// Relasi User ke NutritionLogs (1 to Many)
User.hasMany(NutritionLog, { foreignKey: "user_id", onDelete: "CASCADE" });
NutritionLog.belongsTo(User, { foreignKey: "user_id" });

// Relasi Habit ke HabitLogs (1 to Many)
Habit.hasMany(HabitLog, { foreignKey: "habit_id", onDelete: "CASCADE" });
HabitLog.belongsTo(Habit, { foreignKey: "habit_id" });

module.exports = {
  sequelize,
  User,
  Habit,
  HabitLog,
  SleepLog,
  NutritionLog,
};
