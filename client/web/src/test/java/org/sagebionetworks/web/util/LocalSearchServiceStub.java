package org.sagebionetworks.web.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.server.ServerConstants;
import org.sagebionetworks.web.server.servlet.QueryStringUtils;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters;

/**
 * Stub version of SearchService
 * @author John
 *
 */
@Path("query")
public class LocalSearchServiceStub {
	
	private static Logger logger = Logger.getLogger(LocalSearchServiceStub.class.getName());
	
	public final static String NULL = "NULL";
	
	private static List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
	
	private Properties props;
	private ColumnConfigProvider columnConfigProvider;
	private List<ColumnInfo> validColumns;
	
	public LocalSearchServiceStub() throws IOException{
		props = ServerPropertiesUtils.loadProperties();
		String columnConfigFile = props.getProperty(ServerConstants.KEY_COLUMN_CONFIG_XML_FILE);
		// Create the column config from the classpath
		columnConfigProvider = new ColumnConfigProvider(columnConfigFile);
		// Determine what the valid columns are
		validColumns = new ArrayList<ColumnInfo>();
		Iterator<String> keyIt = columnConfigProvider.getKeyIterator();
		while(keyIt.hasNext()){
			// We only care about the base ColumnInfo here
			String columnKey = keyIt.next();
			HeaderData header = columnConfigProvider.get(columnKey);
			if(header instanceof ColumnInfo){
				validColumns.add((ColumnInfo)header);
			}
		}
	}


	@GET @Produces("application/json")
	public LinkedHashMap<String, Object> executeSearch(@QueryParam("query") String query) {
		logger.info(query);
		LinkedHashMap<String, Object> results = new LinkedHashMap<String, Object>();
		// Extract the query
		SearchParameters params = QueryStringUtils.parseQueryString(query);
		
		// First sort
		List<Map<String, Object>> fullList = ListUtils.getSortedCopy(params.getSort(), params.isAscending(), rows, Map.class);
		// Get the sub-set based on pagination
		List<Map<String, Object>> subList = ListUtils.getSubList(params.getOffset(), params.getLimit(), fullList);
		
		results.put(SearchService.KEY_TOTAL_NUMBER_OF_RESULTS, rows.size());
		results.put(SearchService.KEY_RESULTS, subList);
		return results;
	}
	
	// Generates random data
	@GET @Produces("text/html")
	@Path("/populate/random")
	public String generateRandomRows(@DefaultValue("25") @QueryParam("number") int number){
		// We get around threading issues by making a copy
		List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
		copy.addAll(rows);
		
		// Add to the list of datasts.
		for(int i=0; i<number; i++){
			// Add a new Row
			Map<String, Object> newRow = new HashMap<String, Object>();
			// Add random data for each valid column
			for(ColumnInfo column: validColumns){
				Object value = RandomColumnData.createRandomValue(column.getType());
				newRow.put(column.getId(), value);
			}
			// Add this row
			copy.add(newRow);
		}
		// the copy becomes the new list
		rows = copy;
		return "Total row count: "+Integer.toString(rows.size());
	}
	
	// clear all data
	@GET @Produces("text/html")
	@Path("/clear/all")
	public String clearAll(){
		// Replace the old list with a new list
		rows = new ArrayList<Map<String, Object>>();
		return "Total rows count: "+Integer.toString(rows.size());
	}


	/**
	 * Generate random data using the passed datsets name and id.
	 * @param list
	 */
	public void generateRandomRows(List<Dataset> list) {
		// We get around threading issues by making a copy
		List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
		copy.addAll(rows);
		
		// Add to the list of datasts.
		for(int i=0; i<list.size(); i++){
			Dataset dataset = list.get(i);
			// Add a new Row
			Map<String, Object> newRow = new HashMap<String, Object>();
			// Add random data for each valid column
			for(ColumnInfo column: validColumns){
				Object value = RandomColumnData.createRandomValue(column.getType());
				newRow.put(column.getId(), value);
			}
			// Use the dataset data for name and id
			newRow.put("name", dataset.getName());
			newRow.put("id", dataset.getId());
			// Add this row
			copy.add(newRow);
		}
		// the copy becomes the new list
		rows = copy;
	}
	

}
