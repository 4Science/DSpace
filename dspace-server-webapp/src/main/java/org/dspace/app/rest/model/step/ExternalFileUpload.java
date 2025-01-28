/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class ExternalFileUpload implements SectionData {

    Set<String> sources = new HashSet<>();

    public void addSource(String source) {
        sources.add(source);
    }

}
