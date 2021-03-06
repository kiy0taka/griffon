/*
 * Copyright 2009-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.griffon.runtime.core;

import griffon.core.*;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MethodClosure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static griffon.util.GriffonApplicationUtils.getConfigValueAsBoolean;

/**
 * Handler for 'Service' artifacts.
 *
 * @author Andres Almiray
 * @since 0.9.1
 */
public class ServiceArtifactHandler extends ArtifactHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceArtifactHandler.class);
    private final Map<String, GriffonService> serviceInstances = new LinkedHashMap<String, GriffonService>();

    public ServiceArtifactHandler(GriffonApplication app) {
        super(app, GriffonServiceClass.TYPE, GriffonServiceClass.TRAILING);
        MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(app.getClass());
        if (metaClass instanceof ExpandoMetaClass) {
            ExpandoMetaClass mc = (ExpandoMetaClass) metaClass;
            mc.registerInstanceMethod("getServices", new MethodClosure(this, "getServicesInternal"));
        }
    }

    private Map<String, GriffonService> getServicesInternal() {
        return Collections.unmodifiableMap(serviceInstances);
    }

    protected GriffonClass newGriffonClassInstance(Class clazz) {
        return new DefaultGriffonServiceClass(getApp(), clazz);
    }

    public void initialize(ArtifactInfo[] artifacts) {
        super.initialize(artifacts);
        if (getConfigValueAsBoolean(getApp(), "griffon.basic_injection.disable")) return;
        getApp().addApplicationEventListener(this);
    }

    /**
     * Application event listener.<p>
     * Lazily injects services instances if {@code app.config.griffon.basic_injection.disable}
     * is not set to true
     */
    public void onNewInstance(Class klass, String t, Object instance) {
        if (getType().equals(t) || getConfigValueAsBoolean(getApp(), "griffon.basic_injection.disable")) return;
        MetaClass metaClass = InvokerHelper.getMetaClass(instance);
        for (MetaProperty property : metaClass.getProperties()) {
            String propertyName = property.getName();
            if (!propertyName.endsWith(getTrailing())) continue;
            GriffonService serviceInstance = serviceInstances.get(propertyName);
            if (serviceInstance == null) {
                GriffonClass griffonClass = findClassFor(propertyName);
                if (griffonClass != null) {
                    serviceInstance = (GriffonService) griffonClass.newInstance();
                    InvokerHelper.setProperty(serviceInstance, "app", getApp());
                    getApp().addApplicationEventListener(serviceInstance);
                    serviceInstances.put(propertyName, serviceInstance);
                }
            }

            if (serviceInstance != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Injecting service " + serviceInstance + " on " + instance + " using property '" + propertyName + "'");
                }
                InvokerHelper.setProperty(instance, propertyName, serviceInstance);
            }
        }
    }
}
