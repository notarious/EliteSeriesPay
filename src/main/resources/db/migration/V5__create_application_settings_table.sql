CREATE TABLE application_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    vk_donut_fee_percent INTEGER NOT NULL
        CHECK (vk_donut_fee_percent >= 0 AND vk_donut_fee_percent <= 100)
);

INSERT INTO application_settings (id, vk_donut_fee_percent) VALUES (1, 10);
