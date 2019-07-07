package Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author krishna.barri
 *
 */
@SuppressWarnings({ "unchecked", "resource" })
public class JsonObjects {

	static String path = System.getProperty("user.dir");
	private static final Logger log = LoggerFactory.getLogger(JsonObjects.class);
	private static final String[] GRAPH_LABELS = { "x_time", "y_errors", "y_hits", "y_responsetime", "y_users", "endtime" };
	private static String start_time, end_time;
	private static long duration;
	private static final DateUtils dateUtils = new DateUtils();
	private static final String sDateTimeFormat = "dd/MM/yy HH:mm:ss";
	JSONArray array = new JSONArray();
	JSONObject json = new JSONObject();

	public JsonObjects() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void createAggregateReport(List<List<Object>> tableData, String[] columnNames) throws IOException {
		try {
			File reports_dir = new File(path + "/reports");
			if (!reports_dir.exists()) {
				reports_dir.mkdirs();
			}
			for (int i = 0; i < tableData.size(); i++) {
				JSONObject item = new JSONObject();
				for (int j = 0; j < tableData.get(i).size(); j++) {
					item.put(columnNames[j], tableData.get(i).get(j));
				}
				array.add(item);
			}
			json.put("Aggregate Report", array);
			log.info(json.toString());
			File agg_report = new File(path + "/reports/aggregate_report.json");
			if (!agg_report.exists()) {
				agg_report.createNewFile();
			}
			FileWriter file = new FileWriter(path + "/reports/aggregate_report.json");
			file.write(json.toString());
			file.flush();
			array.clear();
			json.clear();
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	public void createDashboardReport(ArrayList<GraphCoordinates> graphCoordinates) throws IOException {
		File reports_dir = new File(path + "/reports");
		if (!reports_dir.exists()) {
			reports_dir.mkdirs();
		}
		for (int i = 0; i < graphCoordinates.size(); i++) {
			JSONObject item = new JSONObject();

			item.put(GRAPH_LABELS[0], dateUtils.getDateTime(graphCoordinates.get(i).getsStartTime(), sDateTimeFormat));
			item.put(GRAPH_LABELS[1], graphCoordinates.get(i).getsErrors());
			item.put(GRAPH_LABELS[2], graphCoordinates.get(i).getsHits());
			item.put(GRAPH_LABELS[3], graphCoordinates.get(i).getsResponseTime());
			item.put(GRAPH_LABELS[4], graphCoordinates.get(i).getsUsers());
			item.put(GRAPH_LABELS[5], graphCoordinates.get(i).getsEndTime());
			
			array.add(item);
		}
		
		start_time = dateUtils.getDateTime(graphCoordinates.get(0).getsStartTime(), sDateTimeFormat);
		end_time = dateUtils.getDateTime(graphCoordinates.get(graphCoordinates.size()-1).getsEndTime(), sDateTimeFormat);
		System.out.println("start time: "+graphCoordinates.get(0).getsStartTime());
		System.out.println("end time: "+graphCoordinates.get(graphCoordinates.size()-1).getsEndTime());
		duration  = ((graphCoordinates.get(graphCoordinates.size()-1).getsEndTime() - graphCoordinates.get(0).getsStartTime())/1000)/60;
		if(duration<1){
			duration = 1;
		}
		else{
			duration = Math.round(duration);
		}
		json.put("start_time", start_time);
		json.put("end_time", end_time);
		json.put("duration", duration);
		json.put("Dashboard Report", array);
		File agg_report = new File(path + "/reports/dashboard_report.json");
		if (!agg_report.exists()) {
			agg_report.createNewFile();
		}
		FileWriter file = new FileWriter(path + "/reports/dashboard_report.json");
		file.write(json.toString());
		file.flush();
		array.clear();
		json.clear();
	}

}
