/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.coarnotify;

import java.util.List;

/**
 * this class represents the Configuration of Submission COAR Notify
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class COARNotifySubmissionConfiguration {

    /**
     * the map key of configured bean of COARNotifyConfigurationService
     * in coar-notify.xml
     */
    private String id;

    /**
     * the map values of configured bean of COARNotifyConfigurationService
     * in coar-notify.xml
     */
    private List<LDNPattern> patterns;

    public COARNotifySubmissionConfiguration() {

    }

    public COARNotifySubmissionConfiguration(String id, List<LDNPattern> patterns) {
        super();
        this.id = id;
        this.patterns = patterns;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the list of configured COAR Notify Patterns
     *
     * @return the list of configured COAR Notify Patterns
     */
    public List<LDNPattern> getPatterns() {
        return patterns;
    }

    /**
     * Sets the list of configured COAR Notify Patterns
     * @param patterns
     */
    public void setPatterns(final List<LDNPattern> patterns) {
        this.patterns = patterns;
    }
}