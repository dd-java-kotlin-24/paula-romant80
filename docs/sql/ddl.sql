-- Generated 2026-11-16 15:25:10-0700 for database version 1

CREATE TABLE IF NOT EXISTS `category`
(
    `category_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name`        TEXT                              NOT NULL,
    `color_hex`   TEXT                              NOT NULL,
    `icon_name`   TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS `index_category_name` ON `category` (`name`);

CREATE TABLE IF NOT EXISTS `voice_prompt`
(
    `prompt_id`       INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `trigger_event`   TEXT                              NOT NULL,
    `title`           TEXT                              NOT NULL,
    `transcript`      TEXT                              NOT NULL,
    `audio_file_name` TEXT                              NOT NULL,
    `is_authentic`    INTEGER                           NOT NULL,
    `last_played_at`  INTEGER
);

CREATE UNIQUE INDEX IF NOT EXISTS `index_voice_prompt_trigger_event` ON `voice_prompt` (`trigger_event`);

CREATE TABLE IF NOT EXISTS `task`
(
    `task_id`         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `category_id`     INTEGER,
    `voice_prompt_id` INTEGER,
    `title`           TEXT                              NOT NULL,
    `notes`           TEXT,
    `created_at`      INTEGER                           NOT NULL,
    `due_date`        INTEGER,
    `priority`        INTEGER                           NOT NULL,
    `is_completed`    INTEGER                           NOT NULL,
    `completed_at`    INTEGER,
    `photo_uri`       TEXT,
    `tutorial_url`    TEXT,
    FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON UPDATE NO ACTION ON DELETE SET NULL,
    FOREIGN KEY (`voice_prompt_id`) REFERENCES `voice_prompt` (`prompt_id`) ON UPDATE NO ACTION ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS `index_task_category_id` ON `task` (`category_id`);

CREATE INDEX IF NOT EXISTS `index_task_voice_prompt_id` ON `task` (`voice_prompt_id`);

CREATE INDEX IF NOT EXISTS `index_task_is_completed_due_date` ON `task` (`is_completed`, `due_date`);

CREATE TABLE IF NOT EXISTS `routine`
(
    `routine_id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `category_id`         INTEGER,
    `voice_prompt_id`     INTEGER,
    `name`                TEXT                              NOT NULL,
    `description`         TEXT,
    `frequency`           TEXT                              NOT NULL,
    `reminder_time`       TEXT,
    `current_streak`      INTEGER                           NOT NULL,
    `best_streak`         INTEGER                           NOT NULL,
    `is_active`           INTEGER                           NOT NULL,
    `created_at`          INTEGER                           NOT NULL,
    `last_completed_date` INTEGER,
    FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON UPDATE NO ACTION ON DELETE SET NULL,
    FOREIGN KEY (`voice_prompt_id`) REFERENCES `voice_prompt` (`prompt_id`) ON UPDATE NO ACTION ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS `index_routine_category_id` ON `routine` (`category_id`);

CREATE INDEX IF NOT EXISTS `index_routine_voice_prompt_id` ON `routine` (`voice_prompt_id`);

CREATE TABLE IF NOT EXISTS `routine_log`
(
    `log_id`         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `routine_id`     INTEGER                           NOT NULL,
    `completed_date` INTEGER                           NOT NULL,
    `completed_at`   INTEGER                           NOT NULL,
    `notes`          TEXT,
    `photo_uri`      TEXT,
    FOREIGN KEY (`routine_id`) REFERENCES `routine` (`routine_id`) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_log_routine_id_completed_date` ON `routine_log` (`routine_id`, `completed_date`);