package restify

import org.springframework.http.HttpStatus

import gorm.tools.rest.client.OkHttpRestTrait
import grails.testing.mixin.integration.Integration
import okhttp3.HttpUrl
import okhttp3.Response
import spock.lang.Specification

@Integration
class OrgRestApiSpec extends Specification implements OkHttpRestTrait {

    String path = "/api/org"
    Map postData = [num:'foo1', name: "foo", type: 'Customer']
    Map putData = [name: "updated foo1"]

    void "get picklist"() {
        when:
        Response resp = get("$path/picklist")
        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.OK.value()
        body.data.size() == 20
        Map book = body.data[0] as Map
        book.keySet().size() == 3 //should be the id and name and num
        book['id'] == 1
        book['name']
    }

    void "test qSearch"() {
        when:
        Response resp = get("$path?q=org2")
        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.OK.value()
        body.data.size() == 11

        when:
        resp = get("$path?q=flubber")
        body = bodyToMap(resp)

        then:
        body.data.size() == 0

        when: 'num search'
        resp = get("$path?q=11")
        body = bodyToMap(resp)

        then:
        body.data.size() == 1
        body.data[0].num == '11'

        when: 'picklist search'
        resp = get("$path/picklist?q=org12")
        body = bodyToMap(resp)

        then:
        body.data.size() == 1

    }

    void "test q"() {
        when:
        String q = '{name: "Org20"}'
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getUrl(path)).newBuilder()
        urlBuilder.addQueryParameter("q", q)
        def resp = get(urlBuilder.build().toString())
        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.OK.value()
        body.data.size() == 1
        body.data[0].name == "Org20"
    }

    void "test sorting"() {
        when: "sort asc"
        def resp = get("${path}?sort=id&order=asc")
        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.OK.value()
        //the first id should be less than the second
        body.data[0].id < body.data[1].id

        when: "sort desc"
        resp = get("${path}?sort=id&order=desc")
        body = bodyToMap(resp)

        then: "The response is correct"
        resp.code() == HttpStatus.OK.value()
        //the first id should be less than the second
        body.data[0].id > body.data[1].id
    }

    void "test get"() {
        when:
        def resp = get("$path/1")
        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.OK.value()
        body.id
        body.name == 'Org1'
    }

    void "testing post"() {
        when:
        Response resp = post(path, [num: "foobie123", name: "foobie", type: "Customer"])

        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.CREATED.value()
        body.id
        body.name == 'foobie'
        delete(path, body.id)
    }

    void "testing post bad data"() {
        when:
        Response resp = post(path, [name: "foobie", type: "Customer"])

        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.UNPROCESSABLE_ENTITY.value()
        body.total == 1
        body.message == 'Org validation errors'
        body.errors[0].message == 'Property [num] of class [class yakworks.rally.orgs.model.Org] cannot be null'
    }

    void "testing put"() {
        when:
        Response resp = put(path, [name: "9Galt"], 1)

        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.OK.value()
        body.id
        body.name == '9Galt'

    }

    void "testing bulkUpdate"() {
        when:
        Response resp = post("$path/bulkUpdate", [ids: [1, 2], data: [name: "mass Updated"]])

        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.OK.value()
        body.data[0].name == 'mass Updated'
        body.data[1].name == 'mass Updated'

    }

    void "test countTotals"() {
        when:
        String  sums = "id,orgTypeId"
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getUrl("$path/countTotals")).newBuilder()
        urlBuilder.addQueryParameter("sums", sums)
        def resp = get(urlBuilder.build().toString())
        Map body = bodyToMap(resp)

        then:
        resp.code() == HttpStatus.OK.value()
        body.id == 5050
        body.orgTypeId == 110
    }

}
