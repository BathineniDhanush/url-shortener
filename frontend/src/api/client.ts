import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add interceptors for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.data) {
      // You can handle Problem responses here if needed
      console.error('API Error:', error.response.data);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
