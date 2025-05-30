/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.mail.MessagingException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dto.RegistrationDataPatch;

/**
 * Methods for handling registration by email and forgotten passwords. When
 * someone registers as a user, or forgets their password, the
 * sendRegistrationInfo or sendForgotPasswordInfo methods can be used to send an
 * email to the user. The email contains a special token, a long string which is
 * randomly generated and thus hard to guess. When the user presents the token
 * back to the system, the AccountManager can use the token to determine the
 * identity of the eperson.
 *
 * *NEW* now ignores expiration dates so that tokens never expire
 *
 * @author Peter Breton
 * @version $Revision$
 */
public interface AccountService {

    public void sendRegistrationInfo(Context context, String email, List<UUID> groups)
        throws SQLException, IOException, MessagingException, AuthorizeException;

    public void sendForgotPasswordInfo(Context context, String email, List<UUID> groups)
        throws SQLException, IOException, MessagingException, AuthorizeException;

    boolean existsAccountFor(Context context, String token) throws SQLException, AuthorizeException;

    boolean existsAccountWithEmail(Context context, String email) throws SQLException;

    public EPerson getEPerson(Context context, String token)
        throws SQLException, AuthorizeException;


    public String getEmail(Context context, String token)
        throws SQLException;

    public void deleteToken(Context context, String token)
        throws SQLException;

    EPerson mergeRegistration(Context context, UUID userId, String token, List<String> overrides)
        throws AuthorizeException, SQLException;

    RegistrationData renewRegistrationForEmail(
        Context context, RegistrationDataPatch registrationDataPatch
    ) throws AuthorizeException;


    boolean isTokenValidForCreation(RegistrationData registrationData);
}
