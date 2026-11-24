CREATE TABLE IF NOT EXISTS user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    password_hash TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

CREATE TABLE IF NOT EXISTS project (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    description TEXT,
    owner_id BIGINT NOT NULL REFERENCES user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

CREATE TYPE task_status AS ENUM ('todo', 'in_progress', 'done');
CREATE TYPE priority_level AS ENUM ('low', 'medium', 'high');

CREATE TABLE IF NOT EXISTS task (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    description TEXT,
    status task_status DEFAULT 'todo',
    priority priority_level NOT NULL,
    due_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assignee_id BIGINT REFERENCES user(id),
    project_id BIGINT NOT NULL REFERENCES project(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)