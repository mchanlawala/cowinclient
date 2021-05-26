package cowinclient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.gson.internal.LinkedTreeMap;

public class CowinClientExecutor1 implements Callable<String> {

	private LocalDate date;
	private String pincode;
	private Properties prop;

	public CowinClientExecutor1(Properties prop, LocalDate date, String pincode) {
		this.prop = prop;
		this.date = date;
		this.pincode = pincode;
	}

	@Override
	public String call() throws Exception {
		try {
			String mailHeader = "<h1>Co-Win - Center Information</h1>\r\n" + "<table>\r\n" + "	<tr>\r\n"
					+ "		<th>Name</th>\r\n" + "		<th>Address</th>\r\n" + "		<th>Pincode</th>\r\n"
					+ "		<th>Date</th>\r\n" + "		<th>available capacity</th>\r\n"
					+ "		<th>TimeSlot</th>\r\n" + "	</tr>\r\n";
			String mailFooter = "</table>";
			String mailCentersInfo = "";
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
			headers.add("user-agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
			HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

			ResponseEntity<?> response = restTemplate.exchange(
					prop.getProperty("cowin.client.url") + "?district_id=" + pincode + "&" + "date="
							+ date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
					HttpMethod.GET, entity, Object.class);
			LinkedTreeMap<?, ?> responseTree = (LinkedTreeMap<?, ?>) response.getBody();
			for (LinkedTreeMap<?, ?> center : (List<LinkedTreeMap<?, ?>>) responseTree.get("centers")) {
				System.out.println(center);
				for (LinkedTreeMap<?, ?> session : (List<LinkedTreeMap<?, ?>>) center.get("sessions")) {
					BigDecimal availableCapacity = new BigDecimal(session.get("available_capacity").toString());
					BigDecimal minAge = new BigDecimal(session.get("min_age_limit").toString());
					BigDecimal availableCapacityDose1 = new BigDecimal(
							session.get("available_capacity_dose1").toString());
					if (availableCapacity.intValue() > 0 && minAge.intValue() >= 18
							&& availableCapacityDose1.intValue() > 0) {

						System.out.println("Center Name : " + center.get("name"));
						System.out.println("Center Address : " + center.get("address"));
						System.out.println("Center Pincode : " + center.get("pincode"));
						System.out.println("date : " + session.get("date"));
						System.out.println("available capacity : " + session.get("available_capacity"));
						System.out.println("available vaccine : " + session.get("vaccine"));
						System.out.println("available slots : " + session.get("slots"));

						mailCentersInfo =mailCentersInfo + "<tr>" + "<td>" + center.get("name") + "</td>" + "<td>"
								+ center.get("address") + "</td>" + "<td>" + center.get("pincode") + "</td>" + "<td>"
								+ session.get("date") + "</td>" + "<td>" + session.get("available_capacity") + "</td>"
								+ "<td>" + session.get("vaccine") + "</td>" + "<td>" + session.get("slots")
								+ "</td></tr>";
						sendEmail(mailHeader + mailCentersInfo + mailFooter);
					}
				}
			}
			
			return mailHeader + mailCentersInfo + mailFooter;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			// latch.countDown();
		}
		return null;
	}

	private void sendEmail(String emailInfo) {
		try {
			String tos = prop.getProperty("emailTo");
			final String from = prop.getProperty("emailFrom") ;

			Properties properties = new Properties();
			properties.put("mail.smtp.starttls.enable", "true");
			properties.put("mail.smtp.auth", "true");
			properties.put("mail.smtp.host", "smtp.gmail.com");
			properties.put("mail.smtp.port", "587");

			Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(from, prop.getProperty("emailFromPassword"));
				}
			});

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			InternetAddress[] addresses = new InternetAddress[tos.split(";").length];
			int cnt = 0;
			for (String email : tos.split(";")) {
				addresses[cnt] = new InternetAddress(email);
				cnt++;
			}
			message.addRecipients(Message.RecipientType.TO, addresses);
			message.setSubject("Co-Win - Center Information");
			message.setContent(emailInfo, "text/html");

			Transport.send(message);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


}
