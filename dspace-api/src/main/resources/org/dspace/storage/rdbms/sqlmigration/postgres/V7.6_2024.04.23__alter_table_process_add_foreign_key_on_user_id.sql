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

-- Add the foreign key constraint with ON DELETE SET NULL
ALTER TABLE process ADD CONSTRAINT user_id FOREIGN KEY (user_id) REFERENCES eperson (uuid) ON DELETE SET NULL;