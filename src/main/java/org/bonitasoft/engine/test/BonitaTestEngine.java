/*
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.bonitasoft.engine.test;

import javax.naming.Context;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.PlatformAPIAccessor;
import org.bonitasoft.engine.api.PlatformLoginAPI;
import org.bonitasoft.engine.commons.io.IOUtil;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.PlatformLoginException;
import org.bonitasoft.engine.platform.PlatformLogoutException;
import org.bonitasoft.engine.platform.StartNodeException;
import org.bonitasoft.engine.session.PlatformSession;
import org.bonitasoft.engine.session.SessionNotFoundException;
import org.bonitasoft.engine.util.APITypeManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Baptiste Mesta
 */
public class BonitaTestEngine {


    private boolean started;
    private final boolean createPlatform;
    private final boolean local;
    private ClassPathXmlApplicationContext springContext;

    private BonitaTestEngine(boolean local, boolean createPlatform) {
        this.local = local;
        this.createPlatform = createPlatform;
    }


    public static BonitaTestEngine defaultLocalEngine() {
        APITypeManager.setAPITypeAndParams(ApiAccessType.LOCAL, Collections.<String, String>emptyMap());
        return new BonitaTestEngine(true, true);
    }

    public boolean isStarted() {
        return started;
    }

    public void create()throws IOException, BonitaException{
        System.out.println("======================================================");
        System.out.println("===========  INITIALIZATION OF BONITA ENGINE =========");
        System.out.println("======================================================");
        long startTime = System.currentTimeMillis();
        if (local) {
            initializeLocalBonitaHome();
            initializeEnvironment();
        }
        if (createPlatform) {
            createPlatform();
        }
        System.out.println("==== Finished initialization (took " + (System.currentTimeMillis() - startTime) / 1000 + "s)  ===");
    }
    public void destroy(){

    }

    public void start() throws StartNodeException {
        try {
            PlatformSession session = login();
            PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(session);
            platformAPI.startNode();
        } catch (BonitaHomeNotSetException e) {
            throw new StartNodeException(e);
        } catch (ServerAPIException e) {
            throw new StartNodeException(e);
        } catch (UnknownAPITypeException e) {
            throw new StartNodeException(e);
        } catch (PlatformLoginException e) {
            throw new StartNodeException(e);
        }
    }
    public void stop() {

    }

    private void createPlatform() throws BonitaException {
        final PlatformSession session = login();
        final PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(session);
        if (!platformAPI.isPlatformCreated()) {
            platformAPI.createPlatform();
        }
        platformAPI.initializePlatform();
        logout(session);
    }

    private void logout(PlatformSession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, PlatformLogoutException, SessionNotFoundException {
        final PlatformLoginAPI platformLoginAPI = PlatformAPIAccessor.getPlatformLoginAPI();
        platformLoginAPI.logout(session);
    }

    private PlatformSession login() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, PlatformLoginException {
        final PlatformLoginAPI platformLoginAPI = PlatformAPIAccessor.getPlatformLoginAPI();
        return platformLoginAPI.login("platformAdmin", "platform");
    }

    private static void setSystemPropertyIfNotSet(final String property, final String value) {
        System.setProperty(property, System.getProperty(property, value));
    }

    private void initializeEnvironment() {
        setSystemPropertyIfNotSet("sysprop.bonita.db.vendor", "h2");

        // Force these system properties
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.bonitasoft.engine.local.SimpleMemoryContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.bonitasoft.engine.local");

        final List<String> springConfigLocations = getSpringConfigLocations();
        springContext = new ClassPathXmlApplicationContext(springConfigLocations.toArray(new String[springConfigLocations.size()]));
    }

    private void initializeLocalBonitaHome() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/bonita-home.zip");
        File tempFile = File.createTempFile("bonita-home", "");
        tempFile.delete();
        tempFile.mkdir();
        IOUtil.unzipToFolder(resourceAsStream, tempFile);
        System.setProperty("bonita.home", new File(tempFile,"home").getAbsolutePath());
    }

    private List<String> getSpringConfigLocations() {
        return Arrays.asList("datasource.xml", "jndi-setup.xml");
    }



}
