package gpbench.traits

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import gorm.tools.beans.Pager
import gorm.tools.transaction.WithTrx
import gorm.tools.databinding.BindAction
import gorm.tools.mango.DefaultMangoQuery
import gorm.tools.mango.MangoBuilder
import gorm.tools.mango.MangoTidyMap
import gorm.tools.repository.model.PersistableRepoEntity
import gpbench.Region
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import grails.web.databinding.WebDataBinding
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.core.Session

@CompileStatic
abstract class BenchProcessData implements BenchConfig, WithTrx  {

    @Autowired
    @Qualifier("mangoQuery")
    DefaultMangoQuery mangoQuery

    //default for insert, override for updates
    Closure bindAndSaveClosure = { Class<?> domainClass, Map row ->
        bindAndSave((GormEntity)domainClass.newInstance(), row)
    }

    Closure repoBatchClosure = { List batchList, Map args ->
        getRepository().batchCreate(batchList)
    }

    abstract void run()

    void processData(Class domainClass, String binderType, String benchKey) {
        processData( domainClass, data(), binderType, benchKey)
    }

    void processData(Class domainClass, List<Map> dataMap, String binderType, String benchKey) {
        this.binderType = binderType
        this.benchKey = benchKey
        setRepo(domainClass)
        runAndRecord(domainClass,dataMap){
            //cleanup and remove the inserted data
            if(createAction.startsWith('save')){
                if(createAction == 'save batch') {
                    saveBatch(domainClass,dataMap)
                } else if(createAction == 'save async') {
                    saveAsync(domainClass,dataMap)
                }
            } else {
                //it's a createAction of create, update or validate so just spin through.
                for (Map row : dataMap) {

                    getBindAndSaveClosure().call(domainClass, row)
                }
            }
        }
        cleanup(domainClass, dataMap.size())
    }

    void saveAsync(Class domainClass, List dataMap){
        //collates list into list of lists
        List<List> collatedList = asyncSupport.collate(dataMap)
        if(binderType == 'gorm-tools-repo'){
            asyncSupport.parallel(collatedList, getRepoBatchClosure())
        } else {
            asyncSupport.parallelBatch(collatedList) { Map row, Map zargs ->
                getBindAndSaveClosure().call(domainClass, row)
            }
        }
    }

    void saveBatch(Class<?> domainClass, List<Map> dataMap, Closure rowClosure = getBindAndSaveClosure()){
        List<List<Map>> collatedList = dataMap.collate(batchSize)
        for (List<Map> batchList : collatedList) {
            binderType == 'gorm-tools-repo' ? getRepoBatchClosure().call(batchList, [:]) :
                saveBatchChunkTx(domainClass, batchList, rowClosure)
        }
    }

    @Transactional
    void saveBatchChunkTx(Class<?> domainClass, List<Map> dataMap, Closure rowClosure = getBindAndSaveClosure()){
        //println "saveBatchChunkTx $domainClass with ${data.size()} data items "
        for (Map row : dataMap) {
            rowClosure.call(domainClass, row)
        }
        flushAndClear(domainClass)
    }

    void flushAndClear(Class domainClass){
        Session session = GormEnhancer.findStaticApi(domainClass).datastore.currentSession
        session.flush() //probably redundant
        session.clear() //clear the cache
    }

    void bindAndSave(GormEntity instance, Map row) {
        //rowClosure.call(instance, row)
        if(binderType == 'grails') {
            //logMessage "insertRow setProperties"
            ((WebDataBinding)instance).setProperties(row)
        } else if(binderType.startsWith('gorm-tools')) {
            //println "row"
            //println row
            repository.bind(instance, row, BindAction.Create)
            // println "instance.latitude ${instance["latitude"]}"
            //entityMapBinder.bind(instance, row)
        } else if(binderType == 'settersStatic') {
            instance.invokeMethod('setProps', row)
        } else if(binderType == 'settersDynamic') {
            setterDynamic(instance, row)
        }
        else if(binderType == 'settersDynamic-useGet') {
            setterDynamic(instance, row, true)
        }

        if(createAction == 'validate') {
            assert instance.validate()
        } else if(createAction.startsWith('save')) {
            if(binderType.startsWith('gorm-tools')) {
                ((PersistableRepoEntity)instance).persist()
                //instance.save([failOnError:true])
            } else {
                instance.save([failOnError:true])
            }
        }

        //Region.query(name: 'foo', 'country.name': 'bar')
        //mangoQuery.query(Region,[name: 'foo', 'country.name': 'bar'], null)
        //testQuery3(name: 'foo', 'country.name': 'bar')
        //testQuery5(name: 'foo', 'country.name': 'bar')
    }

    @CompileDynamic
    void testQuery(){
        Region.createCriteria().list {
            eq 'name', 'foo'
            country {
                eq 'name', 'bar'
            }
        }
    }

    static Map q2 = MangoTidyMap.tidy([name: 'foo', 'country.name': 'bar'] as Map<String, Object>)

    //@CompileDynamic
    void testQuery2(){
        DetachedCriteria<Region> detachedCriteria = new DetachedCriteria<Region>(Region)
        DetachedCriteria<Region> newCriteria = MangoBuilder.cloneCriteria(detachedCriteria)
        new MangoBuilder().applyMapOrList(newCriteria, q2)
        newCriteria.list()
    }

    void testQuery3(Map map){
        //Map q2 = [name: 'foo', 'country.name': 'bar'] as Map<String, Object>
        DetachedCriteria<Region> detachedCriteria = new DetachedCriteria<Region>(Region)
        DetachedCriteria<Region> newCriteria = MangoBuilder.cloneCriteria(detachedCriteria)
        new MangoBuilder().applyMapOrList(newCriteria, MangoTidyMap.tidy(map))
        newCriteria.list()
    }

    void testQuery4(Map map){
        Map<String, Object> p = mangoQuery.parseParams(map)
        DetachedCriteria dcrit = mangoQuery.query(Region, p.criteria as Map, null)
        mangoQuery.list(dcrit, p.pager as Pager)
        //DetachedCriteria criteria = MangoBuilder.build(Region, map, null)
        //criteria.list(max: 10, offset: 0)
    }

    //static DetachedCriteria dcrit = MangoBuilder.build(Region, [name: 'foo', 'country.name': 'bar'], null)

    void testQuery5(Map map){
        //Map<String, Map> p = mangoQuery.parseParams(map)
        DetachedCriteria dcrit = mangoQuery.query(Region, map, null) //MangoBuilder.build(Region, map, null)
        dcrit.list()
        //mangoQuery.query(dcrit, map)
        //DetachedCriteria criteria = MangoBuilder.build(Region, map, null)
        //criteria.list(max: 10, offset: 0)
    }

    void doSettersStatic(Class domainClass, String benchKey = 'setters static'){
        processData(domainClass, data(), 'settersStatic', benchKey)
    }

    void doSettersDynamic(Class domainClass, String benchKey = 'setters dynamic'){
        processData(domainClass, data(), 'settersDynamic', benchKey)
    }

    void doGormToolsRepo(Class domainClass, String benchKey = 'gorm-tools: repository batch methods'){
        processData(domainClass, data(), 'gorm-tools-repo', benchKey)
    }

    void doGormToolsRepoPersist(Class domainClass, String benchKey = 'gorm-tools: fast binder & persist'){
        processData(domainClass, data(), 'gorm-tools-persist', benchKey)
    }

    //defaults to insert clean up, override for updates
    @CompileDynamic
    void cleanup(Class domainClass, Integer dataSize) {
        if(createAction.startsWith('save')){
            Integer rowCount = GormEnhancer.findStaticApi(domainClass).count()
            // assert dataSize == rowCount
            withTrx { status ->
                domainClass.executeUpdate("delete from ${domainClass.getSimpleName()}".toString())
                flushAndClear(status)
            }
        }
    }

    abstract void setterDynamic(instance, row, useGet = false)
}
