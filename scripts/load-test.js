const axios = require('axios');

const BASE_URL = 'http://localhost:8080/api';
const CATEGORY_ID = 11;
const NUM_USERS = 50;

async function runTest() {
  console.log(`Starting load test with ${NUM_USERS} concurrent users...`);
  const users = [];

  // Step 1: Register 50 users
  console.log('Registering users...');
  for (let i = 0; i < NUM_USERS; i++) {
    const timestamp = Date.now();
    const user = {
      username: `loaduser_${timestamp}_${i}`,
      password: 'password123',
      email: `loaduser_${timestamp}_${i}@test.com`,
      fullName: `Load User ${i}`
    };

    try {
      // Register
      await axios.post(`${BASE_URL}/auth/register`, user);
      
      // Login
      const loginRes = await axios.post(`${BASE_URL}/auth/login`, {
        username: user.username,
        password: user.password
      });
      
      users.push({
        ...user,
        token: loginRes.data.accessToken
      });
    } catch (e) {
      console.error(`Failed to register user ${i}:`, e.response?.data || e.message);
    }
  }

  console.log(`Successfully registered and logged in ${users.length} users.`);

  // Step 2: Concurrent Booking
  console.log(`Attempting to book 1 ticket of category ${CATEGORY_ID} simultaneously...`);
  
  const requests = users.map(user => {
    return axios.post(
      `${BASE_URL}/tickets/purchase`,
      {
        categoryId: CATEGORY_ID,
        quantity: 1
      },
      {
        headers: {
          Authorization: `Bearer ${user.token}`
        }
      }
    ).then(res => {
      return { user: user.username, status: res.status, data: res.data, success: true };
    }).catch(err => {
      return { user: user.username, status: err.response?.status, error: err.response?.data || err.message, success: false };
    });
  });

  const results = await Promise.all(requests);

  const successful = results.filter(r => r.success);
  const failed = results.filter(r => !r.success);

  console.log('\n--- Load Test Results ---');
  console.log(`Total Requests: ${requests.length}`);
  console.log(`Successful Bookings (Expected: 10 max): ${successful.length}`);
  console.log(`Failed Bookings (Expected: ${requests.length - 10}): ${failed.length}`);

  // Group errors
  const errorCounts = {};
  failed.forEach(f => {
    const errMsg = typeof f.error === 'string' ? f.error : JSON.stringify(f.error);
    errorCounts[errMsg] = (errorCounts[errMsg] || 0) + 1;
  });

  console.log('\nError Breakdown:');
  Object.entries(errorCounts).forEach(([err, count]) => {
    console.log(`- ${err}: ${count} times`);
  });
}

runTest();
