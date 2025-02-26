/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * This class contains some ITs for the {@link AnnotationRestController} responsible
 * for all the HTTP calls made through {@linkplain /api/annotation}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AnnotationRestControllerIT extends AbstractControllerIntegrationTest {


    private static final String BASE_TEST_DIR = "/home/vins/dev/projects/DSpace7/dspace-test/dspace/assetstore/annotation/";

    @Test
    public void testCreate() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        byte[] bytes;
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            bytes = fileInputStream.readAllBytes();
        }

        getClient(token)
            .perform(
                post("/api/annotation/create")
                    .content(bytes)
                    .contentType("application/ld+json;charset=UTF-8")
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/ld+json;charset=UTF-8"))
            .andExpect(
                jsonPath("$",
                    allOf(
                        hasJsonPath("$.['@id']", notNullValue()),
                        hasJsonPath("$.['@type']", is("oa:Annotation")),
                        hasJsonPath("$.['@context']", is("http://iiif.io/api/presentation/2/context.json")),
                        hasJsonPath("$.['motivation']", hasItem("oa:commenting"))
                    )
                )
            )
            .andExpect(
                jsonPath(
                    "$.on",
                    hasItem(
                        allOf(
                            hasJsonPath("$.['@type']", is("oa:SpecificResource")),
                            hasJsonPath("$.['@full']", is("http://localhost:8080/server/iiif/af5b8b9a-3883-4764-965c-248f1f1f1546/canvas/3c9e76fd-0ef7-4df7-af7a-7356220e2451")),
                            hasJsonPath("$.['selector']",
                                allOf(
                                    hasJsonPath("$.['@type']", is("oa:Choice"))
                                )
                            )
                        )
                    )
                )
            )
            .andExpect(
                jsonPath(
                    "$.resource",
                    hasItem(
                        allOf(
                            hasJsonPath("$.['@type']", is("dctypes:Text")),
                            hasJsonPath("$.['@chars']", is("<p>Test</p>")),
                            hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("Test"))
                        )
                    )
                )
            )
        ;
    }


    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_TEST_DIR, name));
    }

}