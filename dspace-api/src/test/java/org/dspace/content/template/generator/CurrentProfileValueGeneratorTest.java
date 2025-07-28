/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link CurrentProfileValueGenerator}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CurrentProfileValueGeneratorTest {

    @Mock
    private Context context;

    @Mock
    private ResearcherProfileService mockRPService;

    @Mock
    private Item targetItem;

    @Mock
    private Item templateItem;

    private String extraParams = "";

    private CurrentProfileValueGenerator generator = new CurrentProfileValueGenerator();

    @Test
    public void testWithoutProfileInTheContext() {
        EPerson currentUser = buildEPersonMock("25ad8d1a-e00f-4077-b2a2-326822d6aea4");
        when(context.getCurrentUser()).thenReturn(currentUser);
        generator.setResearcherProfileService(mockRPService);

        List<MetadataValueVO> metadataValueList = generator.generator(context, targetItem, templateItem, extraParams);
        MetadataValueVO metadataValue = metadataValueList.get(0);

        assertThat(metadataValueList.size(), is(1));
        assertThat(metadataValue, notNullValue());
        assertThat(metadataValue.getValue(), is(""));
        assertThat(metadataValue.getAuthority(), nullValue());
        assertThat(metadataValue.getConfidence(), is(-1));
    }

    @Test
    public void testWithProfileInTheContext() throws Exception {
        EPerson currentUser = buildEPersonMock("25ad8d1a-e00f-4077-b2a2-326822d6aea4");
        ResearcherProfile currentProfile = buildProfileMock("b3a09cdc-edd3-482a-bf85-e20ac0578df8", "profile name");
        when(context.getCurrentUser()).thenReturn(currentUser);
        when(mockRPService.findById(context, currentUser.getID())).thenReturn(currentProfile);
        generator.setResearcherProfileService(mockRPService);

        List<MetadataValueVO> metadataValueList = generator.generator(context, targetItem, templateItem, extraParams);
        MetadataValueVO metadataValue = metadataValueList.get(0);

        assertThat(metadataValueList.size(), is(1));
        assertThat(metadataValue, notNullValue());
        assertThat(metadataValue.getValue(), is("profile name"));
        assertThat(metadataValue.getAuthority(), is("b3a09cdc-edd3-482a-bf85-e20ac0578df8"));
        assertThat(metadataValue.getConfidence(), is(600));
    }

    private ResearcherProfile buildProfileMock(String uuid, String name) {
        ResearcherProfile researcherProfile = mock(ResearcherProfile.class);
        Item item = mock(Item.class);
        when(researcherProfile.getItem()).thenReturn(item);
        when(researcherProfile.getItem().getName()).thenReturn(name);
        when(researcherProfile.getItem().getID()).thenReturn(UUID.fromString(uuid));
        return researcherProfile;
    }

    private EPerson buildEPersonMock(String uuid) {
        EPerson ePerson = mock(EPerson.class);
        when(ePerson.getID()).thenReturn(UUID.fromString(uuid));
        return ePerson;
    }

}
