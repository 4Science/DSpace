/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.repository.RegistrationRestRepository.TYPE_QUERY_PARAM;
import static org.dspace.app.rest.repository.RegistrationRestRepository.TYPE_REGISTER;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.RegistrationDataService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EPersonGroupRestControllerIT extends AbstractControllerIntegrationTest {
    @Autowired
    private RegistrationDataService registrationDataService;
    @Test
    public void postGroupsForEpersonUsingRegistrationDataMultipleGroups() throws Exception {
        context.turnOffAuthorisationSystem();
        String newRegisterEmail = "tim.donohue@my.edu";
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        Group firstGroup = GroupBuilder.createGroup(context).withName("firstGroup").build();
        Group secondGroup = GroupBuilder.createGroup(context).withName("secondGroup").build();
        List<UUID> groupList = new ArrayList<>();
        groupList.add(firstGroup.getID());
        groupList.add(secondGroup.getID());
        registrationRest.setGroups(groupList);
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/eperson/registrations")
                        .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(registrationRest)))
                .andExpect(status().isCreated());
        EPerson alreadyRegisteredPerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Tim", "Donohue")
                .withEmail(newRegisterEmail)
                .withPassword(password)
                .build();
        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();
        String token = getAuthToken(alreadyRegisteredPerson.getEmail(), password);
        try {
            getClient(token).perform(post("/api/eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")
                            .param("token", newRegisterToken)
                            .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uuid", is((alreadyRegisteredPerson.getID().toString()))))
                    .andExpect(jsonPath("$.type", is("eperson")))
                    .andExpect(jsonPath("$._links.groups.href",
                            Matchers.endsWith("eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")))
                    .andExpect(jsonPath("$._links.self.href",
                            Matchers.endsWith("eperson/epersons/" + alreadyRegisteredPerson.getID())));
            assertNull(registrationDataService.findByToken(context, newRegisterToken));
            getClient(token).perform(get("/api/eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.number", is(0)))
                    .andExpect(jsonPath("$.page.totalElements", is(2)))
                    .andExpect(jsonPath("$._embedded.groups", hasSize(2)))
                    .andExpect(jsonPath("$._embedded.groups[0].name", is(firstGroup.getName())))
                    .andExpect(jsonPath("$._embedded.groups[1].name", is(secondGroup.getName())));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
            EPersonBuilder.deleteEPerson(alreadyRegisteredPerson.getID());
        }

    }
    @Test
    public void postGroupsForEpersonUsingRegistrationDataOneGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        String newRegisterEmail = "tim.donohue@my.edu";
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        Group firstGroup = GroupBuilder.createGroup(context).withName("firstGroup").build();
        List<UUID> groupList = new ArrayList<>();
        groupList.add(firstGroup.getID());
        registrationRest.setGroups(groupList);
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/eperson/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                        .content(mapper.writeValueAsBytes(registrationRest)))
                .andExpect(status().isCreated());
        EPerson alreadyRegisteredPerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Tim", "Donohue")
                .withEmail(newRegisterEmail)
                .withPassword(password)
                .build();
        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();
        String token = getAuthToken(alreadyRegisteredPerson.getEmail(), password);
        try {
            getClient(token).perform(post("/api/eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")
                            .param("token", newRegisterToken)
                            .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uuid", is((alreadyRegisteredPerson.getID().toString()))))
                    .andExpect(jsonPath("$.type", is("eperson")))
                    .andExpect(jsonPath("$._links.groups.href",
                            Matchers.endsWith("eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")))
                    .andExpect(jsonPath("$._links.self.href",
                            Matchers.endsWith("eperson/epersons/" + alreadyRegisteredPerson.getID())));
            assertNull(registrationDataService.findByToken(context, newRegisterToken));
            getClient(token).perform(get("/api/eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.number", is(0)))
                    .andExpect(jsonPath("$.page.totalElements", is(1)))
                    .andExpect(jsonPath("$._embedded.groups", hasSize(1)))
                    .andExpect(jsonPath("$._embedded.groups[0].name", is(firstGroup.getName())));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
            EPersonBuilder.deleteEPerson(alreadyRegisteredPerson.getID());
        }

    }
    @Test
    public void postGroupsForEpersonDifferentFromLoggedInUsingRegistration() throws Exception {
        context.turnOffAuthorisationSystem();
        String newRegisterEmail = "tim.donohue@my.edu";
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        Group firstGroup = GroupBuilder.createGroup(context).withName("firstGroup").build();
        List<UUID> groupList = new ArrayList<>();
        groupList.add(firstGroup.getID());
        registrationRest.setGroups(groupList);
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/eperson/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                        .content(mapper.writeValueAsBytes(registrationRest)))
                .andExpect(status().isCreated());
        EPerson alreadyRegisteredPerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Tim", "Donohue")
                .withEmail(newRegisterEmail)
                .withPassword(password)
                .build();
        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();
        String token = getAuthToken(eperson.getEmail(), password);
        try {
            getClient(token).perform(post("/api/eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")
                            .param("token", newRegisterToken)
                            .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
            assertNotNull(registrationDataService.findByToken(context, newRegisterToken));
            token = getAuthToken(alreadyRegisteredPerson.getEmail(), password);
            getClient(token).perform(get("/api/eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.number", is(0)))
                    .andExpect(jsonPath("$.page.totalElements", is(0)))
                    .andExpect(jsonPath("$._embedded.groups", hasSize(0)));
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
            EPersonBuilder.deleteEPerson(alreadyRegisteredPerson.getID());
        }

    }
    @Test
    public void postGroupsForEpersonWithFakeTokenRegistration() throws Exception {
        String newRegisterEmail = "tim.donohue@my.edu";
        context.turnOffAuthorisationSystem();
        EPerson alreadyRegisteredPerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Tim", "Donohue")
                .withEmail(newRegisterEmail)
                .withPassword(password)
                .build();
        String token = getAuthToken(admin.getEmail(), password);
        context.restoreAuthSystemState();
        try {
            getClient(token).perform(post("/api/eperson/epersons/" + alreadyRegisteredPerson.getID() + "/groups")
                            .param("token", "justToBeTested")
                            .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        } finally {
            context.turnOffAuthorisationSystem();
            EPersonBuilder.deleteEPerson(alreadyRegisteredPerson.getID());
            context.restoreAuthSystemState();
        }
    }
    @Test
    public void postGroupsForEpersonUnregistered() throws Exception {
        context.turnOffAuthorisationSystem();
        String newRegisterEmail = "tim.donohue@my.edu";
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setEmail(newRegisterEmail);
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/eperson/registrations")
                        .param(TYPE_QUERY_PARAM, TYPE_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(registrationRest)))
                .andExpect(status().isCreated());
        EPerson alreadyRegisteredPerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Tim", "Donohue")
                .withEmail(newRegisterEmail)
                .withPassword(password)
                .build();
        String newRegisterToken = registrationDataService.findByEmail(context, newRegisterEmail).getToken();
        String token = getAuthToken(eperson.getEmail(), password);
        try {
            getClient(token).perform(post("/api/eperson/epersons/" + "J784578h9D" + "/groups")
                            .param("token", newRegisterToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        } finally {
            context.turnOffAuthorisationSystem();
            registrationDataService.deleteByToken(context, newRegisterToken);
            context.restoreAuthSystemState();
            EPersonBuilder.deleteEPerson(alreadyRegisteredPerson.getID());
        }

    }
}
