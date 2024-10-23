package org.adoxx.microservice.api.connectors.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonObject;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;

public class SMTPConnector extends SyncConnectorA {
    
    @Override
    public String getName() {
        return "SMTP Connector (Send Mail)";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Send Mails through a SMTP Server")
            .add("de", "Send Mails through a SMTP Server")
            .build();
    }
    
    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("host", Json.createObjectBuilder()
                .add("name", "Hostname")
                .add("description", Json.createObjectBuilder()
                    .add("en", "SMTP Server Hostname")
                    .add("de", "SMTP Server Hostname"))
                .add("value", ""))
            .add("port", Json.createObjectBuilder()
                .add("name", "Port")
                .add("description", Json.createObjectBuilder()
                    .add("en", "SMTP Server Port (default: 25, 465 SSL, 587 STARTTSL)")
                    .add("de", "SMTP Server Port (default: 25, 465 SSL, 587 STARTTSL)"))
                .add("value", ""))
            .add("fromMail", Json.createObjectBuilder()
                .add("name", "From")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The From mail address")
                    .add("de", "The From mail address"))
                .add("value", ""))
            .add("fromMailPassword", Json.createObjectBuilder()
                .add("name", "From Mail Password")
                .add("description", Json.createObjectBuilder()
                    .add("en", "The From mail password (optional)")
                    .add("de", "The From mail password (optional)"))
                .add("value", ""))
            .add("starttls", Json.createObjectBuilder()
                    .add("name", "STARTTLS")
                    .add("description", Json.createObjectBuilder()
                        .add("en", "STARTTLS Enabled (true/false) (default: false)")
                        .add("de", "STARTTLS Enabled (true/false) (default: false)"))
                    .add("value", "")
                    .add("moreInfos", Json.createObjectBuilder()
                        .add("choiceValues", Json.createArrayBuilder().add("false").add("true"))))
            .add("ssl", Json.createObjectBuilder()
                .add("name", "SSL")
                .add("description", Json.createObjectBuilder()
                    .add("en", "SSL Enabled (true/false) (default: false)")
                    .add("de", "SSL Enabled (true/false) (default: false)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder().add("false").add("true"))))
            .add("oauth2", Json.createObjectBuilder() //https://developers.google.com/oauthplayground/
                .add("name", "OAUTH2")
                .add("description", Json.createObjectBuilder()
                    .add("en", "OAUTH2 Autentication Enabled (true/false) (default: false)")
                    .add("de", "OAUTH2 Autentication Enabled (true/false) (default: false)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("choiceValues", Json.createArrayBuilder().add("false").add("true"))))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("to", Json.createObjectBuilder()
                .add("name", "To")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Comma separated list of 'To' mail addresses")
                    .add("de", "Comma separated list of 'To' mail addresses"))
                .add("value", ""))
            .add("cc", Json.createObjectBuilder()
                .add("name", "CC")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Comma separated list of 'CC' mail addresses")
                    .add("de", "Comma separated list of 'CC' mail addresses"))
                .add("value", ""))
            .add("bcc", Json.createObjectBuilder()
                .add("name", "BCC")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Comma separated list of 'BCC' mail addresses")
                    .add("de", "Comma separated list of 'BCC' mail addresses"))
                .add("value", ""))
            .add("subject", Json.createObjectBuilder()
                .add("name", "Subject")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Mail Subject")
                    .add("de", "Mail Subject"))
                .add("value", ""))
            .add("message", Json.createObjectBuilder()
                .add("name", "Message")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Mail Message")
                    .add("de", "Mail Message"))
                .add("value", ""))
            .add("attachment", Json.createObjectBuilder()
                .add("name", "Attachment")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Attachment (optional)")
                    .add("de", "Attachment (optional)"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("requireUpload", true)))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  dataMIME : 'text/plain'," + "\n"
                + "  dataText : 'OK'," + "\n"
                + "  moreInfo : {" + "\n"
                + "    executionTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
            ;
    }

    String host, port, fromMail, fromMailPassword, starttls, ssl, oauth2 = null;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        host = startConfiguration.getJsonObject("host")==null?"":startConfiguration.getJsonObject("host").getString("value", "");
        if(host.isEmpty()) throw new Exception("host not provided");
        port = startConfiguration.getJsonObject("port")==null?"":startConfiguration.getJsonObject("port").getString("value", "");
        fromMail = startConfiguration.getJsonObject("fromMail")==null?"":startConfiguration.getJsonObject("fromMail").getString("value", "");
        fromMailPassword = startConfiguration.getJsonObject("fromMailPassword")==null?"":startConfiguration.getJsonObject("fromMailPassword").getString("value", "");
        starttls = startConfiguration.getJsonObject("starttls")==null?"":startConfiguration.getJsonObject("starttls").getString("value", "");
        if(starttls.isEmpty()) starttls = "false";
        ssl = startConfiguration.getJsonObject("ssl")==null?"":startConfiguration.getJsonObject("ssl").getString("value", "");
        if(ssl.isEmpty()) ssl = "false";
        oauth2 = startConfiguration.getJsonObject("oauth2")==null?"":startConfiguration.getJsonObject("oauth2").getString("value", "");
        if(oauth2.isEmpty()) oauth2 = "false";
        
        if(port.isEmpty()) {
            port = "25";
            if(starttls.equals("true"))
                port = "587";
            if(ssl.equals("true"))
                port = "465";
        }
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        String to = configuration.getJsonObject("to")==null?"":configuration.getJsonObject("to").getString("value", "");
        String cc = configuration.getJsonObject("cc")==null?"":configuration.getJsonObject("cc").getString("value", "");
        String bcc = configuration.getJsonObject("bcc")==null?"":configuration.getJsonObject("bcc").getString("value", "");
        if(to.isEmpty() && cc.isEmpty() && bcc.isEmpty()) throw new Exception("To, CC or BCC not provided");
        String subject = configuration.getJsonObject("subject")==null?"":configuration.getJsonObject("subject").getString("value", "");
        String message = configuration.getJsonObject("message")==null?"":configuration.getJsonObject("message").getString("value", "");
        String attachment = configuration.getJsonObject("attachment")==null?"":configuration.getJsonObject("attachment").getString("value", "");
        
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", !fromMailPassword.isEmpty());
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.smtp.ssl.enable", ssl);
        if(oauth2.equals("true"))
            props.put("mail.smtp.auth.mechanisms", "XOAUTH2"); //https://github.com/bshannon/JavaMail/wiki/OAuth2
        
        Session session = Session.getInstance(props, null);
        //session.setDebug(true);
        MimeMessage mimeMessage = new MimeMessage(session);
        
        if(fromMail.isEmpty()) 
            mimeMessage.setFrom(); 
        else 
            mimeMessage.setFrom(fromMail);
        
        if(!to.isEmpty())
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        if(!cc.isEmpty())
            mimeMessage.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        if(!bcc.isEmpty())
            mimeMessage.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
        
        mimeMessage.setSubject(subject);
        
        if(attachment.isEmpty()) {
            mimeMessage.setText(message, "utf-8", "html");
        } else {
            MimeBodyPart mimeBodyPart1 = new MimeBodyPart();
            mimeBodyPart1.setText(message, "utf-8", "html");
            MimeBodyPart mimeBodyPart2 = new MimeBodyPart();
            mimeBodyPart2.attachFile(Utils.revealLocalFile(attachment));
            MimeMultipart mimeMultipart = new MimeMultipart();
            mimeMultipart.addBodyPart(mimeBodyPart1);
            mimeMultipart.addBodyPart(mimeBodyPart2);
            mimeMessage.setContent(mimeMultipart);
        }
        
        if(fromMailPassword.isEmpty())
            Transport.send(mimeMessage);
        else
            Transport.send(mimeMessage, fromMail, fromMailPassword);
        
        return Json.createObjectBuilder()
            .add("dataMIME", "text/plain")
            .add("dataText", "OK")
            .add("moreInfo", Json.createObjectBuilder()
                .add("executionTime", Utils.getCurrentTime())
            ).build();
    }
    
    @Override
    public void stop() throws Exception {
        host = null;
        port = null;
        fromMail = null;
        fromMailPassword = null;
        ssl = null;
        starttls = null;
        oauth2 = null;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        SMTPConnector connector = new SMTPConnector();
        Utils.uploadFolder = "";
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("host", Json.createObjectBuilder().add("value", "smtp.gmail.com"))
                .add("port", Json.createObjectBuilder().add("value", "465")) //ssl:465 tls:587
                .add("fromMail", Json.createObjectBuilder().add("value", "damiano.falcioni@gmail.com"))
                .add("fromMailPassword", Json.createObjectBuilder().add("value", "TO_PROVIDE"))  // https://developers.google.com/oauthplayground/
                .add("starttls", Json.createObjectBuilder().add("value", "false"))
                .add("ssl", Json.createObjectBuilder().add("value", "true"))
                .add("oauth2", Json.createObjectBuilder().add("value", "true"))
                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("to", Json.createObjectBuilder().add("value", ""))
                .add("cc", Json.createObjectBuilder().add("value", ""))
                .add("bcc", Json.createObjectBuilder().add("value", "damiano.falcioni@gmail.com"))
                .add("subject", Json.createObjectBuilder().add("value", "test"))
                .add("message", Json.createObjectBuilder().add("value", "aaaaaaah"))
                .add("attachment", Json.createObjectBuilder().add("value", ""))
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
