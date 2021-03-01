package yakworks.rally.orgs

import gorm.tools.security.testing.SecurityTest
import gorm.tools.testing.TestDataJson
import gorm.tools.testing.unit.DomainRepoTest
import yakworks.rally.activity.model.Activity
import yakworks.rally.orgs.model.Contact
import yakworks.rally.orgs.model.ContactEmail
import yakworks.rally.orgs.model.ContactFlex
import yakworks.rally.orgs.model.ContactPhone
import yakworks.rally.orgs.model.ContactSource
import yakworks.rally.orgs.model.Location
import yakworks.rally.orgs.model.Org
import yakworks.rally.orgs.model.OrgFlex
import yakworks.rally.orgs.model.OrgInfo
import yakworks.rally.orgs.model.OrgSource
import yakworks.rally.orgs.model.OrgTag
import yakworks.rally.orgs.model.OrgType
import yakworks.rally.orgs.model.OrgTypeSetup
import spock.lang.Specification
import yakworks.rally.attachment.model.AttachmentLink

class OrgSpec extends Specification implements DomainRepoTest<Org>, SecurityTest {
    //Automatically runs the basic crud tests

    def setupSpec(){
        defineBeans{
            //scriptExecutorService(ScriptExecutorService)
            // orgDimensionService(OrgDimensionService)
        }
        mockDomains(
            Contact, OrgFlex, OrgSource, OrgTag,
            OrgInfo, OrgTypeSetup, Location, ContactPhone,
            ContactEmail, ContactSource, ContactFlex, Activity, AttachmentLink
        )
    }

    def setup(){
        def ots = new OrgTypeSetup(name: 'Customer').persist(flush:true)
        assert ots.id == 1
    }


    void "CRUD tests"() {
        expect:
        createEntity().id
        persistEntity().id
        updateEntity().version > 0
        removeEntity()
    }

    def testOrgSourceChange() {

        when: "create"
        Org org = Org.create([num:"foo", name:"bar", type: [id: 1], companyId: 2], bindId: true)

        org.validate()
        org.createSource()
        org.persist()

        then: "source id is the default"
        assert org.source.sourceId == "foo"

        when: "sourceId is changed"
        org.source.sourceId = "test"
        org.source.persist()
        Long osi = org.source.id
        assert org.source.id

        then: "it should be good"
        assert org.source.sourceId == "test"
        assert OrgSource.get(osi).sourceId == "test"

        when: "flush and clear is called and OrgSource is retreived again"
        flushAndClear()

        then : "should stil be the sourceId that was set"
        assert OrgSource.get(osi).sourceId == "test"
    }

    def "create & update associations"() {
        setup:
        Long orgId = 1000

        Map flex = TestDataJson.buildMap(OrgFlex, includes:"*")
        Map info = TestDataJson.buildMap(OrgInfo, includes:"*")

        Map params = buildMap() << [id: orgId, flex: flex, info: info, orgType: [id: 1]]

        when: "create"
        def org = Org.create(params, bindId: true)

        then:

        org.id == orgId
        org.flex.id
        org.info.id
        //entity.calc.id

        org.flex.text1
        org.info.phone
        org.info.fax
        org.info.website


    }

    def "test insert with locations"() {
        setup:
        Long orgId = 10000
        //Map location = TestDataJson.buildMap(Location, includes:"*")
        List locations = [[street1: "street1"], [street1: "street loc2"]]
        Map params = buildMap() + [locations: locations]

        when:
        def org = Org.create(params)
        def locs = org.locations

        then:
        locs.size() == 2
        locs[0].street1 == locations[0].street1
        locs[1].street1 == locations[1].street1
        locs[0].org == org
    }

    void "test getOrgTypeFromData"() {
        expect:
        Org.repo.getOrgTypeFromData(data) == orgType

        where:

        orgType            | data
        OrgType.Customer   | [orgTypeId: 1]
        OrgType.Branch     | [orgTypeId: '3']
        OrgType.Branch     | [type: OrgType.Branch]
        OrgType.Branch     | [type: [id: 3]]
    }
}
