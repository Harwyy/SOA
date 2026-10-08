const CITY_API_URL = (import.meta.env.VITE_CITY_API_URL || 'http://localhost:8080/city-service').replace(/\/$/, '');
const ROUTE_API_URL = (import.meta.env.VITE_ROUTE_API_URL || 'http://localhost:8081/route').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(code, message, details = []) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.details = details;
  }
}

async function request(baseUrl, path, options = {}) {
  let response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(options.headers || {})
      }
    });
  } catch (error) {
    throw new ApiError(0, 'Не удалось подключиться к сервису. Проверьте HTTP-адрес и доступность сервера.');
  }

  const contentType = response.headers.get('content-type') || '';
  const body = response.status === 204
    ? null
    : contentType.includes('json')
      ? await response.json().catch(() => null)
      : await response.text().catch(() => '');

  if (!response.ok) {
    const message = body && typeof body === 'object' && body.message
      ? body.message
      : `Сервис вернул ошибку HTTP ${response.status}`;
    const details = body && typeof body === 'object' && Array.isArray(body.details) ? body.details : [];
    throw new ApiError(response.status, message, details);
  }

  return body;
}

function addParam(params, key, value) {
  if (value !== undefined && value !== null && String(value).trim() !== '') {
    params.set(key, value);
  }
}

function normalizeDateTime(value) {
  if (!value) return value;
  const match = String(value).trim().match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})(?::\d{2})?(?:Z|[+-]\d{2}:\d{2})?$/);
  return match ? `${match[1]}T${match[2]}:00` : value;
}

function localDateTimeForApi(value) {
  return normalizeDateTime(value);
}

function zonedDateTimeForApi(value) {
  const normalized = normalizeDateTime(value);
  return normalized ? `${normalized}Z` : value;
}

export async function fetchCities({ filters, sort, page, size }) {
  const params = new URLSearchParams();
  const mapping = {
    id: filters.id,
    name: filters.name,
    creationDate: zonedDateTimeForApi(filters.creationDate),
    area: filters.area,
    population: filters.population,
    metersAboveSeaLevel: filters.metersAboveSeaLevel,
    establishmentDate: localDateTimeForApi(filters.establishmentDate),
    capital: filters.capital,
    climate: filters.climate,
    'coordinates.x': filters.coordinatesX,
    'coordinates.y': filters.coordinatesY,
    'governor.birthday': zonedDateTimeForApi(filters.governorBirthday)
  };
  Object.entries(mapping).forEach(([key, value]) => addParam(params, key, value));
  sort.forEach((criterion) => params.append('sort', criterion));
  params.set('page', page);
  params.set('size', size);
  const result = await request(CITY_API_URL, `/cities?${params.toString()}`);
  return Array.isArray(result) ? result : [];
}

export function getCity(id) {
  return request(CITY_API_URL, `/cities/${encodeURIComponent(id)}`);
}

function cityPayload(form) {
  const payload = {
    name: form.name,
    coordinates: {
      x: Number(form.coordinatesX),
      y: Number(form.coordinatesY)
    },
    area: Number(form.area),
    population: Number(form.population),
    capital: form.capital,
    climate: form.climate || null,
    metersAboveSeaLevel: form.metersAboveSeaLevel === '' ? null : Number(form.metersAboveSeaLevel),
    establishmentDate: form.establishmentDate ? localDateTimeForApi(form.establishmentDate) : null,
    governor: form.governorBirthday
      ? { birthday: zonedDateTimeForApi(form.governorBirthday) }
      : null
  };
  return payload;
}

export function createCity(form) {
  return request(CITY_API_URL, '/cities', {
    method: 'POST',
    body: JSON.stringify(cityPayload(form))
  });
}

export function updateCity(id, form) {
  return request(CITY_API_URL, `/cities/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(cityPayload(form))
  });
}

export function deleteCity(id) {
  return request(CITY_API_URL, `/cities/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

export function deleteByMeters(value) {
  return request(CITY_API_URL, `/cities/by-meters-above-sea-level?metersAboveSeaLevel=${encodeURIComponent(value)}`, {
    method: 'DELETE'
  });
}

export function deleteByEstablishmentDate(value) {
  return request(CITY_API_URL, `/cities/by-establishment-date?establishmentDate=${encodeURIComponent(localDateTimeForApi(value))}`, {
    method: 'DELETE'
  });
}

export function fetchAverage() {
  return request(CITY_API_URL, '/cities/average-meters-above-sea-level');
}

export function calculateRoute(kind) {
  return request(ROUTE_API_URL, `/calculate/${kind}`);
}

export const apiConfig = { CITY_API_URL, ROUTE_API_URL };
