/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.Objects;

import org.dspace.content.DSpaceObject;
import org.dspace.eperson.EPerson;

/**
 * Class that model a key of the authorizedActionsCache map stored by
 * {@link ContextReadOnlyCache}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class AuthorizedActionKey {

    private final String dspaceObjectId;

    private final Integer action;

    private final String ePerson;

    private final Boolean inheritance;

    private AuthorizedActionKey(String dspaceObjectId, Integer action, String ePerson, Boolean inheritance) {
        this.dspaceObjectId = dspaceObjectId;
        this.action = action;
        this.ePerson = ePerson;
        this.inheritance = inheritance;
    }

    public static AuthorizedActionKey of(DSpaceObject dspaceObject, int action, EPerson eperson, Boolean inheritance) {
        String dspaceObjectId = dspaceObject == null ? "" : dspaceObject.getID().toString();
        String epersonId = eperson == null ? "" : eperson.getID().toString();
        return new AuthorizedActionKey(dspaceObjectId, action, epersonId, inheritance);
    }

    public String getDspaceObjectId() {
        return dspaceObjectId;
    }

    public Integer getAction() {
        return action;
    }

    public String getePerson() {
        return ePerson;
    }

    public Boolean getInheritance() {
        return inheritance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dspaceObjectId, ePerson, inheritance);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AuthorizedActionKey other = (AuthorizedActionKey) obj;
        return Objects.equals(action, other.action) && Objects.equals(dspaceObjectId, other.dspaceObjectId)
            && Objects.equals(ePerson, other.ePerson) && Objects.equals(inheritance, other.inheritance);
    }

}
