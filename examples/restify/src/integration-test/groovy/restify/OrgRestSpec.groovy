package restify

import geb.spock.GebSpec
import gorm.tools.rest.testing.RestApiFuncSpec
import gorm.tools.rest.testing.RestApiTestTrait
import gorm.tools.testing.TestDataJson
import gorm.tools.testing.TestTools
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import yakworks.taskify.domain.Org
import yakworks.taskify.domain.OrgType

import static org.springframework.http.HttpStatus.CREATED

@Integration
@Rollback
class OrgRestSpec extends RestApiFuncSpec {

    String path = "api/org"

    //@Transactional
    //Map getPostData() { return TestDataJson.buildMap(Org, type: OrgType.load(1)) }
    Map postData = [num:'foo1', name: "foo", type: [id: 1]]

    Map putData = [name: "Name Update"]

    Map invalidData = ["name": null]

    @Override
    def testPost() {
        // "The save action is executed with valid data"
        def response = restBuilder.post(resourcePath) {
            json postData
        }

        assert response.status == CREATED.value()
        //response.json.id
        assert response.json.name == 'foo'
        assert response.json.type.id == 1
        def rget = restBuilder.get("$resourcePath/${response.json.id}")
        assert rget.json.name == 'foo'
        assert rget.json.type.id == 1
        return rget
    }

    void test_save_post() {
        given:
        def response
        Map invalidData2 = [num:'foo1', name: "foo", type: [id: 1], link: ["name": "", num:'foo2', type: [id: 1]]]
        when: "The save action is executed with invalid data for nested property"
        response = restBuilder.post(resourcePath) {
            json invalidData2
        }
        then: "The response is UNPROCESSABLE_ENTITY"
        verify_UNPROCESSABLE_ENTITY(response)
        response.json.total == 2
        response.json.message == 'yakworks.taskify.domain.Org save failed'
        response.json.errors[0].field == 'link.kind'
        response.json.errors[0].message == 'Property [kind] of class [class yakworks.taskify.domain.Org] cannot be null'
        response.json.errors[1].field == 'link.name'
        response.json.errors[1].message == 'Property [name] of class [class yakworks.taskify.domain.Org] cannot be null'

    }


}
