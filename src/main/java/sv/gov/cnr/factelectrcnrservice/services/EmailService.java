package sv.gov.cnr.factelectrcnrservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import sv.gov.cnr.factelectrcnrservice.entities.Transaccion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;



@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final DteTransaccionService dteTransaccionService;

    @Value("${configuracion.smtp.email}")
    private String correoEmisor;
    @Value("${configuracion.smtp.password}")
    private String correoPassword;
    @Value("${configuracion.smtp.host}")
    private String host;
    @Value("${configuracion.smtp.port}")
    private String port;
    @Value("${configuracion.smtp.auth}")
    private String auth;
    @Value("${configuracion.smtp.starttls-enable}")
    private String starttls;
    @Value("${configuracion.smtp.trust}")
    private String trust;
    @Value("${configuracion.ambiente}")
    private String ambiente;


    private final TemplateEngine templateEngine;
    public static final String ASUNTO = "FACTURACIÓN ELECTRÓNICA CNR";
    public static final String UTF_8_ENCODING = "UTF-8";
    public static final String EMAIL_TEMPLATE = "plantillaEmail";
    public static final String TEXT_HTML_ENCONDING = "text/html";


    public Boolean sendDteViaEmail(Transaccion transaccion, ByteArrayOutputStream dtePDF, String correo){
        String placeholderCorreo = correoEmisor;
        if(correo == null){
            correo = placeholderCorreo;
        }

        var infoDte = dteTransaccionService.findDteTransaccionByTransaccion(transaccion);
        Properties prop = new Properties();
        prop.put("mail.smtp.host", host);
        prop.put("mail.smtp.port", port);
        prop.put("mail.smtp.auth", auth);
        prop.put("mail.smtp.starttls.enable", starttls);
        prop.put("mail.smtp.ssl.trust", trust);


        Session session = Session.getInstance(prop, new jakarta.mail.Authenticator() {
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(correoEmisor, correoPassword);
            }
        });



        //Session session = Session.getInstance(prop);

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(correoEmisor));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correo));
            message.setSubject(ASUNTO);

            Context context = new Context();
            context.setVariable("textAnulado", "");
            if (ambiente.equalsIgnoreCase("00")) {
                context.setVariable("msjPrueba","Le informamos que este correo ha sido enviado con el " +
                        "prop&oacute;sito de realizar pruebas en nuestro sistema de facturaci&oacute;n electr&oacute;nica.");
            }else{
                context.setVariable("msjPrueba","");
            }
            MimeBodyPart leyenda = new MimeBodyPart();
            //leyenda.setContent(new StringBuilder().append("El Centro Nacional de Registro (CNR) \n").append("ha emitido\n").append("el siguiente Documento Tributario Electrónico -DTE-\n").append(" con la siguiente información").toString(), "text/html");
            String bodyEmail = templateEngine.process(EMAIL_TEMPLATE, context);
            leyenda.setContent(bodyEmail, TEXT_HTML_ENCONDING);

            // ENVIAR IMAGEN AL CUERPO DEL CORREO
            BodyPart imageBodyPart = new MimeBodyPart();
            try {

                Resource resource = new ClassPathResource("templates/Logo-CNR2.png"); // Ruta relativa a resources
                InputStream imageStream = resource.getInputStream();
                DataSource dataSource = new ByteArrayDataSource(imageStream, "image/png"); // Ajusta el tipo de contenido
                // según el formato de tu imagen
                imageBodyPart.setDataHandler(new DataHandler(dataSource));
                imageBodyPart.setHeader("Content-ID", "image");
            }catch (MessagingException e){
                log.error("Error al incrustar imagen en el correo " +e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            MimeBodyPart pdfDteAdjunto = new MimeBodyPart();

            ByteArrayDataSource dataSource = new ByteArrayDataSource(dtePDF.toByteArray(), "application/pdf");
            pdfDteAdjunto.setDataHandler(new DataHandler(dataSource));
            pdfDteAdjunto.setFileName(infoDte.getCodigoGeneracion());

            Map<String, Object> map = new LinkedHashMap<>();
            ObjectMapper objectMapper = new ObjectMapper();
            // Parseamos el String JSON a un LinkedHashMap manteniendo el orden
            map = objectMapper.readValue(infoDte.getDteJson(), LinkedHashMap.class);

            // Agregamos los nuevos atributos "sello" y "firma"
            map.put("firma", infoDte.getDteJsonFirmado());
            map.put("sello", infoDte.getSelloRecibidoMh());

            // Convertimos el mapa nuevamente a un String JSON
            String jsonConAtributosAdicionales = objectMapper.writeValueAsString(map);

            MimeBodyPart jsonDteAdjunto = new MimeBodyPart();
            DataSource jsonDataSource = new ByteArrayDataSource(jsonConAtributosAdicionales.getBytes(StandardCharsets.UTF_8), "application/json");
            jsonDteAdjunto.setDataHandler(new DataHandler(jsonDataSource));
            jsonDteAdjunto.setFileName(infoDte.getCodigoGeneracion());


            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(imageBodyPart);
            multipart.addBodyPart(leyenda);
            multipart.addBodyPart(pdfDteAdjunto);
            multipart.addBodyPart(jsonDteAdjunto);

            message.setContent(multipart);

            Transport.send(message);

            log.info("Correo enviado exitosamente");
            return true;

        } catch (MessagingException e) {
            log.error("Error al enviar correo: "+ e.getMessage());
            return false;
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }


    public Boolean sendDteViaEmailAnulado(Transaccion transaccion, ByteArrayOutputStream dtePDF, String correo){
        String placeholderCorreo = correoEmisor;
        if(correo == null){
            correo = placeholderCorreo;
        }

        var infoDte = dteTransaccionService.findDteTransaccionByTransaccion(transaccion);
        Properties prop = new Properties();
        prop.put("mail.smtp.host", host);
        prop.put("mail.smtp.port", port);
        prop.put("mail.smtp.auth", auth);
        prop.put("mail.smtp.starttls.enable", starttls);
        prop.put("mail.smtp.ssl.trust", trust);


        Session session = Session.getInstance(prop, new jakarta.mail.Authenticator() {
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(correoEmisor, correoPassword);
            }
        });



        //Session session = Session.getInstance(prop);

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(correoEmisor));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correo));
            message.setSubject(ASUNTO);

            Context context = new Context();
            context.setVariable("textAnulado", " Anulado");

            MimeBodyPart leyenda = new MimeBodyPart();
            //leyenda.setContent(new StringBuilder().append("El Centro Nacional de Registro (CNR) \n").append("ha emitido\n").append("el siguiente Documento Tributario Electrónico -DTE-\n").append(" con la siguiente información").toString(), "text/html");
            String bodyEmail = templateEngine.process(EMAIL_TEMPLATE, context);
            leyenda.setContent(bodyEmail, TEXT_HTML_ENCONDING);

            // ENVIAR IMAGEN AL CUERPO DEL CORREO
            BodyPart imageBodyPart = new MimeBodyPart();
            try {

                Resource resource = new ClassPathResource("templates/Logo-CNR2.png"); // Ruta relativa a resources
                InputStream imageStream = resource.getInputStream();
                DataSource dataSource = new ByteArrayDataSource(imageStream, "image/png"); // Ajusta el tipo de contenido
                // según el formato de tu imagen
                imageBodyPart.setDataHandler(new DataHandler(dataSource));
                imageBodyPart.setHeader("Content-ID", "image");
            }catch (MessagingException e){
                log.error("Error al incrustar imagen en el correo " +e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            MimeBodyPart pdfDteAdjunto = new MimeBodyPart();

            ByteArrayDataSource dataSource = new ByteArrayDataSource(dtePDF.toByteArray(), "application/pdf");
            pdfDteAdjunto.setDataHandler(new DataHandler(dataSource));
            pdfDteAdjunto.setFileName(infoDte.getCodigoGeneracion());

            MimeBodyPart jsonDteAdjunto = new MimeBodyPart();
            DataSource jsonDataSource = new ByteArrayDataSource(infoDte.getDteJson().getBytes(StandardCharsets.UTF_8), "application/json");
            jsonDteAdjunto.setDataHandler(new DataHandler(jsonDataSource));
            jsonDteAdjunto.setFileName(infoDte.getCodigoGeneracion());


            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(imageBodyPart);
            multipart.addBodyPart(leyenda);
            multipart.addBodyPart(pdfDteAdjunto);
            multipart.addBodyPart(jsonDteAdjunto);

            message.setContent(multipart);

            Transport.send(message);

            log.info("Correo enviado exitosamente");
            return true;

        } catch (MessagingException e) {
            log.error("Error al enviar correo: "+ e.getMessage());
            return false;
        }

    }

}
