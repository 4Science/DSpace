/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import java.util.Date;

/**
 * Class that model the value of ACCESS_CONDITION_CELL
 * of sheet BITSTREAM_METADATA of the Bulk import excel.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class AccessCondition {

    private final String name;

    private final String description;

    private final Date startDate;

    private final Date endDate;

    public AccessCondition(String name, String description, Date startDate, Date endDate) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

}
