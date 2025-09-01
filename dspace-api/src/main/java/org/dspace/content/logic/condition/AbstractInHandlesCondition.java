/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.logic.supplier.HandleSupplier;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an {@link AbstractCondition} that checks if a given {@link Collection} of {@code handles}, contains at least
 * one handle provided by the {@link AbstractInHandlesCondition#handleSupplier}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class AbstractInHandlesCondition implements Condition {

    public static Boolean isInHandles(
        Context context, Item item, HandleSupplier handleSupplier, Collection<String> handles
    ) {
        return handleSupplier.getHandles(context, item)
                             .flatMap(col ->
                                          col.stream()
                                             .map(handle -> handles.contains(handle))
                                             .filter(Boolean::booleanValue)
                                             .findFirst()
                             )
                             .orElse(false);
    }

    @Autowired
    protected ItemService itemService;

    protected Map<String, Object> parameters = new HashMap<>();
    protected HandleSupplier handleSupplier;

    private AbstractInHandlesCondition() {
    }

    public AbstractInHandlesCondition(HandleSupplier handleSupplier) {
        this.handleSupplier = handleSupplier;
    }

    public AbstractInHandlesCondition(
        HandleSupplier handleSupplier,
        Map<String, Object> parameters
    ) {
        this(handleSupplier);
        this.parameters = parameters;
    }

    /**
     * Abstract method that retrieves the handles for the condition.
     *
     * @return Collection of handles in String format
     * @param <T>
     */
    protected abstract <T extends Collection<String>> T getHandles();

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        return isInHandles(context, item, handleSupplier, getHandles());
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }
}
