
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.BaseScript
import groovy.util.logging.Slf4j

@BaseScript org.commonjava.maven.ext.manip.groovy.BaseScript pme

@Slf4j
public class Processor {
    File basedir

    private void parseDeps(Map n) {
        def iterator = n.iterator()
        while (iterator.hasNext()) {
            def entry = iterator.next()

            if (entry != null && entry.getKey() == "resolved") {
                iterator.remove()
            }

            if (entry != null && entry.getValue() instanceof Map) {
                parseDeps(entry.getValue())
            }
        }
    }

    def execute() {
        log.info("Running ShrinkwrapProcessor...")

        def shrinkwrap = new File (basedir.toString() +
                java.nio.file.FileSystems.getDefault().getSeparator() + "shrink.json")

        log.info("shrinkwrap json is " + shrinkwrap)

        if (shrinkwrap.exists()) {
            log.info ("Found file {}", shrinkwrap)

            // Annoyingly the JsonSlurper reads them and does NOT maintain the order. Therefore
            // the diff is not overly helpful
            LinkedHashMap json = new JsonSlurper().parseText(shrinkwrap.text)

            parseDeps(json)

            shrinkwrap.write(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
        }
    }
}
// These are both debug AND test statements - do NOT remove. If the injection (in GroovyManipulator)
// fails these prints will cause the test to fail.
println "#### BINDINGS:"
println binding.variables.basedir
println binding.variables.gav
println binding.variables.projects
println binding.variables.project

binding.variables.each{
//  println it.key
//  println it.value
  println it.value.getClass().toString()
}
println "#### BINDINGS END"

println "#### BASESCRIPT:"
println pme.getBaseDir()
println pme.getGAV()
println pme.getProjects()
println pme.getProject()
println "#### BASESCRIPT END"
// End...

def Processor sp = new Processor(basedir:pme.getBaseDir())
sp.execute()
