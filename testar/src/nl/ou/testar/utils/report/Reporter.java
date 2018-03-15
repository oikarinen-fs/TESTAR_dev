package nl.ou.testar.utils.report;

import java.io.File;
import java.text.SimpleDateFormat;

import nl.ou.testar.utils.consumer.Consumer;
import nl.ou.testar.utils.consumer.ConsumerImpl;

/**
 * Reporter class. 
 *
 */
public class Reporter {
	
	private static final Reporter INSTANCE = new Reporter();
	/**
	 * Standard name of report file.
	 */
	public static final String STANDARD_FILE_NAME = "output" + File.separator + "report_" + new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(System.currentTimeMillis()) + ".csv";
	
	
	private Consumer consumer;
	
	/**
	 * Constructor.
	 */
	private Reporter(){ 
		consumer = new ConsumerImpl(1);
	}
	
	/**
	 * Get singleton instance.
	 * @return instance
	 */
	public static Reporter getInstance(){
		return INSTANCE;
	}	
	
	/**
	 * Report.
	 * @param data to be reported data
	 */
	public void report(String data){
		report(STANDARD_FILE_NAME, data);
	}

	/**
	 * Report.
	 * @param fileName name of report file
	 * @param data to be reported data
	 */
	public void report(String fileName, String data){
		consumer.consume(new ReportItem(fileName, data, true));
	}

	/**
	 * Finish report.
	 */
	public void finish(){
		consumer.finishConsumption();
	}	
	
}
