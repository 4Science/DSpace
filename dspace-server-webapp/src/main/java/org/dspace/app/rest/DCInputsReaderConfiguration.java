/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.utils.DCInputsReaderFactory;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Configuration class to create instances of {@link DCInputsReader}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Configuration
public class DCInputsReaderConfiguration {

    @Bean
    @RequestScope
    public DCInputsReader dcInputsReader() throws DCInputsReaderException {
        return new DCInputsReader();
    }

    @Bean
    public DCInputsReaderFactory dcInputsReaderFactory() {
        return new DCInputsReaderFactory();
    }

}
