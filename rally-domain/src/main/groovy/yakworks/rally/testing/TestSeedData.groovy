package yakworks.rally.testing

import gorm.tools.testing.unit.DataRepoTest
import gorm.tools.testing.unit.DomainRepoTest
import grails.buildtestdata.BuildDataTest
import grails.buildtestdata.TestData
import grails.buildtestdata.TestDataBuilder
import groovy.transform.CompileStatic
import yakworks.rally.orgs.model.Location
import yakworks.rally.orgs.model.Org
import yakworks.rally.orgs.model.OrgFlex
import yakworks.rally.orgs.model.OrgInfo
import yakworks.rally.orgs.model.OrgSource
import yakworks.rally.orgs.model.OrgTag
import yakworks.rally.orgs.model.OrgType
import yakworks.rally.orgs.model.OrgTypeSetup

@CompileStatic
class TestSeedData   implements BuildDataTest {

    static void buildOrgs(int count){
        // mockDomains (OrgTypeSetup, Location)
        def loc = Location.create(city: "City1").persist()
        def ots = new OrgTypeSetup(name: 'Customer').persist(flush:true)

        (1..count).each { index ->
            String value = "Name$index"
            Org.create([id: index,
                name: value,
                type: [id: 1],
                inactive: (index % 2 == 0)]
            ).persist()
        }
    }


}
