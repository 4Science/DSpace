/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ror.service;
import java.util.List;
import java.util.Optional;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.ror.ROROrgUnitDTO;

public interface RORApiService {

    public List<ROROrgUnitDTO> getOrgUnits(String query);

    public Optional<ROROrgUnitDTO> getOrgUnit(String rorId);

    public List<MetadataValueDTO> getMetadataValues(ROROrgUnitDTO orgUnit);

    public List<MetadataValueDTO> getMetadataValues(String rorId);

    public List<String> getMetadataFields();

}
