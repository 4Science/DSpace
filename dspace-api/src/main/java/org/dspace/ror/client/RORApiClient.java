/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ror.client;

import java.util.List;
import java.util.Optional;

import org.dspace.ror.ROROrgUnitDTO;

public interface RORApiClient {

    List<ROROrgUnitDTO> searchOrganizations(String text);

    Optional<ROROrgUnitDTO> findOrganizationByRORId(String rorId);
}
