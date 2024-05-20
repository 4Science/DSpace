/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.deduplication.model.DuplicateDecisionObjectRest;
import org.dspace.app.deduplication.service.DedupService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface IDedupUtils {
    Collection<DuplicateInfo> findSignatureWithDuplicate(Context context, String signatureType, int resourceType,
                                                         int limit, int offset, int rule) throws SearchServiceException,
        SQLException;

    Map<String, Integer> countSignaturesWithDuplicates(String query, int resourceTypeId)
        throws SearchServiceException;

    Map<String, Integer> countSuggestedDuplicate(String query, int resourceTypeId)
        throws SearchServiceException;

    boolean matchExist(Context context, UUID itemID, UUID targetItemID, Integer resourceType,
                       String signatureType, Boolean isInWorkflow) throws SQLException, SearchServiceException;

    boolean rejectAdminDups(Context context, UUID firstId, UUID secondId, Integer type)
        throws SQLException, AuthorizeException;

    boolean rejectAdminDups(Context context, UUID itemID, String signatureType, int resourceType)
        throws SQLException, AuthorizeException, SearchServiceException;

    void rejectAdminDups(Context context, List<DSpaceObject> items, String signatureID)
        throws SQLException, AuthorizeException, SearchServiceException;

    void verify(Context context, int dedupId, UUID firstId, UUID secondId, int type, boolean toFix, String note,
                boolean check) throws SQLException, AuthorizeException;

    void setDuplicateDecision(Context context, UUID firstId, UUID secondId, Integer type,
                              DuplicateDecisionObjectRest decisionObject)
        throws AuthorizeException, SQLException, SearchServiceException;

    boolean validateDecision(DuplicateDecisionObjectRest decisionObject);

    boolean rejectDups(Context context, UUID firstId, UUID secondId, Integer type, boolean notDupl, String note,
                       boolean check) throws SQLException;

    DedupService getDedupService();

    void setDedupService(DedupService dedupService);

    void commit();

    List<DuplicateItemInfo> getDuplicateByIDandType(Context context, UUID itemID, int typeID,
                                                    boolean isInWorkflow) throws SQLException, SearchServiceException;

    List<DuplicateItemInfo> getDuplicateByIdAndTypeAndSignatureType(Context context, UUID itemID, int typeID,
                                                                    String signatureType, boolean isInWorkflow)
        throws SQLException, SearchServiceException;

    List<DuplicateItemInfo> getAdminDuplicateByIdAndType(Context context, UUID itemID, int typeID)
        throws SQLException, SearchServiceException;

    Collection<DuplicateInfo> findSuggestedDuplicate(Context context, int resourceType, int start, int rows)
        throws SearchServiceException, SQLException;
}
