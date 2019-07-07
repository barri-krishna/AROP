package Utils;

/**
 * @author krishna.barri
 *
 */
public class GraphCoordinates {

	private long sStartTime;
	private long lResponseTime;
	private String sHits;
	private String sUsers;
	private long sEndTime;

	public long getsEndTime() {
		return sEndTime;
	}

	public void setsEndTime(long sEndTime) {
		this.sEndTime = sEndTime;
	}

	public long getsStartTime() {
		return sStartTime;
	}

	public void setsStartTime(long sStartTime) {
		this.sStartTime = sStartTime;
	}

	public long getsResponseTime() {
		return lResponseTime;
	}

	public void setsResponseTime(long sResponseTime) {
		this.lResponseTime = sResponseTime;
	}

	public String getsHits() {
		return sHits;
	}

	public void setsHits(String sHits) {
		this.sHits = sHits;
	}

	public String getsUsers() {
		return sUsers;
	}

	public void setsUsers(String sUsers) {
		this.sUsers = sUsers;
	}

	public String getsErrors() {
		return sErrors;
	}

	public void setsErrors(String sErrors) {
		this.sErrors = sErrors;
	}

	private String sErrors;

	public GraphCoordinates(long lResponseTime, long sStartTime, String sHits, String sUsers, String sErrors,
			long sEndTime) {
		super();
		this.sStartTime = sStartTime;
		this.lResponseTime = lResponseTime;
		this.sHits = sHits;
		this.sUsers = sUsers;
		this.sErrors = sErrors;
		this.sEndTime = sEndTime;

	}

}
