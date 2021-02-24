package yakworks.rally.testing

import grails.buildtestdata.TestData
import net.bytebuddy.pool.TypePool
import yakworks.rally.orgs.model.Location
import yakworks.rally.orgs.model.Org
import yakworks.rally.orgs.model.OrgType
import yakworks.rally.orgs.model.OrgTypeSetup


class TestSeedData {

    static void buildOrgs(int count){
        // def typeSetup = OrgTypeSetup.create([id:1, name:"Customer"])
        // typeSetup.persist()
        def type = OrgType.Customer
        // def type = TestData.build(OrgType)
        // def type = new OrgTypeSetup(name:"Customer", orgType:OrgType.Customer).orgType
        (1..count).each { index ->
            String value = "Name$index"
            Org.create([id: index,
                name: value,
                type: [id: 1],
                inactive: (index % 2 == 0)]
            ).persist(failOnError: true)
        }
    }
}
