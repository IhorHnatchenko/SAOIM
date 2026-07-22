-- Manual fallback for stage 5 if the application user cannot ALTER tables.
-- Make a database backup before running schema changes manually.

ALTER TABLE profiles ADD COLUMN nickname VARCHAR(50) NULL;
ALTER TABLE profiles ADD COLUMN title VARCHAR(50) NOT NULL DEFAULT 'Энтузиаст';
ALTER TABLE profiles ADD COLUMN level INT NOT NULL DEFAULT 1;
ALTER TABLE profiles ADD COLUMN current_xp INT NOT NULL DEFAULT 0;
ALTER TABLE profiles ADD COLUMN required_xp INT NOT NULL DEFAULT 1000;
ALTER TABLE profiles ADD COLUMN avatar_uri VARCHAR(1000) NULL;

UPDATE profiles p
JOIN accounts a ON a.id = p.account_id
SET p.nickname = a.username
WHERE p.nickname IS NULL OR TRIM(p.nickname) = '';

-- Run only after checking that profiles has no duplicate account_id values:
-- ALTER TABLE profiles ADD UNIQUE INDEX uk_profiles_account_id (account_id);
