/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dto representing Unpaywall api response.
 */
public class UnpaywallApiResponse {

    @JsonProperty("oa_locations")
    private List<OaLocation> oaLocations;

    public List<OaLocation> getOaLocations() {
        return oaLocations;
    }

    public void setOaLocations(List<OaLocation> oaLocations) {
        this.oaLocations = oaLocations;
    }

    /**
     * Dto represents unpaywall api response Oa. Location.
     */
    public static class OaLocation {

        @JsonProperty("url_for_landing_page")
        private String urlForLandingPage;

        @JsonProperty("url_for_pdf")
        private String urlToPdf;

        @JsonProperty("version")
        private String version;

        @JsonProperty("host_type")
        private String hostType;

        @JsonProperty("is_best")
        private boolean isBest;

        @JsonProperty("endpoint_id")
        private String endpointId;

        @JsonProperty("license")
        private String license;

        public String getUrlForLandingPage() {
            return urlForLandingPage;
        }

        public void setUrlForLandingPage(String urlForLandingPage) {
            this.urlForLandingPage = urlForLandingPage;
        }

        public String getUrlToPdf() {
            return urlToPdf;
        }

        public void setUrlToPdf(String urlToPdf) {
            this.urlToPdf = urlToPdf;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getHostType() {
            return hostType;
        }

        public void setHostType(String hostType) {
            this.hostType = hostType;
        }

        public boolean isBest() {
            return isBest;
        }

        public void setBest(boolean best) {
            isBest = best;
        }

        public String getEndpointId() {
            return endpointId;
        }

        public void setEndpointId(String endpointId) {
            this.endpointId = endpointId;
        }

        public String getLicense() {
            return license;
        }

        public void setLicense(String license) {
            this.license = license;
        }
    }
}