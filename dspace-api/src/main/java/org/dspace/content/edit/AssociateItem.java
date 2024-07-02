/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Class representing an item in the process of associate item triggered by a user.
 * It is some wrapper if the {@AssociateItemMode} and the both items where the mode interacts.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public class AssociateItem {

    private Item sourceitem;
    private Item targetitem;
    private AssociateItemMode mode;
    private Context context;

    public AssociateItem(Context context, Item sourceitem, Item targetitem, AssociateItemMode mode) {
        this.context = context;
        this.sourceitem = sourceitem;
        this.mode = mode;
        this.targetitem = targetitem;
    }

    public AssociateItemMode getMode() {
        return mode;
    }

    public void setMode(AssociateItemMode mode) {
        this.mode = mode;
    }

    public Item getTargetitem() {
        return targetitem;
    }

    public void setTargetitem(Item targetitem) {
        this.targetitem = targetitem;
    }

    public Item getSourceitem() {
        return sourceitem;
    }

    public void setSourceitem(Item sourceitem) {
        this.sourceitem = sourceitem;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
