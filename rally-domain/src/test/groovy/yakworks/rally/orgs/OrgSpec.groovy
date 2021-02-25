package yakworks.rally.orgs


import gorm.tools.security.testing.SecurityTest
import gorm.tools.testing.unit.DomainRepoTest
import spock.lang.Specification
import yakworks.rally.orgs.model.Org
import yakworks.rally.orgs.model.OrgSource
import yakworks.rally.orgs.model.OrgType
import yakworks.rally.orgs.model.OrgTypeSetup

class OrgSpec extends Specification implements DomainRepoTest<Org>, SecurityTest {

    void setupSpec() {
        mockDomains OrgTypeSetup, OrgSource
    }

    void "test org create with type"() {
        when:
        def typeSetup = OrgTypeSetup.create([name:"Customer"]).persist()
        def o = Org.create(
            [
                id      : 10,
                name    : "foo",
                num     : "123",
                companyId: 1,
                source  : [
                    sourceId    : "123",
                    sourceVersion: 1,
                ],
                type    : [id: 1],
                orgTypeId: 1
            ]
        ).persist(failOnError: true)

        then:
        def org = Org.findByName("foo")
        org
        // 1 == org.type.id
        // def orgType = OrgType.get(org.type.id)
        // null == orgType

    }

}
