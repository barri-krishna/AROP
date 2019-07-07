package Jmeter.All_Reports;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.apache.jmeter.samplers.Clearable;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.SamplingStatCalculator;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;
import org.apache.jorphan.gui.NumberRenderer;
import org.apache.jorphan.gui.ObjectTableModel;
import org.apache.jorphan.gui.RateRenderer;
import org.apache.jorphan.gui.RendererUtils;
import org.apache.jorphan.reflect.Functor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Utils.DateUtils;
import Utils.GraphCoordinates;
import Utils.JsonObjects;

// @GUIMenuSortOrder(2)
/**
 * @author krishna.barri
 *
 */
@SuppressWarnings("unused")
public class AROP extends AbstractVisualizer implements Clearable, ActionListener {

	private static final long serialVersionUID = 240L;

	private static final String USE_GROUP_NAME = "useGroupName"; //$NON-NLS-1$

	private static final String SAVE_HEADERS = "saveHeaders"; //$NON-NLS-1$

	private ArrayList<GraphCoordinates> graphCoordinates = new ArrayList<GraphCoordinates>();

	private static final String sDateTimeFormat = "dd/MM/yy HH:mm:ss";

	private static final Logger log = LoggerFactory.getLogger(AROP.class);

	private static final JsonObjects jsonObjects = new JsonObjects();

	private final String TOTAL_ROW_LABEL = JMeterUtils.getResString("aggregate_report_total_label"); //$NON-NLS-1$

	private static ArrayList<SampleResult> GraphResults = new ArrayList<SampleResult>();

	private static final int REFRESH_PERIOD = JMeterUtils.getPropDefault("jmeter.gui.refresh_period", 500); // $NON-NLS-1$

	private JTable myJTable;

	private final JButton generateReports = new JButton("Generate Reports"); //$NON-NLS-1$

	private final JCheckBox saveHeaders = // should header be saved with the
											// data?
			new JCheckBox(JMeterUtils.getResString("aggregate_graph_save_table_header"), true); //$NON-NLS-1$

	private final JCheckBox useGroupName = new JCheckBox(JMeterUtils.getResString("aggregate_graph_use_group_name")); //$NON-NLS-1$

	private transient ObjectTableModel model;

	private static final String pct1Label = JMeterUtils.getPropDefault("aggregate_rpt_pct1", "90");

	private static final Float PCT1_VALUE = new Float(Float.parseFloat(pct1Label) / 100);

	/**
	 * Lock used to protect tableRows update + model update
	 */
	private final transient Object lock = new Object();

	private volatile boolean dataChanged;

	private final Map<String, SamplingStatCalculator> tableRows = new ConcurrentHashMap<>();

	private final Deque<SamplingStatCalculator> newRows = new ConcurrentLinkedDeque<>();

	private static final String[] COLUMNS_BEFORE_JM_2_13 = { "sampler_label", "aggregate_report_count", "average",
			"aggregate_report_min", "aggregate_report_max", "aggregate_report_90%_line", "aggregate_report_stddev",
			"aggregate_report_error%", "aggregate_report_rate", "aggregate_report_bandwidth", "average_bytes" };

	private static final String[] COLUMNS_AFTER_OR_EQUAL_JM_2_13 = { "sampler_label", "aggregate_report_count",
			"average", "aggregate_report_min", "aggregate_report_max", "aggregate_report_xx_pct1_line",
			"aggregate_report_stddev", "aggregate_report_error%", "aggregate_report_rate", "aggregate_report_bandwidth",
			"average_bytes" };

	private static boolean bOldVersion = false;

	private static final String[] COLUMNS = bOldVersion ? COLUMNS_BEFORE_JM_2_13 : COLUMNS_AFTER_OR_EQUAL_JM_2_13;
	
//	private static final String[] COLUMNS = { "sampler_label", //$NON-NLS-1$
//			"aggregate_report_count", //$NON-NLS-1$
//			"average", //$NON-NLS-1$
//			"aggregate_report_min", //$NON-NLS-1$
//			"aggregate_report_max", //$NON-NLS-1$
//			"aggregate_report_xx_pct1_line", 
//			"aggregate_report_stddev", //$NON-NLS-2$
//			"aggregate_report_error%", //$NON-NLS-1$
//			"aggregate_report_rate", //$NON-NLS-1$
//			"aggregate_report_bandwidth", //$NON-NLS-1$
//			"average_bytes", //$NON-NLS-1$
//	};

	static final Object[][] COLUMNS_MSG_PARAMETERS = { null, null, null, null, null, new Object[] { pct1Label }, null,
			null, null, null, null };
	
	// Column renderer's
	private static final TableCellRenderer[] RENDERERS = new TableCellRenderer[] { null, // Label
			null, // count
			null, // Mean
			null, // Min
			null, // Max
			null, // 90%
			new NumberRenderer("#0.00"), // Std Dev. //$NON-NLS-1$
			new NumberRenderer("#0.00%"), // Error %age //$NON-NLS-1$
			new RateRenderer("#.0"), // Throughput //$NON-NLS-1$
			new NumberRenderer("#0.00"), // kB/sec //$NON-NLS-1$
			new NumberRenderer("#.0"), // avg. pageSize //$NON-NLS-1$
	};

	// Column formats
	private static final Format[] FORMATS = new Format[] { null, // Label
			null, // count
			null, // Mean
			null, // Min
			null, // Max
			null, // 90%
			new DecimalFormat("#0.00"), // Std Dev. //$NON-NLS-1$
			new DecimalFormat("#0.00%"), // Error %age //$NON-NLS-1$
			new DecimalFormat("#.000"), // Throughput //$NON-NLS-1$
			new DecimalFormat("#0.00"), // kB/sec //$NON-NLS-1$
			new DecimalFormat("#.0"), // avg. pageSize //$NON-NLS-1$
	};

	public AROP() {
		super();
		model = new ObjectTableModel(COLUMNS, SamplingStatCalculator.class, // All
																			// rows
																			// have
																			// this
																			// class
				new Functor[] { new Functor("getLabel"), //$NON-NLS-1$
						new Functor("getCount"), //$NON-NLS-1$
						new Functor("getMeanAsNumber"), //$NON-NLS-1$
						new Functor("getMin"), //$NON-NLS-1$
						new Functor("getMax"), //$NON-NLS-1$
						new Functor("getPercentPoint", //$NON-NLS-1$
								new Object[] { PCT1_VALUE }),
						new Functor("getStandardDeviation"), //$NON-NLS-1$
						new Functor("getErrorPercentage"), //$NON-NLS-1$
						new Functor("getRate"), //$NON-NLS-1$
						new Functor("getKBPerSecond"), //$NON-NLS-1$
						new Functor("getAvgPageBytes"), //$NON-NLS-1$
				}, new Functor[] { null, null, null, null, null, null, null, null, null, null, null }, new Class[] { String.class, Integer.class, Long.class, Long.class, Long.class, Long.class, Double.class, Double.class, Double.class, Double.class,
						// Double.class,
						Double.class });
		clearData();
		init();
		new Timer(REFRESH_PERIOD, e -> {
			if (!dataChanged) {
				return;
			}
			dataChanged = false;
			synchronized (lock) {
				while (!newRows.isEmpty()) {
					model.insertRow(newRows.pop(), model.getRowCount() - 1);
				}
				model.fireTableDataChanged();
			}
		}).start();
	}

	/**
	 * @return <code>true</code> if all functors can be found
	 * @deprecated - only for use in testing
	 */
	@Deprecated
	public static boolean testFunctors() {
		AROP instance = new AROP();
		return instance.model.checkFunctors(null, instance.getClass());
	}

	@Override
	public String getLabelResource() {
		return null; // $NON-NLS-1$
	}

	@Override
	public String getStaticLabel() {
		return "AROP";
	}

	@Override
	public void add(final SampleResult res) {
		GraphResults.add(res);
		SamplingStatCalculator row = tableRows.computeIfAbsent(res.getSampleLabel(useGroupName.isSelected()), label -> {
			SamplingStatCalculator newRow = new SamplingStatCalculator(label);
			newRows.add(newRow);
			return newRow;
		});
		/*
		 * Synch is needed because multiple threads can update the counts.
		 */
		synchronized (row) {
			row.addSample(res);
		}
		SamplingStatCalculator tot = tableRows.get(TOTAL_ROW_LABEL);
		synchronized (lock) {
			tot.addSample(res);
		}
		dataChanged = true;
	}

	/**
	 * Clears this visualizer and its model, and forces a repaint of the table.
	 */
	@Override
	public void clearData() {
		// Synch is needed because a clear can occur while add occurs
		synchronized (lock) {
			model.clearData();
			newRows.clear();
			tableRows.clear();
			tableRows.put(TOTAL_ROW_LABEL, new SamplingStatCalculator(TOTAL_ROW_LABEL));
			model.addRow(tableRows.get(TOTAL_ROW_LABEL));
			GraphResults.clear();
			graphCoordinates.clear();
		}
		dataChanged = true;
	}

	/**
	 * Main visualizer setup.
	 */
	private void init() { // WARNING: called from ctor so must not be overridden
							// (i.e. must be private or final)
		this.setLayout(new BorderLayout());

		// MAIN PANEL
		JPanel mainPanel = new JPanel();
		Border margin = new EmptyBorder(10, 10, 5, 10);

		mainPanel.setBorder(margin);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		myJTable = new JTable(model);
		JTable myJTable = new JTable(model);
		myJTable.getTableHeader().setDefaultRenderer(new JMeterHeaderAsPropertyRenderer(COLUMNS_MSG_PARAMETERS));
		RendererUtils.applyRenderers(myJTable, RENDERERS);
		JScrollPane myScrollPane = new JScrollPane(myJTable);
		this.add(mainPanel, BorderLayout.NORTH);
		this.add(myScrollPane, BorderLayout.CENTER);
		generateReports.addActionListener(this);
		JPanel opts = new JPanel();
		opts.add(generateReports, BorderLayout.CENTER);
		this.add(opts, BorderLayout.SOUTH);
	}

	/**
	 * Renders items in a JTable by converting from resource names.
	 */
	private class JMeterHeaderAsPropertyRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 240L;
		private Object[][] columnsMsgParameters;

		/**
		 *
		 */
		public JMeterHeaderAsPropertyRenderer() {
			this(null);
		}

		/**
		 * @param columnsMsgParameters
		 *            Optional parameters of i18n keys
		 */
		public JMeterHeaderAsPropertyRenderer(Object[][] columnsMsgParameters) {
			super();
			this.columnsMsgParameters = columnsMsgParameters;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (table != null) {
				JTableHeader header = table.getTableHeader();
				if (header != null) {
					setForeground(header.getForeground());
					setBackground(header.getBackground());
					setFont(header.getFont());
				}
				setText(getText(value, row, column));
				setBorder(UIManager.getBorder("TableHeader.cellBorder"));
				setHorizontalAlignment(SwingConstants.CENTER);
			}
			return this;
		}

		/**
		 * Get the text for the value as the translation of the resource name.
		 *
		 * @param value
		 *            value for which to get the translation
		 * @param column
		 *            index which column message parameters should be used
		 * @param row
		 *            not used
		 * @return the text
		 */
		protected String getText(Object value, int row, int column) {
			if (value == null) {
				return "";
			}
			if (columnsMsgParameters != null && columnsMsgParameters[column] != null) {
				return MessageFormat.format(JMeterUtils.getResString(value.toString()), columnsMsgParameters[column]);
			} else {
				return JMeterUtils.getResString(value.toString());
			}
		}
	}
	
	@Override
	public void modifyTestElement(TestElement c) {
		super.modifyTestElement(c);
		c.setProperty(USE_GROUP_NAME, useGroupName.isSelected(), false);
		c.setProperty(SAVE_HEADERS, saveHeaders.isSelected(), true);
	}

	@Override
	public void configure(TestElement el) {
		super.configure(el);
		useGroupName.setSelected(el.getPropertyAsBoolean(USE_GROUP_NAME, false));
		saveHeaders.setSelected(el.getPropertyAsBoolean(SAVE_HEADERS, true));
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (ev.getSource() == generateReports) {
			try {
				log.info("Testing generate Reports: " + GraphResults.size());
				jsonObjects.createAggregateReport(getAllTableData(model, FORMATS), COLUMNS);
				createGraphResults(GraphResults);
			} catch (Exception e) {
				JMeterUtils.reportErrorToUser(e.getMessage(), "Error saving data");
			}
		}
	}

	/**
	 * We use this method to get the data, since we are using ObjectTableModel,
	 * so the calling getDataVector doesn't work as expected.
	 *
	 * @param model
	 *            {@link ObjectTableModel}
	 * @param formats
	 *            Array of {@link Format} array can contain null formatters in
	 *            this case value is added as is
	 * @return the data from the model
	 */
	public static List<List<Object>> getAllTableData(ObjectTableModel model, Format[] formats) {
		List<List<Object>> data = new ArrayList<>();
		if (model.getRowCount() > 0) {
			for (int rw = 0; rw < model.getRowCount(); rw++) {
				int cols = model.getColumnCount();
				List<Object> column = new ArrayList<>();
				data.add(column);
				for (int idx = 0; idx < cols; idx++) {
					Object val = model.getValueAt(rw, idx);
					if (formats[idx] != null) {
						column.add(formats[idx].format(val));
					} else {
						column.add(val);
					}
				}
			}
		}
		return data;
	}

	public void createGraphResults(ArrayList<SampleResult> graphResults) throws IOException {
		log.info("createGraphResults results size: " + graphResults.size());
		int error_count = 0, sample_count = 0;
		long avg_response = 0;
		for (int i = 0; i < graphResults.size(); i++) {
			if (graphResults.get(i).getSamplerData() != null) {
				error_count += graphResults.get(i).getErrorCount();
				sample_count += graphResults.get(i).getSampleCount();
				avg_response += graphResults.get(i).getLatency();
				graphCoordinates.add(new GraphCoordinates(avg_response / sample_count,
						graphResults.get(i).getStartTime(),
						Integer.toString(sample_count), Integer.toString(graphResults.get(i).getAllThreads()),
						Integer.toString(error_count), graphResults.get(i).getEndTime()));
			}
		}
		jsonObjects.createDashboardReport(graphCoordinates);
	}

}
