bonita-test-api
===============
Module that contains utilities to create junit tests on processes

current features
----------------
* @Engine and @BusinessArchive annotations to have in a unit test a bonita engine running and processes deployed
* BonitaTestEngine that handle the creation of the platform

Features to come
-----------------
* Stop/destroy of the engine
* more flexible way to have BAR deployed, e.g. on methods, even if the is no engine in the class but in a test suite
* add more ways to retrieve/construct the bar
* add utility methods for tests like engine.verify(task).isDone()
* packaging
** the bonita home is commited, need to get it in build


Usage
------

* Add dependency on
    <parent>
        <groupId>org.bonitasoft.engine</groupId>
        <artifactId>bonita-test-api</artifactId>
        <version>${bonita.version}</version>
    </parent>
* Example test file
```java
@RunWith(BonitaEngineTestRunner.class)
public class BonitaTestEngineRuleIT {

    @Engine
    public BonitaTestEngine engine;

    @BusinessArchive(resource = "<Path to the .bar on file system>")
    public ProcessDefinition processDefinition;

    @Test
    public void check_started_with_annotation() throws Exception {
        engine.getProcessAPI().startProcess(processDefinition.getId());
        System.out.println(processDefinition.getName());

    }
}
```
