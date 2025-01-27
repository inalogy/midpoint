/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.csv;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The test of Provisioning service on the API level. The test is using CSV resource.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestCsvGuid extends AbstractCsvTest {

    private static final File RESOURCE_CSV_GUID_FILE = new File(TEST_DIR, "resource-csv-guid.xml");
    private static final String RESOURCE_CSV_GUID_OID = "b39e0b10-2449-11e7-b0c1-73f52bb2a496";

    private static final File ACCOUNT_JACK_FILE = new File(TEST_DIR, "account-jack-guid.xml");
    private static final String ACCOUNT_JACK_OID = "2db718b6-243a-11e7-a9e5-bbb2545f80ed";
    private static final String ACCOUNT_JACK_GUID = "007";
    private static final String ACCOUNT_JACK_UNAME = "jack";

    private static final File CSV_SOURCE_FILE = new File(TEST_DIR, "midpoint-guid.csv");

    protected static final String ATTR_GUID = "guid";
    protected static final QName ATTR_GUID_QNAME = new QName(RESOURCE_NS, ATTR_GUID);

    protected static final String ATTR_UNAME = "uname";
    protected static final QName ATTR_UNAME_QNAME = new QName(RESOURCE_NS, ATTR_UNAME);

    @Override
    protected File getResourceFile() {
        return RESOURCE_CSV_GUID_FILE;
    }

    @Override
    protected String getResourceOid() {
        return RESOURCE_CSV_GUID_OID;
    }

    @Override
    protected File getSourceCsvFile() {
        return CSV_SOURCE_FILE;
    }

    @Override
    protected File getAccountJackFile() {
        return ACCOUNT_JACK_FILE;
    }

    @Override
    protected String getAccountJackOid() {
        return ACCOUNT_JACK_OID;
    }

    @Override
    protected void assertAccountDefinition(ResourceObjectClassDefinition accountDef) {

        assertEquals("Unexpected number of definitions", 5, accountDef.getDefinitions().size());

        ShadowSimpleAttributeDefinition<?> guidDef = accountDef.findSimpleAttributeDefinition(ATTR_GUID);
        assertNotNull("No definition for guid", guidDef);
        assertEquals(1, guidDef.getMaxOccurs());
        assertEquals(1, guidDef.getMinOccurs());
        assertTrue("No guid create", guidDef.canAdd());
        assertTrue("No guid update", guidDef.canModify());
        assertTrue("No guid read", guidDef.canRead());

        ShadowSimpleAttributeDefinition<?> unameDef = accountDef.findSimpleAttributeDefinition(ATTR_UNAME);
        assertNotNull("No definition for uname", unameDef);
        assertEquals(1, unameDef.getMaxOccurs());
        assertEquals(0, unameDef.getMinOccurs()); // TODO: should be 1
        assertTrue("No uname create", unameDef.canAdd());
        assertTrue("No uname update", unameDef.canModify());
        assertTrue("No uname read", unameDef.canRead());

        assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
    }

    @Override
    protected void assertAccountJackAttributes(ShadowType shadowType) {
        assertEquals("Wrong guid", ACCOUNT_JACK_GUID, getAttributeValue(shadowType, ATTR_GUID_QNAME));
        assertEquals("Wrong uname", ACCOUNT_JACK_UNAME, getAttributeValue(shadowType, ATTR_UNAME_QNAME));
        assertEquals("Wrong firstname", ACCOUNT_JACK_FIRSTNAME, getAttributeValue(shadowType, ATTR_FIRSTNAME_QNAME));
        assertEquals("Wrong lastname", ACCOUNT_JACK_LASTNAME, getAttributeValue(shadowType, ATTR_LASTNAME_QNAME));
    }

    @Override
    protected void assertAccountJackAttributesRepo(ShadowType repoShadowType) {
        assertEquals("Wrong guid (repo)", ACCOUNT_JACK_GUID, getAttributeValue(repoShadowType, ATTR_GUID_QNAME));
        assertEquals("Wrong uname (repo)", ACCOUNT_JACK_UNAME, getAttributeValue(repoShadowType, ATTR_UNAME_QNAME));
    }

}
