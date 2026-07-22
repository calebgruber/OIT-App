class OITApiClient {
  constructor() {
    this.baseUrl = this.normalizeBaseUrl(localStorage.getItem('api_url') || 'http://localhost:3000/api/v1');
    this.token = localStorage.getItem('auth_token');
  }

  normalizeBaseUrl(url) {
    const value = (url || '').trim().replace(/\/$/, '');
    if (!value) {
      return 'http://localhost:3000/api/v1';
    }
    if (/\/api\/v\d+$/i.test(value)) {
      return value;
    }
    if (/\/api$/i.test(value)) {
      return `${value}/v1`;
    }
    return `${value}/api/v1`;
  }

  refreshConfig() {
    this.baseUrl = this.normalizeBaseUrl(localStorage.getItem('api_url') || 'http://localhost:3000/api/v1');
    this.token = localStorage.getItem('auth_token');
  }

  buildUrl(path, params = null) {
    const url = new URL(path.startsWith('http') ? path : `${this.baseUrl}${path}`);
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          url.searchParams.set(key, value);
        }
      });
    }
    return url.toString();
  }

  async request(method, path, body = null, params = null) {
    this.refreshConfig();
    const headers = { Accept: 'application/json' };
    const options = { method, headers };

    if (this.token) {
      headers.Authorization = 'Bearer ' + this.token;
    }

    if (body !== null) {
      headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }

    const response = await fetch(this.buildUrl(path, params), options);
    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('application/json') ? await response.json().catch(() => ({})) : await response.text();

    if (response.status === 401) {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('current_user');
      if (!window.location.pathname.endsWith('index.html')) {
        window.location.href = 'index.html';
      }
      throw new Error('Your session has expired. Please sign in again.');
    }

    if (!response.ok) {
      const message = typeof payload === 'string'
        ? payload
        : payload?.message || payload?.error || `Request failed with status ${response.status}`;
      throw new Error(message);
    }

    return payload;
  }

  unwrap(payload) {
    if (payload && typeof payload === 'object') {
      if (Array.isArray(payload)) {
        return payload;
      }
      if ('data' in payload) {
        return payload.data;
      }
      if ('items' in payload) {
        return payload.items;
      }
      if ('results' in payload) {
        return payload.results;
      }
    }
    return payload;
  }

  async login(username, password) {
    const response = await this.request('POST', '/auth/login', { username, password });
    const token = response?.token || response?.jwt || response?.access_token || response?.data?.token || response?.data?.access_token;
    if (!token) {
      throw new Error('Authentication token missing from response.');
    }
    const user = response?.user || response?.data?.user || {
      name: username,
      username,
      role: 'Clinical Staff'
    };
    this.token = token;
    localStorage.setItem('auth_token', token);
    localStorage.setItem('current_user', JSON.stringify(user));
    return { token, user, response };
  }

  async logout() {
    try {
      if (this.token) {
        await this.request('POST', '/auth/logout');
      }
    } catch (error) {
      console.warn('Logout request failed:', error);
    } finally {
      this.token = null;
      localStorage.removeItem('auth_token');
      localStorage.removeItem('current_user');
    }
  }

  async getPatients(search = '') {
    return this.unwrap(await this.request('GET', '/patients', null, { search }));
  }

  async getPatient(id) {
    return this.unwrap(await this.request('GET', `/patients/${id}`));
  }

  async getPatientByBarcode(barcode) {
    return this.unwrap(await this.request('GET', '/patients/barcode', null, { barcode }));
  }

  async getDosePlan(patientId) {
    return this.unwrap(await this.request('GET', `/patients/${patientId}/dose-plan`));
  }

  async getUpdoseDecisions(patientId) {
    return this.unwrap(await this.request('GET', `/patients/${patientId}/updose-decisions`));
  }

  async getSyringes(patientId, status) {
    return this.unwrap(await this.request('GET', '/syringes', null, { patient_id: patientId, status }));
  }

  async getCartons(patientId) {
    return this.unwrap(await this.request('GET', '/cartons', null, { patient_id: patientId }));
  }

  async getPowderSupplies(patientId) {
    return this.unwrap(await this.request('GET', '/powder-supplies', null, { patient_id: patientId }));
  }

  async getMAR(patientId, dateFrom, dateTo) {
    return this.unwrap(await this.request('GET', '/mar', null, { patient_id: patientId, date_from: dateFrom, date_to: dateTo }));
  }

  async getFoodLogs(patientId, date) {
    return this.unwrap(await this.request('GET', '/food-logs', null, { patient_id: patientId, date }));
  }

  async getSymptomLogs(patientId) {
    return this.unwrap(await this.request('GET', '/symptom-logs', null, { patient_id: patientId }));
  }

  async getNotifications(patientId, unread) {
    return this.unwrap(await this.request('GET', '/notifications', null, { patient_id: patientId, unread }));
  }

  async markNotificationRead(id) {
    return this.unwrap(await this.request('PATCH', `/notifications/${id}/read`, {}));
  }

  async getAuditLogs(filters = {}) {
    return this.unwrap(await this.request('GET', '/audit-logs', null, filters));
  }

  async getMissions(patientId, date) {
    return this.unwrap(await this.request('GET', '/missions', null, { patient_id: patientId, date }));
  }

  async getBadges(patientId) {
    return this.unwrap(await this.request('GET', '/badges', null, { patient_id: patientId }));
  }
}

const api = new OITApiClient();
window.OITApiClient = OITApiClient;
window.api = api;
