/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.scopus.dto;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.util.XMLUtils;
import org.w3c.dom.Element;

public class ScopusResponse {

    private static Logger log = Logger.getLogger(ScopusResponse.class);
    
	private boolean error = false;

	private CrisMetrics scopusCitation;

	public ScopusResponse(String response, String label) {
		this.error = true;
		this.scopusCitation = new CrisMetrics();
		scopusCitation.setMetricType(label);
		scopusCitation.setRemark(response);
		scopusCitation.setMetricCount(-1);
	}

	public ScopusResponse(Element dataRoot) {
		try {
			
			this.scopusCitation = new CrisMetrics();
			
			Element errorScopusResp = XMLUtils.getSingleElement(dataRoot, "error");
			if (dataRoot != null && errorScopusResp == null) {
				String eid = XMLUtils.getElementValue(dataRoot, "eid");
				String doi = XMLUtils.getElementValue(dataRoot, "prism:doi");
				String pmid = XMLUtils.getElementValue(dataRoot, "pubmed-id");
				String numCitations = XMLUtils.getElementValue(dataRoot, "citedby-count");
				List<Element> citedByLinkElements = XMLUtils.getElementList(dataRoot, "link");
				
				for(Element element : citedByLinkElements) {
					if(element.hasAttribute("ref")) {
						if("scopus-citedby".equals(element.getAttribute("ref"))) {
							scopusCitation.getTmpRemark().put("link", element.getAttribute("href"));
							break;
						}
					}
				}
				
				if (StringUtils.isNotBlank(eid)) {
					scopusCitation.getTmpRemark().put("identifier", eid);
				}
				if (StringUtils.isNotBlank(doi)) {
					scopusCitation.getTmpRemark().put("doi", doi);
				}
				if (StringUtils.isNotBlank(pmid)) {
					scopusCitation.getTmpRemark().put("pmid", pmid);
				}
				try {
				    scopusCitation.setMetricCount(Double.parseDouble(numCitations));
				}
				catch(NullPointerException ex) {
				    log.error("try to parse numCitations:" + numCitations);
				    throw new Exception(ex);
				}
				scopusCitation.setEndDate(new Date());
				scopusCitation.setMetricType(ConstantMetrics.STATS_INDICATOR_TYPE_SCOPUS);
	            scopusCitation.setRemark(scopusCitation.buildMetricsRemark());
				
	            
			}
			else {
				error = true;
				if (dataRoot == null) {
					log.debug("No citation entry found in Scopus");
				}
				else {
					log.debug("Error citation entry found in Scopus: " + errorScopusResp.getTextContent());
				}
			}
		} catch (Exception e) {
		    log.error(e.getMessage(), e);
			error = true;
		}
	}

	public CrisMetrics getCitation() {
		return scopusCitation;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

    public CrisMetrics getScopusCitation()
    {
        return scopusCitation;
    }

    public void setScopusCitation(CrisMetrics scopusCitation)
    {
        this.scopusCitation = scopusCitation;
    }


}
