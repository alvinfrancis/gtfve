CREATE TABLE agencies (
agency_id TEXT PRIMARY KEY,
agency_name TEXT NOT NULL,
agency_url TEXT NOT NULL,
agency_timezone TEXT NOT NULL,
agency_lang TEXT,
agency_phone TEXT
);

CREATE TABLE routes (
agency_id TEXT NOT NULL REFERENCES agencies,
route_short_name TEXT,
route_long_name TEXT NOT NULL,
route_desc TEXT,
route_type TEXT NOT NULL,
route_url TEXT,
route_color TEXT,
route_text_color TEXT,
route_id TEXT PRIMARY KEY
);

CREATE TABLE stops (
stop_id TEXT PRIMARY KEY,
stop_code TEXT,
stop_name TEXT NOT NULL,
stop_desc TEXT,
stop_lat TEXT NOT NULL,
stop_lon TEXT NOT NULL,
zone_id TEXT,
stop_url TEXT,
location_type TEXT,
parent_station TEXT,
wheelchair_boarding TEXT
);

CREATE TABLE calendar (
service_id TEXT PRIMARY KEY,
monday BIT NOT NULL,
tuesday BIT NOT NULL,
wednesday BIT NOT NULL,
thursday BIT NOT NULL,
friday BIT NOT NULL,
saturday BIT NOT NULL,
sunday BIT NOT NULL,
start_date TEXT NOT NULL,
end_date TEXT NOT NULL
);

CREATE TABLE shapes (
shape_id TEXT NOT NULL,
shape_pt_sequence INT NOT NULL,
shape_dist_traveled NUMERIC,
shape_pt_lat TEXT NOT NULL,
shape_pt_lon TEXT NOT NULL
);

CREATE TABLE trips (
route_id TEXT NOT NULL REFERENCES routes,
service_id TEXT NOT NULL REFERENCES calendar,
trip_short_name TEXT,
trip_headsign TEXT,
direction_id BIT,
block_id TEXT,
shape_id TEXT,
trip_id TEXT PRIMARY KEY
);

CREATE TABLE frequencies (
trip_id TEXT NOT NULL REFERENCES trips,
start_time TEXT NOT NULL,
end_time TEXT NOT NULL,
headway_secs INT NOT NULL,
exact_times BIT
);

CREATE TABLE stop_times (
trip_id TEXT NOT NULL REFERENCES trips,
stop_sequence INT NOT NULL,
stop_id TEXT NOT NULL REFERENCES stops,
arrival_time TEXT NOT NULL,
departure_time TEXT NOT NULL,
stop_headsign TEXT,
pickup_type INT,
drop_off_type INT,
shape_dist_traveled NUMERIC
);

COPY agencies FROM '/Users/alvin/Desktop/gtfs/agency.txt' DELIMITER ',' CSV HEADER;
COPY routes FROM '/Users/alvin/Desktop/gtfs/routes.txt' DELIMITER ',' CSV HEADER;
COPY stops FROM '/Users/alvin/Desktop/gtfs/stops.txt' DELIMITER ',' CSV HEADER;
COPY calendar FROM '/Users/alvin/Desktop/gtfs/calendar.txt' DELIMITER ',' CSV HEADER;
COPY shapes FROM '/Users/alvin/Desktop/gtfs/shapes.txt' DELIMITER ',' CSV HEADER;
COPY trips FROM '/Users/alvin/Desktop/gtfs/trips.txt' DELIMITER ',' CSV HEADER;
COPY frequencies FROM '/Users/alvin/Desktop/gtfs/frequencies.txt' DELIMITER ',' CSV HEADER;
COPY stop_times FROM '/Users/alvin/Desktop/gtfs/stop_times.txt' DELIMITER ',' CSV HEADER;

---

-- DROP TABLE stop_times;
-- DROP TABLE frequencies;
-- DROP TABLE trips;
-- DROP TABLE shapes;
-- DROP TABLE calendar;
-- DROP TABLE stops;
-- DROP TABLE routes;
-- DROP TABLE agencies;
