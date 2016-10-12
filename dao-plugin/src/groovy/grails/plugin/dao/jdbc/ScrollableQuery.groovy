package grails.plugin.dao.jdbc

import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.springframework.jdbc.core.RowMapper

import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.Statement

/**
 * Groovy Sql wrapper for running scrollable/streaming queries.
 */
@CompileStatic
class ScrollableQuery {

	private String query
	private DataSource dataSource
	private RowMapper rowMapper

	public ScrollableQuery(String query, RowMapper mapper, DataSource dataSource) {
		this.query = query
		this.dataSource = dataSource
		this.rowMapper = mapper
	}

	/**
	 * Executes the query, and calls the closure for each row.
	 * @param Closure cl
	 */
	public void eachRow(Closure cl) {
		Sql sql = prepareSql()
		int index = 1

		sql.query(query) { ResultSet r ->
			while(r.next()) {
				index++
				def row = rowMapper.mapRow(r, index)
				cl.call(row)
			}
		}
	}

	/**
	 * Executes the query, and calls the closure for each batch.
	 * @param int batchSize
	 */
	public void eachBatch(int batchSize, Closure cl) {
		List batch = []
		this.eachRow { def row ->
			batch.add(row)
			if((batch.size() == batchSize)) {
				cl.call(batch)
				batch = []
			}
		}
		//there could be remaning rows
		if(batch.size() > 0) cl.call(batch)
	}

	/**
	 * Load all rows and return as a list, each row in the list would be converted using the passed RowMapper
	 * This method holds all rows in memory, so this should not be used if there is going to be large number of rows.
	 * instead use the eachRow, eachBatch which works with the scrollable resultset
	 *
	 * @return List
	 */
	public List rows() {
		List result = []

		this.eachRow {def row ->
			result.add(row)
		}

		return result
	}

	protected Sql prepareSql() {
		Sql sql = new Sql(dataSource)
		sql.resultSetConcurrency = java.sql.ResultSet.CONCUR_READ_ONLY
		sql.withStatement { Statement stmt -> stmt.fetchSize = Integer.MIN_VALUE }
		return sql
	}

}
