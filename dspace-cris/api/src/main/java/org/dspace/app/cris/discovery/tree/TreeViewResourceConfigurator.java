package org.dspace.app.cris.discovery.tree;

public class TreeViewResourceConfigurator
{
    private String label;
    private String labelKey;
    private String metadata;
    private boolean url;
    public String getLabel()
    {
        return label;
    }
    public void setLabel(String label)
    {
        this.label = label;
    }
    public String getMetadata()
    {
        return metadata;
    }
    public void setMetadata(String metadata)
    {
        this.metadata = metadata;
    }
    public boolean isUrl()
    {
        return url;
    }
    public void setUrl(boolean url)
    {
        this.url = url;
    }
    public String getLabelKey()
    {
        return labelKey;
    }
    public void setLabelKey(String labelKey)
    {
        this.labelKey = labelKey;
    }
}
