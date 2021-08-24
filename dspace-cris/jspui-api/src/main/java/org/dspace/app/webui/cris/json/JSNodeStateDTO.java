package org.dspace.app.webui.cris.json;

/**
 * Class to represent a state for a single node 
 * 
 * @author Luigi Andrea Pascarelli
 *
 */
public class JSNodeStateDTO
{

    private boolean opened;

    private boolean disabled;

    private boolean selected;

    public boolean isOpened()
    {
        return opened;
    }

    public void setOpened(boolean opened)
    {
        this.opened = opened;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

}
