const API_KEY =
  "hk_mrf0rupn6dbb6dff3a6ce06116f9f7dbe8216b30641a8c992f02c95231fbefd82f0c742b";
const URL = "https://doctorsapi.com/api/doctors";

async function testDoctorsApi() {
  try {
    console.log(`Sending GET request to ${URL}...`);
    const response = await fetch(URL, {
      method: "GET",
      headers: {
        "api-key": API_KEY,
        "Content-Type": "application/json",
      },
    });

    const status = response.status;
    console.log(`Response Status: ${status} ${response.statusText}`);

    const text = await response.text();
    try {
      const data = JSON.parse(text);
      console.log("Response Data (JSON):");
      console.log(JSON.stringify(data, null, 2));
    } catch (e) {
      console.log("Response Data (Raw):");
      console.log(text);
    }
  } catch (error) {
    console.error("Failed to fetch from Doctors API:");
    console.error(error.message);
  }
}

testDoctorsApi();
