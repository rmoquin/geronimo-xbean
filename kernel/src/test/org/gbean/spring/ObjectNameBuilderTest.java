/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gbean.spring;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectName;

import junit.framework.TestCase;
import org.gbean.kernel.ServiceName;
import org.gbean.metadata.MetadataManager;
import org.gbean.metadata.simple.PropertiesMetadataProvider;
import org.gbean.metadata.simple.SimpleMetadataManager;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @version $Revision$ $Date$
 */
public class ObjectNameBuilderTest extends TestCase {
    private DefaultListableBeanFactory factory;

    public void testObjectNameProperty() {
        String beanName = "hal";
        assertTrue(factory.containsBeanDefinition(beanName));
        BeanDefinition beanDefinition = factory.getBeanDefinition(beanName);
        assertNotNull(beanDefinition);
        assertTrue(beanDefinition.getPropertyValues().contains("gbean-objectName"));

        ObjectName objectName = ServiceName.createName((String) beanDefinition.getPropertyValues().getPropertyValue("gbean-objectName").getValue());

        // convert properties into named constructor args
        List metadataProviders = new ArrayList(2);
        metadataProviders.add(new PropertiesMetadataProvider());
        MetadataManager metadataManager = new SimpleMetadataManager(metadataProviders);

        ObjectNameBuilder objectNameBuilder = new ObjectNameBuilder(metadataManager, "domain", "server", "application");
        objectNameBuilder.postProcessBeanFactory(factory);

        assertFalse(beanDefinition.getPropertyValues().contains("gbean-objectName"));
        assertEquals(objectName, objectNameBuilder.getObjectName(beanName));
    }

    public void testObjectNameCreation() {
        String beanName = "foobar";
        assertTrue(factory.containsBeanDefinition(beanName));
        BeanDefinition beanDefinition = factory.getBeanDefinition(beanName);
        assertNotNull(beanDefinition);
        assertFalse(beanDefinition.getPropertyValues().contains("gbean-objectName"));

        // convert properties into named constructor args
        List metadataProviders = new ArrayList(2);
        metadataProviders.add(new PropertiesMetadataProvider());
        MetadataManager metadataManager = new SimpleMetadataManager(metadataProviders);

        ObjectNameBuilder objectNameBuilder = new ObjectNameBuilder(metadataManager, "domain", "server", "module");
        objectNameBuilder.postProcessBeanFactory(factory);

        ObjectName objectName = ServiceName.createName("domain:J2EEServer=server,J2EEApplication=null,J2EEModule=module,name=" + beanName);

        objectNameBuilder.postProcessBeanFactory(factory);

        assertFalse(beanDefinition.getPropertyValues().contains("gbean-objectName"));
        assertEquals(objectName, objectNameBuilder.getObjectName(beanName));
    }

    protected void setUp() throws Exception {
        factory = new DefaultListableBeanFactory();
        Resource resource = new FileSystemResource(new File("src/test-data/PostProcessorTest.xml"));
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        reader.loadBeanDefinitions(resource);
    }
}