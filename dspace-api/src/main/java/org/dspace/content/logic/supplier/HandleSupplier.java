/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.supplier;

import java.util.Collection;
import java.util.Optional;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Functional interface that can be used to mark an implementation
 * as an {@code HandleSupplier} (i.e. a function that retrieves some handles given {@link Context} and {@link Item}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@FunctionalInterface
public interface HandleSupplier {

    Optional<? extends Collection<String>> getHandles(Context context, Item item);

}
