package yakworks.rally.orgs

import gorm.tools.testing.integration.DataIntegrationTest
import gorm.tools.testing.unit.DomainRepoTest
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import yakworks.rally.orgs.model.Org
import yakworks.rally.orgs.model.Contact

@Integration
@Rollback
class ContactRepoSpec extends Specification implements DomainRepoTest<Contact> {


void "test copy"() {
        when:
        def org = Org.findByName("Name1")
    def ooo = org
        def contact = Contact.create(org: org, name:"Joe", firstName: "Joe")

        then:
        contact

    }

}
