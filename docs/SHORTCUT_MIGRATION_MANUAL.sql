-- SAOIM Step 7: manual app_shortcuts migration for MySQL 8+
-- Run this only if the application database user cannot CREATE/ALTER tables.

USE sao_server;

CREATE TABLE IF NOT EXISTS app_shortcuts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    category_id BIGINT NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    launch_type VARCHAR(32) NOT NULL DEFAULT 'EXECUTABLE',
    target VARCHAR(2000) NOT NULL,
    `arguments` VARCHAR(2000) NULL,
    working_directory VARCHAR(1000) NULL,
    icon_source VARCHAR(1000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deletion_batch_id CHAR(36) NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_shortcuts_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_shortcuts_category
        FOREIGN KEY (category_id)
        REFERENCES app_categories(id)
        ON DELETE CASCADE,

    INDEX ix_shortcuts_account_category
        (account_id, category_id, deleted_at, sort_order),

    INDEX ix_shortcuts_deletion_batch
        (account_id, deletion_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SHOW COLUMNS FROM app_shortcuts;
SHOW INDEX FROM app_shortcuts;
