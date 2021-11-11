package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.integration.CRISAuthority;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;

public class ItemCrisRefNestedDisplayStrategy extends ItemCrisRefDisplayStrategy {

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit,
			boolean viewFull, String browseType, int colIdx, int itemId,
			String field, Metadatum[] metadataArray, boolean disableCrossLinks,
			boolean emph) throws JspException {
    	String publicPath = null;
    	int minConfidence = -1;
		if (metadataArray.length > 0) {
			ChoiceAuthorityManager cam;
            try
            {
                cam = ChoiceAuthorityManager.getManager(UIUtil.obtainContext(hrq));
            }
            catch (SQLException e)
            {
                throw new JspException(e);
            }
			ChoiceAuthority ca = cam.getChoiceAuthority(metadataArray[0].schema, metadataArray[0].element, metadataArray[0].qualifier);
			minConfidence = MetadataAuthorityManager.getManager().getMinConfidence(metadataArray[0].schema, metadataArray[0].element, metadataArray[0].qualifier);
			if (ca != null && ca instanceof CRISAuthority) {
				CRISAuthority crisAuthority = (CRISAuthority) ca;
				publicPath = crisAuthority.getPublicPath();
				if (publicPath == null) {
					publicPath = ConfigurationManager.getProperty("ItemCrisRefDisplayStrategy.publicpath."+field);
					if (publicPath == null) {
						publicPath = metadataArray[0].qualifier;
					}
				}
			}
		}
		
		if (publicPath == null) {
			return "";
		}
		
        String metadata="";
        // limit the number of records if this is the author field (if
        // -1, then the limit is the full list)
        boolean truncated = false;
        int loopLimit = metadataArray.length;
        if (limit != -1)
        {
            loopLimit = (limit > metadataArray.length ? metadataArray.length
                    : limit);
            truncated = (limit < metadataArray.length);
        }

		HashMap<String,Metadatum[]> dc2metadatum = new HashMap<String,Metadatum[]>();
		String nestedList = ConfigurationManager.getProperty("itemcrisrefnested."+field);
		String[] netstedMetadatas = StringUtils.stripAll(StringUtils.split(nestedList,","));
		try {
			if(StringUtils.isNotBlank(nestedList)) {
				Item i  = Item.find(UIUtil.obtainContext(hrq), itemId);
				for(String nested : netstedMetadatas) {
					String[] md = Utils.tokenize(nested);
					Metadatum[] meta = i.getMetadataWithoutPlaceholder(md[0], md[1], md[2], Item.ANY);
					dc2metadatum.put(nested, meta);
				}
			}
        
	        StringBuffer sb = new StringBuffer();
	        for (int j = 0; j < loopLimit; j++)
	        {
	            if (metadataArray != null && metadataArray.length > 0)
	            {
	                buildBrowseLink(hrq, viewFull, browseType, metadataArray[j].value, metadataArray[j].authority, metadataArray[j].language, metadataArray[j].confidence,
	                        minConfidence, disableCrossLinks, sb);
	                if (StringUtils.isNotBlank(metadataArray[j].authority)
	                        && metadataArray[j].confidence >= minConfidence)
	                {
	                    buildAuthority(hrq, metadataArray[j].value, metadataArray[j].authority, publicPath, sb);
	                }
	    			if(StringUtils.isNotBlank(nestedList)) {
	    				
	    				for(String nested: netstedMetadatas) {
	    					Metadatum[] n = dc2metadatum.get(nested);
	    					if(j < n.length) {
	    						String nvalue = StringUtils.equals(n[j].value,  MetadataValue.PARENT_PLACEHOLDER_VALUE)? "":" ("+n[j].value+")";
	    						sb.append(nvalue);
	    					}
	    				}
	    			}
	                if (j < (loopLimit - 1))
	                {
	                    if (colIdx != -1) // we are showing metadata in a table row
	                                      // (browse or item list)
	                    {
	                        sb.append("; ");
	                    }
	                    else
	                    {
	                        // we are in the item tag
	                        sb.append("<br />");
	                    }
	                }
	            }
	            else
	            {
	                break;
	            }
	        }
	        if (truncated)
	        {
	            Locale locale = UIUtil.getSessionLocale(hrq);
	        	String etal = I18nUtil.getMessage("itemlist.et-al", locale);
	            sb.append(", " + etal);
	        }
	
	        if (colIdx != -1) // we are showing metadata in a table row (browse or
	                          // item list)
	        {
	            metadata = (emph ? "<strong><em>" : "<em>") + sb.toString()
	                    + (emph ? "</em></strong>" : "</em>");
	        }
	        else
	        {
	            // we are in the item tag
	            metadata = (emph ? "<strong>" : "") + sb.toString()
	                    + (emph ? "</strong>" : "");
	        }
		}catch (SQLException e) {
			log.error(e.getMessage(), e);
		}
        return metadata;
    }
	
}
