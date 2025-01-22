package org.dspace.identifier.doi.action;

import static org.dspace.identifier.doi.DOIIdentifierException.DOI_DOES_NOT_EXIST;
import static org.junit.Assert.assertThrows;

import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.logic.Filter;
import org.dspace.core.Constants;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ReserveDOIActionTest extends AbstractUnitTest {

    @Mock
    DOI doi;

    @Mock
    Filter filter;

    @Mock
    DOIIdentifierProvider provider;

    @InjectMocks
    ReserveDOIAction reserveDOIAction;

    @Test
    public void testNullDSpaceObject() {
        Mockito.when(doi.getDSpaceObject()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> reserveDOIAction.apply(context, doi));
    }

    @Test
    public void testInvalidDspaceObjectType() {
        DSpaceObject dso = Mockito.mock(DSpaceObject.class);
        Mockito.when(doi.getDSpaceObject())
               .thenReturn(dso);
        Mockito.when(dso.getType()).thenReturn(Constants.BITSTREAM);
        assertThrows(IllegalArgumentException.class, () -> reserveDOIAction.apply(context, doi));

        Mockito.when(dso.getType()).thenReturn(Constants.BUNDLE);
        assertThrows(IllegalArgumentException.class, () -> reserveDOIAction.apply(context, doi));

        Mockito.when(dso.getType()).thenReturn(Constants.COLLECTION);
        assertThrows(IllegalArgumentException.class, () -> reserveDOIAction.apply(context, doi));
    }

    @Test
    public void testProviderHasBeenCalled() throws Exception {
        DSpaceObject dso = Mockito.mock(DSpaceObject.class);
        Mockito.when(doi.getDSpaceObject())
               .thenReturn(dso);
        Mockito.when(dso.getType()).thenReturn(Constants.ITEM);
        Mockito.when(doi.getDoi()).thenReturn("CUSTOM_DOI");

        reserveDOIAction.apply(context, doi);

        Mockito.verify(provider, Mockito.times(1))
               .reserveOnline(
                   Mockito.eq(context), Mockito.eq(dso), Mockito.contains("CUSTOM_DOI"), Mockito.eq(filter)
               );
    }


    @Test
    public void testProviderThrowsSQLException() throws Exception {
        DSpaceObject dso = Mockito.mock(DSpaceObject.class);
        Mockito.when(doi.getDSpaceObject())
               .thenReturn(dso);
        Mockito.when(dso.getType()).thenReturn(Constants.ITEM);

        Mockito.doThrow(SQLException.class)
               .when(provider)
               .reserveOnline(Mockito.eq(context), Mockito.eq(dso), Mockito.any(), Mockito.eq(filter));

        assertThrows(RuntimeException.class, () -> reserveDOIAction.apply(context, doi));
    }


    @Test
    public void testProviderThrowsIllegalArgumentException() throws Exception {
        DSpaceObject dso = Mockito.mock(DSpaceObject.class);
        Mockito.when(doi.getDSpaceObject())
               .thenReturn(dso);
        Mockito.when(dso.getType()).thenReturn(Constants.ITEM);

        Mockito.doThrow(IllegalArgumentException.class)
               .when(provider)
               .reserveOnline(
                   Mockito.eq(context), Mockito.eq(dso), Mockito.any(), Mockito.eq(filter)
               );

        assertThrows(IllegalStateException.class, () -> reserveDOIAction.apply(context, doi));
    }

    @Test
    public void testProviderThrowsIdentifierException() throws Exception {
        DSpaceObject dso = Mockito.mock(DSpaceObject.class);
        Mockito.when(doi.getDSpaceObject())
               .thenReturn(dso);
        Mockito.when(dso.getType()).thenReturn(Constants.ITEM);
        Mockito.when(doi.getDoi()).thenReturn("DOI");

        Mockito.doThrow(IdentifierException.class)
               .when(provider)
               .reserveOnline(
                   Mockito.eq(context), Mockito.eq(dso), Mockito.contains("DOI"), Mockito.eq(filter)
               );

        assertThrows(IdentifierException.class, () -> reserveDOIAction.apply(context, doi));
    }


    @Test
    public void testProviderHandlesDOIIdentifierException() throws Exception {
        DSpaceObject dso = Mockito.mock(DSpaceObject.class);
        Mockito.when(doi.getDSpaceObject())
               .thenReturn(dso);
        Mockito.when(dso.getType()).thenReturn(Constants.ITEM);
        Mockito.when(doi.getDoi()).thenReturn("DOESN'T EXIST!");

        DOIIdentifierException ex = new DOIIdentifierException(DOI_DOES_NOT_EXIST);

        Mockito.doThrow(ex)
               .when(provider)
               .reserveOnline(
                   Mockito.eq(context), Mockito.eq(dso), Mockito.contains("DOESN'T EXIST!"), Mockito.eq(filter)
               );

        try (
            MockedStatic<DOIActionFactory> actionMock = Mockito.mockStatic(DOIActionFactory.class)
        ) {

            DOIActionFactory factoryMock = Mockito.mock(DOIActionFactory.class);
            EmailAlertDOIAction<DSpaceObject> emailMock = Mockito.mock(EmailAlertDOIAction.class);

            actionMock.when(DOIActionFactory::instance)
                      .thenReturn(factoryMock);
            Mockito.when(factoryMock.createAlertEmailAction(Mockito.any()))
                   .thenReturn(emailMock);
            Mockito.doNothing()
                   .when(emailMock)
                   .sendAlertMail(
                       Mockito.any(),
                       Mockito.any(),
                       Mockito.anyString()
                   );

            assertThrows(DOIIdentifierException.class, () -> reserveDOIAction.apply(context, doi));

            Mockito.verify(factoryMock, Mockito.times(1)).createAlertEmailAction(Mockito.eq(dso));
            Mockito.verify(emailMock, Mockito.times(1)).sendAlertMail(
                Mockito.eq("Reserve"),
                Mockito.contains("DOESN'T EXIST!"),
                Mockito.eq(DOIIdentifierException.codeToString(DOI_DOES_NOT_EXIST))
            );
            Mockito.verifyNoMoreInteractions(factoryMock, emailMock);
        }
    }

}