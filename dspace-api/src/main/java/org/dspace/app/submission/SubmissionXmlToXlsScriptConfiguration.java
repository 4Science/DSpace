/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.submission;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration for {@link SubmissionXmlToXls}.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 *
 * @param  <T> the {@link SubmissionXmlToXls} type
 */
public class SubmissionXmlToXlsScriptConfiguration<T extends SubmissionXmlToXls> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {

        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            super.options = options;
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Generic setter for the dspaceRunnableClass
     *
     * @param dspaceRunnableClass The dspaceRunnableClass to be set on this
     *                            BulkImportScriptConfiguration
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}
