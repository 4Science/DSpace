/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.Date;
import java.util.UUID;

import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;

/**
 * Dto class to expose the section unpaywall api call during in progress submission.
 */
public class DataUnpaywall implements SectionData {

    private Integer id;

    private String doi;

    private UUID itemId;

    private UnpaywallStatus status;

    private String jsonRecord;

    private Date timestampCreated;

    private Date timestampLastModified;

    public DataUnpaywall() {
    }

    public DataUnpaywall(Unpaywall unpaywall) {
        id = unpaywall.getID();
        doi = unpaywall.getDoi();
        status = unpaywall.getStatus();
        itemId = unpaywall.getItemId();
        jsonRecord = unpaywall.getJsonRecord();
        timestampCreated = unpaywall.getTimestampCreated();
        timestampLastModified = unpaywall.getTimestampLastModified();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public UnpaywallStatus getStatus() {
        return status;
    }

    public void setStatus(UnpaywallStatus status) {
        this.status = status;
    }

    public String getJsonRecord() {
        return jsonRecord;
    }

    public void setJsonRecord(String jsonRecord) {
        this.jsonRecord = jsonRecord;
    }

    public Date getTimestampCreated() {
        return timestampCreated;
    }

    public void setTimestampCreated(Date timestampCreated) {
        this.timestampCreated = timestampCreated;
    }

    public Date getTimestampLastModified() {
        return timestampLastModified;
    }

    public void setTimestampLastModified(Date timestampLastModified) {
        this.timestampLastModified = timestampLastModified;
    }
}
