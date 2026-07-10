const { Sequelize, Model, DataTypes, Op } = require("sequelize");
require("dotenv").config();

const DB_NAME = process.env.DB_NAME;
const DB_USER = process.env.DB_USER;
const DB_PASS = process.env.DB_PASS;
const DB_HOST = process.env.DB_HOST;

const sequelize = new Sequelize(DB_NAME, DB_USER, DB_PASS, {
  host: DB_HOST,
  dialect: "mysql",
  logging: false,
});

// 1. Model: User (Updated with chatSummary)
class User extends Model {}
User.init(
  {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true,
    },
    username: {
      type: DataTypes.STRING(100),
      allowNull: false,
      unique: true,
    },
    password: {
      type: DataTypes.STRING(255),
      allowNull: false,
    },
    fullName: {
      type: DataTypes.STRING(150),
      allowNull: false,
      defaultValue: "",
    },
    height: {
      type: DataTypes.FLOAT,
      allowNull: true,
    },
    weight: {
      type: DataTypes.FLOAT,
      allowNull: true,
    },
    birthDate: {
      type: DataTypes.BIGINT,
      allowNull: true,
    },
    bloodType: {
      type: DataTypes.STRING(10),
      allowNull: true,
    },
    conditions: {
      type: DataTypes.TEXT,
      defaultValue: "",
    },
    emergencyContactName: {
      type: DataTypes.STRING(150),
      allowNull: true,
    },
    emergencyContactPhone: {
      type: DataTypes.STRING(30),
      allowNull: true,
    },
    profilePicturePath: {
      type: DataTypes.STRING(500),
      allowNull: true,
    },
    chatSummary: {
      type: DataTypes.TEXT,
      defaultValue: "",
    },
  },
  {
    sequelize,
    tableName: "users",
    timestamps: true,
    createdAt: "created_at",
    updatedAt: "updated_at",
  },
);

// 2. Model: Habit (disesuaikan dengan Android Room entity)
class Habit extends Model {}
Habit.init(
  {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true,
    },
    userId: {
      type: DataTypes.INTEGER,
      allowNull: false,
    },
    name: {
      type: DataTypes.STRING(150),
      allowNull: false,
    },
    category: {
      type: DataTypes.STRING(50),
      defaultValue: "Focus",
    },
    subtitle: {
      type: DataTypes.TEXT,
      defaultValue: "",
    },
    isCompleted: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
    },
    streak: {
      type: DataTypes.INTEGER,
      defaultValue: 0,
    },
    startTime: {
      type: DataTypes.BIGINT,
      allowNull: false,
    },
    endTime: {
      type: DataTypes.BIGINT,
      allowNull: false,
    },
    createdAt: {
      type: DataTypes.BIGINT,
      defaultValue: () => Date.now(),
    },
    deletedAt: {
      type: DataTypes.BIGINT,
      allowNull: true,
    },
  },
  {
    sequelize,
    tableName: "habits",
    timestamps: false,
  },
);

// 3. Model: SleepLog (disesuaikan dengan Android Room entity)
class SleepLog extends Model {}
SleepLog.init(
  {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true,
    },
    userId: {
      type: DataTypes.INTEGER,
      allowNull: false,
    },
    startTime: {
      type: DataTypes.BIGINT,
      allowNull: false,
    },
    endTime: {
      type: DataTypes.BIGINT,
      allowNull: false,
    },
    quality: {
      type: DataTypes.INTEGER,
      validate: { min: 1, max: 5 },
    },
    date: {
      type: DataTypes.BIGINT,
      defaultValue: () => Date.now(),
    },
  },
  {
    sequelize,
    tableName: "sleep_logs",
    timestamps: false,
  },
);

// 4. Model: NutritionLog (tetap ada untuk API)
class NutritionLog extends Model {}
NutritionLog.init(
  {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true,
    },
    userId: {
      type: DataTypes.INTEGER,
      allowNull: false,
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
      type: DataTypes.BIGINT,
      defaultValue: () => Date.now(),
    },
    meal_type: {
      type: DataTypes.STRING(20),
      allowNull: false,
      defaultValue: "additional",
    },
  },
  {
    sequelize,
    tableName: "nutrition_logs",
    timestamps: false,
  },
);

// 5. Model: ChatLog
class ChatLog extends Model {}
ChatLog.init(
  {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true,
    },
    userId: {
      type: DataTypes.INTEGER,
      allowNull: false,
    },
    sender: {
      type: DataTypes.ENUM("USER", "AI"),
      allowNull: false,
    },
    message: {
      type: DataTypes.TEXT,
      allowNull: false,
    },
    isSummarized: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
    },
    createdAt: {
      type: DataTypes.BIGINT,
      defaultValue: () => Date.now(),
    },
  },
  { sequelize, tableName: "chat_logs", timestamps: false },
);

// ==========================================
// MENDEFINISIKAN RELASI (FOREIGN KEYS)
// ==========================================

// Relasi User ke Habits (1 to Many)
User.hasMany(Habit, { foreignKey: "userId", onDelete: "CASCADE" });
Habit.belongsTo(User, { foreignKey: "userId" });

// Relasi User ke SleepLogs (1 to Many)
User.hasMany(SleepLog, { foreignKey: "userId", onDelete: "CASCADE" });
SleepLog.belongsTo(User, { foreignKey: "userId" });

// Relasi User ke NutritionLogs (1 to Many)
User.hasMany(NutritionLog, { foreignKey: "userId", onDelete: "CASCADE" });
NutritionLog.belongsTo(User, { foreignKey: "userId" });

// Relasi User ke ChatLogs
User.hasMany(ChatLog, { foreignKey: "userId", onDelete: "CASCADE" });
ChatLog.belongsTo(User, { foreignKey: "userId" });

module.exports = {
  sequelize,
  Op,
  User,
  Habit,
  SleepLog,
  NutritionLog,
  ChatLog,
};
