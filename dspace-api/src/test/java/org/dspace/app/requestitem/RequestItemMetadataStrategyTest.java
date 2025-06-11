/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test suite for RequestItemMetadataStrategy
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class RequestItemMetadataStrategyTest extends AbstractUnitTest {
    private static final String AUTHOR_ADDRESS = "john.doe@example.com";

    private ItemService itemService;
    private ConfigurationService configurationService;

    private static EPerson johnDoe;

    private Item item;
    private Item orphanItem;
    private Item notValidEmailItem;

    @BeforeClass
    public static void setUpClass() throws SQLException {
        AbstractBuilder.init();
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        johnDoe = EPersonBuilder.createEPerson(context)
                                .withEmail(AUTHOR_ADDRESS)
                                .withNameInMetadata("John", "Doe")
                                .build();
        context.restoreAuthSystemState();
        context.complete();
    }

    @AfterClass
    public static void tearDownClass() {
        AbstractBuilder.destroy();
    }

    @Before
    public void setUp() {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        this.configurationService.setProperty("mail.helpdesk", "helpdesk@test.com");
        this.configurationService.setProperty("mail.helpdesk.name", "Helpdesk Name");

        context = new Context();
        context.setCurrentUser(johnDoe);
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Misha Boychuk")
                          .withPersonEmail("misha.boychuk@test.com")
                          .build();

        orphanItem = ItemBuilder.createItem(context, collection)
                                .withTitle("Orphan Item")
                                .withPersonEmail("orphanItem@test.com")
                                .build();
        orphanItem.setSubmitter(null);

        notValidEmailItem = ItemBuilder.createItem(context, collection)
                                       .withTitle("Not Valid email Item")
                                       .withPersonEmail("test@test")
                                       .build();
        context.restoreAuthSystemState();
    }

    /**
     * Test of getRequestItemAuthor method, of class RequestItemMetadataStrategy.
     * @throws Exception passed through.
     */
    @Test
    public void testMetadataStrategy() throws Exception {
        RequestItemMetadataStrategy metadataStrategy = new RequestItemMetadataStrategy();
        metadataStrategy.setItemService(this.itemService);
        metadataStrategy.setConfigurationService(this.configurationService);
        metadataStrategy.setEmailMetadata("person.email");
        metadataStrategy.setFullNameMetadata("dc.title");

        List<RequestItemAuthor> authors = metadataStrategy.getRequestItemAuthor(context, item);
        assertEquals("misha.boychuk@test.com", authors.get(0).getEmail());
        assertEquals("Misha Boychuk", authors.get(0).getFullName());

        List<RequestItemAuthor> authorsOforphanItem = metadataStrategy.getRequestItemAuthor(context, orphanItem);
        assertEquals("orphanItem@test.com", authorsOforphanItem.get(0).getEmail());
        assertEquals("Orphan Item", authorsOforphanItem.get(0).getFullName());

        List<RequestItemAuthor> authorsOfnotValidEmailItem =
                                metadataStrategy.getRequestItemAuthor(context, notValidEmailItem);
        assertEquals(AUTHOR_ADDRESS, authorsOfnotValidEmailItem.get(0).getEmail());
        assertEquals("John Doe", authorsOfnotValidEmailItem.get(0).getFullName());
    }

    @Test
    public void testMissingMetadataStrategy() throws Exception {
        RequestItemMetadataStrategy metadataStrategy = new RequestItemMetadataStrategy();
        metadataStrategy.setItemService(this.itemService);
        metadataStrategy.setConfigurationService(this.configurationService);
        metadataStrategy.setEmailMetadata("person.notExist");
        metadataStrategy.setFullNameMetadata("dc.title");

        List<RequestItemAuthor> authors = metadataStrategy.getRequestItemAuthor(context, item);
        assertEquals(AUTHOR_ADDRESS, authors.get(0).getEmail());
        assertEquals("John Doe", authors.get(0).getFullName());

        List<RequestItemAuthor> authorsOforphanItem = metadataStrategy.getRequestItemAuthor(context, orphanItem);
        assertEquals("helpdesk@test.com", authorsOforphanItem.get(0).getEmail());
        assertEquals("Helpdesk Name", authorsOforphanItem.get(0).getFullName());

        List<RequestItemAuthor> authorsOfnotValidEmailItem =
                                metadataStrategy.getRequestItemAuthor(context, notValidEmailItem);
        assertEquals(AUTHOR_ADDRESS, authorsOfnotValidEmailItem.get(0).getEmail());
        assertEquals("John Doe", authorsOfnotValidEmailItem.get(0).getFullName());
    }

    @Test
    public void testNotConfiguredMetadataStrategy() throws Exception {
        RequestItemMetadataStrategy metadataStrategy = new RequestItemMetadataStrategy();
        metadataStrategy.setItemService(this.itemService);
        metadataStrategy.setConfigurationService(this.configurationService);

        List<RequestItemAuthor> authors = metadataStrategy.getRequestItemAuthor(context, item);
        assertEquals(AUTHOR_ADDRESS, authors.get(0).getEmail());
        assertEquals("John Doe", authors.get(0).getFullName());

        List<RequestItemAuthor> authorsOforphanItem = metadataStrategy.getRequestItemAuthor(context, orphanItem);
        assertEquals("helpdesk@test.com", authorsOforphanItem.get(0).getEmail());
        assertEquals("Helpdesk Name", authorsOforphanItem.get(0).getFullName());

        // set helpdesk to null
        this.configurationService.setProperty("mail.helpdesk", null);
        this.configurationService.setProperty("mail.admin", "adminTest@test.com");
        this.configurationService.setProperty("mail.admin.name", "Admin Name");

        List<RequestItemAuthor> authorsOforphanItem2 = metadataStrategy.getRequestItemAuthor(context, orphanItem);
        assertEquals("adminTest@test.com", authorsOforphanItem2.get(0).getEmail());
        assertEquals("Admin Name", authorsOforphanItem2.get(0).getFullName());

        List<RequestItemAuthor> authorsOfnotValidEmailItem =
                                metadataStrategy.getRequestItemAuthor(context, notValidEmailItem);
        assertEquals(AUTHOR_ADDRESS, authorsOfnotValidEmailItem.get(0).getEmail());
        assertEquals("John Doe", authorsOfnotValidEmailItem.get(0).getFullName());
    }

}
