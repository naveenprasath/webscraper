import org.apache.pdfbox.pdfviewer.ArrayEntry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by naveen on 9/19/2016.
 */
public class RebateChanges {
    static Properties mailServerProperties;
    static Session getMailSession;
    static MimeMessage generateMailMessage;
    static ArrayList<String> line = new ArrayList<>();
    static Date date = new Date();
    static Format formatter = new SimpleDateFormat("YYYY-MM-dd_hh-mm-ss");
    static String str2 = "";
    static String str3 = "";
    static String str4 = "";
    static String str5 = "";
    static String str6 = "";
    static String loc1 = "";
    static String loc2 = "";
    static int lineno = 0;
    static File file = new File("");
    static FileWriter fw;
    static StringBuilder sb = new StringBuilder();
    static {
        try {
            fw = new FileWriter("Audit\\audit" + formatter.format(date) + ".txt", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, MessagingException, KeyManagementException, NoSuchAlgorithmException, InterruptedException {
        webchanges();
        pdfchanges();
        difference(loc1, loc2);
        generateAndSendEmail();
    }

    public static void webchanges() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }

        } };

        SSLContext sc5 = SSLContext.getInstance("SSL");
        sc5.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc5.getSocketFactory());

        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };

        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        fw.append("Read Text from the URLs listed in urls.txt file and save contents in separate text file").append("\n");
        fw.append("***************************************************************************************").append("\n");
        String l = null;
        String url = null;
        FileWriter fw1 = new FileWriter("Contents\\URL_Contents\\URL_Contents" + formatter.format(date) + ".txt", true);
        Scanner sc = new Scanner(new File("urls.txt"));
        while (sc.hasNextLine()) {
            l = sc.nextLine();
            url = l.substring(l.indexOf("\t"));
            fw.append("Extracting text from - "+url).append("\n");
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:26.0) Gecko/20100101 Firefox/26.0")
                    .timeout(100000).ignoreHttpErrors(true).get();
            doc.select("script").remove();
            String text = doc.body().text().toString().trim().replaceAll("[\t]*\r?\n", "");
            fw1.append(l + " >>> " + text);
            fw1.append("\n");
        }
        fw1.close();
        fw.append("Text file created with the contents in the above URLs").append("\n");
        fw.append("Comparing the recent text Files for Changes").append("\n");
        File directory = new File("Contents\\URL_Contents");
        File[] files = directory.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f1.lastModified(), f2.lastModified());
            }
        });
        Scanner sc1 = new Scanner(new File("Contents\\URL_Contents\\"+files[files.length-1].getName()));
        Scanner sc2 = new Scanner(new File("Contents\\URL_Contents\\"+files[files.length-2].getName()));
        TreeMap<String, String> ldcurls = new TreeMap<>();
        int count = 0;
        fw.append("Writing the Changes to a string").append("\n");
        FileWriter ch1 = new FileWriter("Changes\\CurrentDay.txt");
        FileWriter ch2 = new FileWriter("Changes\\PreviousDay.txt");
        while(sc1.hasNextLine() && sc2.hasNextLine()) {
            String l1 = sc1.nextLine().trim().replaceAll(" +", " ");
            String l2 = sc2.nextLine().trim().replaceAll(" +", " ");
            if(!(l1.equals(l2))) {
                if(count == 0) {
                    sb.append("There are some changes in the following URLS");
                    sb.append("<br><br>");
                }
                count++;
                ch1.append(l1).append("\n");
                ch2.append(l2).append("\n");
                ldcurls.put(l1.substring(0,l1.indexOf("\t")), "<a href="+l1.substring(l1.indexOf("\t"), l1.indexOf(">>>")-1)+">"+l1.substring(0,l1.indexOf("\t"))+"</a>");
            }
        }
        Iterator it = ldcurls.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            sb.append(pair.getValue() + "<br>");
            it.remove();
        }
        ch1.close();
        ch2.close();
        File Folder = new File("Contents\\URL_Contents");
        File[] Changedfiles = Folder.listFiles();
        Arrays.sort(Changedfiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f1.lastModified(), f2.lastModified());
            }
        });
        loc1 = "Changes\\CurrentDay.txt";
        loc2 = "Changes\\PreviousDay.txt";
    }

    public static void difference(String loc1, String loc2) throws IOException, InterruptedException {
        String cmd = "cmd /c diffchecker "+ loc1 + " " + loc2 + " --expires 3 day";
        try
        {
            Process p=Runtime.getRuntime().exec(cmd);
            p.waitFor();
            BufferedReader reader=new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );
            String line;
            while((line = reader.readLine()) != null)
            {
                sb.append("<br>" + "The Changes are in this URL - " +line.substring(line.indexOf(":") + 1) + "<br>" + "Note - This URL expires in 3 days");
            }

        }
        catch(IOException e1) {}
        catch(InterruptedException e2) {}

        System.out.println("Done");
    }

    public static void pdfchanges() throws IOException {
        fw.append("Read Text from the PDF URLs listed in pdfs.txt file and save contents in separate text file").append("\n");
        fw.append("*******************************************************************************************").append("\n");
        String str = null;
        FileWriter fw1 = new FileWriter("Contents\\PDF_Contents\\PDF_Contents" + formatter.format(date) + ".txt", true);
        Scanner sc = new Scanner(new File("pdfs.txt"));
        while (sc.hasNextLine()) {
            String url = sc.nextLine();
            PDDocument pddDocument = PDDocument.load(new URL(url));
            PDFTextStripper textStripper = new PDFTextStripper();
            str = textStripper.getText(pddDocument).replace("\n", "").replace("\r", "");
            pddDocument.close();
            fw1.append(url +" >>> ").append(str).append("\n");
        }
        fw1.close();
        fw.append("Comparing the recent text Files for Changes").append("\n");
        File directory = new File("Contents\\PDF_Contents");
        File[] files = directory.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f1.lastModified(), f2.lastModified());
            }
        });
        Scanner sc1 = new Scanner(new File("Contents\\PDF_Contents\\"+files[files.length-1].getName()));
        Scanner sc2 = new Scanner(new File("Contents\\PDF_Contents\\"+files[files.length-2].getName()));
        ArrayList<String> ldcpdfs = new ArrayList<>();
        int count = 0;
        fw.append("Writing the Changes to a string").append("\n");
        while(sc1.hasNextLine() && sc2.hasNextLine()) {
            String l1 = sc1.nextLine().trim().replaceAll(" +", " ");
            String l2 = sc2.nextLine().trim().replaceAll(" +", " ");
            if(!(l1.equals(l2))) {
                if(count == 0) {
                    sb.append("There are some changes in the following PDFS");
                    sb.append("<br><br>");
                }
                count++;
                ldcpdfs.add(l1.substring(0, l1.indexOf(">>>")));
            }
        }
        Collections.sort(ldcpdfs);
        for(String urls : ldcpdfs) {
            sb.append(urls + "<br>");
        }
    }

    public static void generateAndSendEmail() throws AddressException, MessagingException, IOException {
        fw.append("Sending Email Alert").append("\n");
        fw.append("*******************").append("\n");
        fw.close();
        final String username = "naveen.prasath@intelent.com";
        final String password = "Int@NJ1854";

        Properties props = new Properties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Format formatter = new SimpleDateFormat("MM-dd-YYYY");
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("naveen.prasath@intelent.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("DIST_Structure_Supply@justenergy.com"));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse("suresh.selvarangan@intelent.com"));
            message.addRecipient(Message.RecipientType.CC, new InternetAddress("naveen.prasath@intelent.com"));
            message.setSubject("Rebate Updates");
            String emailBody = "<br><br><u><H3>Rebate Updates for "+formatter.format(date)+":-"+"</H3></u>"+"<br>"+sb.toString()+"<br>";
            Multipart multipart = new MimeMultipart();
            MimeBodyPart header = new MimeBodyPart();
            String headerText = "<img src = \"cid:image1\">"+"<br>"+emailBody;
            header.setContent(headerText, "text/html");
            multipart.addBodyPart(header);
            MimeBodyPart HeaderImage = new MimeBodyPart();
            DataSource fds1 = new FileDataSource("JustEnergy.jpg");
            HeaderImage.setDataHandler(new DataHandler(fds1));
            HeaderImage.setHeader("Content-ID", "<image1>");
            multipart.addBodyPart(HeaderImage);
            MimeBodyPart attach1 = new MimeBodyPart();
            String file1 = "Changes\\CurrentDay.txt";
            String fileName1 = "Changes\\CurrentDay.txt";
            DataSource source1 = new FileDataSource(file1);
            attach1.setDataHandler(new DataHandler(source1));
            attach1.setFileName(fileName1);
            String file2 = "Changes\\PreviousDay.txt";
            String fileName2 = "Changes\\PreviousDay.txt";
            DataSource source2 = new FileDataSource(file2);
            MimeBodyPart attach2 = new MimeBodyPart();
            attach2.setDataHandler(new DataHandler(source2));
            attach2.setFileName(fileName2);
            multipart.addBodyPart(attach1);
            multipart.addBodyPart(attach2);
            message.setContent(multipart);

            System.out.println("Sending");
            Transport.send(message);
            System.out.println("Done");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
