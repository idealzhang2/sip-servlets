/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.testsuite.composition;

import gov.nist.javax.sip.message.RequestExt;
import java.util.HashMap;
import java.util.Map;

import javax.sip.SipProvider;
import javax.sip.address.SipURI;
import static junit.framework.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.NetworkPortAssigner;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.startup.SipStandardContext;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class ProxyB2BUACompositionTest extends SipServletTestCase {

    private static transient Logger logger = Logger.getLogger(ProxyB2BUACompositionTest.class);

    private static final String TRANSPORT = "udp";
    private static final boolean AUTODIALOG = true;
    private static final int TIMEOUT = 10000;
//	private static final int TIMEOUT = 100000000;

    TestSipListener sender;
    TestSipListener receiver;
    ProtocolObjects senderProtocolObjects;
    ProtocolObjects receiverProtocolObjects;

    public ProxyB2BUACompositionTest(String name) {
        super(name);
        autoDeployOnStartup = false;
    }

    @Override
    public void deployApplication() {
    }

    private void deployCallForwarding(Map<String, String> params) {
        SipStandardContext ctx = deployApplication(projectHome
                + "/sip-servlets-test-suite/applications/call-forwarding-b2bua-servlet/src/main/sipapp",
                "call-forwarding-b2bua",
                params, null);
        assertTrue(ctx.getAvailable());
    }

    private void deployLocationService(Map<String, String> params) {
        SipStandardContext ctx = deployApplication(projectHome
                + "/sip-servlets-test-suite/applications/speed-dial-servlet/src/main/sipapp",
                "location-servicea",
                params, null);
        assertTrue(ctx.getAvailable());
    }

    @Override
    protected String getDarConfigurationFile() {
        return "file:///"
                + projectHome
                + "/sip-servlets-test-suite/testsuite/src/test/resources/"
                + "org/mobicents/servlet/sip/testsuite/composition/proxy-b2bua-dar.properties";
    }

    @Override
    protected void setUp() throws Exception {
        containerPort = NetworkPortAssigner.retrieveNextPort();
        super.setUp();

        senderProtocolObjects = new ProtocolObjects("sender",
                "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
        receiverProtocolObjects = new ProtocolObjects("receiver",
                "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
    }

    public void testSpeedDialLocationServiceCallerSendBye() throws Exception {
        
        int senderPort = NetworkPortAssigner.retrieveNextPort();
        sender = new TestSipListener(senderPort, containerPort, senderProtocolObjects, true);
        sender.setRecordRoutingProxyTesting(true);
        sender.sendByeInNewThread = true;
        SipProvider senderProvider = sender.createProvider();

        int receiverPort = NetworkPortAssigner.retrieveNextPort();
        receiver = new TestSipListener(receiverPort, containerPort, receiverProtocolObjects, false);
        receiver.setRecordRoutingProxyTesting(true);
        SipProvider receiverProvider = receiver.createProvider();

        receiverProvider.addSipListener(receiver);
        senderProvider.addSipListener(sender);

        senderProtocolObjects.start();
        receiverProtocolObjects.start();
        
        Map<String, String> params = new HashMap();
        params.put("servletContainerPort", String.valueOf(containerPort));
        params.put("testPort", String.valueOf(receiverPort));
        params.put("senderPort", String.valueOf(senderPort));          
        deployCallForwarding(params);
        deployLocationService(params);        

        String fromName = "sender";
        String fromHost = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromHost);

        String toUser = "b2bua";
        String toHost = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toHost);

        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
        Thread.sleep(TIMEOUT);
        assertTrue(sender.b2buamessagereceived);
        assertTrue(sender.getOkToByeReceived());
        assertTrue(receiver.getByeReceived());
    }

    public void testSpeedDialLocationServiceCallerReInviteSendBye() throws Exception {
        int senderPort = NetworkPortAssigner.retrieveNextPort();
        sender = new TestSipListener(senderPort, containerPort, senderProtocolObjects, true);
        sender.setRecordRoutingProxyTesting(true);
        sender.sendByeInNewThread = true;
        SipProvider senderProvider = sender.createProvider();

        int receiverPort = NetworkPortAssigner.retrieveNextPort();
        receiver = new TestSipListener(receiverPort, containerPort, receiverProtocolObjects, false);
        receiver.setRecordRoutingProxyTesting(true);
        SipProvider receiverProvider = receiver.createProvider();

        receiverProvider.addSipListener(receiver);
        senderProvider.addSipListener(sender);

        senderProtocolObjects.start();
        receiverProtocolObjects.start();
        
        Map<String, String> params = new HashMap();
        params.put("servletContainerPort", String.valueOf(containerPort));
        params.put("testPort", String.valueOf(receiverPort));
        params.put("senderPort", String.valueOf(senderPort));          
        deployCallForwarding(params);
        deployLocationService(params);           

        String fromName = "forward-pending-changeFromTo-sender";
        String fromHost = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromHost);

        String toUser = "b2bua";
        String toHost = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toHost);

        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
        Thread.sleep(TIMEOUT);
        assertTrue(sender.b2buamessagereceived);
        assertEquals(200, sender.getFinalResponseStatus());
        sender.setFinalResponseStatus(-1);

        receiver.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        Thread.sleep(TIMEOUT);
        assertEquals(200, receiver.getFinalResponseStatus());

        sender.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        Thread.sleep(TIMEOUT);
        assertEquals(200, sender.getFinalResponseStatus());

        sender.sendInDialogSipRequest("BYE", null, null, null, null, null);
        Thread.sleep(TIMEOUT);
        assertTrue(sender.getOkToByeReceived());
        assertTrue(receiver.getByeReceived());
    }

    /**
     * Non regression test for changing From and To Headers
     * https://telestax.zendesk.com/tickets/31838 Test both at the Proxy and
     * B2BUA side
     *
     * @throws Exception
     */
    public void testSpeedDialLocationServiceCallerReInviteChangeToFromHeadersSendBye() throws Exception {
        int senderPort = NetworkPortAssigner.retrieveNextPort();
        sender = new TestSipListener(senderPort, containerPort, senderProtocolObjects, false);
        sender.setRecordRoutingProxyTesting(true);
        sender.sendByeInNewThread = true;
        SipProvider senderProvider = sender.createProvider();

        int receiverPort = NetworkPortAssigner.retrieveNextPort();
        receiver = new TestSipListener(receiverPort, containerPort, receiverProtocolObjects, false);
        receiver.setRecordRoutingProxyTesting(true);
        SipProvider receiverProvider = receiver.createProvider();

        receiverProvider.addSipListener(receiver);
        senderProvider.addSipListener(sender);

        senderProtocolObjects.start();
        receiverProtocolObjects.start();
        
        Map<String, String> params = new HashMap();
        params.put("servletContainerPort", String.valueOf(containerPort));
        params.put("testPort", String.valueOf(receiverPort));
        params.put("senderPort", String.valueOf(senderPort));          
        deployCallForwarding(params);
        deployLocationService(params);           

        String fromName = "forward-pending-sender";
        String fromHost = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromHost);

        String toUser = "b2bua";
        String toHost = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toHost);

        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
        Thread.sleep(TIMEOUT);
        assertTrue(sender.b2buamessagereceived);
        assertEquals(200, sender.getFinalResponseStatus());
        sender.setFinalResponseStatus(-1);

        receiver.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        Thread.sleep(TIMEOUT);
        assertEquals(200, receiver.getFinalResponseStatus());

        sender.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        Thread.sleep(TIMEOUT);
        assertEquals(200, sender.getFinalResponseStatus());
        assertEquals("sip:fromchanged@sip-servlets.com", ((RequestExt) receiver.getInviteRequest()).getFromHeader().getAddress().getURI().toString().trim());
        assertEquals("sip:tochanged@sip-servlets.com", ((RequestExt) receiver.getInviteRequest()).getToHeader().getAddress().getURI().toString().trim());

        sender.sendInDialogSipRequest("BYE", null, null, null, null, null);
        Thread.sleep(TIMEOUT);
        assertTrue(sender.getOkToByeReceived());
        assertTrue(receiver.getByeReceived());
    }

    public void test491Response() throws Exception {
        int senderPort = NetworkPortAssigner.retrieveNextPort();
        sender = new TestSipListener(senderPort, containerPort, senderProtocolObjects, false);
        sender.setRecordRoutingProxyTesting(true);
        sender.sendByeInNewThread = true;
        SipProvider senderProvider = sender.createProvider();

        int receiverPort = NetworkPortAssigner.retrieveNextPort();
        receiver = new TestSipListener(receiverPort, containerPort, receiverProtocolObjects, false);
        receiver.setRecordRoutingProxyTesting(true);
        SipProvider receiverProvider = receiver.createProvider();

        receiverProvider.addSipListener(receiver);
        senderProvider.addSipListener(sender);

        senderProtocolObjects.start();
        receiverProtocolObjects.start();
        
        Map<String, String> params = new HashMap();
        params.put("servletContainerPort", String.valueOf(containerPort));
        params.put("testPort", String.valueOf(receiverPort));
        params.put("senderPort", String.valueOf(senderPort));          
        deployCallForwarding(params);
        deployLocationService(params);           

        String fromName = "forward-pending-sender";
        String fromHost = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromHost);

        String toUser = "b2bua";
        String toHost = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toHost);

        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
        Thread.sleep(TIMEOUT);
        assertTrue(sender.b2buamessagereceived);
        assertEquals(200, sender.getFinalResponseStatus());
        sender.setFinalResponseStatus(-1);
        Thread.sleep(3000);
        sender.sendInDialogSipRequest("INVITE", null, null, null, null, null);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sender.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        Thread.sleep(TIMEOUT);
        assertEquals(1, sender.numberOf491s);
    }

    public void test491ResponseTough() throws Exception {
        int senderPort = NetworkPortAssigner.retrieveNextPort();
        sender = new TestSipListener(senderPort, containerPort, senderProtocolObjects, false);
        sender.setRecordRoutingProxyTesting(true);
        sender.sendByeInNewThread = true;
        SipProvider senderProvider = sender.createProvider();

        int receiverPort = NetworkPortAssigner.retrieveNextPort();
        receiver = new TestSipListener(receiverPort, containerPort, receiverProtocolObjects, false);
        receiver.setRecordRoutingProxyTesting(true);
        SipProvider receiverProvider = receiver.createProvider();

        receiverProvider.addSipListener(receiver);
        senderProvider.addSipListener(sender);

        senderProtocolObjects.start();
        receiverProtocolObjects.start();
        
        Map<String, String> params = new HashMap();
        params.put("servletContainerPort", String.valueOf(containerPort));
        params.put("testPort", String.valueOf(receiverPort));
        params.put("senderPort", String.valueOf(senderPort));          
        deployCallForwarding(params);
        deployLocationService(params);           

        String fromName = "forward-pending-sender";
        String fromHost = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(
                fromName, fromHost);

        String toUser = "b2bua";
        String toHost = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(
                toUser, toHost);

        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
        Thread.sleep(TIMEOUT);
        assertTrue(sender.b2buamessagereceived);
        assertEquals(200, sender.getFinalResponseStatus());
        sender.setFinalResponseStatus(-1);
        Thread.sleep(3000);
        receiver.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        receiver.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        sender.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        sender.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        sender.sendInDialogSipRequest("INVITE", null, null, null, null, null);
        sender.sendInDialogSipRequest("BYE", null, null, null, null, null);
        Thread.sleep(TIMEOUT);
        assertEquals(2, sender.numberOf491s);
        assertEquals(2, receiver.numberOf491s);
    }

    @Override
    protected void tearDown() throws Exception {
        senderProtocolObjects.destroy();
        receiverProtocolObjects.destroy();
        logger.info("Test completed");
        super.tearDown();
    }

}
