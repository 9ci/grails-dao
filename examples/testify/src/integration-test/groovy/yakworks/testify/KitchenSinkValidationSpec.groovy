package yakworks.rally.orgs

import gorm.tools.repository.errors.EntityValidationException
import gorm.tools.security.testing.SecuritySpecHelper
import gorm.tools.testing.integration.DataIntegrationTest
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import yakworks.testify.model.Address
import yakworks.testify.model.KitchenSink
import yakworks.testify.model.KitchenSinkExt

@Integration
@Rollback
class KitchenSinkValidationSpec extends Specification implements DataIntegrationTest, SecuritySpecHelper {

    def "test Org create"(){
        when:
        Long id = KitchenSink.create([num:'123', name:"Wyatt Oil"]).id
        flushAndClear()

        then:
        def c = KitchenSink.get(id)
        c.num == '123'
        c.name == 'Wyatt Oil'
        c.createdDate
        c.createdBy == 1
        c.editedDate
        c.editedBy == 1
    }

    def "test Org create fail"(){
        when:
        Map invalidData2 = [num:'foo1', name: "foo", link: ["name": "", num:'foo2']]
        def sink = new KitchenSink()

        sink.bind(invalidData2)
        sink.validate()
        //Org.create(invalidData2)

        then:
        //def ex = thrown(EntityValidationException)
        sink.errors.errorCount == 3
        sink.errors['kind'].code == 'nullable'
        sink.errors['link.kind'].code == 'nullable'
        sink.errors['link.name'].code == 'nullable'
    }

    void "org and orgext validation success"(){
        when:
        def sink = new KitchenSink(num:'foo1', name: "foo", kind: KitchenSink.Kind.CLIENT)
        sink.ext = new KitchenSinkExt(kitchenSink: sink, textMax: 'fo')
        sink.persist()

        then:
        sink.id
        sink.ext.id
    }

    void "rejectValue only in LocationRepo beforeValidate"(){
        when:
        def sink = new KitchenSink(num:'foo1', name: "foo", kind: KitchenSink.Kind.CLIENT)
        sink.location = new Address(city: "LocationRepoVille")
        sink.persist(flush: true)
        //flushAndClear()

        then:
        def ex = thrown(EntityValidationException)
        ex.errors.errorCount == 1
        sink.location.errors['city'].code == 'from.LocationRepo'
        //TODO need somethign to make this work
        // org.errors.errorCount == 1
    }

    void "org and orgext rejectValue in beforeValidate"(){
        when:
        def sink = new KitchenSink(num:'foo1', name: "foo", kind: KitchenSink.Kind.CLIENT)
        sink.location = new Address(city: "LocationRepoVille", country: 'USA', street: 'OrgRepoStreet')
        sink.ext = new KitchenSinkExt(kitchenSink: sink, textMax: 'foo') //foo is 3 chars and should fail validation
        sink.persist()
        //flushAndClear()

        then:
        def ex = thrown(EntityValidationException)
        //normal validation errors
        sink.errors['ext.textMax'].code == 'maxSize.exceeded'
        sink.errors['location.country'].code == 'maxSize.exceeded'
        //since its in orgRepo beforeValidate it shows up as nested
        sink.errors['location.street'].code == 'from.OrgRepo'
        //error is on the association for rejects in beforeValidation
        sink.location.errors['city'].code == 'from.LocationRepo'
    }

    def "test Org create validation fail"(){
        when:
        Map invalidData2 = [num:'foo1', name: "foo", link: ["name": "", num:'foo2']]
        KitchenSink.create(invalidData2)

        then:
        def ex = thrown(EntityValidationException)
        //its only 2 on this one as a default kind is set in the repo during create
        ex.errors.errorCount == 2
        ex.errors['link.kind']
        ex.errors['link.name'].code == 'nullable'
    }

}
