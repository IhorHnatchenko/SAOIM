-- SAOIM Stage 11 manual load-test data for MySQL 8+
-- IMPORTANT: set the account ID before running.
SET @account_id = 1;
SET @root_name = '[TEST11] Нагрузочный тест';

DROP PROCEDURE IF EXISTS saoim_stage11_seed;
DELIMITER //
CREATE PROCEDURE saoim_stage11_seed(IN p_account_id INT, IN p_root_name VARCHAR(80))
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE root_id BIGINT;
    DECLARE parent_id BIGINT;
    DECLARE created_category_id BIGINT;

    INSERT INTO app_categories(
        account_id, parent_category_id, name, icon_key, root_pinned, sort_order
    ) VALUES (
        p_account_id, NULL, p_root_name, '🧪', FALSE, 900000
    );
    SET root_id = LAST_INSERT_ID();
    SET parent_id = root_id;

    -- A chain 100 levels deep.
    WHILE i <= 100 DO
        INSERT INTO app_categories(
            account_id, parent_category_id, name, icon_key, root_pinned, sort_order
        ) VALUES (
            p_account_id, parent_id, CONCAT('[TEST11] Глубина ', i), '◇', FALSE, i
        );
        SET parent_id = LAST_INSERT_ID();
        SET i = i + 1;
    END WHILE;

    -- 200 sibling categories, each with five URL shortcuts (1,000 shortcuts total).
    SET i = 1;
    WHILE i <= 200 DO
        INSERT INTO app_categories(
            account_id, parent_category_id, name, icon_key, root_pinned, sort_order
        ) VALUES (
            p_account_id, root_id, CONCAT('[TEST11] Категория ', i), '◇', FALSE, 1000 + i
        );
        SET created_category_id = LAST_INSERT_ID();

        INSERT INTO app_shortcuts(
            account_id, category_id, display_name, launch_type, target, sort_order, enabled
        ) VALUES
            (p_account_id, created_category_id, CONCAT('[TEST11] URL ', i, '.1'), 'URL', CONCAT('https://example.com/', i, '/1'), 1, TRUE),
            (p_account_id, created_category_id, CONCAT('[TEST11] URL ', i, '.2'), 'URL', CONCAT('https://example.com/', i, '/2'), 2, TRUE),
            (p_account_id, created_category_id, CONCAT('[TEST11] URL ', i, '.3'), 'URL', CONCAT('https://example.com/', i, '/3'), 3, TRUE),
            (p_account_id, created_category_id, CONCAT('[TEST11] URL ', i, '.4'), 'URL', CONCAT('https://example.com/', i, '/4'), 4, TRUE),
            (p_account_id, created_category_id, CONCAT('[TEST11] URL ', i, '.5'), 'URL', CONCAT('https://example.com/', i, '/5'), 5, TRUE);

        SET i = i + 1;
    END WHILE;
END//
DELIMITER ;

CALL saoim_stage11_seed(@account_id, @root_name);
DROP PROCEDURE saoim_stage11_seed;

-- Cleanup after testing (physical removal is intentional for test-only data):
-- DELETE FROM app_categories
-- WHERE account_id = @account_id
--   AND parent_category_id IS NULL
--   AND name = @root_name;
-- Child categories and shortcuts are removed by ON DELETE CASCADE.
