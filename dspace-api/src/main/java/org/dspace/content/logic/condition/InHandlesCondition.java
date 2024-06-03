/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.dspace.content.logic.supplier.HandleSupplier;

/**
 * Generic condition that checks obtained handles with {@param HandleSupplier} are
 * contained inside the provided Set of {@param handles}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class InHandlesCondition extends AbstractInHandlesCondition {

    public InHandlesCondition(HandleSupplier handleSupplier, Set<String> handles) {
        super(handleSupplier, Map.of("handles", handles));
    }

    @Override
    protected Set<String> getHandles() {
        return Optional.ofNullable(getParameters().get("handles"))
                       .map(handles -> (Set<String>)handles)
                       .orElse(Set.of());
    }
}
