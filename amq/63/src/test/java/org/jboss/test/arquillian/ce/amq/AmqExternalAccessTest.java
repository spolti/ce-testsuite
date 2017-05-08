/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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

package org.jboss.test.arquillian.ce.amq;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.amq.support.AmqClient;
import org.jboss.test.arquillian.ce.amq.support.AmqSslTestBase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/amq/amq63-ssl.json",
        parameters = {
                @TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
                @TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
                @TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
                @TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
                @TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp"),
                @TemplateParameter(name = "AMQ_TRUSTSTORE", value = "amq-test.ts"),
                @TemplateParameter(name = "AMQ_TRUSTSTORE_PASSWORD", value = "amq-test"),
                @TemplateParameter(name = "AMQ_KEYSTORE", value = "amq-test.ks"),
                @TemplateParameter(name = "AMQ_KEYSTORE_PASSWORD", value = "amq-test")})
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
        @OpenShiftResource("classpath:amq-routes.json"),
        @OpenShiftResource("classpath:amq-app-secret.json"),
        @OpenShiftResource("classpath:testrunner-secret.json")
})
public class AmqExternalAccessTest extends AmqSslTestBase {

    static {
        System.setProperty("javax.net.ssl.trustStore", AmqSslTestBase.class.getClassLoader().getResource("").getPath() + "/amq-test.ts");
        System.setProperty("javax.net.ssl.trustStorePassword", "amq-test");
    }

    @RouteURL("amq-test-stomp")
    private URL stompUrl;

    @RouteURL("amq-test-mqtt")
    private URL mqttUrl;

    @RouteURL("amq-test-amqp")
    private URL amqpUrl;

    @RouteURL("amq-test-tcp")
    private URL openwireUrl;

    private String openWireMessage = "Arquillian test - OpenWire";
    private String amqpMessage = "Arquillian Test - AMQP";
    private String mqttMessage = "Arquillian test - MQTT";
    private String stompMessage = "Arquillian test - STOMP";

    @Test
    @RunAsClient
    public void testOpenWireConnection() throws Exception {
        Tools.trustAllCertificates();
        AmqClient client = new AmqClient(getRouteUrl(openwireUrl, "ssl"), USERNAME, PASSWORD);

        client.produceOpenWireJms(openWireMessage, true);
        String received = client.consumeOpenWireJms(true);

        assertEquals(openWireMessage, received);
    }

    @Test
    @RunAsClient
    public void testAmqpConnection() throws Exception {
        StringBuilder connectionUrl = new StringBuilder();
        connectionUrl.append(getRouteUrl(amqpUrl, "amqps"));
        connectionUrl.append("?transport.trustStoreLocation=");
        connectionUrl.append(System.getProperty("javax.net.ssl.trustStore"));
        connectionUrl.append("&transport.trustStorePassword=");
        connectionUrl.append(System.getProperty("javax.net.ssl.trustStorePassword"));
        connectionUrl.append("&transport.verifyHost=false");
        AmqClient client = new AmqClient(connectionUrl.toString(), USERNAME, PASSWORD);

        client.produceAmqp(amqpMessage);
        String received = client.consumeAmqp();

        assertEquals(amqpMessage, received);
    }

    @Test
    @RunAsClient
    @Ignore
    public void testMqttConnection() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost(getRouteUrl(mqttUrl, "ssl"));
        mqtt.setUserName(USERNAME);
        mqtt.setPassword(PASSWORD);

        BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();

        Topic[] topics = {new Topic("topics/foo", QoS.EXACTLY_ONCE)};
        connection.subscribe(topics);

        connection.publish("topics/foo", mqttMessage.getBytes(), QoS.EXACTLY_ONCE, false);

        Message msg = connection.receive(5, TimeUnit.SECONDS);

        String received = new String(msg.getPayload());
        assertEquals(mqttMessage, received);
    }

    @Test
    @RunAsClient
    public void testStompConnection() throws Exception {
        AmqClient client = new AmqClient(getRouteUrl(stompUrl, "ssl"), USERNAME, PASSWORD);

        client.produceStomp(stompMessage);
        String received = client.consumeStomp();

        assertEquals(stompMessage, received);
    }

}
