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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.test.annotations.BusinessArchive;
import org.bonitasoft.engine.test.annotations.Engine;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * @author Baptiste Mesta
 */
public class BonitaEngineTestRunner extends BlockJUnit4ClassRunner {

    private Field engineField;
    private BonitaTestEngine engine;
    private HashMap<Field, String> businessArchives = new HashMap<Field, String>();
    private HashMap<Field, ProcessDefinition> processes = new HashMap<Field, ProcessDefinition>();

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws org.junit.runners.model.InitializationError if the test class is malformed.
     */
    public BonitaEngineTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
        processAnnotedField(klass);
    }

    private void processAnnotedField(Class<?> klass) {
        Field[] fields = klass.getDeclaredFields();
        for (Field field : fields) {
            System.out.println("========");
            System.out.println(field.getName());
            System.out.println(field.getType());
            for(Annotation annotation : field.getDeclaredAnnotations()) {
                System.out.println(annotation);
                System.out.println(annotation.annotationType());
                if(annotation.annotationType().equals(BusinessArchive.class)){
                    //deploy
                    field.setAccessible(true);
                    businessArchives.put(field,((BusinessArchive)annotation).resource());
                }
                if(annotation.annotationType().equals(Engine.class)){
                    //deploy
                    field.setAccessible(true);
                    engineField = field;
                    engine = BonitaTestEngine.defaultLocalEngine();
                }
            }
        }
    }

    @Override
    protected Object createTest() throws Exception {
        Object test = super.createTest();
        if(engineField != null){
            engineField.set(test,engine);
        }
        for (Map.Entry<Field, ProcessDefinition> fieldProcessDefinitionEntry : processes.entrySet()) {
            fieldProcessDefinitionEntry.getKey().set(test,fieldProcessDefinitionEntry.getValue());
        }
        return test;
    }

    @Override
    protected Statement classBlock(final RunNotifier notifier) {

        Statement statement = super.classBlock(notifier);
        statement = withGlobalBefore(statement);
        statement = withGlobalAfter(statement);
        return statement;
    }

    private Statement withGlobalBefore(final Statement statement) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                launchTheEngine();
                deployBusinessArchives();
                statement.evaluate();
            }
        };
    }

    private void deployBusinessArchives() {
        for (Map.Entry<Field, String> fieldStringEntry : businessArchives.entrySet()) {
            Field key = fieldStringEntry.getKey();
            String value = fieldStringEntry.getValue();
            processes.put(key,engine.deploy(value));
        }
    }

    private void launchTheEngine() throws IOException, BonitaException {
        if(engine != null){

            engine.create();
            engine.start();
            engine.login();
        }
    }

    private Statement withGlobalAfter(final Statement statement) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                statement.evaluate();
                undeployBusinessArchives();
                stopTheEngine();
            }
        };
    }

    private void undeployBusinessArchives() {

    }

    private void stopTheEngine() {
        if(engine != null){
            engine.stop();
            engine.destroy();
        }
    }


}
