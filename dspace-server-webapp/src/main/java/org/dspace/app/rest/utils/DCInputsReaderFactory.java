/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.dspace.app.util.DCInputsReader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Factory to retrieve instances of {@link DCInputsReader} from the Spring
 * context.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DCInputsReaderFactory implements ApplicationContextAware {

    private static DCInputsReader dcInputsReader;

    public static DCInputsReader getDCInputsReader() {
        return dcInputsReader;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        dcInputsReader = applicationContext.getBean(DCInputsReader.class);
    }
}
