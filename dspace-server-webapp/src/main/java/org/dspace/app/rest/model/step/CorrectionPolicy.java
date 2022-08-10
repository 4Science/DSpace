/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.Objects;

public class CorrectionPolicy {
    private String newValue;
    private String oldValue;
    private String label;

    public CorrectionPolicy() {

    }

    public CorrectionPolicy(String newValue, String oldValue, String label) {
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.label = label;
    }


    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CorrectionPolicy policy = (CorrectionPolicy) o;
        return Objects.equals(newValue, policy.newValue) &&
            Objects.equals(oldValue, policy.oldValue) &&
            Objects.equals(label, policy.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(newValue, oldValue, label);
    }
}
