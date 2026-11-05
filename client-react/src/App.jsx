import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from './api/httpClient.js';
import {
  createCity, getAverageMeters, getCity, listCities, removeCitiesByMeters,
  removeCity, removeCityByEstablishmentDate, updateCity
} from './api/cityService.js';
import { calculateRoute } from './api/routeService.js';
import { Feedback } from './components/Feedback.jsx';
import { CityFilters } from './features/cities/CityFilters.jsx';
import { CityForm } from './features/cities/CityForm.jsx';
import { cityToForm, emptyCityFilters, emptyCityForm } from './features/cities/cityForm.js';
import { CityTable } from './features/cities/CityTable.jsx';
import { OperationsPanel } from './features/operations/OperationsPanel.jsx';
import { RoutesPanel } from './features/routes/RoutesPanel.jsx';

const pageSizes = [5, 10, 25, 50, 100];

function errorFor(error) {
  return error instanceof ApiError
    ? error
    : new ApiError(0, error?.message || 'Произошла непредвиденная ошибка');
}

export default function App() {
  const [tab, setTab] = useState('cities');
  const [cities, setCities] = useState([]);
  const [filters, setFilters] = useState(emptyCityFilters);
  const [sort, setSort] = useState([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(emptyCityForm);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState('');
  const [lookupId, setLookupId] = useState('');
  const [lookupCity, setLookupCity] = useState(null);
  const [meters, setMeters] = useState('');
  const [date, setDate] = useState('');
  const [average, setAverage] = useState(null);
  const [routeResults, setRouteResults] = useState({});

  const showError = useCallback((exception) => { setNotice(''); setError(errorFor(exception)); }, []);
  const loadCities = useCallback(async () => {
    setLoading(true);
    try { setCities(await listCities({ filters, sort, page, size })); setError(null); }
    catch (exception) { showError(exception); }
    finally { setLoading(false); }
  }, [filters, page, showError, size, sort]);

  useEffect(() => { loadCities(); }, [loadCities]);

  const totalPages = useMemo(() => cities.length < size ? Math.max(1, page) : page + 1, [cities.length, page, size]);
  const updateForm = (name, value) => setForm((current) => ({ ...current, [name]: value }));
  const updateFilter = (name, value) => setFilters((current) => ({ ...current, [name]: value }));
  const finish = async (message) => { setNotice(message); setError(null); await loadCities(); };

  function toggleSort(field) {
    setSort((current) => {
      const active = current.find((criterion) => criterion.startsWith(`${field},`));
      if (!active) return [...current, `${field},asc`];
      if (active.endsWith(',asc')) return current.map((criterion) => criterion === active ? `${field},desc` : criterion);
      return current.filter((criterion) => criterion !== active);
    });
    setPage(1);
  }

  async function saveCity(event) {
    event.preventDefault();
    try {
      if (editingId === null) await createCity(form); else await updateCity(editingId, form);
      setForm(emptyCityForm); setEditingId(null);
      await finish(editingId === null ? 'Город успешно создан' : `Город #${editingId} обновлён`);
    } catch (exception) { showError(exception); }
  }

  function editCity(city) { setForm(cityToForm(city)); setEditingId(city.id); setTab('cities'); window.scrollTo({ top: 0, behavior: 'smooth' }); }
  async function deleteCity(id) { if (window.confirm(`Удалить город #${id}?`)) { try { await removeCity(id); await finish(`Город #${id} удалён`); } catch (exception) { showError(exception); } } }
  async function lookup(event) { event.preventDefault(); try { setLookupCity(await getCity(lookupId)); setError(null); } catch (exception) { setLookupCity(null); showError(exception); } }
  async function special(action, message) { try { await action(); await finish(message); } catch (exception) { showError(exception); } }
  async function route(kind) { try { const result = await calculateRoute(kind); const city = result ? await getCity(result.cityId).catch(() => null) : null; setRouteResults((current) => ({ ...current, [kind]: result ? { ...result, city } : null })); setError(null); } catch (exception) { showError(exception); } }

  return <main className="app-shell">
    <header className="hero"><h1>Города</h1></header>
    <Feedback error={error} notice={notice} onClose={() => { setError(null); setNotice(''); }} />
    <nav className="tabs" aria-label="Разделы приложения">
      <button type="button" className={tab === 'cities' ? 'tab active' : 'tab'} onClick={() => setTab('cities')}>Коллекция</button>
      <button type="button" className={tab === 'operations' ? 'tab active' : 'tab'} onClick={() => setTab('operations')}>Операции</button>
      <button type="button" className={tab === 'routes' ? 'tab active' : 'tab'} onClick={() => setTab('routes')}>Маршруты</button>
    </nav>
    {tab === 'cities' && <section className="dashboard-grid"><section className="panel collection-panel">
      <div className="section-heading"><div><h2>Коллекция городов</h2></div><button type="button" className="button secondary" onClick={loadCities} disabled={loading}>{loading ? 'Загрузка…' : 'Обновить'}</button></div>
      <CityFilters filters={filters} onChange={updateFilter} onSubmit={(event) => { event.preventDefault(); setPage(1); }} onReset={() => { setFilters(emptyCityFilters); setSort([]); setPage(1); }} />
      <CityTable cities={cities} sort={sort} onSort={toggleSort} onEdit={editCity} onDelete={deleteCity} />
      <div className="pagination"><label>Размер <select value={size} onChange={(e) => { setSize(Number(e.target.value)); setPage(1); }}>{pageSizes.map((option) => <option key={option}>{option}</option>)}</select></label><button type="button" className="button ghost" disabled={page <= 1} onClick={() => setPage(page - 1)}>← Назад</button><strong>Страница {page}</strong><button type="button" className="button ghost" disabled={page >= totalPages} onClick={() => setPage(page + 1)}>Вперёд →</button></div>
    </section><CityForm form={form} editingId={editingId} onChange={updateForm} onSubmit={saveCity} onCancel={() => { setForm(emptyCityForm); setEditingId(null); }} /></section>}
    {tab === 'operations' && <OperationsPanel lookupId={lookupId} onLookupIdChange={setLookupId} lookupCity={lookupCity} onLookup={lookup} meters={meters} onMetersChange={setMeters} date={date} onDateChange={setDate} average={average} onEdit={editCity} onDeleteByMeters={() => special(() => removeCitiesByMeters(meters), 'Города с заданной высотой удалены')} onDeleteByDate={() => special(() => removeCityByEstablishmentDate(date), 'Город с заданной датой основания удалён')} onAverage={async () => { try { const result = await getAverageMeters(); setAverage(result?.averageMetersAboveSeaLevel ?? null); setError(null); } catch (exception) { showError(exception); } }} />}
    {tab === 'routes' && <RoutesPanel results={routeResults} onCalculate={route} />}
  </main>;
}
