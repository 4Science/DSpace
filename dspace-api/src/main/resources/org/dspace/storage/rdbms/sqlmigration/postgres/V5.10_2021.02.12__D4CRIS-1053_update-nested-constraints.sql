--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE cris_rp_no
  DROP CONSTRAINT cris_rp_no_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_ou_no
  DROP CONSTRAINT cris_ou_no_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_pj_no
  DROP CONSTRAINT cris_pj_no_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_do_no
  DROP CONSTRAINT cris_do_no_positiondef_typo_id_parent_id_key;

ALTER TABLE cris_rp_no
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;

ALTER TABLE cris_ou_no
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;

ALTER TABLE cris_pj_no
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;

ALTER TABLE cris_do_no
  ADD UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE;