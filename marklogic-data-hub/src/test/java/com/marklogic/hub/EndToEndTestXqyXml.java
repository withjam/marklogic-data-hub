package com.marklogic.hub;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import com.marklogic.hub.plugin.PluginFormat;
import com.marklogic.hub.scaffold.Scaffolding;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.Format;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.flow.FlowType;

public class EndToEndTestXqyXml extends HubTestBase {
    private static final String ENTITY = "e2eentity";
    private static File projectDir = new File("ye-olde-project");

    @BeforeClass
    public static void setup() throws IOException {
        XMLUnit.setIgnoreWhitespace(true);

        if (projectDir.isDirectory() && projectDir.exists()) {
            FileUtils.deleteDirectory(projectDir);
        }

        installHub();

        enableDebugging();

        Scaffolding scaffolding = new Scaffolding(projectDir.toString());
        scaffolding.createEntity(ENTITY);
        scaffolding.createFlow(ENTITY, "testinput", FlowType.INPUT,
                PluginFormat.XQUERY, Format.XML);
        scaffolding.createFlow(ENTITY, "testharmonize", FlowType.HARMONIZE,
                PluginFormat.XQUERY, Format.XML);

        DataHub dh = new DataHub(getHubConfig());
        dh.clearUserModules();
        dh.installUserModules();

        installModule("/entities/" + ENTITY + "/harmonize/testharmonize/headers/headers.xqy", "e2e-test/xqy-flow/headers/headers-xml.xqy");
        installModule("/entities/" + ENTITY + "/harmonize/testharmonize/triples/triples.xqy", "e2e-test/xqy-flow/triples/triples.xqy");
    }

    @AfterClass
    public static void teardown() throws IOException {
        FileUtils.deleteDirectory(projectDir);
    }

    @Test
    public void runFlows() throws IOException, ParserConfigurationException, SAXException {
        FlowManager fm = new FlowManager(getHubConfig());
        Flow harmonizeFlow = fm.getFlow(ENTITY, "testharmonize",
                FlowType.HARMONIZE);

        stagingDocMgr.write("/input.xml", new DOMHandle(getXmlFromResource("e2e-test/staged.xml")));

        JobFinishedListener harmonizeFlowListener = new JobFinishedListener();
        fm.runFlow(harmonizeFlow, 10, 1, harmonizeFlowListener);
        harmonizeFlowListener.waitForFinish();
        assertXMLEqual(getXmlFromResource("e2e-test/final.xml"), finalDocMgr.read("/input.xml").next().getContent(new DOMHandle()).get());
    }
}
