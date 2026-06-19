CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    episode_cost_rub NUMERIC NOT NULL CHECK (episode_cost_rub > 0)
);
