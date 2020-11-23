--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
  BEGIN
    INSERT
    INTO cris_metrics
      (
        id,
        metriccount,
        enddate,
        remark,
        resourceid,
        resourcetypeid,
        startdate,
        timestampcreated,
        timestamplastmodified,
        metrictype,
        uuid
      )
    SELECT CRIS_METRICS_SEQ.nextval,
      pmc.numcitations AS COUNT,
      CURRENT_TIMESTAMP,
      NULL,
      pmc1.element AS itemId,
      2,
      NULL,
      CURRENT_TIMESTAMP,
      NULL,
      'pubmed'  AS pubmed,
      hh.handle AS handle
    FROM cris_pmc_citation pmc
    JOIN cris_pmc_citation_itemids pmc1
    ON pmc.pubmedid = pmc1.cris_pmc_citation_pubmedid
    JOIN handle hh
    ON pmc1.cris_pmc_citation_pubmedid = hh.resource_id
    WHERE hh.resource_type_id = 2;
  EXCEPTION
  WHEN OTHERS THEN
    NULL;
  END;
  
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE cris_pmc_citation DROP COLUMN numcitations';
  EXCEPTION
  WHEN OTHERS THEN
    NULL;
  END;
END;