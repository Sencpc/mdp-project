const fs = require("fs");
const dotenv = require("dotenv");
const { sequelize, Doctor } = require("./db");

const SATUSEHAT_CLIENT_ID = process.env.SATUSEHAT_CLIENT_ID;
const SATUSEHAT_CLIENT_SECRET = process.env.SATUSEHAT_CLIENT_SECRET;

async function seedSurabayaDoctors() {
  console.log("Connecting to Database...");
  await sequelize.authenticate();
  await Doctor.sync({ force: true });
  console.log("Database connected & Doctors table recreated.\n");

  const surabayaDoctorIds = new Set();
  const files = ["practitioners.csv", "practitioners2.csv"];

  for (const file of files) {
    if (!fs.existsSync(file)) continue;
    const content = fs.readFileSync(file, "utf-8");
    const lines = content.split("\n");

    // Skip header
    for (let i = 1; i < lines.length; i++) {
      const line = lines[i].trim();
      if (!line) continue;

      // Basic CSV parsing for "ID","Address","City"
      // Match something like: "10000232558","","Kota Surabaya"
      const parts = line.split('","');
      if (parts.length >= 3) {
        const id = parts[0].replace(/^"/, "");
        const city = parts[2].replace(/"$/, "");

        if (city.toLowerCase().includes("surabaya")) {
          surabayaDoctorIds.add(id);
        }
      } else if (parts.length === 2) {
        // Fallback for practitioners.csv which might just be ID,Location
        const id = parts[0].replace(/^"/, "");
        const location = parts[1].replace(/"$/, "");
        if (location.toLowerCase().includes("surabaya")) {
          surabayaDoctorIds.add(id);
        }
      }
    }
  }

  const doctorIds = Array.from(surabayaDoctorIds);
  console.log(`Found ${doctorIds.length} doctors from Surabaya in CSVs.`);
  if (doctorIds.length === 0) {
    console.log("Nothing to seed. Exiting.");
    process.exit(0);
  }

  console.log("\nRequesting SATUSEHAT OAuth Token...");
  const authResponse = await fetch(
    "https://api-satusehat-stg.dto.kemkes.go.id/oauth2/v1/accesstoken?grant_type=client_credentials",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
      },
      body: `client_id=${SATUSEHAT_CLIENT_ID}&client_secret=${SATUSEHAT_CLIENT_SECRET}`,
    },
  );

  const authData = await authResponse.json();
  if (!authData.access_token) {
    console.error("Failed to get token:", authData);
    process.exit(1);
  }

  const token = authData.access_token;
  console.log("Token received successfully!\n");

  let seedIndex = 0;
  for (const id of doctorIds) {
    try {
      console.log(`Fetching full data for Practitioner/${id}...`);
      const res = await fetch(
        `https://api-satusehat-stg.dto.kemkes.go.id/fhir-r4/v1/Practitioner/${id}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
          },
        },
      );

      if (res.status !== 200) {
        console.log(
          `Failed to fetch Practitioner/${id} (Status: ${res.status})`,
        );
        continue;
      }

      const rawData = await res.json();

      // Parse data for DB
      const nameObj =
        rawData.name && rawData.name.length > 0 ? rawData.name[0] : null;
      const name = nameObj && nameObj.text ? nameObj.text : "Unknown Name";

      // Convert ALL CAPS to Title Case (e.g. "ARI YULIANTO" -> "Ari Yulianto")
      let displayName = name
        .toLowerCase()
        .split(" ")
        .map((word) => {
          return word.charAt(0).toUpperCase() + word.slice(1);
        })
        .join(" ");

      // Add "Dr. " prefix if it doesn't already have one
      if (
        !displayName.toLowerCase().startsWith("dr.") &&
        !displayName.toLowerCase().startsWith("dr ")
      ) {
        displayName = "Dr. " + displayName;
      }

      const gender = rawData.gender || "Unknown";
      const birthDate = rawData.birthDate || "Unknown";

      let city = "Surabaya"; // Default since we filtered for it
      if (
        rawData.address &&
        rawData.address.length > 0 &&
        rawData.address[0].city
      ) {
        city = rawData.address[0].city;
      }

      // Fake Data Generation
      const categories = [
        "General Practice",
        "Cardiology",
        "Neurology",
        "Pediatrics",
        "Dermatology",
        "Orthopedics",
        "Psychiatry",
      ];
      const category =
        categories[Math.floor(Math.random() * categories.length)];
      const description = `Experienced ${category.toLowerCase()} specialist dedicated to providing comprehensive and personalized patient care.`;
      const rating = parseFloat((Math.random() * 1.5 + 3.5).toFixed(1));
      const icons = [
        "medical_services",
        "stethoscope",
        "local_hospital",
        "healing",
        "favorite",
      ];
      const profileIcon = icons[Math.floor(Math.random() * icons.length)];

      // Available time: tomorrow 08:00 + offset based on index (so each doctor differs)
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      const baseHour = 8;
      const offsetMinutes = (seedIndex * 30) % (12 * 60); // spread across 8AM - 8PM
      const offsetHours = Math.floor(offsetMinutes / 60);
      const offsetMins = offsetMinutes % 60;
      tomorrow.setHours(baseHour + offsetHours, offsetMins, 0, 0);
      const availableTime = tomorrow.getTime();
      seedIndex++;

      // Save to database
      await Doctor.create({
        satusehatId: id,
        name: name,
        displayName: displayName,
        gender: gender,
        birthDate: birthDate,
        city: city,
        category: category,
        description: description,
        rating: rating,
        profileIcon: profileIcon,
        availableTime: availableTime,
        raw_data: rawData,
      });

      console.log(`Saved ${displayName} (${id}) to Database.`);

      // Wait to avoid rate limits
      await new Promise((resolve) => setTimeout(resolve, 250));
    } catch (err) {
      console.error(`Error processing Practitioner/${id}:`, err.message);
    }
  }

  console.log("\nFinished seeding Surabaya doctors!");
  process.exit(0);
}

seedSurabayaDoctors();
