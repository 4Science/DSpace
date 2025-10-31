/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.authority.zdb.ZDBAuthorityValue;
import org.dspace.content.authority.zdb.ZDBService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ZDBAuthorityTestConfig {

    @Bean(name = "ZDBSource")
    @Primary
    public ZDBService zdbService() {
        ZDBService mock = Mockito.mock(ZDBService.class);
        try {
            Mockito.when(mock.list("Acta AND Mathematica AND informatica", 0, 10)).thenReturn(createMockResults());
        } catch (IOException e) {
            // Should not happen for mock
        }
        return mock;
    }

    private List<ZDBAuthorityValue> createMockResults() {
        List<ZDBAuthorityValue> results = new ArrayList<>();
        // Create the first entry
        ZDBAuthorityValue zdb1 = new ZDBAuthorityValue();
        zdb1.setServiceId("1447228-4");
        zdb1.setValue("Acta mathematica et informatica");
        zdb1.addOtherMetadata("journalZDBID", "1447228-4");
        zdb1.addOtherMetadata("journalTitle", "Acta mathematica et informatica");

        // Create the second entry
        ZDBAuthorityValue zdb2 = new ZDBAuthorityValue();
        zdb2.setServiceId("1194912-0");
        zdb2.setValue("Acta mathematica Universitatis Ostraviensis");
        zdb2.addOtherMetadata("journalZDBID", "1194912-0");
        zdb2.addOtherMetadata("journalTitle", "Acta mathematica Universitatis Ostraviensis");
        zdb2.addOtherMetadata("journalIssn", "1211-4774");

        // Create the third entry
        ZDBAuthorityValue zdb3 = new ZDBAuthorityValue();
        zdb3.setServiceId("2618143-5");
        zdb3.setValue("Acta mathematica Universitatis Ostraviensis");
        zdb3.addOtherMetadata("journalZDBID", "2618143-5");
        zdb3.addOtherMetadata("journalTitle", "Acta mathematica Universitatis Ostraviensis");

        results.add(zdb1);
        results.add(zdb2);
        results.add(zdb3);
        return results;
    }
}
