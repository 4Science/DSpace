/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif;

import org.dspace.content.Bitstream;


/**
 * Simple Mock of the IIIF API Query Service to avoid the HTTP call
 *
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 */
public class MockIIIFApiQueryServiceImpl implements IIIFApiQueryService {

    @Override
    public int[] getImageDimensions(Bitstream bitstream) {
        return null;
    }

}
