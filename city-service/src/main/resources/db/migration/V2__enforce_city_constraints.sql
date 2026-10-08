ALTER TABLE cities
    ADD CONSTRAINT IF NOT EXISTS ck_cities_id_positive CHECK (id > 0);

ALTER TABLE cities
    ADD CONSTRAINT IF NOT EXISTS ck_cities_name_not_blank CHECK (TRIM(name) <> '');

ALTER TABLE cities
    ADD CONSTRAINT IF NOT EXISTS ck_cities_area_positive CHECK (area > 0);

ALTER TABLE cities
    ADD CONSTRAINT IF NOT EXISTS ck_cities_population_positive CHECK (population > 0);

ALTER TABLE cities
    ADD CONSTRAINT IF NOT EXISTS ck_cities_coordinates_y_max CHECK (coordinates_y <= 952);
