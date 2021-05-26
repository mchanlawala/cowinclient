package cowinclient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import com.google.gson.internal.LinkedTreeMap;

public class CowinClientApp1 {

	private InputStream inputStream;
	private static Properties prop = null;

	public void getPropValues() throws IOException {
		prop = new Properties();
		String propFileName = "application1.properties";
		inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

		if (inputStream != null) {
			prop.load(inputStream);
		} else {
			throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.initialize();
		while (true) {
			try {
				int totalCnt = 0;
				CowinClientApp1 app = new CowinClientApp1();
				app.getPropValues();

				Map<Integer, Integer> pinCodes = getPinCods();

				LocalDate startDate = LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd")
						.format(new SimpleDateFormat("dd-MM-yyyy").parse(prop.getProperty("cowin.start.date"))));
				LocalDate endDate = LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd")
						.format(new SimpleDateFormat("dd-MM-yyyy").parse(prop.getProperty("cowin.end.date"))));
				List<LinkedTreeMap<?, ?>> selectedCenters = new ArrayList<LinkedTreeMap<?, ?>>();
				String districtId = prop.getProperty("cowin.district_id");
				// String mailCentersInfo = "";
				LocalDate date = startDate.minusDays(1);
				List<Future<String>> results = new ArrayList<>();
				while ((date = date.plusDays(1)).isBefore(endDate.plusDays(1))) {
					try {
						restresultNull: {
							System.out.println(totalCnt);
							System.out.println("district_id : " + districtId);
							System.out.println("Date : " + date);
							Future<String> result = executor.submit(new CowinClientExecutor1(prop, date, districtId));
							Thread.sleep(2000l);
							String resultStr = result.get();
							System.out.println(resultStr);
							if (resultStr == null) {
								System.out.println("Waitting time start ....");
								Thread.sleep(1 * 30 * 1000);
								System.out.println("Waitting time end ....");
								break restresultNull;
							}

							totalCnt = totalCnt + 1;
							// results.add(result);
							// app.findingCenterInformation(date, pincode);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						try {
							Thread.sleep(10000l);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				// for (Future<String> result : results) {
				// System.out.println(result.get());
				// }
				// if (!selectedCenters.isEmpty()) {
				// app.sendEmail(mailHeader + mailCentersInfo + mailFooter);
				// }
				Thread.sleep(30 * // minutes to sleep
						60 * // seconds to a minute
						1000); // milliseconds to a second
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private static Map<Integer, Integer> getPinCods() {
		Map<Integer, Integer> pinCodes = new LinkedHashMap<Integer, Integer>();
		for (Entry<Object, Object> propKey : prop.entrySet()) {
			if (propKey.getKey().toString().contains("cowin.start.pincode.start.option")
					&& !propKey.getKey().toString().endsWith("count")) {
				System.out.println(propKey.getKey());
				pinCodes.put(Integer.parseInt(propKey.getValue().toString()),
						Integer.parseInt(prop.get(propKey.getKey() + ".count").toString()));
			}
		}
		return pinCodes;
	}

}
