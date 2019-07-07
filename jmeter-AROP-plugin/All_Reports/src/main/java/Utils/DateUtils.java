package Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author krishna.barri
 *
 */
public class DateUtils{
	public DateFormat formatter;

	public String getDateTime(long sMilliSeconds, String sDateTimeFormat) {
		formatter = new SimpleDateFormat(sDateTimeFormat);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(sMilliSeconds);
		return formatter.format(calendar.getTime());
	}
}
