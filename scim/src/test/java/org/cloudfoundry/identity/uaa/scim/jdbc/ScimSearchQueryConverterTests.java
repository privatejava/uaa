/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.scim.jdbc;

import org.cloudfoundry.identity.uaa.rest.SimpleAttributeNameMapper;
import org.cloudfoundry.identity.uaa.rest.jdbc.SearchQueryConverter.ProcessedFilter;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ScimSearchQueryConverterTests {

    private ScimSearchQueryConverter filterProcessor = new ScimSearchQueryConverter();

    @Before
    public void setUp() {
        Map<String, String> replaceWith = new HashMap<>();
        replaceWith.put("emails\\.value", "email");
        replaceWith.put("groups\\.display", "authorities");
        replaceWith.put("phoneNumbers\\.value", "phoneNumber");
        filterProcessor.setAttributeNameMapper(new SimpleAttributeNameMapper(replaceWith));
    }

    @Test
    public void canConvertValidFilters() throws Exception {
        validate(filterProcessor.convert("username pr", null, false), "username IS NOT NULL", 0);
        validate(filterProcessor.convert("username eq \"joe\"", null, false), "LOWER(username) = LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("username eq \"'bar\"", null, false), "LOWER(username) = LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("displayName eq \"openid\"", null, false), "LOWER(displayName) = LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("USERNAME eq \"joe\"", null, false), "LOWER(USERNAME) = LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("username EQ \"joe\"", null, false), "LOWER(username) = LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("username eq \"Joe\"", null, false), "LOWER(username) = LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("username eq \"Joe\"", null, false), "LOWER(username) = LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("displayName co \"write\"", null, false), "LOWER(displayName) LIKE LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("displayName sw \"scim.\"", null, false), "LOWER(displayName) LIKE LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("username gt \"joe\"", null, false), "LOWER(username) > LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("userName eq \"joe\" and meta.version eq 0", null, false),"(LOWER(userName) = LOWER(:__value_0) AND version = :__value_1)", 2);
        validate(filterProcessor.convert("meta.created gt \"1970-01-01T00:00:00.000Z\"", null, false),"created > :__value_0", 1);
        validate(filterProcessor.convert("username pr and active eq true", null, false),"(username IS NOT NULL AND active = :__value_0)", 1);
        validate(filterProcessor.convert("username pr", "username", true),"username IS NOT NULL ORDER BY username ASC", 0);
        validate(filterProcessor.convert("displayName pr", "displayName", false),"displayName IS NOT NULL ORDER BY displayName DESC", 0);
        validate(filterProcessor.convert("username pr and emails.value co \".com\"", null, false),"(username IS NOT NULL AND LOWER(email) LIKE LOWER(:__value_0))", 1);
        validate(filterProcessor.convert("username eq \"joe\" or emails.value co \".com\"", null, false),"(LOWER(username) = LOWER(:__value_0) OR LOWER(email) LIKE LOWER(:__value_1))", 2);
    }

    @Test
    public void canConvertWithReplacePatterns() {


        validate(filterProcessor.convert("emails.value sw \"joe\"", null, false), "LOWER(email) LIKE LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("groups.display co \"org.foo\"", null, false),"LOWER(authorities) LIKE LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("phoneNumbers.value sw \"+1-222\"", null, false),"LOWER(phoneNumber) LIKE LOWER(:__value_0)", 1);
        validate(filterProcessor.convert("username pr", "emails.value", true),"username IS NOT NULL ORDER BY email ASC", 0);
        

    }

    @Test
    public void testFilterWithApostrophe() throws Exception {
        validate(filterProcessor.convert("username eq \"marissa'@test.org\"", null, false),
                "LOWER(username) = LOWER(:__value_0)", 1);
    }

    private void validate(ProcessedFilter filter, String expectedSql, int expectedParamCount) {
        assertNotNull(filter);
        assertEquals(expectedSql, filter.getSql());
        assertEquals(expectedParamCount, filter.getParams().size());
    }
}
