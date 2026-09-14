/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Service that marks a metadata value as authority-backed once its authority has
 * been resolved to a related item. This is the single place where such values are
 * stamped, so that the system path (reference-token resolution) and the user path
 * (directly-picked UUID) cannot drift apart.
 * <p>
 * The stamped relationship is not a separate entity: the two sides are held on the
 * metadata value itself (see {@link MetadataValue#setLeftItem} and
 * {@link MetadataValue#setRightItem}) and are written by Hibernate into the
 * {@code relationship} secondary table together with the value's own {@code INSERT}.
 * The row therefore lives and dies with the metadata value it belongs to.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public interface AuthorityBackedRelationshipService {

    /**
     * Stamp the given owning metadata value as authority-backed towards the resolved
     * related item, by setting its relationship sides (owner item &rarr; left,
     * related item &rarr; right).
     * <p>
     * The method is idempotent: if the value already carries both sides (see
     * {@link MetadataValue#isRelationshipBacked()}), it is left untouched and
     * {@code false} is returned. This method never modifies the owning metadata
     * value's {@code value} or {@code authority}; stamping the authority is the
     * caller's concern.
     * </p>
     *
     * @param context            the DSpace context
     * @param ownerItem          the item that owns the metadata value; becomes the left side
     * @param ownerMetadataValue the owning metadata value, stamped in place
     * @param relatedItem        the resolved target item; becomes the right side
     * @return {@code true} if the value was stamped, {@code false} if it already was
     *         (or if there is nothing to stamp)
     * @throws SQLException       if a database error occurs
     * @throws AuthorizeException if the current user may not write on either item
     */
    boolean markRelationshipForResolvedAuthority(Context context, Item ownerItem,
        MetadataValue ownerMetadataValue, Item relatedItem) throws SQLException, AuthorizeException;

    /**
     * Reconcile the authority-backed relationship carried by the given metadata value
     * with the value's current authority.
     * <p>
     * The authority is the single source of truth and the relationship sides are a
     * derived projection, so a relationship row exists if and only if the authority
     * names an existing item. Depending on the outcome the method creates, rewrites or
     * removes the row:
     * </p>
     * <ul>
     *     <li>authority names an existing item, no sides present &rarr; the row is created</li>
     *     <li>authority names a different item than the current sides &rarr; both sides are overwritten</li>
     *     <li>authority names the same item as the current sides &rarr; nothing changes</li>
     *     <li>authority does not name an existing item but sides are present &rarr; both sides
     *         are cleared, which removes the row</li>
     *     <li>authority does not name an existing item and no sides are present &rarr; nothing changes</li>
     * </ul>
     * <p>
     * The check runs cheapest first: the string shape of the authority is validated before
     * anything is parsed, and the item existence check is the only database access. Archived
     * state is deliberately not part of validity: the foreign key on the relationship sides
     * only guarantees that the item exists, and a non-archived item is a legitimate target.
     * </p>
     * <p>
     * This method never modifies the owning metadata value's {@code value} or {@code authority}.
     * It only applies to item owners: for any other DSpace object it is a no-op, because the
     * left side must reference an item.
     * </p>
     *
     * @param context       the DSpace context
     * @param owner         the object that owns the metadata value; must be an item to have effect
     * @param metadataValue the owning metadata value, whose relationship sides are reconciled in place
     * @return {@code true} if the relationship sides were created or changed, {@code false} otherwise
     * @throws SQLException       if a database error occurs
     * @throws AuthorizeException if the current user may not write on the owning item
     */
    boolean reconcileRelationshipForAuthority(Context context, DSpaceObject owner, MetadataValue metadataValue)
        throws SQLException, AuthorizeException;

    /**
     * Check, cheaply and without loading any entity, whether the given authority names an
     * existing item.
     * <p>
     * Authorities that are empty, that carry a "will be generated" / "will be referenced"
     * marker, that carry the virtual authority prefix, or that are not shaped like a UUID
     * are rejected before any parsing or database access. Only authorities that survive all
     * string checks reach the existence query.
     * </p>
     *
     * @param context   the DSpace context
     * @param authority the authority key to validate
     * @return {@code true} if the authority names an existing item, {@code false} otherwise
     * @throws SQLException if a database error occurs
     */
    boolean isValidTarget(Context context, String authority) throws SQLException;

}
