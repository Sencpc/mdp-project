const dotenv = require("dotenv");
const fs = require("fs");
dotenv.config();

const SATUSEHAT_CLIENT_ID = process.env.SATUSEHAT_CLIENT_ID;
const SATUSEHAT_CLIENT_SECRET = process.env.SATUSEHAT_CLIENT_SECRET;
const ORGANIZATION_ID = process.env.SATUSEHAT_ORGANIZATION_ID;

async function testSatuSehat() {
  console.log("1. Requesting OAuth Token...");
  const authResponse = await fetch(
    "https://api-satusehat-stg.dto.kemkes.go.id/oauth2/v1/accesstoken?grant_type=client_credentials",
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: `client_id=${SATUSEHAT_CLIENT_ID}&client_secret=${SATUSEHAT_CLIENT_SECRET}`,
    },
  );

  const authData = await authResponse.json();
  if (!authData.access_token) {
    console.error("Failed to get token:", authData);
    return;
  }

  console.log("Token received successfully!\n");
  const token = authData.access_token;

  const startId = 10000232457;
  const endId = 19999999999; // Note: This is an enormous range.

  const csvFile = "practitioners2.csv";

  // Only write header if file doesn't exist to avoid overwriting old data
  if (!fs.existsSync(csvFile)) {
    fs.writeFileSync(csvFile, "ID,Address,City\n");
  }
  console.log(`Saving data to ${csvFile} (Resuming from ${startId})...`);

  for (let id = startId; id <= endId; id++) {
    try {
      const practitionerRes = await fetch(
        `https://api-satusehat-stg.dto.kemkes.go.id/fhir-r4/v1/Practitioner/${id}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      const status = practitionerRes.status;
      if (status !== 200) {
        continue;
      }

      const practitionerData = await practitionerRes.json();

      let addressLine = "Unknown";
      let city = "Unknown";
      if (practitionerData.address && practitionerData.address.length > 0) {
        const addr = practitionerData.address[0];
        addressLine = addr.line ? addr.line.join(" ").replace(/"/g, '""') : "";
        city = addr.city ? addr.city.replace(/"/g, '""') : "";
      }

      fs.appendFileSync(
        csvFile,
        `"${practitionerData.id || id}","${addressLine}","${city}"\n`,
      );
      console.log(`Saved Practitioner/${id} - ${city}`);

      // Optional: Add a small delay to prevent getting rate-limited
      await new Promise((resolve) => setTimeout(resolve, 200));
    } catch (error) {
      console.error(`Error fetching Practitioner/${id}:`, error.message);
    }
  }
}

testSatuSehat();
