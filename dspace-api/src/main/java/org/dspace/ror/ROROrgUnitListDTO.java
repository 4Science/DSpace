/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ror;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ROROrgUnitListDTO {
    private ROROrgUnitDTO[] items;

    @JsonProperty(value = "number_of_results")
    private int total;

    public ROROrgUnitDTO[] getItems() {
        return items;
    }

    public void setItems(ROROrgUnitDTO[] items) {
        this.items = items;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
