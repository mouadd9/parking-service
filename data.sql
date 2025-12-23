INSERT INTO parking_zones (id, name, latitude, longitude, hourly_rate)
VALUES (1, 'Centre Ville', 35.5723, -5.3621, 10.0)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO parking_zones (id, name, latitude, longitude, hourly_rate)
VALUES (2, 'Plage Martil', 35.6200, -5.2700, 15.0)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO parking_spots (id, spot_number, status, sensor_id, zone_id)
VALUES (1, 'P-101', true, 'SENSOR-01', 1)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO parking_spots (id, spot_number, status, sensor_id, zone_id)
VALUES (2, 'P-102', true, 'SENSOR-02', 1)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO parking_spots (id, spot_number, status, sensor_id, zone_id)
VALUES (3, 'P-201', true, 'SENSOR-03', 2)
    ON CONFLICT (id) DO NOTHING;