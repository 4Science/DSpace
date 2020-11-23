--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

DECLARE
  VCOUNT NUMBER (10);
BEGIN
	BEGIN
		SELECT COUNT(*)
		INTO VCOUNT
		FROM user_tab_cols
		WHERE lower(TABLE_NAME) = 'cris_metrics'
		AND lower(COLUMN_NAME)  = 'last';
		
		IF (VCOUNT=0) THEN
			EXECUTE IMMEDIATE 'ALTER TABLE cris_metrics ADD (last NUMBER(1) NOT NULL)';
		ELSE
			EXECUTE IMMEDIATE 'ALTER TABLE cris_metrics MODIFY (last NUMBER(1) NOT NULL)';
		END IF;
	EXCEPTION WHEN OTHERS THEN
		NULL;
	END;

	BEGIN
		update cris_metrics set last = 1 where id in (select max(id) from cris_metrics group by resourceid, resourcetypeid, metrictype);
		EXCEPTION
		WHEN OTHERS
		THEN
			NULL;
	END;

	BEGIN
		EXECUTE IMMEDIATE
			'CREATE INDEX metrics_uuid_idx ON cris_metrics (uuid ASC)';
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;

	BEGIN
		EXECUTE IMMEDIATE
			'CREATE INDEX metric_resourceuid_idx ON cris_metrics (resourceid ASC, resourcetypeid ASC)';    	
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;

	BEGIN
		EXECUTE IMMEDIATE
			'CREATE INDEX metric_bid_idx ON cris_metrics (resourceid ASC, resourcetypeid ASC, metrictype ASC)';
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;

	BEGIN
		EXECUTE IMMEDIATE
			'CREATE INDEX metrics_last_idx ON cris_metrics (last)';		 
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;
END;
