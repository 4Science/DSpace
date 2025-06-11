/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;
import org.dspace.unpaywall.service.UnpaywallClientAPI;
import org.dspace.unpaywall.service.UnpaywallService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration tests for {@link UnpaywallServiceImpl}.
 */
public class UnpaywallServiceImplIT extends AbstractIntegrationTestWithDatabase {

    private final UnpaywallServiceImpl unpaywallService =
            (UnpaywallServiceImpl) ContentServiceFactory.getInstance().getUnpaywallService();

    private UnpaywallClientAPI origClientAPI;
    private UnpaywallClientAPI spyClientAPI;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        origClientAPI = unpaywallService.getUnpaywallClientAPI();
        spyClientAPI = spy(origClientAPI);
        unpaywallService.setUnpaywallClientAPI(spyClientAPI);
    }

    @After
    public void after() throws SQLException, AuthorizeException {
        List<Unpaywall> unpaywallRecords = unpaywallService.findAll(context);
        for (Unpaywall unpaywall : unpaywallRecords) {
            unpaywallService.delete(context, unpaywall);
        }
        unpaywallService.setUnpaywallClientAPI(origClientAPI);
    }

    @Test
    public void testFindUnpaywall() {
        UUID item1Id = UUID.randomUUID();
        String doi1 = "testDoi1";
        Unpaywall unpaywall1 = new Unpaywall();
        unpaywall1.setItemId(item1Id);
        unpaywall1.setDoi(doi1);
        unpaywall1 = unpaywallService.create(context, unpaywall1);

        Unpaywall unpaywall2 = new Unpaywall();
        unpaywall2.setItemId(UUID.randomUUID());
        unpaywall2.setDoi("testDoi2");
        unpaywallService.create(context, unpaywall2);

        Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi1, item1Id);

        assertTrue(unpaywall.isPresent());
        Assert.assertEquals(unpaywall1.getID(), unpaywall.get().getID());
    }

    @Test
    public void testFindAll() {
        Unpaywall unpaywall1 = new Unpaywall();
        unpaywall1.setItemId(UUID.randomUUID());
        unpaywall1.setDoi("testDoi1");
        unpaywall1 = unpaywallService.create(context, unpaywall1);

        Unpaywall unpaywall2 = new Unpaywall();
        unpaywall2.setItemId(UUID.randomUUID());
        unpaywall2.setDoi("testDoi2");
        unpaywallService.create(context, unpaywall2);

        List<Unpaywall> unpaywallRecords = unpaywallService.findAll(context);

        Assert.assertEquals(2, unpaywallRecords.size());
        Assert.assertEquals(unpaywall1.getID(), unpaywallRecords.get(0).getID());
        Assert.assertEquals(unpaywall2.getID(), unpaywallRecords.get(1).getID());
    }

    @Test
    public void testDelete() {
        UUID itemId = UUID.randomUUID();
        String doi = "testDoi1";
        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setItemId(itemId);
        unpaywall.setDoi(doi);
        unpaywall = unpaywallService.create(context, unpaywall);

        unpaywallService.delete(context, unpaywall);

        Optional<Unpaywall> result = unpaywallService.findUnpaywall(context, doi, itemId);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInitUnpaywallCallSuccessful()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        UUID itemId = UUID.randomUUID();
        String doi = "10.1504/ijmso.2012.048507";

        unpaywallService.initUnpaywallCall(context, doi, itemId);
        assertTrue(isRequestInitialized(unpaywallService));
        Thread.sleep(1000);

        Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi, itemId);
        assertTrue(unpaywall.isPresent());
        Assert.assertEquals(UnpaywallStatus.SUCCESSFUL, unpaywall.get().getStatus());
        Assert.assertNotNull(unpaywall.get().getJsonRecord());
    }

    @Test
    public void testInitUnpaywallCallNotFound()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        UUID itemId = UUID.randomUUID();
        String doi = "10.1504/not-found";

        unpaywallService.initUnpaywallCall(context, doi, itemId);
        assertTrue(isRequestInitialized(unpaywallService));
        Thread.sleep(1000);

        Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi, itemId);
        assertTrue(unpaywall.isPresent());
        Assert.assertEquals(UnpaywallStatus.NOT_FOUND, unpaywall.get().getStatus());
        Assert.assertNull(unpaywall.get().getJsonRecord());
    }

    @Test
    public void testInitUnpaywallCallIfNeededWithInitialization()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        UUID itemId = UUID.randomUUID();
        String doi = "10.1504/ijmso.2012.048507";

        unpaywallService.initUnpaywallCallIfNeeded(context, doi, itemId);
        assertTrue(isRequestInitialized(unpaywallService));
        Thread.sleep(1000);

        Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi, itemId);
        assertTrue(unpaywall.isPresent());
        Assert.assertEquals(UnpaywallStatus.SUCCESSFUL, unpaywall.get().getStatus());
        Assert.assertNotNull(unpaywall.get().getJsonRecord());
        verify(spyClientAPI, times(1)).callUnpaywallApi(any());
    }

    @Test
    public void testInitUnpaywallCallIfNeededWithoutInitialization()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException, SQLException {
        UUID itemId = UUID.randomUUID();
        String doi = "10.1504/ijmso.2012.048507";

        context.turnOffAuthorisationSystem();
        Unpaywall newUnpaywall = new Unpaywall();
        newUnpaywall.setDoi(doi);
        newUnpaywall.setItemId(itemId);
        newUnpaywall.setStatus(UnpaywallStatus.SUCCESSFUL);
        unpaywallService.create(context, newUnpaywall);
        context.commit();
        context.restoreAuthSystemState();

        unpaywallService.initUnpaywallCallIfNeeded(context, doi, itemId);
        assertFalse(isRequestInitialized(unpaywallService));
        Thread.sleep(1000);

        Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi, itemId);
        assertTrue(unpaywall.isPresent());
        Assert.assertEquals(UnpaywallStatus.SUCCESSFUL, unpaywall.get().getStatus());
        verify(spyClientAPI, times(0)).callUnpaywallApi(any());
    }

    private static boolean isRequestInitialized(UnpaywallService unpaywallService)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> myClass = unpaywallService.getClass();
        Field myField;
        myField = myClass.getDeclaredField("requestMap");
        myField.setAccessible(true);
        Map<String, CompletableFuture<Void>> requests =
                (Map<String, CompletableFuture<Void>>) myField.get(unpaywallService);
        myField.setAccessible(false);
        return !requests.isEmpty();
    }
}
