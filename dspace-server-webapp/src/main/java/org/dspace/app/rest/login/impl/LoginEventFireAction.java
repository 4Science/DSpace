/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.login.impl;

import java.sql.SQLException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.login.PostLoggedInAction;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.services.EventService;
import org.dspace.usage.UsageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Implementation of {@link PostLoggedInAction} that fire an LOGIN event.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class LoginEventFireAction implements PostLoggedInAction {

    private static final Logger log = LoggerFactory.getLogger(LoginEventFireAction.class);

    @Autowired
    private EventService eventService;

    @Override
    public void loggedIn(Context context) {

        HttpServletRequest request = getCurrentRequest();

        UUID epersonId = null;
        try (Context ctx = new Context(Context.Mode.READ_ONLY)) {
            epersonId = context.getCurrentUser().getID();
            ctx.setCurrentUser(
                EPersonServiceFactory.getInstance().getEPersonService().find(ctx, epersonId));

            eventService.fireEvent(new UsageEvent(UsageEvent.Action.LOGIN, request, ctx, ctx.getCurrentUser()));

            ctx.abort();
        } catch (SQLException e) {
            log.error("Error firing login event for user: {}", epersonId, e);
        }

    }

    private HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

}
