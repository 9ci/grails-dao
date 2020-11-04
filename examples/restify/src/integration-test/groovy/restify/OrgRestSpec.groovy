package restify

import geb.spock.GebSpec
import gorm.tools.rest.client.RestApiTestTrait
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Ignore
import spock.lang.IgnoreRest

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.OK

@Integration
@Rollback
class OrgRestSpec extends GebSpec implements RestApiTestTrait {

    String path = "api/org"

    //@Transactional
    //Map getPostData() { return TestDataJson.buildMap(Org, type: OrgType.load(1)) }
    // Map postData = [num:'foo1', name: "foo", type: [id: 1]]
    //
    // Map putData = [name: "Name Update"]
    //
    // Map invalidData = ["name": null]

    void "test get"() {
        when:
        String resPath = getResourcePath()
        def response = restBuilder.get("${getResourcePath()}/1")

        then:
        response.status == OK.value()
        // resPath == 'foo'

        // String baseUrl = "http://localhost:$serverPort"
        // this.client = HttpClient.create(baseUrl.toURL())
        // HttpRequest request = HttpRequest.GET('/api/book')
        // HttpResponse<List<Map>> resp = client.toBlocking().exchange(request, Argument.of(List, Map))
        //
        // then:
        // HttpClientResponseException ex = thrown()
        // ex.message == "Access Denied"
    }

    @Ignore
    void "exercise api"() {
        expect:
        testGet()
        testPost()
        //testPut()
        //testDelete()
    }

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
    def testPostWithNestedId() {
        given:
        def response
        Map data = [num:'foo1', name: "foo"]
        // "The save action is executed with valid data"
        when:
        response = restBuilder.post(resourcePath+'?typeId=1') {
            json data
        }
        then:
        response.status == CREATED.value()
        response.json.name == 'foo'
        response.json.type.id == 1
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
        response.json.errors.find{ it.field == 'link.kind' }.message == 'Property [kind] of class [class yakworks.taskify.domain.Org] cannot be null'
        // ex.errors['link.kind'].message == 'Property [kind] of class [class yakworks.taskify.domain.Org] cannot be null'
        // response.json.errors[0].field == 'link.kind'
        // response.json.errors[0].message == 'Property [kind] of class [class yakworks.taskify.domain.Org] cannot be null'
        response.json.errors.find{ it.field == 'link.name' }
        // response.json.errors[1].field == 'link.name'
        // response.json.errors[1].message == 'Property [name] of class [class yakworks.taskify.domain.Org] cannot be null'

    }


}
