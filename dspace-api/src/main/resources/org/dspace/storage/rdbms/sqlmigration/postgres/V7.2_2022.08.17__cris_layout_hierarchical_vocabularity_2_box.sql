--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Hierarchical Vocabularity 2 Box
-----------------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS cris_layout_box2hierarchicalvocabulary_id_seq;
CREATE TABLE IF NOT EXISTS cris_layout_box2hierarchicalvocabulary (
    vocabulary character varying(255) NOT NULL,
    cris_layout_box_id integer NOT NULL,
    id integer NOT NULL,
    metadata_field_id integer NOT NULL,
    CONSTRAINT cris_layout_box2hierarchicalvocabulary_pkey PRIMARY KEY (id),
    CONSTRAINT cris_layout_box2hierarchicalvocabulary_box_id_fkey FOREIGN KEY (cris_layout_box_id)
    REFERENCES cris_layout_box (id)
);