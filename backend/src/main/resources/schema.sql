create table if not exists users (
    id integer primary key autoincrement,
    username text not null,
    display_name text not null,
    password_hash text not null,
    super_admin integer not null default 0,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    check (super_admin in (0, 1))
);

create unique index if not exists idx_users_username on users (username);

create table if not exists teams (
    id integer primary key autoincrement,
    name text not null,
    parent_id integer,
    created_by integer not null,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    foreign key (parent_id) references teams (id),
    foreign key (created_by) references users (id)
);

create table if not exists team_memberships (
    id integer primary key autoincrement,
    team_id integer not null,
    user_id integer not null,
    role text not null,
    created_at text not null default current_timestamp,
    foreign key (team_id) references teams (id),
    foreign key (user_id) references users (id),
    check (role in ('TEAM_CREATOR', 'TEAM_ADMIN', 'TEAM_MEMBER'))
);

create unique index if not exists idx_team_memberships_team_user
    on team_memberships (team_id, user_id);

create table if not exists sprints (
    id integer primary key autoincrement,
    team_id integer not null,
    name text not null,
    active integer not null default 1,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    foreign key (team_id) references teams (id),
    check (active in (0, 1))
);

create table if not exists tasks (
    id integer primary key autoincrement,
    team_id integer not null,
    title text not null,
    description text not null default '',
    remarks text not null default '',
    risks text not null default '',
    status text not null default 'TODO',
    sprint_id integer,
    assignee_id integer,
    created_by integer not null,
    deleted_at text,
    created_at text not null default current_timestamp,
    updated_at text not null default current_timestamp,
    foreign key (team_id) references teams (id),
    foreign key (sprint_id) references sprints (id),
    foreign key (assignee_id) references users (id),
    foreign key (created_by) references users (id),
    check (status in ('TODO', 'IN_PROGRESS', 'DONE'))
);

create table if not exists system_settings (
    key text primary key,
    value text not null,
    updated_at text not null default current_timestamp
);
