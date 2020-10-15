/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.solr.client.solrj.beans.Field;

/**
 * This class represent the notification broker data as loaded in our solr
 * nbevent core
 *
 */
public class NBEvent {
    public static final char[] HEX_DIGITS =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    @Field("event_id")
    private String eventId;

    @Field("original_id")
    private String originalId;

    @Field("resource_uuid")
    private String target;

    @Field("title")
    private String title;

    @Field("topic")
    private String topic;

    @Field("trust")
    private double trust;

    @Field("message")
    private String message;

    @Field("last_update")
    private Date lastUpdate;

    public NBEvent(String originalId, String target, String title, String topic, double trust, String message,
            Date lastUpdate) {
        super();
        this.originalId = originalId;
        this.target = target;
        this.title = title;
        this.topic = topic;
        this.trust = trust;
        this.message = message;
        this.lastUpdate = lastUpdate;

        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            String dataToString = "originalId=" + originalId + ", title=" + title + ", topic=" + topic + ", trust="
                    + trust + ", message=" + message;
            digester.update(dataToString.getBytes("UTF-8"));
            byte[] signature = digester.digest();
            char[] arr = new char[signature.length << 1];
            for (int i = 0; i < signature.length; i++) {
                int b = signature[i];
                int idx = i << 1;
                arr[idx] = HEX_DIGITS[(b >> 4) & 0xf];
                arr[idx + 1] = HEX_DIGITS[b & 0xf];
            }
            eventId = new String(arr);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public double getTrust() {
        return trust;
    }

    public void setTrust(double trust) {
        this.trust = trust;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
