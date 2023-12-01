/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.dto;

/**
 * Dto represents unpaywall item version.
 */
public class UnpaywallItemVersionDto {

    private String version;

    private String license;

    private String landingPageUrl;

    private String pdfUrl;

    private String hostType;

    public UnpaywallItemVersionDto() {
    }

    public UnpaywallItemVersionDto(
            String version,
            String license,
            String landingPageUrl,
            String pdfUrl,
            String hostType
    ) {
        this.version = version;
        this.license = license;
        this.landingPageUrl = landingPageUrl;
        this.pdfUrl = pdfUrl;
        this.hostType = hostType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLandingPageUrl() {
        return landingPageUrl;
    }

    public void setLandingPageUrl(String landingPageUrl) {
        this.landingPageUrl = landingPageUrl;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getHostType() {
        return hostType;
    }

    public void setHostType(String hostType) {
        this.hostType = hostType;
    }
}
