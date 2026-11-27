/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
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
import java.util.UUID;
import javax.mail.Transport;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dspace.AbstractUnitTest;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.core.Constants;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
    public void testSendEmail() {
        List<CrisMetrics> crisMetricsList = getMockedCrisMetricsList();
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            statisticGenerator.notifyForSubscriptions(context, eperson, crisMetricsList);
            mockedTransport.verify(() -> Transport.send(any()), times(1));
        }
    }

    /**
     * Checks that, by default, the attached file is a valid OOXML (.xlsx) workbook.
     */
    @Test
    public void testSendXslx() throws Exception {
        List<CrisMetrics> crisMetricsList = getMockedCrisMetricsList();
        Email email = mock(Email.class);
        doNothing().when(email).send();
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        try (MockedStatic<Email> mockedEmail = mockStatic(Email.class)) {
            when(Email.getEmail(anyString())).thenReturn(email);
            statisticGenerator.notifyForSubscriptions(context, eperson, crisMetricsList);
            verify(email).addAttachment(fileCaptor.capture(), eq("subscriptions.xlsx"));
        }
        assertGeneratedWorkbook(fileCaptor.getValue(), XSSFWorkbook.class);
    }

    /**
     * Checks that, when the subscription.statistics.legacy-excel.enabled property is
     * set to false, the attached file is a valid OOXML (.xlsx) workbook.
     */
    @Test
    public void testSendXslxWhenSpecified() throws Exception {
        configurationService.setProperty("subscription.statistics.legacy-excel.enabled", "false");
        List<CrisMetrics> crisMetricsList = getMockedCrisMetricsList();
        Email email = mock(Email.class);
        doNothing().when(email).send();
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        try (MockedStatic<Email> mockedEmail = mockStatic(Email.class)) {
            when(Email.getEmail(anyString())).thenReturn(email);
            statisticGenerator.notifyForSubscriptions(context, eperson, crisMetricsList);
            verify(email).addAttachment(fileCaptor.capture(), eq("subscriptions.xlsx"));
        }
        assertGeneratedWorkbook(fileCaptor.getValue(), XSSFWorkbook.class);
    }

    /**
     * Checks that, when the subscription.statistics.legacy-excel.enabled property is
     * set to true, the attached file is a valid legacy (.xls) workbook.
     */
    @Test
    public void testSendXsl() throws Exception {
        configurationService.setProperty("subscription.statistics.legacy-excel.enabled", "true");
        List<CrisMetrics> crisMetricsList = getMockedCrisMetricsList();
        Email email = mock(Email.class);
        doNothing().when(email).send();
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        try (MockedStatic<Email> mockedEmail = mockStatic(Email.class)) {
            when(Email.getEmail(anyString())).thenReturn(email);
            statisticGenerator.notifyForSubscriptions(context, eperson, crisMetricsList);
            verify(email).addAttachment(fileCaptor.capture(), eq("subscriptions.xls"));
        }
        assertGeneratedWorkbook(fileCaptor.getValue(), HSSFWorkbook.class);
    }

    /**
     * Asserts that the generated attachment is a non-null file that can be opened by Apache POI
     * and is an instance of the expected workbook format. This verifies that the file content
     * actually matches the declared extension (the root cause of DSC-2040).
     *
     * @param file         the generated attachment file
     * @param expectedType the expected {@link Workbook} implementation (XSSF for .xlsx, HSSF for .xls)
     * @throws Exception if the file cannot be read as a workbook
     */
    private void assertGeneratedWorkbook(File file, Class<? extends Workbook> expectedType) throws Exception {
        assertThat("An attachment file must have been generated", file, notNullValue());
        try (Workbook workbook = WorkbookFactory.create(file)) {
            assertThat(workbook, instanceOf(expectedType));
        } finally {
            file.delete();
        }
    }

    private List<CrisMetrics> getMockedCrisMetricsList() {
        CrisMetrics crisMetrics = mock(CrisMetrics.class);
        when(crisMetrics.getResource()).thenReturn(UUID.randomUUID());
        when(crisMetrics.getResourceType()).thenReturn(Constants.ITEM);
        when(crisMetrics.getMetricType()).thenReturn("Metric Type");
        when(crisMetrics.getMetricCount()).thenReturn(100d);
        when(crisMetrics.getDeltaPeriod1()).thenReturn(10d);
        when(crisMetrics.getDeltaPeriod2()).thenReturn(20d);
        return List.of(crisMetrics);
    }
}
