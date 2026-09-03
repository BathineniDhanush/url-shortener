import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    redirects: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.REQUESTS_PER_SECOND || 50),
      timeUnit: '1s',
      duration: __ENV.DURATION || '30s',
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200'],
  },
};

export default function () {
  const response = http.get(`${__ENV.BASE_URL || 'http://localhost:8080'}/${__ENV.CODE}`, {
    redirects: 0,
  });
  check(response, {
    'redirect is 302': (result) => result.status === 302,
    'location is present': (result) => Boolean(result.headers.Location),
  });
  sleep(0.01);
}
