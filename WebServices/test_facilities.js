require('dotenv').config();

async function test() {
  const SATUSEHAT_CLIENT_ID = process.env.SATUSEHAT_CLIENT_ID;
  const SATUSEHAT_CLIENT_SECRET = process.env.SATUSEHAT_CLIENT_SECRET;
  
  if (!SATUSEHAT_CLIENT_ID || !SATUSEHAT_CLIENT_SECRET) {
    console.log('No credentials in env');
    return;
  }
  
  const authResponse = await fetch('https://api-satusehat-stg.dto.kemkes.go.id/oauth2/v1/accesstoken?grant_type=client_credentials', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `client_id=${SATUSEHAT_CLIENT_ID}&client_secret=${SATUSEHAT_CLIENT_SECRET}`
  });
  const authData = await authResponse.json();
  const token = authData.access_token;
  
  const res = await fetch('https://api-satusehat-stg.dto.kemkes.go.id/masterdata/v1/mastersaranaindex/mastersarana?limit=50&page=1&jenis_sarana=104&status_aktif=true&kode_provinsi=35&kode_kabkota=3578&kode_kecamatan=357804', {
    headers: { Authorization: `Bearer ${token}` }
  });
  const data = await res.json();
  console.log(JSON.stringify(data, null, 2));
}
test();
