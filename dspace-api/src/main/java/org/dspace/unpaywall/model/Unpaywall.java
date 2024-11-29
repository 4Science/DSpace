/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.model;

import java.util.Date;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Class representing an unpaywall api call.
 */
@Entity
@Table(name = "cris_unpaywall")
public class Unpaywall implements ReloadableEntity<Integer> {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_unpaywall_seq")
    @SequenceGenerator(name = "cris_unpaywall_seq", sequenceName = "cris_unpaywall_seq", allocationSize = 1)
    private Integer id;

    @Column
    private String doi;

    @Column(unique = true, name = "item_id")
    private UUID itemId;

    @Column
    @Enumerated(EnumType.STRING)
    private UnpaywallStatus status;

    @Column(columnDefinition = "jsonb", name = "json_record")
    private String jsonRecord;

    @CreationTimestamp
    @Column(name = "timestamp_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestampCreated;

    @UpdateTimestamp
    @Column(name = "timestamp_last_modified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestampLastModified;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Override
    public Integer getID() {
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

    public UnpaywallStatus getStatus() {
        return status;
    }

    public void setStatus(UnpaywallStatus status) {
        this.status = status;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
}
