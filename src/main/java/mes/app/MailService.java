package mes.app;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;

@Service
public class MailService {


    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String usernm, String uuid, String content){
        String subject = content + " 인증 메일입니다.";
        String text = "안녕하세요, " + usernm + "님.\n\n"
                + "다음 인증 코드를 입력하여 " + content + "을 완료하세요:\n"
                + uuid + "\n\n"
                + "이 코드는 3분 동안 유효합니다.";

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom("replusshare@naver.com");

        mailSender.send(message);
    }

    public void sendMailWithAttachment(String recipient, String subject, String body, File attachment, String attachmentFileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, true);

            FileSystemResource file = new FileSystemResource(attachment);
            helper.addAttachment(attachmentFileName, file);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("메일 전송 실패");
        }
    }

}
