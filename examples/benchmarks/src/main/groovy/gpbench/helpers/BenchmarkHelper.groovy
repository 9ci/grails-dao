package gpbench.helpers

import gorm.tools.async.AsyncBatchSupport
import gorm.tools.repository.api.RepositoryApi
import gpbench.Country
import gpbench.Region
import grails.core.GrailsApplication
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.stereotype.Component

import javax.sql.DataSource
import java.sql.Connection

@Component
@CompileStatic
class BenchmarkHelper {

    @Autowired
    JdbcTemplate jdbcTemplate
    @Autowired
    GrailsApplication grailsApplication
    @Autowired
    DataSource dataSource
    @Autowired
    AsyncBatchSupport asyncBatchSupport
    @Autowired
    CsvReader csvReader

    @CompileDynamic
    void initBaseData() {
        truncateTables()
        executeSqlScript("test-tables.sql")
        List<List<Map>> countries = csvReader.read("Country").collate(batchSize)
        List<List<Map>> regions = csvReader.read("Region").collate(batchSize)
        insert(countries, Country.repo)
        insert(regions, Region.repo)

        assert Country.count() == 275
        assert Region.count() == 3953
    }

    void insert(List<List<Map>> batchList, RepositoryApi repo) {
        asyncBatchSupport.parallel(batchList) { List<Map> list, Map args ->
            repo.batchCreate(list)
        }
    }

    void executeSqlScript(String file) {
        Resource resource = grailsApplication.mainContext.getResource("classpath:$file")
        assert resource.exists()
        Connection connection = dataSource.getConnection()
        ScriptUtils.executeSqlScript(connection, resource)
    }

    void truncateTables() {
        jdbcTemplate.update("DELETE FROM origin")
        jdbcTemplate.update("DELETE FROM city")
        jdbcTemplate.update("DELETE FROM region")
        jdbcTemplate.update("DELETE FROM country")
    }
}
