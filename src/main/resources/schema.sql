-- Schema for Notes Service (works for H2 and PostgreSQL with minor tweaks)

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS team_members (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    UNIQUE(team_id, user_id)
);

CREATE TABLE IF NOT EXISTS notes (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id),
    team_id UUID REFERENCES teams(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_notes_owner ON notes(owner_id);
CREATE INDEX IF NOT EXISTS idx_notes_team ON notes(team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user ON team_members(user_id);

-- Minimal users for FK constraints in tests (and demo)
-- Carol is also pre-created so the seeder doesn't need to insert users (avoids PK conflicts with insert after schema init)
MERGE INTO users (id, name, email, created_at) KEY (id) VALUES
 ('11111111-1111-1111-1111-111111111111', 'Alice', 'alice@example.com', CURRENT_TIMESTAMP),
 ('22222222-2222-2222-2222-222222222222', 'Bob', 'bob@example.com', CURRENT_TIMESTAMP),
 ('33333333-3333-3333-3333-333333333333', 'Carol', 'carol@example.com', CURRENT_TIMESTAMP);
