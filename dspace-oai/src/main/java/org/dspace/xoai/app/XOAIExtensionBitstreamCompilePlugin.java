/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

/**
 * This interface can be implemented by plugins that aim to contribute to the
 * generation of the xoai document stored in the bitstream.compile solr OAI core
 * field.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface XOAIExtensionBitstreamCompilePlugin {

    void appendElements(Context context, Element bitstreamElement, Bitstream bitstream);

}
