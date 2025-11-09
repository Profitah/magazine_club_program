const axios = require('axios');
const env = require('../config/env');

async function verifyAdminSession(email, sessionCookie) {
  try {
    const response = await axios.post(
      `${env.services.springBootUrl}/admin/verify-for-chat`,
      { email },
      {
        headers: {
          'Content-Type': 'application/json',
          Cookie: sessionCookie || '',
        },
        validateStatus: (status) => status < 500,
      },
    );

    if (response.status === 200 && response.data.authenticated) {
      return {
        authenticated: true,
        adminId: response.data.adminId,
        email: response.data.email,
      };
    }

    return { authenticated: false };
  } catch (error) {
    console.error('Spring Boot 인증 확인 실패:', error.message);
    return { authenticated: false };
  }
}

module.exports = {
  verifyAdminSession,
};

