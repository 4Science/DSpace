/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao.impl;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.Query;
import org.dspace.AbstractUnitTest;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.CoreHelpers;
import org.dspace.core.HibernateDBConnection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author mwood
 */
public class ChecksumHistoryDAOImplTest
    extends AbstractUnitTest {
    public ChecksumHistoryDAOImplTest() {
    }

    @BeforeClass
    public static void setUpClass()
        throws SQLException, ClassNotFoundException {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown()
        throws SQLException {
    }

    /**
     * Test of deleteByDateAndCode method, of class ChecksumHistoryDAOImpl.
     */
    @Test
    public void testDeleteByDateAndCode()
        throws Exception {
        System.out.println("deleteByDateAndCode");

        GregorianCalendar cal = new GregorianCalendar();
        Date retentionDate = cal.getTime();
        ChecksumResultCode resultCode = ChecksumResultCode.CHECKSUM_MATCH;

        // Create two older rows
        HibernateDBConnection dbc = (HibernateDBConnection) CoreHelpers.getDBConnection(context);
        Query qry = dbc.getSession().createNativeQuery(
            "INSERT INTO checksum_history"
                + "(check_id, process_end_date, result, bitstream_id)"
                + " VALUES (:id, :date, :result, :bitstream)");
        int checkId = 0;

        // Row with matching result code
        BitstreamService bss = ContentServiceFactory.getInstance().getBitstreamService();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream bs = bss.create(context, is);
        context.turnOffAuthorisationSystem();
        bss.update(context, bs);
        context.restoreAuthSystemState();
        context.flush();

        cal.add(Calendar.DATE, -1);
        Date matchDate = cal.getTime();
        checkId++;
        qry.setParameter("id", checkId);
        qry.setParameter("date", matchDate);
        qry.setParameter("result", ChecksumResultCode.CHECKSUM_MATCH.name());
        qry.setParameter("bitstream", bs.getID()); // FIXME identifier not being set???
        qry.executeUpdate();

        // Row with nonmatching result code
        cal.add(Calendar.DATE, -1);
        Date noMatchDate = cal.getTime();
        checkId++;
        qry.setParameter("id", checkId);
        qry.setParameter("date", noMatchDate);
        qry.setParameter("result", ChecksumResultCode.CHECKSUM_NO_MATCH.name());
        qry.setParameter("bitstream", bs.getID()); // FIXME identifier not being set???
        qry.executeUpdate();

        // Create one newer row
        cal.add(Calendar.DATE, +3);
        Date futureDate = cal.getTime();
        checkId++;
        qry.setParameter("id", checkId);
        qry.setParameter("date", new java.sql.Date(futureDate.getTime()));
        qry.setParameter("result", ChecksumResultCode.CHECKSUM_MATCH.name());
        qry.setParameter("bitstream", bs.getID()); // FIXME identifier not being set???
        qry.executeUpdate();

        // Test!
        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();
        int expResult = 1;
        int result = instance.deleteByDateAndCode(context, retentionDate,
                                                  resultCode);
        assertEquals(expResult, result);

        // See if matching old row is gone.
        qry = dbc.getSession().createQuery(
            "SELECT COUNT(*) FROM ChecksumHistory WHERE processEndDate = :date");
        long count;

        qry.setParameter("date", matchDate);
        count = (Long) qry.getSingleResult();
        assertEquals("Should find no row at matchDate", 0, count);

        // See if nonmatching old row is still present.
        qry.setParameter("date", noMatchDate);
        count = (Long) qry.getSingleResult();
        assertEquals("Should find one row at noMatchDate", 1, count);

        // See if new row is still present.
        qry.setParameter("date", futureDate);
        count = (Long) qry.getSingleResult();
        assertEquals("Should find one row at futureDate", 1, count);
    }

    /**
     * Test of deleteByBitstream method, of class ChecksumHistoryDAOImpl.
     */
/*
    @Test
    public void testDeleteByBitstream()
            throws Exception
    {
        System.out.println("deleteByBitstream");
        Bitstream bitstream = null;
        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();
        instance.deleteByBitstream(context, bitstream);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
