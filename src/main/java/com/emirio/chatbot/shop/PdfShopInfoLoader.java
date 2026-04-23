package com.emirio.chatbot.shop;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfShopInfoLoader {

    private ShopDetails shopDetails;

    @PostConstruct
    public void loadFromPdf() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("shop-info.pdf")) {
            if (is == null) {
                throw new RuntimeException("shop-info.pdf not found in classpath");
            }
            try (PDDocument document = Loader.loadPDF(is.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String pdfText = stripper.getText(document);
                shopDetails = parseTextToShopDetails(pdfText);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load shop info PDF", e);
        }
    }

    private ShopDetails parseTextToShopDetails(String text) {
        ShopDetails details = new ShopDetails();
        details.setShopName("Emirio Chaussures");
        details.setFounded("2010");
        details.setFounder("Nejaj Maalej");
        details.setSpeciality("Vente de chaussures et accessoires pour toute la famille (sport, chic, confort)");

        Pattern phonePattern = Pattern.compile("Téléphone / WhatsApp : (\\d+)");
        Matcher phoneMatcher = phonePattern.matcher(text);
        if (phoneMatcher.find()) details.setPhone(phoneMatcher.group(1));

        Pattern emailPattern = Pattern.compile("Email : (\\S+@\\S+)");
        Matcher emailMatcher = emailPattern.matcher(text);
        if (emailMatcher.find()) details.setEmail(emailMatcher.group(1));

        Pattern fbPattern = Pattern.compile("Facebook : (https://[\\S]+)");
        Matcher fbMatcher = fbPattern.matcher(text);
        if (fbMatcher.find()) details.setFacebook(fbMatcher.group(1));

        StringBuilder addresses = new StringBuilder();
        Pattern addressPattern = Pattern.compile("– (.*?)(?=\\n|$)", Pattern.MULTILINE);
        Matcher addressMatcher = addressPattern.matcher(text);
        while (addressMatcher.find()) {
            addresses.append(addressMatcher.group(1).trim()).append("\n");
        }
        details.setAddressDetails(addresses.toString().trim());

        return details;
    }

    public ShopDetails getShopDetails() {
        return shopDetails;
    }
}