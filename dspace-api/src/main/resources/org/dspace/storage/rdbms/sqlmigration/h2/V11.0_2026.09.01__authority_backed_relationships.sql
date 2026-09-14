--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Authority-backed relationships: persistence substrate (additive, inert).
--   1. Make relationship.type_id nullable so a type-less relationship row may exist.
--   2. Add relationship.metadata_value_id, the owning pointer used by the
--      MetadataValue @SecondaryTable join (1:1, unique, ON DELETE CASCADE). A row
--      is written together with its metadata value and removed with it.
--   3. Default relationship.left_place / right_place to 0: Hibernate only writes the
--      mapped left_id/right_id through the secondary table, while the Relationship
--      entity maps the places as primitive int and would otherwise fail to load a row.
--   4. Give relationship.id a database default so Hibernate may omit it when the
--      row is inserted through that secondary table.
-- Nothing populates these yet; existing rows keep type_id set and metadata_value_id NULL.
-- The existing UNIQUE (left_id, type_id, right_id) constraint is intentionally left
-- unchanged: under NULL semantics a NULL type_id yields distinct rows, so the editor
-- and the author of the same person are two separate rows.
-----------------------------------------------------------------------------------

-- 1. relationship.type_id becomes nullable
ALTER TABLE relationship ALTER COLUMN type_id DROP NOT NULL;

-- 2. relationship.metadata_value_id: owning link used by the MetadataValue @SecondaryTable join
ALTER TABLE relationship ADD COLUMN IF NOT EXISTS metadata_value_id INTEGER;

ALTER TABLE relationship
    ADD CONSTRAINT relationship_metadata_value_id_fk
    FOREIGN KEY (metadata_value_id) REFERENCES metadatavalue (metadata_value_id) ON DELETE CASCADE;

CREATE UNIQUE INDEX IF NOT EXISTS relationship_metadata_value_id_uq
    ON relationship (metadata_value_id);

-- 3. relationship places default to 0 for rows written through the secondary table
ALTER TABLE relationship ALTER COLUMN left_place SET DEFAULT 0;
ALTER TABLE relationship ALTER COLUMN right_place SET DEFAULT 0;

-- 4. relationship.id default so Hibernate can omit it on the secondary-table insert path
ALTER TABLE relationship ALTER COLUMN id SET DEFAULT NEXT VALUE FOR relationship_id_seq;
