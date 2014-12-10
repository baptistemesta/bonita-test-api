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
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.PermissionAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.PlatformAPIAccessor;
import org.bonitasoft.engine.api.PlatformLoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.api.ThemeAPI;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDeployException;
import org.bonitasoft.engine.bpm.process.ProcessEnablementException;
import org.bonitasoft.engine.commons.io.IOUtil;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.PlatformLoginException;
import org.bonitasoft.engine.platform.PlatformLogoutException;
import org.bonitasoft.engine.platform.StartNodeException;
import org.bonitasoft.engine.session.APISession;
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


    private APISession session;

    private ProcessAPI processAPI;

    private IdentityAPI identityAPI;

    private CommandAPI commandAPI;

    private ProfileAPI profileAPI;

    private ThemeAPI themeAPI;

    private PermissionAPI permissionAPI;

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
            PlatformSession session = loginPlatform();
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
        started = true;
    }
    public void stop() {

    }

    private void createPlatform() throws BonitaException {
        final PlatformSession session = loginPlatform();
        final PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(session);
        if (!platformAPI.isPlatformCreated()) {
            platformAPI.createPlatform();
        }
        platformAPI.initializePlatform();
        logoutPlatform(session);
    }

    private void logoutPlatform(PlatformSession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, PlatformLogoutException, SessionNotFoundException {
        final PlatformLoginAPI platformLoginAPI = PlatformAPIAccessor.getPlatformLoginAPI();
        platformLoginAPI.logout(session);
    }

    private PlatformSession loginPlatform() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, PlatformLoginException {
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


    public ProcessDefinition deploy(String value) {
        try {
            return getProcessAPI().deployAndEnableProcess(BusinessArchiveFactory.readBusinessArchive(new File(value)));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

    }



    public LoginAPI getLoginAPI() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        return TenantAPIAccessor.getLoginAPI();
    }

    public ProcessAPI getProcessAPI() {
        return processAPI;
    }

    public void setProcessAPI(final ProcessAPI processAPI) {
        this.processAPI = processAPI;
    }

    public IdentityAPI getIdentityAPI() {
        return identityAPI;
    }

    public void setIdentityAPI(final IdentityAPI identityAPI) {
        this.identityAPI = identityAPI;
    }

    public CommandAPI getCommandAPI() {
        return commandAPI;
    }

    public void setCommandAPI(final CommandAPI commandAPI) {
        this.commandAPI = commandAPI;
    }

    public ProfileAPI getProfileAPI() {
        return profileAPI;
    }

    public void setProfileAPI(final ProfileAPI profileAPI) {
        this.profileAPI = profileAPI;
    }

    public ThemeAPI getThemeAPI() {
        return themeAPI;
    }

    public void setThemeAPI(final ThemeAPI themeAPI) {
        this.themeAPI = themeAPI;
    }

    public PermissionAPI getPermissionAPI() {
        return permissionAPI;
    }

    public void setPermissionAPI(final PermissionAPI permissionAPI) {
        this.permissionAPI = permissionAPI;
    }

    public APISession getSession() {
        return session;
    }

    public void setSession(final APISession session) {
        this.session = session;
    }
    public void login() throws BonitaException {
        final LoginAPI loginAPI = getLoginAPI();
        setSession(loginAPI.login("install", "install"));
        setAPIs();
    }

    protected void setAPIs() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        setIdentityAPI(TenantAPIAccessor.getIdentityAPI(getSession()));
        setProcessAPI(TenantAPIAccessor.getProcessAPI(getSession()));
        setCommandAPI(TenantAPIAccessor.getCommandAPI(getSession()));
        setProfileAPI(TenantAPIAccessor.getProfileAPI(getSession()));
        setThemeAPI(TenantAPIAccessor.getThemeAPI(getSession()));
        setPermissionAPI(TenantAPIAccessor.getPermissionAPI(getSession()));
    }
}
