import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ApiError,
  calculateRoute,
  createCity,
  deleteByEstablishmentDate,
  deleteByMeters,
  deleteCity,
  fetchAverage,
  fetchCities,
  getCity,
  updateCity
} from './api.js';

const PAGE_SIZE_OPTIONS = [5, 10, 25, 50, 100];
const DATE_TIME_PLACEHOLDER = 'гггг-мм-дд чч:мм';
const SORT_FIELDS = [
  ['id', 'ID'],
  ['name', 'Название'],
  ['area', 'Площадь'],
  ['population', 'Население'],
  ['metersAboveSeaLevel', 'Высота'],
  ['establishmentDate', 'Дата основания'],
  ['capital', 'Столица'],
  ['climate', 'Климат'],
  ['coordinates.x', 'X'],
  ['coordinates.y', 'Y'],
  ['governor.birthday', 'День рождения губернатора'],
  ['creationDate', 'Создана']
];

const emptyFilters = {
  id: '', name: '', creationDate: '', area: '', population: '', metersAboveSeaLevel: '',
  establishmentDate: '', capital: '', climate: '', coordinatesX: '', coordinatesY: '', governorBirthday: ''
};

const emptyForm = {
  name: '', coordinatesX: '', coordinatesY: '', area: '', population: '', metersAboveSeaLevel: '',
  establishmentDate: '', capital: false, climate: '', governorBirthday: ''
};

function parseDate(value) {
  if (!value) return null;
  const normalized = String(value).replace(/\[[^\]]+\]$/, '');
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
}

function pad(value, length = 2) {
  return String(value).padStart(length, '0');
}

function formatDateParts(date, separator = ' ') {
  return [
    `${pad(date.getFullYear(), 4)}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`,
    `${pad(date.getHours())}:${pad(date.getMinutes())}`
  ].join(separator);
}

function hasTimezone(value) {
  return /[zZ]|[+-]\d{2}:\d{2}/.test(String(value));
}

function formatDateTime(value) {
  if (!value) return '—';

  const raw = String(value);
  if (!hasTimezone(raw)) {
    const localDateTime = raw.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/);
    if (localDateTime) return `${localDateTime[1]} ${localDateTime[2]}`;
  }

  const date = parseDate(value);
  return date ? formatDateParts(date) : '—';
}

function formatLocalDate(value) {
  if (!value) return '';

  const raw = String(value);
  if (!hasTimezone(raw)) {
    const localDateTime = raw.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/);
    if (localDateTime) return `${localDateTime[1]} ${localDateTime[2]}`;
  }

  const date = parseDate(value);
  return date ? formatDateParts(date) : '';
}

function formatNumber(value, digits = 2) {
  return value === null || value === undefined ? '—' : Number(value).toLocaleString('ru-RU', { maximumFractionDigits: digits });
}

function cityToForm(city) {
  return {
    name: city.name || '',
    coordinatesX: city.coordinates?.x ?? '',
    coordinatesY: city.coordinates?.y ?? '',
    area: city.area ?? '',
    population: city.population ?? '',
    metersAboveSeaLevel: city.metersAboveSeaLevel ?? '',
    establishmentDate: formatLocalDate(city.establishmentDate),
    capital: Boolean(city.capital),
    climate: city.climate || '',
    governorBirthday: city.governor?.birthday ? formatLocalDate(city.governor.birthday) : ''
  };
}

function getErrorMessage(error) {
  if (error instanceof ApiError) {
    const prefix = error.code ? `HTTP ${error.code}: ` : '';
    return { title: prefix + error.message, details: error.details };
  }
  return { title: error?.message || 'Произошла непредвиденная ошибка', details: [] };
}

function App() {
  const [cities, setCities] = useState([]);
  const [filters, setFilters] = useState(emptyFilters);
  const [sort, setSort] = useState([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalHint, setTotalHint] = useState(0);
  const [activeTab, setActiveTab] = useState('collection');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [lookupId, setLookupId] = useState('');
  const [lookupCity, setLookupCity] = useState(null);
  const [metersForDelete, setMetersForDelete] = useState('');
  const [dateForDelete, setDateForDelete] = useState('');
  const [average, setAverage] = useState(null);
  const [routeResults, setRouteResults] = useState({});

  const showError = useCallback((exception) => {
    setNotice('');
    setError(getErrorMessage(exception));
  }, []);

  const loadCities = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchCities({ filters, sort, page, size });
      setCities(result);
      setTotalHint(result.length);
      setError(null);
    } catch (exception) {
      showError(exception);
    } finally {
      setLoading(false);
    }
  }, [filters, page, showError, size, sort]);

  useEffect(() => { loadCities(); }, [loadCities]);

  const totalPages = useMemo(() => {
    if (cities.length < size && page === 1) return 1;
    return cities.length < size ? page : page + 1;
  }, [cities.length, page, size]);

  function updateFilter(key, value) {
    setFilters((current) => ({ ...current, [key]: value }));
  }

  function updateForm(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function submitFilters(event) {
    event.preventDefault();
    setPage(1);
  }

  function resetFilters() {
    setFilters(emptyFilters);
    setSort([]);
    setPage(1);
  }

  function toggleSort(field) {
    setSort((current) => {
      const existing = current.find((criterion) => criterion.startsWith(`${field},`));
      if (!existing) return [...current, `${field},asc`];
      if (existing.endsWith(',asc')) return current.map((criterion) => criterion === existing ? `${field},desc` : criterion);
      return current.filter((criterion) => criterion !== existing);
    });
    setPage(1);
  }

  async function saveCity(event) {
    event.preventDefault();
    try {
      if (editingId === null) {
        await createCity(form);
        setNotice('Город успешно создан');
      } else {
        await updateCity(editingId, form);
        setNotice(`Город #${editingId} обновлён`);
      }
      setError(null);
      cancelEdit();
      await loadCities();
    } catch (exception) {
      showError(exception);
    }
  }

  function startEdit(city) {
    setEditingId(city.id);
    setForm(cityToForm(city));
    setActiveTab('collection');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(emptyForm);
  }

  async function removeCity(id) {
    if (!window.confirm(`Удалить город #${id}?`)) return;
    try {
      await deleteCity(id);
      setNotice(`Город #${id} удалён`);
      setError(null);
      await loadCities();
    } catch (exception) {
      showError(exception);
    }
  }

  async function findCity(event) {
    event.preventDefault();
    if (!lookupId) return;
    try {
      setLookupCity(await getCity(lookupId));
      setError(null);
    } catch (exception) {
      setLookupCity(null);
      showError(exception);
    }
  }

  async function runSpecialAction(action, successMessage) {
    try {
      await action();
      setNotice(successMessage);
      setError(null);
      await loadCities();
    } catch (exception) {
      showError(exception);
    }
  }

  async function loadAverage() {
    try {
      const result = await fetchAverage();
      setAverage(result?.averageMetersAboveSeaLevel ?? null);
      setError(null);
      if (!result) setNotice('В коллекции нет городов с указанной высотой');
    } catch (exception) {
      showError(exception);
    }
  }

  async function loadRoute(kind) {
    try {
      const result = await calculateRoute(kind);
      setRouteResults((current) => ({ ...current, [kind]: result || null }));
      setError(null);
    } catch (exception) {
      showError(exception);
    }
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">Лабораторная работа</p>
          <h1>Города</h1>
          <p className="hero-copy">Управление коллекцией городов и расчёт маршрутов.</p>
        </div>
      </header>

      {(error || notice) && <Feedback error={error} notice={notice} onClose={() => { setError(null); setNotice(''); }} />}

      <nav className="tabs" aria-label="Разделы приложения">
        <button className={activeTab === 'collection' ? 'tab active' : 'tab'} onClick={() => setActiveTab('collection')}>Коллекция</button>
        <button className={activeTab === 'routes' ? 'tab active' : 'tab'} onClick={() => setActiveTab('routes')}>Маршруты</button>
      </nav>

      {activeTab === 'collection' ? (
        <>
          <section className="workspace-grid">
            <CityForm form={form} editingId={editingId} onChange={updateForm} onSubmit={saveCity} onCancel={cancelEdit} />
            <div className="side-stack">
              <LookupCard lookupId={lookupId} setLookupId={setLookupId} lookupCity={lookupCity} onSubmit={findCity} onEdit={startEdit} />
              <SpecialOperations
                meters={metersForDelete}
                setMeters={setMetersForDelete}
                date={dateForDelete}
                setDate={setDateForDelete}
                average={average}
                onDeleteMeters={() => runSpecialAction(() => deleteByMeters(metersForDelete), 'Города с заданной высотой удалены')}
                onDeleteDate={() => runSpecialAction(() => deleteByEstablishmentDate(dateForDelete), 'Город с заданной датой основания удалён')}
                onAverage={loadAverage}
              />
            </div>
          </section>

          <section className="panel collection-panel">
            <div className="section-heading">
              <div><p className="eyebrow">GET /cities</p><h2>Коллекция городов</h2></div>
              <button className="button secondary" onClick={loadCities} disabled={loading}>{loading ? 'Загрузка…' : 'Обновить'}</button>
            </div>
            <FilterPanel filters={filters} onChange={updateFilter} onSubmit={submitFilters} onReset={resetFilters} />
            <CityTable cities={cities} sort={sort} onSort={toggleSort} onEdit={startEdit} onDelete={removeCity} />
            <Pagination page={page} size={size} totalPages={totalPages} onPage={setPage} onSize={(value) => { setSize(Number(value)); setPage(1); }} count={totalHint} />
          </section>
        </>
      ) : (
        <RoutesPanel results={routeResults} onCalculate={loadRoute} />
      )}
    </main>
  );
}

function Feedback({ error, notice, onClose }) {
  return <div className={error ? 'feedback error' : 'feedback success'}>
    <div><strong>{error ? 'Ошибка запроса' : 'Готово'}</strong><p>{error?.title || notice}</p>{error?.details?.length > 0 && <ul>{error.details.map((detail) => <li key={detail}>{detail}</li>)}</ul>}</div>
    <button aria-label="Закрыть уведомление" onClick={onClose}>×</button>
  </div>;
}

function CityForm({ form, editingId, onChange, onSubmit, onCancel }) {
  return <form className="panel city-form" onSubmit={onSubmit}>
    <div className="section-heading"><div><p className="eyebrow">{editingId === null ? 'POST /cities' : `PUT /cities/${editingId}`}</p><h2>{editingId === null ? 'Добавить город' : `Редактировать город #${editingId}`}</h2></div>{editingId !== null && <button type="button" className="icon-button" onClick={onCancel}>×</button>}</div>
    <div className="form-grid">
      <label>Название<input required minLength="1" value={form.name} onChange={(e) => onChange('name', e.target.value)} /></label>
      <label>Площадь<input required type="number" min="0.000001" step="any" value={form.area} onChange={(e) => onChange('area', e.target.value)} /></label>
      <label>Население<input required type="number" min="1" step="1" value={form.population} onChange={(e) => onChange('population', e.target.value)} /></label>
      <label>Высота над уровнем моря<input type="number" step="any" value={form.metersAboveSeaLevel} onChange={(e) => onChange('metersAboveSeaLevel', e.target.value)} /></label>
      <label>Координата X<input required type="number" step="1" value={form.coordinatesX} onChange={(e) => onChange('coordinatesX', e.target.value)} /></label>
      <label>Координата Y<input required type="number" max="952" step="any" value={form.coordinatesY} onChange={(e) => onChange('coordinatesY', e.target.value)} /></label>
      <label>Дата основания<input type="text" inputMode="numeric" placeholder={DATE_TIME_PLACEHOLDER} pattern="[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}" value={form.establishmentDate} onChange={(e) => onChange('establishmentDate', e.target.value)} /></label>
      <label>День рождения губернатора<input type="text" inputMode="numeric" placeholder={DATE_TIME_PLACEHOLDER} pattern="[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}" value={form.governorBirthday} onChange={(e) => onChange('governorBirthday', e.target.value)} /></label>
      <label>Климат<select value={form.climate} onChange={(e) => onChange('climate', e.target.value)}><option value="">Не указан</option><option>RAIN_FOREST</option><option>TROPICAL_SAVANNA</option><option>HUMIDCONTINENTAL</option></select></label>
      <label className="checkbox-label"><input type="checkbox" checked={form.capital} onChange={(e) => onChange('capital', e.target.checked)} /> Столица</label>
    </div>
    <button className="button primary" type="submit">{editingId === null ? 'Создать город' : 'Сохранить изменения'}</button>
  </form>;
}

function LookupCard({ lookupId, setLookupId, lookupCity, onSubmit, onEdit }) {
  return <section className="panel compact-card"><div className="section-heading"><div><p className="eyebrow">GET /cities/{'{id}'}</p><h2>Найти город</h2></div></div><form className="inline-form" onSubmit={onSubmit}><input required min="1" type="number" placeholder="ID города" value={lookupId} onChange={(e) => setLookupId(e.target.value)} /><button className="button secondary">Найти</button></form>{lookupCity && <div className="lookup-result"><strong>#{lookupCity.id} · {lookupCity.name}</strong><span>{formatNumber(lookupCity.area)} км² · население {formatNumber(lookupCity.population, 0)}</span><button className="text-button" onClick={() => onEdit(lookupCity)}>Открыть в форме</button></div>}</section>;
}

function SpecialOperations({ meters, setMeters, date, setDate, average, onDeleteMeters, onDeleteDate, onAverage }) {
  return <section className="panel compact-card"><div className="section-heading"><div><p className="eyebrow">Специальные операции</p><h2>Операции коллекции</h2></div></div><div className="operation"><label>Удалить по высоте<input type="number" step="any" value={meters} onChange={(e) => setMeters(e.target.value)} placeholder="metersAboveSeaLevel" /></label><button className="button danger" disabled={!meters} onClick={onDeleteMeters}>Удалить все</button></div><div className="operation"><label>Удалить по дате основания<input type="text" inputMode="numeric" placeholder={DATE_TIME_PLACEHOLDER} pattern="[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}" value={date} onChange={(e) => setDate(e.target.value)} /></label><button className="button danger" disabled={!date} onClick={onDeleteDate}>Удалить один</button></div><div className="average-row"><button className="button secondary" onClick={onAverage}>Рассчитать среднее</button><strong>{average === null ? '—' : `${formatNumber(average)} м`}</strong></div></section>;
}

function FilterPanel({ filters, onChange, onSubmit, onReset }) {
  return <form className="filters" onSubmit={onSubmit}><div className="filter-grid"><Filter label="ID" value={filters.id} onChange={(v) => onChange('id', v)} /><Filter label="Название (с начала)" value={filters.name} onChange={(v) => onChange('name', v)} placeholder="Например, Моск" /><Filter label="Создана" value={filters.creationDate} onChange={(v) => onChange('creationDate', v)} placeholder={DATE_TIME_PLACEHOLDER} /><Filter label="Площадь" value={filters.area} onChange={(v) => onChange('area', v)} /><Filter label="Население" value={filters.population} onChange={(v) => onChange('population', v)} /><Filter label="Высота" value={filters.metersAboveSeaLevel} onChange={(v) => onChange('metersAboveSeaLevel', v)} /><Filter label="Основан" value={filters.establishmentDate} onChange={(v) => onChange('establishmentDate', v)} placeholder={DATE_TIME_PLACEHOLDER} /><Filter label="Столица" type="select" value={filters.capital} onChange={(v) => onChange('capital', v)} options={[['', 'Все'], ['true', 'Да'], ['false', 'Нет']]} /><Filter label="Климат" type="select" value={filters.climate} onChange={(v) => onChange('climate', v)} options={[['', 'Все'], ['RAIN_FOREST', 'RAIN_FOREST'], ['TROPICAL_SAVANNA', 'TROPICAL_SAVANNA'], ['HUMIDCONTINENTAL', 'HUMIDCONTINENTAL']]} /><Filter label="Координата X" value={filters.coordinatesX} onChange={(v) => onChange('coordinatesX', v)} /><Filter label="Координата Y" value={filters.coordinatesY} onChange={(v) => onChange('coordinatesY', v)} /><Filter label="Губернатор" value={filters.governorBirthday} onChange={(v) => onChange('governorBirthday', v)} placeholder={DATE_TIME_PLACEHOLDER} /></div><div className="filter-actions"><button className="button primary">Применить фильтры</button><button type="button" className="button ghost" onClick={onReset}>Сбросить</button></div></form>;
}

function Filter({ label, value, onChange, type = 'text', options, placeholder }) {
  return <label className="filter-field">{label}{type === 'select' ? <select value={value} onChange={(e) => onChange(e.target.value)}>{options.map(([key, text]) => <option key={key} value={key}>{text}</option>)}</select> : <input type={type} value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} />}</label>;
}

function CityTable({ cities, sort, onSort, onEdit, onDelete }) {
  const sortMark = (field) => { const criterion = sort.find((item) => item.startsWith(`${field},`)); return criterion ? criterion.endsWith(',asc') ? ' ↑' : ' ↓' : ''; };
  return <div className="table-wrap"><table><thead><tr><th>Действия</th>{SORT_FIELDS.map(([field, label]) => <th key={field}><button className="sort-button" onClick={() => onSort(field)}>{label}{sortMark(field)}</button></th>)}</tr></thead><tbody>{cities.length === 0 ? <tr><td colSpan="13" className="empty-state">Нет объектов на текущей странице</td></tr> : cities.map((city) => <tr key={city.id}><td className="actions"><button className="text-button" onClick={() => onEdit(city)}>Изменить</button><button className="text-button danger-text" onClick={() => onDelete(city.id)}>Удалить</button></td><td>{city.id}</td><td>{city.name}</td><td>{formatNumber(city.area)}</td><td>{formatNumber(city.population, 0)}</td><td>{formatNumber(city.metersAboveSeaLevel)}</td><td>{formatDateTime(city.establishmentDate)}</td><td>{city.capital ? 'Да' : 'Нет'}</td><td>{city.climate || '—'}</td><td>{city.coordinates?.x ?? '—'}</td><td>{city.coordinates?.y ?? '—'}</td><td>{formatDateTime(city.governor?.birthday)}</td><td>{formatDateTime(city.creationDate)}</td></tr>)}</tbody></table></div>;
}

function Pagination({ page, size, totalPages, onPage, onSize, count }) {
  return <div className="pagination"><span>На странице: {count}</span><label>Размер<select value={size} onChange={(e) => onSize(e.target.value)}>{PAGE_SIZE_OPTIONS.map((option) => <option key={option}>{option}</option>)}</select></label><button className="button ghost" disabled={page <= 1} onClick={() => onPage(page - 1)}>← Назад</button><strong>Страница {page}</strong><button className="button ghost" disabled={page >= totalPages} onClick={() => onPage(page + 1)}>Вперёд →</button></div>;
}

function RoutesPanel({ results, onCalculate }) {
  const cards = [
    ['to-largest', 'До самого большого города', 'Город с максимальной площадью'],
    ['to-oldest', 'До самого старого города', 'От точки (0, 0, 0) до минимальной даты основания']
  ];
  return <section className="routes-view"><div className="section-heading"><div><p className="eyebrow">GET /route/calculate</p><h2>Расчёт маршрутов</h2></div><p className="muted">Route service получает коллекцию через City service по HTTP.</p></div><div className="route-grid">{cards.map(([kind, title, description]) => <article className="route-card" key={kind}><div className="route-icon">⌁</div><h3>{title}</h3><p>{description}</p><button className="button primary" onClick={() => onCalculate(kind)}>Рассчитать маршрут</button>{results[kind] && <div className="route-result"><span>Длина маршрута</span><strong>{formatNumber(results[kind].length, 4)}</strong><small>Город #{results[kind].cityId}</small></div>}{results[kind] === null && <div className="empty-route">Подходящих городов нет (204)</div>}</article>)}</div></section>;
}

export default App;
