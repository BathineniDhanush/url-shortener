import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    redirects: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 300,
      maxVUs: 1000,
      stages: [
        { target: 100, duration: '10s' },
        { target: 500, duration: '15s' },
        { target: 1000, duration: '15s' },
        { target: 2000, duration: '20s' },
        { target: 3000, duration: '20s' },
        { target: 3000, duration: '20s' },
        { target: 0, duration: '10s' },
      ],
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    dropped_iterations: ['count==0'],
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const response = http.get(`${__ENV.BASE_URL}/${__ENV.CODE}`, {
    redirects: 0,
    timeout: '10s',
  });

  check(response, {
    'redirect is 302': (result) => result.status === 302,
    'location is present': (result) => Boolean(result.headers.Location),
  });
}
