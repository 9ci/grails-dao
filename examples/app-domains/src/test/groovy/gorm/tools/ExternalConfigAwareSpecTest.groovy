package gorm.tools

import gorm.tools.testing.unit.ExternalConfigAwareSpec
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class ExternalConfigAwareSpecTest extends Specification implements GrailsUnitTest, ExternalConfigAwareSpec {

    void  "test external config is loaded"() {
        expect:
        applicationContext.getBean('externalConfigLoader') != null
        config.foo.bar == "test" //this comes from a file defined in config.locations
    }

}
