package org.dspace.app.webui.cris.json;

import java.util.List;
import java.util.Map;

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
public class JSNodeDTO
{
    private String id;
    private String parent;
    private String text;
    private String icon;
    private JSNodeStateDTO state;
    private String li_attr;
    private Map<String,String> a_attr;
    private List<JSNodeChildrenDTO> children;
    
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    public String getParent()
    {
        return parent;
    }
    public void setParent(String parent)
    {
        this.parent = parent;
    }
    public String getText()
    {
        return text;
    }
    public void setText(String text)
    {
        this.text = text;
    }
    public String getIcon()
    {
        return icon;
    }
    public void setIcon(String icon)
    {
        this.icon = icon;
    }
    public JSNodeStateDTO getState()
    {
        return state;
    }
    public void setState(JSNodeStateDTO state)
    {
        this.state = state;
    }
    public String getLi_attr()
    {
        return li_attr;
    }
    public void setLi_attr(String li_attr)
    {
        this.li_attr = li_attr;
    }
    public Map<String,String> getA_attr()
    {
        return a_attr;
    }
    public void setA_attr(Map<String,String> a_attr)
    {
        this.a_attr = a_attr;
    }
    public List<JSNodeChildrenDTO> getChildren()
    {
        return children;
    }
    public void setChildren(List<JSNodeChildrenDTO> children)
    {
        this.children = children;
    }
}
