--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Authority-elision backfill (substep 3 / D23): single-store internal references.
-- Converge already-stored resolved internal references on the single-store model by
-- nulling metadatavalue.authority for exactly the rows whose authority string equals
-- their backing relationship's right_id. After this, getAuthority() reconstitutes the
-- UUID from right_id, so every reader is unaffected.
--
-- Only the exactly-redundant subset is nulled:
--   * external authority keys (ORCID/VIAF/handles/reference tokens) have no backing
--     relationship row, so they are never matched and stay in the authority column;
--   * any mismatch (authority string != right_id) is left untouched.
-- Confidence is intentionally left as-is: a minted internal reference legitimately
-- stays accepted.
-----------------------------------------------------------------------------------

UPDATE metadatavalue mv
   SET authority = NULL
 WHERE mv.authority IS NOT NULL
   AND EXISTS (
       SELECT 1
         FROM relationship r
        WHERE r.metadata_value_id = mv.metadata_value_id
          AND r.right_id::text = mv.authority
   );
