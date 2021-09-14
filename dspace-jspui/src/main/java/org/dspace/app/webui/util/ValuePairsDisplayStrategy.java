/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;

public class ValuePairsDisplayStrategy extends ASimpleDisplayStrategy
{

    private static final Logger log = Logger
            .getLogger(ValuePairsDisplayStrategy.class);

    private Map<String, DCInputsReader> dcInputsReader = new HashMap<>();

    private void init() throws DCInputsReaderException
    {
        if(dcInputsReader.isEmpty()) {
            for (Locale locale : I18nUtil.getSupportedLocales())
            {
                dcInputsReader.put(locale.getLanguage(),
                    new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
            }
        }
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, int itemid,
            String field, Metadatum[] metadataArray, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        try
        {
            init();
        }
        catch (DCInputsReaderException e)
        {
            log.error(e.getMessage(), e);
        }

        List<String> result = new ArrayList<>();
        try
        {
            Context obtainContext = UIUtil.obtainContext(hrq);
            Collection collection = Collection.find(obtainContext, colIdx);
            if (collection == null)
            {
                Item item = Item.find(obtainContext, itemid);
                collection = item.getParentObject();
            }
            // Try to find the labels for the valuepairs based on language and collection or value
            result.addAll(getLabels(colIdx, field, metadataArray,
            		obtainContext, collection));
        }
        catch (SQLException | DCInputsReaderException e)
        {
            throw new JspException(e);
        }
        
        StringBuffer sb = new StringBuffer();
        int indexSplit = 0;
    	boolean link = !disableCrossLinks && StringUtils.isNotEmpty(browseType);
        for (Metadatum mm : metadataArray)
        {
            try
            {
            	if (indexSplit > 0) {
					sb.append("<br />");
				}
            	
            	if (link)
            	{
            		sb.append("<a href=\"").append(hrq.getContextPath())
	            		.append("/browse?type=").append(browseType).append("&amp;")
	            		.append(viewFull ? "vfocus" : "value").append("=")
	            		.append(URLEncoder.encode(mm.value, "UTF-8"))
	            		.append("\">");
				}
            	
                if (result.size() >= (1 + indexSplit) && StringUtils.isNotBlank(result.get(indexSplit)))
                {
					sb.append(result.get(indexSplit));
				} else {
					sb.append(mm.value);
				}
                
                if (link) {
                	sb.append("</a>");
				}
            }
            catch (UnsupportedEncodingException e)
            {
                log.warn(e.getMessage());
            }
            indexSplit++;
        }
        return sb.toString();
        
    }

    /**
     * Try to find the labels for the specified metadata by collection and language,
     * if not found look in all valuepairs
     * 
     * @param colIdx
     * @param field
     * @param metadataArray
     * @param obtainContext
     * @param collection
     * @return
     * @throws DCInputsReaderException
     */
    private List<String> getLabels(int colIdx, String field,
            Metadatum[] metadataArray, Context obtainContext,
            Collection collection) throws DCInputsReaderException
    {
    	List<String> results = new ArrayList<String>();
    	
        String language = I18nUtil.getSupportedLocale(obtainContext.getCurrentLocale()).getLanguage();
        DCInputSet dcInputSet = dcInputsReader.get(language)
                .getInputs(collection.getHandle());
        parent:for (int i = 0; i < dcInputSet.getNumberPages(); i++)
        {
            DCInput[] dcInput = dcInputSet.getPageRows(i, false, false);
            for (DCInput myInput : dcInput)
            {
                String key = myInput.getPairsType();
                if (StringUtils.isNotBlank(key))
                {
                    String inputField = Utils.standardize(myInput.getSchema(), myInput.getElement(), myInput.getQualifier(), ".");

                    if (inputField.equals(field))
                    {
                        results.addAll(getLabel(colIdx, field, metadataArray, obtainContext, key));
                        if (!results.isEmpty())
                        {
							break parent;
						}
                    }
                }
            }
        }
        
        // workaround, a sort of fuzzy match search in all valuepairs (possible wrong result due to the same stored value in many valuepairs)
        if (results.isEmpty())
        {
            Map<String, List<String>> mappedValuePairs = dcInputsReader.get(language)
                    .getMappedValuePairs();
            
            if (mappedValuePairs != null)
            {
                for (String key : mappedValuePairs.keySet())
                {
                    if (mappedValuePairs.get(key).contains(field))
                    {
                    	results.addAll(getLabel(colIdx, field, metadataArray, obtainContext, key));
						if (!results.isEmpty())
						{
							break;
						}
					}
                }
            }
        }
        return results;
    }
    
    /**
     * Get the labels for the specified value pairs
     * 
     * @param colIdx
     * @param field
     * @param metadataArray
     * @param obtainContext
     * @param key
     * @return
     */
    private List<String> getLabel(int colIdx, String field,
    		Metadatum[] metadataArray, Context obtainContext,
    		String key)
    {
    	List<String> results = new ArrayList<String>();
    	ChoiceAuthority choice = (ChoiceAuthority) PluginManager
                .getNamedPlugin(ChoiceAuthority.class, key);
    	int nulls = 0;
    	
        for (Metadatum r : metadataArray)
        {
            Choices choices = choice.getBestMatch(obtainContext,
                    field, r.value, colIdx, obtainContext
                            .getCurrentLocale().toString());
            if (choices != null)
            {
            	StringBuilder result = new StringBuilder();
                boolean multiple = false;
                for (Choice ch : choices.values)
                {
                	if (multiple)
                	{
                		result.append(" ");
                	}else {
						multiple = true;
					}
                    result.append(ch.label);
                }
                results.add(result.toString());
            }else {
				results.add(null);
				nulls ++;
			}
        }
        
        if (nulls == metadataArray.length)
        {
			results.clear();
		}
        return results;
    }

}
