--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Alter TABLE process
-----------------------------------------------------------------------------------

-- Drop the NOT NULL constraint on user_id column
ALTER TABLE process ALTER COLUMN user_id DROP NOT NULL;

-- Sets null the invalid user_id inside the process table
UPDATE process
set user_id = null
where not exists(
    select 1 from eperson where eperson.uuid = user_id
);

-- Add the foreign key constraint with ON DELETE SET NULL
ALTER TABLE process ADD CONSTRAINT user_id FOREIGN KEY (user_id) REFERENCES eperson (uuid) ON DELETE SET NULL;