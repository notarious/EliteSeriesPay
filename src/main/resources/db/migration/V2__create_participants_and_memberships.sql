CREATE TABLE participants (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vk_id TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    comment TEXT
);

CREATE TABLE project_memberships (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    participant_id INTEGER NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'LEFT')),
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (participant_id) REFERENCES participants(id),
    UNIQUE (project_id, participant_id)
);

CREATE INDEX idx_project_memberships_project_id ON project_memberships(project_id);
CREATE INDEX idx_project_memberships_participant_id ON project_memberships(participant_id);
CREATE INDEX idx_project_memberships_status ON project_memberships(status);
