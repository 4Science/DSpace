package org.dspace.app.cris.integration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choice;

/**
 * 
 * Generic generator to work on nested/simple metadata
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public class RPExtraAuthorityMetadataGenerator
        implements RPAuthorityExtraMetadataGenerator
{

    private String relatedInputformMetadata;
    
    private String additionalInputformMetadata;
    
    private String parentInputFormMetadata;

    private String schema;

    private String element;

    private String qualifier;

    private String additionalRpMetadata;
    
    //use with aggregate mode
    private boolean singleResultOnAggregate = true;

    @Override
    public Map<String, String> build(ResearcherPage rp)
    {
        String orcid = null;
        if(StringUtils.isNotBlank(getAdditionalRpMetadata())) {
            orcid = rp.getMetadata(additionalRpMetadata);
            if(StringUtils.isBlank(orcid)) {
                orcid = "";
            }
        }
        // only single result is supported
        Map<String, String> extras = new HashMap<String, String>();
        buildSingleExtraByMetadataFromRP(rp, extras);
        if(StringUtils.isNotEmpty(getAdditionalInputformMetadata())) {
            extras.put("data-" + getAdditionalInputformMetadata(), orcid);
        }
        return extras;
    }

    @Override
    public List<Choice> buildAggregate(ResearcherPage rp)
    {
        List<Choice> choiceList = new LinkedList<Choice>();
        
		List<RPProperty> dyna = rp.getAnagrafica4view().get(additionalRpMetadata);
		
		String orcid = null;
		if(! dyna.isEmpty()) {
			orcid = dyna.get(0).toString();
		}
		
		if (StringUtils.isBlank(orcid)) 
		{
			orcid = "";
		}
		
        if (isSingleResultOnAggregate())
        {
            Map<String, String> extras = new HashMap<String, String>();
            buildSingleExtraByMetadataFromRP(rp, extras);
            if(StringUtils.isNotEmpty(getAdditionalInputformMetadata())) {
                extras.put("data-" + getAdditionalInputformMetadata(), orcid);
            }
            choiceList.add(
                    new Choice(ResearcherPageUtils.getPersistentIdentifier(rp),
                            ResearcherPageUtils.getLabel(rp.getFullName(), rp),
                            rp.getFullName(),
                            extras));
        }
        else
        {
            Metadatum[] metadatas = rp.getMetadata(getSchema(), getElement(),
                    getQualifier(), Item.ANY);
            for (Metadatum mm : metadatas)
            {
                Map<String, String> extras = new HashMap<String, String>();
                buildSingleExtraByMetadata(mm, extras);
                if(StringUtils.isNotEmpty(getAdditionalInputformMetadata())) {
                    extras.put("data-" + getAdditionalInputformMetadata(), orcid);
                }
                choiceList.add(new Choice(
                        ResearcherPageUtils.getPersistentIdentifier(rp),
                        ResearcherPageUtils.getLabel(rp.getFullName(), rp)  + "(" + mm.value + ")",
                        rp.getFullName(),
                        extras));
            }
            // manage value to empty html element
            if (metadatas == null || metadatas.length == 0)
            {
                Map<String, String> extras = new HashMap<String, String>();
                extras.put("data-" + getRelatedInputformMetadata(), "");
                if(StringUtils.isNotEmpty(getAdditionalInputformMetadata())) {
                    extras.put("data-" + getAdditionalInputformMetadata(), orcid);
                }
                choiceList.add(new Choice(
                        ResearcherPageUtils.getPersistentIdentifier(rp),
                        ResearcherPageUtils.getLabel(rp.getFullName(), rp),
                        rp.getFullName(),
                        extras));
            }

        }
        return choiceList;
    }

    protected void buildSingleExtraByMetadata(Metadatum mm,
            Map<String, String> extras)
    {
        if (mm == null)
        {
            extras.put("data-" + getRelatedInputformMetadata(), "");
        }
        else
        {
            if (StringUtils.isNotBlank(mm.authority))
            {
                extras.put("data-" + getRelatedInputformMetadata(),
                        mm.value + "::" + mm.authority);
            }
            else
            {
                extras.put("data-" + getRelatedInputformMetadata(), mm.value);
            }
        }        
    }

    protected void buildSingleExtraByMetadataFromRP(ResearcherPage rp,
            Map<String, String> extras)
    {
        Metadatum mm = rp.getMetadatumFirstValue(getSchema(), getElement(),
                getQualifier(), Item.ANY);
        buildSingleExtraByMetadata(mm, extras);
    }

    public String getRelatedInputformMetadata()
    {
        return relatedInputformMetadata;
    }

    public void setRelatedInputformMetadata(String relatedInputformMetadata)
    {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

    public String getElement()
    {
        return element;
    }

    public void setElement(String element)
    {
        this.element = element;
    }

    public String getQualifier()
    {
        return qualifier;
    }

    public void setQualifier(String qualifier)
    {
        this.qualifier = qualifier;
    }

    public String getSchema()
    {
        return schema;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    public boolean isSingleResultOnAggregate()
    {
        return singleResultOnAggregate;
    }

    public void setSingleResultOnAggregate(boolean singleResultOnAggregate)
    {
        this.singleResultOnAggregate = singleResultOnAggregate;
    }

    public String getAdditionalInputformMetadata()
    {
        return additionalInputformMetadata;
    }

    public void setAdditionalInputformMetadata(String additionalInputformMetadata)
    {
        this.additionalInputformMetadata = additionalInputformMetadata;
    }

    public String getParentInputFormMetadata()
    {
        return parentInputFormMetadata;
    }

    public void setParentInputFormMetadata(String parentMetadata)
    {
        this.parentInputFormMetadata = parentMetadata;
    }

	public String getAdditionalRpMetadata() {
		return additionalRpMetadata;
	}

	public void setAdditionalRpMetadata(String additionalRpMetadata) {
		this.additionalRpMetadata = additionalRpMetadata;
	}
}
