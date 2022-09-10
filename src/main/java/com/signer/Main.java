package com.signer;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.examples.signature.CreateVisibleSignature;
import org.apache.pdfbox.examples.signature.cert.CertificateVerificationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.interactive.form.PDVariableText;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPException;
import org.apache.pdfbox.Loader;

public class Main {
    public static String name = "name", location = "location", imgPath = "", inputPath = "", outputPath = "", reason = "security", keystorePath = "", passphrase = "";

    public static Integer page = 1, x = 0, y = 0, signatureLevel = 2, scale = -50, preferSize = 0;

    public static void parseParams(String[] args) throws IOException {
//--imgPath /home/ben19024/workspace/java-sign/resources/001.0068.png --keystorePath /home/ben19024/workspace/java-sign/resources/certificate.p12 --scale -80 --input /home/ben19024/workspace/java-sign/resources/2.pdf --output /home/ben19024/workspace/java-sign/test-output/test.out.pdf
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

        }
    }

    public static void main(String[] args)
            throws IOException, CMSException, OperatorCreationException, GeneralSecurityException,
            TSPException, CertificateVerificationException {
//        parseParams(args);
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
//        keyStore.load(new FileInputStream(keystorePath), passphrase.toCharArray());
//        File destFile;
//        try (FileInputStream fis = new FileInputStream(imgPath)) {
//            CreateVisibleSignature signing = new CreateVisibleSignature(keyStore, passphrase.toCharArray());
//            signing.setVisibleSignDesigner(inputPath, x, y, scale, fis, page);
//            signing.setVisibleSignatureProperties(name, location, reason, preferSize, page, true);
//            signing.setExternalSigning(false);
//            destFile = new File(outputPath);
//            signing.signPDF(new File(inputPath), destFile, null);
//        }
        PDDocument document = Loader.loadPDF(new File("resources/3.pdf"));

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
        PDFont font = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
        COSName fontName = resources.add(font);
//        resources.put(COSName.getPDFName("Helv"), font);
        form.setDefaultResources(resources);

        PDTextField textField = new PDTextField(form);
        textField.setPartialName("SampleField");

        String defaultAppearance = "/" +fontName.getName()+ " 10 Tf 0 0 1 rg";
        textField.setDefaultAppearance(defaultAppearance);

        form.getFields().add(textField);

        PDAnnotationWidget widget = textField.getWidgets().get(0);
        PDRectangle rect = new PDRectangle(450, 350, 200, 50);
        widget.setRectangle(rect);
        widget.setPage(page);

//        PDAppearanceCharacteristicsDictionary fieldAppearance
//                = new PDAppearanceCharacteristicsDictionary(new COSDictionary());
//        fieldAppearance.setBorderColour(new PDColor(new float[]{0,1,0}, PDDeviceRGB.INSTANCE));
//        fieldAppearance.setBackground(new PDColor(new float[]{1,1,0}, PDDeviceRGB.INSTANCE));
//        widget.setAppearanceCharacteristics(fieldAppearance);

        widget.setPrinted(true);

        page.getAnnotations().add(widget);

        // set the alignment ("quadding")
        textField.setQ(PDVariableText.QUADDING_LEFT);

        // set the field value
        textField.setValue("Sample field content");

        FileOutputStream fos = new FileOutputStream(new File("resources/4.pdf"));
        document.saveIncremental(fos);
    }
}