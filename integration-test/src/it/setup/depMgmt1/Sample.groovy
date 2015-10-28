
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
public class Processor {
    def binding

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

        def shrinkwrap = new File (binding.variables.basedir +
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
// Debug...
println "#### BINDINGS:"
binding.variables.each{
  println it.key
  println it.value
  println it.value.getClass().toString()
}
println "#### BINDINGS END"
// End...

def Processor sp = new Processor(binding:binding)
sp.execute()
