/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import javax.mail.Transport;

import org.dspace.AbstractUnitTest;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;


/**
 * Basic tests for {@link StatisticsGenerator}
 *
 * @author Daniele Ninfo (daniele.ninfo at 4science.com)
 */
public class StatisticsGeneratorTest extends AbstractUnitTest {

    private ConfigurationService configurationService;
    private StatisticsGenerator statisticGenerator;

    @Before
    public void setup() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        statisticGenerator = (new DSpace()).getServiceManager()
                .getServiceByName("statisticsNotifyGenerator", StatisticsGenerator.class);
        configurationService.setProperty("mail.server.disabled", "false");
    }

    @After
    public void tearDown() {
        configurationService.reloadConfig();
    }

    /**
     * Checks that the mail is actually sent
     */
    @Test
    public void testSendEmail() throws Exception {
        List<CrisMetrics> crisMetricsList = getMockedCrisMetricsList();
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            statisticGenerator.notifyForSubscriptions(context, eperson, crisMetricsList);
            mockedTransport.verify(() -> Transport.send(any()), times(1));
        }
    }

    /**
     * Checks that, by default, a xlsx file is attached to the email
     */
    @Test
    public void testSendXslx() throws Exception {
        List<CrisMetrics> crisMetricsList = getMockedCrisMetricsList();
        Email email = mock(Email.class);
        doNothing().when(email).send();
        try (MockedStatic<Email> mockedEmail = mockStatic(Email.class)) {
            when(Email.getEmail(anyString())).thenReturn(email);
            statisticGenerator.notifyForSubscriptions(context, eperson, crisMetricsList);
            verify(email).addAttachment(any(File.class), eq("subscriptions.xlsx"));
        }
    }

    /**
     * Checks that, when the subscription.statistics.legacy-excel.enabled property is
     * set to false, a xlsx file is sent
     */
    @Test
    public void testSendXslxWhenSpecified() throws Exception {
        configurationService.setProperty("subscription.statistics.legacy-excel.enabled", "false");
        List<CrisMetrics> crisMetricsList = getMockedCrisMetricsList();
        Email email = mock(Email.class);
        doNothing().when(email).send();
        try (MockedStatic<Email> mockedEmail = mockStatic(Email.class)) {
            when(Email.getEmail(anyString())).thenReturn(email);
            statisticGenerator.notifyForSubscriptions(context, eperson, crisMetricsList);
            verify(email).addAttachment(any(File.class), eq("subscriptions.xlsx"));
        }
    }

    /**
     * Checks that, when the subscription.statistics.legacy-excel.enabled property is
     * set to true, a xls file is sent
     */
    @Test
    public void testSendXsl() throws Exception {
        configurationService.setProperty("subscription.statistics.legacy-excel.enabled", "true");
        List<CrisMetrics> crisMetricsList = getMockedCrisMetricsList();
        Email email = mock(Email.class);
        doNothing().when(email).send();
        try (MockedStatic<Email> mockedEmail = mockStatic(Email.class)) {
            when(Email.getEmail(anyString())).thenReturn(email);
            statisticGenerator.notifyForSubscriptions(context, eperson, crisMetricsList);
            verify(email).addAttachment(any(File.class), eq("subscriptions.xls"));
        }
    }

    private List<CrisMetrics> getMockedCrisMetricsList() {
        DSpaceObject resource = mock(DSpaceObject.class);
        when(resource.getName()).thenReturn("Resource Name");

        CrisMetrics crisMetrics = mock(CrisMetrics.class);
        when(crisMetrics.getResource()).thenReturn(resource);
        when(crisMetrics.getMetricType()).thenReturn("Metric Type");
        when(crisMetrics.getMetricCount()).thenReturn(100d);
        when(crisMetrics.getDeltaPeriod1()).thenReturn(10d);
        when(crisMetrics.getDeltaPeriod2()).thenReturn(20d);

        return List.of(crisMetrics);
    }
}