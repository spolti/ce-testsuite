package org.jboss.test.arquillian.ce.amq.support;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.arquillian.ce.shrinkwrap.Files;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class AmqTestBase {
    public static final String FILENAME = "amq.properties";
    public static final String USERNAME = System.getProperty("amq.username", "amq-test");
    public static final String PASSWORD = System.getProperty("amq.password", "redhat");

    protected static WebArchive getDeploymentBase(Class... classes) throws IOException {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML(new StringAsset("<web-app/>"));
        war.addClass(AmqTestBase.class);
        war.addClass(AmqClient.class);
        war.addClasses(classes);

        war.addAsLibraries(Libraries.transitive("org.apache.activemq", "activemq-client"));
        war.addAsLibraries(Libraries.transitive("org.apache.qpid", "qpid-jms-client"));
        war.addAsLibraries(Libraries.transitive("org.fusesource.mqtt-client", "mqtt-client"));
        war.addAsLibraries(Libraries.transitive("org.fusesource.stompjms", "stompjms-client"));

        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
        handle.addProperty("amq.username", USERNAME);
        handle.addProperty("amq.password", PASSWORD);
        handle.store(war);

        return war;
    }

    protected static void restartAmq(OpenShiftHandle handler) throws Exception {
        handler.scaleDeployment("amq-test-amq", 0);
        handler.scaleDeployment("amq-test-amq", 1);
    }

    protected AmqClient createAmqClient(String url) throws Exception {
        Properties properties = Tools.loadProperties(getClass(), FILENAME);
        String username = properties.getProperty("amq.username");
        String password = properties.getProperty("amq.password");
        return new AmqClient(url, username, password);
    }

    protected String getRouteUrl(URL routeUrl, String scheme) {
        StringBuilder routeString = new StringBuilder();
        routeString.append(scheme);
        routeString.append("://");
        routeString.append(routeUrl.getHost());
        routeString.append(":");
        routeString.append(routeUrl.getPort());

        return routeString.toString();
    }
}