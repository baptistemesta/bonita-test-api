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

import java.io.IOException;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * @author Baptiste Mesta
 */
public class BonitaTestEngineRule extends TestWatcher {

    private static BonitaTestEngine bonitaTestEngine;

    public BonitaTestEngineRule() {

    }


    public static BonitaTestEngineRule defaultLocalEngine() {
        bonitaTestEngine = BonitaTestEngine.defaultLocalEngine();
        return new BonitaTestEngineRule();
    }


    @Override
    protected void starting(Description description) {
        if (!bonitaTestEngine.isStarted()) {
            initialize();
        }
    }

    private void initialize() {
        try {
            bonitaTestEngine.create();
            bonitaTestEngine.start();
//            bonitaTestEngine.login();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to initialize engine", e);
        }
    }

    public BonitaTestEngine getEngine() {
        return bonitaTestEngine;
    }
}
