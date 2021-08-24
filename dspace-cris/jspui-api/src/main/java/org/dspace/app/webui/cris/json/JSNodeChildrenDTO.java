package org.dspace.app.webui.cris.json;

import java.util.List;

//Alternative format of the node (id & parent are required)
//{
//    id          : "string" // required
//    parent      : "string" // required
//    text        : "string" // node text
//    icon        : "string" // string for custom
//    state       : {
//      opened    : boolean  // is the node open
//      disabled  : boolean  // is the node disabled
//      selected  : boolean  // is the node selected
//    },
//    li_attr     : {}  // attributes for the generated LI node
//    a_attr      : {}  // attributes for the generated A node
//  }
/**
 * 
 * Class to provide the tree structure to fill in the JSON
 * 
 * @author Luigi Andrea Pascarelli
 *
 */
public class JSNodeChildrenDTO
{
    private String id;    
    private String text;
    private String icon;
    private boolean children;
    
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    public String getText()
    {
        return text;
    }
    public void setText(String text)
    {
        this.text = text;
    }
    public boolean isChildren()
    {
        return children;
    }
    public void setChildren(boolean children)
    {
        this.children = children;
    }
    public String getIcon()
    {
        return icon;
    }
    public void setIcon(String icon)
    {
        this.icon = icon;
    }
}
