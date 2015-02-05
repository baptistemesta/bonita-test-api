/*
 * Copyright (C) 2015 BonitaSoft S.A.
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


import org.bonitasoft.engine.test.annotations.Engine;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BonitaEngineTestRunner.class)
public class Client {

    @Engine(type = "HTTP", url = "http://localhost:8080", name = "bonita")
    public BonitaTestEngine engine;


    @Test
    public void run() throws Exception {
        engine.login();
//       engine.getProcessAPI().evaluateExpressionOnProcessDefinition(new )

    }
}