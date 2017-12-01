/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other
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

package org.jboss.test.arquillian.ce.eap70;

import io.fabric8.openshift.clnt.v2_6.OpenShiftConfig;
import org.arquillian.cube.kubernetes.impl.portforward.PortForwarder;
import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.clnt.v2_6.Config;

/**
 * @author Marko Luksa
 */
@RunWith(Arquillian.class)
//@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/eap/eap70-basic-s2i.json",
@Template(url = "file:///dados/dev/ce/repos/ce-testsuite/eap/eap70/eap70-basic-s2i.json",
        parameters = {
                @TemplateParameter(name = "SOURCE_REPOSITORY_URL", value = "https://github.com/spolti/jboss-eap-quickstarts"),
                @TemplateParameter(name = "SOURCE_REPOSITORY_REF", value = "cloud2151Tests"),
                @TemplateParameter(name = "CONTEXT_DIR", value = "kitchensink")
        }
)
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
public class Eap70ActiveMQClusterTest {

    @Test
    @InSequence(2)
    public void scalePodsUp(@ArquillianResource OpenShiftHandle adapter) throws Exception {
//        System.out.println("Waiting 90 seconds to scale up");
//        Thread.sleep(300000);
        System.out.println("scaling deployment eap-app up");
        adapter.scaleDeployment("eap-app", 2);
    }


    @Test
    @InSequence(3)
    public void scalePodsDown(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.scaleDeployment("eap-app", 1);

        Map<String, String> labels = new HashMap<>();
        labels.put("application", "eap-app");
        labels.put("deployment","eap-app-1");


        adapter.waitForReadyPods("eap-app", 1);

        String result = adapter.getLog(null, labels);


//        List<String> pods = adapter.getPods("eap-app");
//        Optional<String> pod = pods.stream().filter(e -> !e.contains("build")).findFirst();

        System.out.println("Test waiting -");
        //Thread.sleep(120000);

        Thread.sleep(99999999);



    }
}
