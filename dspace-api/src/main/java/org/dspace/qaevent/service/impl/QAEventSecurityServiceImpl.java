/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service.impl;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.qaevent.security.QASecurity;
import org.dspace.qaevent.service.QAEventSecurityService;

public class QAEventSecurityServiceImpl implements QAEventSecurityService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(QAEventSecurityServiceImpl.class);

    private Map<String, QASecurity> qaSecurityConfiguration;

    private QASecurity defaultSecurity;

    public void setQaSecurityConfiguration(Map<String, QASecurity> qaSecurityConfiguration) {
        this.qaSecurityConfiguration = qaSecurityConfiguration;
    }

    public void setDefaultSecurity(QASecurity defaultSecurity) {
        this.defaultSecurity = defaultSecurity;
    }

    @Override
    public Optional<String> generateQAEventFilterQuery(Context context, EPerson user, String qaSource) {
        QASecurity qaSecurity = getQASecurity(qaSource);
        return qaSecurity.generateFilterQuery(context, user);
    }

    private QASecurity getQASecurity(String qaSource) {
        return qaSecurityConfiguration.getOrDefault(qaSource, defaultSecurity);
    }

    @Override
    public boolean canSeeEvent(Context context, EPerson user, QAEvent qaEvent) {
        String source = qaEvent.getSource();
        QASecurity qaSecurity = getQASecurity(source);
        return qaSecurity.canSeeQASource(context, user)
                && qaSecurity.canSeeQAEvent(context, user, qaEvent);
    }

    @Override
    public boolean canSeeSource(Context context, EPerson user, String qaSource) {
        QASecurity qaSecurity = getQASecurity(qaSource);
        return qaSecurity.canSeeQASource(context, user);
    }

}