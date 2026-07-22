-- SAOIM step 6: manual fallback migration for MySQL 8+
-- Run this only when the application user cannot CREATE/ALTER tables.

USE sao_server;

CREATE TABLE IF NOT EXISTS app_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    parent_category_id BIGINT NULL,
    name VARCHAR(80) NOT NULL,
    icon_key VARCHAR(32) NOT NULL DEFAULT '◇',
    root_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    deletion_batch_id CHAR(36) NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_account
        FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_category_id) REFERENCES app_categories(id) ON DELETE CASCADE,
    INDEX ix_categories_account_parent
        (account_id, parent_category_id, deleted_at, sort_order),
    INDEX ix_categories_deletion_batch
        (account_id, deletion_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Three editable/removable initial categories are created only for accounts
-- that have no active categories at all.
INSERT INTO app_categories (
    account_id,
    parent_category_id,
    name,
    icon_key,
    root_pinned,
    sort_order
)
SELECT
    a.id,
    NULL,
    defaults_data.category_name,
    defaults_data.icon_key,
    TRUE,
    defaults_data.sort_order
FROM accounts a
CROSS JOIN (
    SELECT 'Работа' AS category_name, '▣' AS icon_key, 0 AS sort_order
    UNION ALL SELECT 'Медиа', '▶', 1
    UNION ALL SELECT 'Система', '⚙', 2
) defaults_data
WHERE NOT EXISTS (
    SELECT 1
    FROM app_categories existing
    WHERE existing.account_id = a.id
);

SELECT
    id,
    account_id,
    parent_category_id,
    name,
    icon_key,
    root_pinned,
    sort_order,
    deleted_at
FROM app_categories
ORDER BY account_id, parent_category_id, sort_order, id;
