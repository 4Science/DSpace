/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.submission;

import org.apache.commons.cli.Options;

/**
 * Script configuration for {@link SubmissionXmlToXlsCLI}.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 *
 * @param  <T> the {@link SubmissionXmlToXlsCLI} type
 */
public class SubmissionXmlToXlsScriptConfigurationCLI<T extends SubmissionXmlToXlsCLI>
        extends SubmissionXmlToXlsScriptConfiguration<SubmissionXmlToXls> {


    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            super.options = options;
        }
        return options;
    }

}
