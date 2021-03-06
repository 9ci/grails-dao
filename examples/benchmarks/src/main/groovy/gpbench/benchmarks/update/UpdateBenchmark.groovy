package gpbench.benchmarks.update

import gpbench.basic.CityBasicRepo
import gpbench.basic.CityBasic
import gpbench.benchmarks.legacy.BaseBenchmark
import groovyx.gpars.GParsPool
import grails.gorm.transactions.Transactional

class UpdateBenchmark extends BaseBenchmark {

    CityBasicRepo cityRepo
    int poolSize

    /**
     * List with ids of saved records.
     */
    List<Long> ids

    UpdateBenchmark(boolean databinding) {
        super(databinding)
        this.ids = Collections.synchronizedList([])
    }

    @Override
    void setup() {
        super.setup()

        GParsPool.withPool(poolSize) {
            cities.eachParallel { Map record ->
                try {
                    ids.add(cityRepo.create(record).id)
                } catch (Exception e) {
                    e.printStackTrace()
                }
            }
        }
        assert CityBasic.count() == 37230
    }

    @Override
    def execute() {
        //reading records for the first time to copy them to second level cache
        ids.each { Long id -> cityRepo.get(id, null) }

        GParsPool.withPool(poolSize) {
            ids.eachParallel { Long id -> update(id) }
        }
    }
    @Transactional
    void update(Long id) {
        CityBasic.withNewSession {
            cityRepo.update([flush: true], [id: id, name: "cityId=${id}"])
        }
    }

    @Override
    String getDescription() {
        return "UpdateBenchmark"
    }

}
