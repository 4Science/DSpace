/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import static org.dspace.authorize.RegexPasswordValidator.REGEX_VALIDATION_PATTERN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Unit tests for {@link RegexPasswordValidator}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class RegexPasswordValidatorTest extends AbstractUnitTest {

    @Mock
    private ConfigurationService configurationService;

    private RegexPasswordValidator regexPasswordValidator;

    @Before
    public void setup() {
        // Mock the configuration service to return a specific regex pattern
        when(configurationService.getProperty(REGEX_VALIDATION_PATTERN))
            .thenReturn("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\da-zA-Z]).{8,15}$");
        // Initialize the RegexPasswordValidator with the mocked configuration service
        this.regexPasswordValidator = new RegexPasswordValidator(configurationService);
    }

    @Test
    public void testValidPassword() {
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingSpecialCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword01"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword01?"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingNumber() {
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword1!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingUppercaseCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid("testpassword01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("testPassword01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingLowercaseCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid("TESTPASSWORD01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("TESTPASSWORd01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForTooShortValue() {
        assertThat(regexPasswordValidator.isPasswordValid("Test01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("Test012!"), is(true));
    }

    @Test
    public void testInvalidPasswordForTooLongValue() {
        assertThat(regexPasswordValidator.isPasswordValid("ThisIsAVeryLongPassword01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("IsAPassword012!"), is(true));
    }

}