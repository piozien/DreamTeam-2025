CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE global_role_enum AS ENUM ('client', 'admin');
CREATE TYPE project_user_role_enum AS ENUM ('pm', 'member', 'viewer');
CREATE TYPE task_priority_enum AS ENUM ('critical', 'important', 'optional');
CREATE TYPE task_status_enum AS ENUM ('to do', 'in progress', 'finished');

CREATE TABLE Users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),
    global_role global_role_enum NOT NULL
);

CREATE TABLE Projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    startDate DATE NOT NULL,
    endDate DATE NOT NULL CHECK (endDate >= startDate)
);

CREATE TABLE Project_Members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role project_user_role_enum NOT NULL,
    FOREIGN KEY (project_id) REFERENCES Projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Task (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    startDate DATE NOT NULL,
    endDate DATE NOT NULL CHECK (endDate >= startDate),
    priority task_priority_enum NOT NULL,
    status task_status_enum NOT NULL,
    FOREIGN KEY (project_id) REFERENCES Projects(id) ON DELETE CASCADE
);

CREATE TABLE Task_Assignees (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    FOREIGN KEY (task_id) REFERENCES Task(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Task_Comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    comment TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES Task(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Task_History (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL,
    changed_by UUID NOT NULL,
    old_status task_status_enum NOT NULL,
    new_status task_status_enum NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES Task(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Task_Files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES Task(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Task_Dependencies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL,
    depends_on_task_id UUID NOT NULL,
    FOREIGN KEY (task_id) REFERENCES Task(id) ON DELETE CASCADE,
    FOREIGN KEY (depends_on_task_id) REFERENCES Task(id) ON DELETE CASCADE
);

CREATE TABLE Notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);
