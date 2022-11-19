package com.signer;


import java.awt.geom.Rectangle2D;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import com.signature.CreateVisibleSignature;
import com.signature.CreateVisibleSignature2;
import org.apache.pdfbox.examples.signature.cert.CertificateVerificationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.interactive.form.PDVariableText;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPException;
import org.apache.pdfbox.Loader;

import static java.lang.Math.round;

public class Main {
    public static String soVanBan = "", ttfPath = "", name = "name", location = "location", imgPath = "", inputPath = "", outputPath = "", reason = "security", keystorePath = "", passphrase = "", mode = "addSignature";

    public static Integer page = 1, x = 0, y = 0, signatureLevel = 1, scale = -50, preferSize = 0, width = 400, xOffset = 0, yOffset = 0;

    public static Float fontSize = 20.0f;

    public static void parseParams(String[] args) throws IOException {
        for (int i = 0; i + 1 < args.length; i++) {
            System.out.println("\"" + args[i] + "\"" + " " + "\"" + args[i + 1] + "\"");
            if (args[i].equals("--input"))
                inputPath = args[++i];
            else if (args[i].equals("--output"))
                outputPath = args[++i];
            else if (args[i].equals("--x"))
                x = Integer.parseInt(args[++i]);
            else if (args[i].equals("--y"))
                y = Integer.parseInt(args[++i]);
            else if (args[i].equals("--page"))
                page = Integer.parseInt(args[++i]);
            else if (args[i].equals("--signatureLevel"))
                signatureLevel = Integer.parseInt(args[++i]);
            else if (args[i].equals("--preferSize"))
                preferSize = Integer.parseInt(args[++i]);
            else if (args[i].equals("--imgPath"))
                imgPath = args[++i];
            else if (args[i].equals("--name"))
                name = args[++i];
            else if (args[i].equals("--location"))
                location = args[++i];
            else if (args[i].equals("--reason"))
                reason = args[++i];
            else if (args[i].equals("--passphrase"))
                passphrase = args[++i];
            else if (args[i].equals("--keystorePath")) {
                keystorePath = args[++i];
            } else if (args[i].equals("--scale"))
                scale = Integer.parseInt(args[++i]);
            else if (args[i].equals("--fontSize"))
                fontSize = Float.parseFloat(args[++i]);
            else if (args[i].equals("--mode"))
                mode = args[++i];
            else if (args[i].equals("--ttfPath"))
                ttfPath = args[++i];
            else if (args[i].equals("--soVanBan"))
                soVanBan = args[++i];
            else if (args[i].equals("--width"))
                width = Integer.parseInt(args[++i]);
            else if (args[i].equals("--xOffset"))
                xOffset = Integer.parseInt(args[++i]);
            else if (args[i].equals("--yOffset"))
                yOffset = Integer.parseInt(args[++i]);
        }
    }

    public static void addSignature() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(keystorePath), passphrase.toCharArray());
        File destFile;
        try (FileInputStream fis = new FileInputStream(imgPath)) {
            CreateVisibleSignature signing = new CreateVisibleSignature(keyStore, passphrase.toCharArray());
            signing.setVisibleSignDesigner(inputPath, x + xOffset, y + yOffset, scale, fis, page);
            signing.setVisibleSignatureProperties(name, location, reason, preferSize, page, true);
            signing.setExternalSigning(false);
            destFile = new File(outputPath);
            signing.signPDF(new File(inputPath), destFile, null, signatureLevel);
        }
    }


    public static void fillSoVanBanForm() throws IOException {
        System.out.println("fillSoVanBanForm");

        PDDocument document = Loader.loadPDF(new File(inputPath));
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm != null) {
            PDField field = acroForm.getField("soVanBan");
            if (field != null) {
                field.setValue(soVanBan);
            }
        }
        FileOutputStream fos = new FileOutputStream(outputPath);
        document.saveIncremental(fos);
    }

    public static void addSoVanBanForm() throws IOException {
        System.out.println("addSoVanBanForm");

        PDDocument document = Loader.loadPDF(new File(inputPath));

        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        PDPage page = document.getDocumentCatalog().getPages().get(0);
        if (form == null) {
            form = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(form);
        }

        PDResources resources = form.getDefaultResources();
        if (resources == null) {
            resources = new PDResources();
        }

        PDFont font = PDType0Font.load(document, new FileInputStream(ttfPath), false);
        COSName fontName = resources.add(font);
        form.setDefaultResources(resources);

        PDTextField textField = new PDTextField(form);
        textField.setPartialName("soVanBan");

        String defaultAppearance = "/" + fontName.getName() + " " + fontSize + " Tf 0 0 0 rg";
        textField.setDefaultAppearance(defaultAppearance);

        form.getFields().add(textField);

        PDAnnotationWidget widget = textField.getWidgets().get(0);
        float pageHeight = page.getMediaBox().getHeight();
        PDRectangle rect = new PDRectangle(round(x - width / 2), pageHeight - (fontSize + 8) / 2 - y, width, fontSize + 8);
        widget.setRectangle(rect);
        widget.setPage(page);

        widget.setPrinted(true);

        page.getAnnotations().add(widget);

        // set the alignment ("quadding")
        textField.setQ(PDVariableText.QUADDING_LEFT);

        // set the field value
        textField.setValue("{Số văn bản}");

        FileOutputStream fos = new FileOutputStream(new File(outputPath));
        document.saveIncremental(fos);
    }

    public static void signWithText() throws IOException, GeneralSecurityException {
        // 0,0 means most left and top
//        inputPath = "resources/Baocaohoatdongnam.pdf";
//        keystorePath = "resources/certificate.p12";
//        outputPath = "resources/outhello.pdf";
//        ttfPath = "resources/times.ttf";
//        soVanBan = "kasdj lkasjdlk jaslkjd jlkasjdlkasjd lkajd \nad kajdslk jaslskdj  llkasjdlkasj dlkaj d";
//        fontSize = 14f;
        String tsaUrl = null;
        // External signing is needed if you are using an external signing service, e.g. to sign
        // several files at once.
        boolean externalSig = true;

        File ksFile = new File(keystorePath);
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(ksFile))
        {
            keystore.load(is, passphrase.toCharArray());
        }

        File documentFile = new File(inputPath);

        CreateVisibleSignature2 signing = new CreateVisibleSignature2(keystore, passphrase.toCharArray());

        File signedDocumentFile;
        signedDocumentFile = new File(outputPath);

        signing.setExternalSigning(externalSig);
        signing.setTtfPath(ttfPath);
        signing.setFontSize(fontSize);
        signing.setVisibleText(soVanBan);
        // Set the signature rectangle
        // Although PDF coordinates start from the bottom, humans start from the top.
        // So a human would want to position a signature (x,y) units from the
        // top left of the displayed page, and the field has a horizontal width and a vertical height
        // regardless of page rotation.
        Rectangle2D humanRect = new Rectangle2D.Float(x + xOffset, y + yOffset, width, fontSize + 10);

        signing.signPDF(documentFile, signedDocumentFile, humanRect, tsaUrl);
    }

    public static void main(String[] args)
            throws IOException, CMSException, OperatorCreationException, GeneralSecurityException,
            TSPException, CertificateVerificationException {
        parseParams(args);
//        mode = "signWithText";
        if (mode.equals("addSignature")) addSignature();
        else if (mode.equals("addSoVanBanForm")) addSoVanBanForm();
        else if (mode.equals("fillSoVanBanForm")) fillSoVanBanForm();
        else if (mode.equals("signWithText")) signWithText();
    }
}