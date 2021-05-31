Gorm-tools provides a convenient way for iterating over records which correspond to a given SQL query.

## Mango Overview

The primary motive here is to create an easy dynamic way to query via a rest api or using a simple map.
The repository comes with a `query(criteriaMap, closure)` method. It allows to get paged list of entities restricted by
the properties in the `criteriaMap`. The map could be passed as JSON string or Map. All restrictions should be under 
`criteria` keyword by default, but it can be changed with [config](queries-mango.md#configuration).

Anything in the optional closure will be passed into Gorm/Hibernate criteria closure

* A lot of inspiration was drawn from [Restdb.io]{.new-tab}
* the query language is similar to [Mongo's]{.new-tab}
* and CouchDB's new [Mango selector-syntax]{.new-tab}
* Also inspired by [json-sql]{.new-tab}

> :memo: 
Whilst selectors have many similarities with MongoDB query documents, 
these arise more from a similarity of purpose and do not necessarily extend to commonality of function or result.

**Basic example**

``` java
Org.repo.query([
  criteria: [name: "Bill%", type: "New"],
  sort: [name:"asc"],
  max: 20
]){
  gt "id", 5
}

```

The same result can be reached with criteria:

```javascript
Criteria criteria = Org.createCriteria()
criteria.list(max: 20) {
    ilike "name", "Bill%"
    eq "type", "New"
    gt "id", 5
    order("name", "asc")
}
```

### Restful API query

See the docs here for more examples and info https://yakworks.github.io/gorm-rest-api/

### Mango QL

The Mango query language is similar to [Mongo's][mongo]
and CouchDB's new [Mango selector-syntax][couchDB]
with some inspiration from [json-sql][json-sql] as well

#### Logical

|  Op  |      Description      |                                              Examples                                              |
| ---- | --------------------- | -------------------------------------------------------------------------------------------------- |
| $and | default               | `$and: [ {"name": "Belak"}, {"status": "D"} ]` <br> equivalent to `"name": "Belak", "status": "D"` |
| $or  | "ors" them all        | `$or: [ {"name": "Belak"}, {"fork": true} ]` <br> `$or: {"name": "Belak", "fork": true }`          |
| $not | ALL not equal, !=, <> | `$not:{ "status": "Destroyed", "dateStatus": "2371" }`                                             |
| $nor | ANY one is not equal  | `$nor:{ "name": "Romulan", "fork": 12`}                                                               |

#### Comparison

|     Op     |           Description            |                   Example                    |
| ---------- | -------------------------------- | -------------------------------------------- |
| $gt        | >  greater than                  | `"cargo": {"$gt": 10000}`                    |
| $gte       | >= greater than or equal         | `"cargo": {"$gte": 10000}`                   |
| $lt        | <  less than                     | `"cargo": {"$lt": 10000}`                    |
| $lte       | <= less than or equal            | `"cargo": {"$lte": 10000}`                   |
| $between   | between two distinct values      | `"dateStatus": {"$between": [2300, 2400]}`   |
| $like      | like expression                  | `"name": {"$like": "Rom%"}`                  |
| $ilike     | like auto-append %               | `"name": {"$ilike": "rom"}`                  |
| $eq        | = equal, concieince for builders | `"salary": {"$eq": 10}` \| `"salary": 10`    |
| $ne        | not equal, !=, <>                | `"age" : {"$ne" : 12}}`                      |
| $in        | Match any value in array         | `"field" : {"$in" : [value1, value2, ...]`   |
| $nin       | Not match any value in array     | `"field" : {"$nin" : [value1, value2, ...]}` |
| $isNull    | Value is null                    | `"name": "$isNull" \|  `"name": null         |
| $isNotNull | Value is not null                | `"name": "$isNotNull" \| `"name":{$ne: null} |

#### Fields

|  Op   |    Description    |                Example                 |
| ----- | ----------------- | -------------------------------------- |
| $gtf  | >  another field  | `"cargo": {"$gtf": "maxCargo"}`        |
| $gtef | >= field          | `"cargo": {"$gtef": "maxCargo"}`       |
| $ltf  | <  field          | `"cargo": {"$ltf": "maxCargo"}`        |
| $ltef | <= field          | `"cargo": {"$ltef": "maxCargo"}`       |
| $eqf  | = field           | `"cargo": {"$eqf": "controlTotal"}`    |
| $nef  | not equal, !=, <> | `"cargo" : {"$nef" : "controlTotal"}}` |

#### Convert to Mango QL

The [MangoTidyMap utility][MangoTidyMap]
transforms parameters map to Mango QL criteria map. 
Here is several examples of usage:

```java
assert MangoTidyMap.tidy([a:5]) == [a:['$eq':5]]
assert MangoTidyMap.tidy([b:"Bill%"]) == [b:['$ilike':5]]
assert MangoTidyMap.tidy([c: [1,2,3]]) == [c:['$in':[1,2,3]]]

```

One of the useful features of the MangoTidyMap is that it converts path to nested map which is required by [mangoQuery](queries-mango.md#mango-query)  
```java
assert MangoTidyMap.tidy['a.b.c': 3]) == [a:[b:[c:['$eq':3]]]]
```
For more example take a look on [MangoTidyMapSpec][MangoTidyMapSpec] 

### Mango query

The repository by default does not have all the features of mango service, just `query` and `queryList` methods, but both automatically
convert map params to mango map, so just `params` or `request.JSON` can be passed. 

See [Mango Api](queries-mango.md#mango-api) to know all features that are provided.

#### Examples

Bellow will be a list of supported syntax for params in json format, which is supported:
Assume we are running these on star trek characters http://stapi.co/api/v1/rest/character?uid=CHMA0000128908

``` js
{
  "criteria": {
    "name": "Kira%", /* if it ends with % then it will us an ilike */
    "gender": "F", //no % its straight up
    "placeOfBirth": {"$ilike": "bajor%"}, /* a case-insensitive 'like' expression appends the % */
    "hologram": true, /* boolean */
    "createdDate": "1993-05-16T00:00:00.000Z", // dates
    "dateOfBirth": "1957-07-26" // dates
    "placeOfBirth": {"$eqf": "$placeOfDeath"} //equals another field in set
  },
  "sort":"name" // asc by default
}
```

This would produce in a round about way with criteria builders a where clause like this

```sql
  .. name like "Kira%" AND gender="F" AND placeOfBirth like "bajor%" AND hologram = true
  AND createdDate = ??? AND dateOfBirth = ??? AND placeOfBirth = placeOfDeath
  ORDER BY name ASC, dateOfBirth DESC;
```


#### Associations
```js
{
  "criteria": {
    "customer.id": 101,
    "customerId": 101, /* check if domain has customerId property, if not then uses customer.id 101 above */
    "customer": { /* nested object way */
      "id": 101,
      "name": "Wal%"
    }
  }
  "sort": {
    "customer.name": "asc",
    "tranDate": "desc"
  }
}
```

#### IN Clause
```js
{
  "criteria": {
    "customer.id": [101,102,103], /* an array means it will use in/inList */
    "customer": [{"id":101},{"id":102},{"id":103}], //can be in summarized object form as well
    "customer.id": [101,102,103], /* an array means it will use in/inList */
    //the 3 above are different ways to do this
    "customer.id": {"$in": [101,102,103]},
    "customer": {
      "id": {"$in": [101,102,103]}
    },

    "customer.id": {"$nin": [101,102,103]}, /* an array means it will use `not { in/inList }`*/
  }
}
```


#### Fields Comparison Examples
```js
  "amount": {"$ne": 50}, /*not equal*/
  "amount": {"$gt": 100}, /* greater than value */
  "amount.$gt": 100 /* another form of the above one, can be useful when json is build, for example from angular model, where  you can't right in object form*/
  "amount": {"$ge": 100}, /* greater or equal than value */

  "amount": {"$lt": "$origAmount"}, /* less than value of another field*/
  "amount": {"$le": "$origAmount"}, /* less or equal than value */

  "amount":{ //all these will get anded together
    "$gt": 5.0,
    "$lt": 15.50,
    "$ne": 9.99
  },

  "amount": {"$between": [0,100]}, /* between value */

  "status": "$isNull" /* translates to isNull*/
  "status": {"$isNull": true}, /* translates to isNull*/
  "status": {"$isNull": false}, /* translates to not{ isNull}*/
  "status": null /* translates to isNull*/
```

#### Logical operators
```js
    "$or": { // if a single or then it can be done like this
      "customer.name":{"$ilike": "wal"},
      "customer.num":{"$ilike": "wal"}
    },
    "$and":[ // multiple ors would need to look like this in an array. only one and can be present too
      {
        "$or": {
          "customer.name": "John",
          "customer.name": "Jon"
        }
      },
      {
        "$or": {
          "customer.name": "Mark",
          "customer.name": "Marc"
        }
      }
    ], /* this would end up generating `.... and ( (customer.name = 'John' or customer.name = 'Jon')
          AND (customer.name = 'Mark' or customer.name = 'Mark') ) ....` */

    "$or":[ // again you can only have one of these
      { // the and is default and optional and this accomplishes the same thing as example sbelow
        "customer.name": "Mark",
        "$or": {
          "customer.sales": {"$lt": 10},
          "customer.sales": "$isNull"
        }
      },
      {
        "$and": { //the and can be explicitly specified too if you wish
          "customer.name": "Jim",
          "customer.sales": {"$lt": 15}
        }
      },
    ], /* this would end up generating
        ....
        AND
        (
          (customer.name = 'mark' and ( customer.sales < 10 or customer.sales IS NULL))
          OR
          (customer.name = 'jim' and customer.sales < 15 )
        )
        .... */
  }
}

```
### Q or Quick Search

Q search - similiar to a search engine search that uses a `q` paramter, 
ability to search by one string in criteria filters against several domain fields, the value for quick
search can be passed in `$qSearch` or `$q` keywords. 
There are 2 ways to specify fields that should be searched against:

1. in static property `qSearchIncludes` as list of strings, see example bellow:
```groovy
class Org {
	String name
    Address address

    static qSearchIncludes = ["name", "address.city"]
    ...

```
1. default fields in config
```yaml
gorm:
    tools:
        mango:
            qSearchDefault: [name, num, address.city]

```

Mango criteria will add `%` automatically, if quick search string doesn't have it and will apply `ilike` statement
for each field in `qSearchIncludes`. If domain field is not string type, then `eq` statement will be used.

```groovy
Org.repo.search([criteria: [$q: "abc"], max: 20])

```
So it is the same as:

```groovy
Criteria criteria = Org.createCriteria()
criteria.list(max: 20) {
    or {
        ilike "name", "abc%"
        ilike "address.city", "abc%"
    }
}
```

### Configuration

The default `criteria` keyword for the restriction map can be changed in config:
```yml
gorm:
    tools:
        mango:
            criteriaKeyName: filters
```

With such configuration restrictions for Mango criteria should be under `filters` keyword.


### Mango API

#### Mango in the Repository
[The Repository](repository/ref.md) implements [MangoQueryTrait] Trait
which contains implementation for two query methods:

```groovy
DetachedCriteria query( Map params=[:], Closure closure=null) {
        mangoQuery.query(getDomainClass(), params, closure)
    }
```
see docs for [DetachedCriteria][DetachedCriteria]

```groovy
 List queryList(Map params=[:], Closure closure=null){
         mangoQuery.queryList(getDomainClass(), params, closure)
     }
```
returns list of entities with pagination. For pagination take a look at [Pager][Pager]

If one need to override mango bean for a certain repo it can be achieved in two ways:

1. implement `getMangoQuery()` method that should return instance of the class that implements [MangoQueryApi]

1. register a new bean for custom criteria, and set it for the repo with ``@Qualifier`` annotation

```java
beans = {
    newMangoQuery(NewMangoQuery) 
}

```

```java
class NewMangoQuery implements MangoQueryApi {

    @Override
    DetachedCriteria query(Class domainClass, Map params, Closure closure = null) {
       return null
    }

    @Override
    List queryList(Class domainClass, Map params, Closure closure = null) {
       return []
    }
}
```

```java
class CityRepo extends DefaultGormRepo<City> {

    @Autowired
    @Qualifier("newMangoQuery")
    NewMangoQuery mangoQuery
}
```

 
#### Build Mango criteria

Under the hood Mango uses [DetachedCriteria][DetachedCriteria] take a look on main [build method][build method]
One can see that it normalizes params map to make it Mango QL and then adds restrictions to detached criteria based on map,
if closure is passed then applies it too. 

### Count totals

If one needs to compute totals for some fields, [MangoQuery][MangoQuery] 
has `countTotals` method. Restrictions for it are working in the same way as for query method, so it can be specified 
with params map and criteria closure.

But the repository beans don't contain this method, so one can call it on mangoQuery bean.
To specify what fields sums should be computed for, the list with fields name should be passed.
See example:
```groovy
Org.repo.mangoQuery.countTotals(domainClass, [
  criteria: [name: "Virgin%", type: "New"]
], ["amount", "credit"]){
  gt "id", 5
}
```
Result will be look like: `[amount: 1500, credit: 440]`, it doesn't take into account pagination.


## ScrollableQuery
See [ScrollableQuery][ScrollableQuery]

### Execute a closure for each record

As you can see in the example below, we can specify the SQL query and provide the closure which is called for each record:

```groovy
    ScrollableQuery scrollableQuery = new ScrollableQuery(new ColumnMapRowMapper(), dataSource, 50)

    scrollableQuery.eachRow("select * from ScrollableQueryTest") { Object row ->
        println row
    }
```

### Execute a closure for each batch of records

Using ```eachBatch``` we can execute a closure for a batch of records.
This closure is called for a specified number of records. For example, code below prints size of each batch
(which is 5) to console:

```groovy

    scrollableQuery.eachBatch("select * from ScrollableQueryTest", 5) { List batch ->
        println "batchSize=${batch.size()}"
    }

```

### Fetching a list of all records:

> :memo: This method holds all rows in memory, so this should not be used if there is going to be large number of rows.

```groovy

    List values = scrollableQuery.rows("select * from ScrollableQueryTest where value='test'")

```

## GrailsParameterMapRowMapper

See [GrailsParameterMapRowMapper][GrailsParameterMapRowMapper]

Row mapper which allows to convert data from a given ResultSet instance
to a grails parameter map, which can be used for databinding.

[mongo]:https://docs.mongodb.com/manual/reference/operator/query/
[couchDB]:http://docs.couchdb.org/en/latest/api/database/find.html#selector-syntax
[json-sql]:https://github.com/2do2go/json-sql/
[MangoTidyMap]:https://yakworks.github.io/gorm-tools/api/gorm/tools/mango/MangoTidyMap.html
[MangoTidyMapSpec]:https://github.com/yakworks/gorm-tools/blob/master/plugin/src/test/groovy/gorm/tools/mango/MangoTidyMapSpec.groovy 
[MangoQueryRepo]:https://yakworks.github.io/gorm-tools/api/gorm/tools/mango/api/MangoQueryApi.html
[DetachedCriteria]:http://gorm.grails.org/latest/hibernate/manual/index.html#detachedCriteria
[Pager]:https://yakworks.github.io/gorm-tools/api/gorm/tools/Pager.html
[build method]:https://github.com/yakworks/gorm-tools/blob/master/plugin/src/main/groovy/gorm/tools/mango/MangoBuilder.groovy#L73
[ScrollableQuery]:https://yakworks.github.io/gorm-tools/api/gorm/tools/jdbc/ScrollableQuery.html
[GrailsParameterMapRowMapper]:https://yakworks.github.io/gorm-tools/api/gorm/tools/jdbc/GrailsParameterMapRowMapper.html
[MangoQueryApi]:https://yakworks.github.io/gorm-tools/api/gorm/tools/mango/api/MangoQueryApi.html
[MangoQuery]:https://yakworks.github.io/gorm-tools/api/gorm/tools/mango/MangoQuery.html
[MangoQueryTrait]: https://yakworks.github.io/gorm-tools/api/gorm/tools/mango/api/MangoQueryTrait.html
[Restdb.io]:https://restdb.io/docs/querying-with-the-api
[Mongo's]:https://docs.mongodb.com/manual/reference/operator/query/
[Mango selector-syntax]:http://docs.couchdb.org/en/latest/api/database/find.html#selector-syntax
[json-sql]:https://github.com/2do2go/json-sql/
