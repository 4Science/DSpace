/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.supplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BrowseSupplier implements Supplier<List<String>> {

    private static final Logger log = LogManager.getLogger(BrowseSupplier.class);

    @Override
    public List<String> get() {
        try {
            List<String> browseIndices = Arrays.stream(BrowseIndex.getBrowseIndices())
                    .map(b -> b.getName())
                    .collect(Collectors.toList());
            List<String> result = new ArrayList<>(browseIndices);
            result.add("default");
            return result;
        } catch (BrowseException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }
}
