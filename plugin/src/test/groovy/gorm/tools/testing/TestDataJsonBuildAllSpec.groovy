package gorm.tools.testing

import gorm.tools.beans.BeanPathTools
import gorm.tools.testing.unit.DataRepoTest
import grails.buildtestdata.BuildDataTest
import spock.lang.Specification
import testing.Location
import testing.Org
import testing.OrgExt

class TestDataJsonBuildAllSpec extends Specification implements BuildDataTest, DataRepoTest{

    def setupSpec(){
        mockDomains(Org, OrgExt, Location)
    }

    void "test getJsonIncludes"(){
        when:
        def incs = TestDataJson.getFieldsToBuild(Org, '*')
        def bpathIncs = BeanPathTools.getIncludes(Org.name, incs)

        then:
        incs.containsAll(['location', 'ext', 'ext.*', 'location.id'])
        bpathIncs.containsAll(['amount', 'ext', 'ext.id', 'ext.text1', 'location.id'])
    }

    void "test includes *"(){
        when:
        def map = TestDataJson.buildMap(Org, includes:'*', deep:true)//, location: build(Location), ext: build(OrgExt), deep:true)
        then:
        map.ext.id
        map.ext.text1
        map.location.id
    }

}

