package com.signature;

import com.signature.cert.CertificateVerificationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MyRunner {
    private static final String IN_DIR = "resources/";
    private static final String OUT_DIR = "test-output/";
    private static final String KEYSTORE_PATH = IN_DIR + "certificate.p12";
    private static final String JPEG_PATH = IN_DIR + "1.png";
    private static final String PASSWORD = "";
    private static final String TSA_RESPONSE = "tsa_response.asn1";
    private static final String SIMPLE_FORM_FILENAME = "target/TestCreateSignatureSimpleForm.pdf";

    private static CertificateFactory certificateFactory = null;
    private static KeyStore keyStore = null;
    private static Certificate certificate;
    private static String tsa;

    private String getOutputFileName(String filePattern, boolean externallySign)
    {
        return MessageFormat.format(filePattern, (externallySign ? "_ext" : ""));
    }

    //    @ParameterizedTest
//    @MethodSource("signingTypes")
    public void testCreateVisibleSignature(boolean externallySign)
            throws IOException, CMSException, OperatorCreationException, GeneralSecurityException,
            TSPException, CertificateVerificationException
    {
        // sign PDF
        keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(KEYSTORE_PATH), PASSWORD.toCharArray());
        String inPath = IN_DIR + "2.pdf";
        File destFile;
        try (FileInputStream fis = new FileInputStream(JPEG_PATH))
        {
            CreateVisibleSignature signing = new CreateVisibleSignature(keyStore, PASSWORD.toCharArray());
            System.out.println(signing);
            signing.setVisibleSignDesigner(inPath, 0, 0, -50, fis, 1);
            signing.setVisibleSignatureProperties("name", "location", "Security", 0, 1, true);
            signing.setExternalSigning(externallySign);
            destFile = new File(OUT_DIR + getOutputFileName("signed{0}_visible.pdf", externallySign));
            signing.signPDF(new File(inPath), destFile, null);
        }

        checkSignature(new File(inPath), destFile, false);
    }

    private void checkSignature(File origFile, File signedFile, boolean checkTimeStamp)
            throws IOException, CMSException, OperatorCreationException, GeneralSecurityException,
            TSPException, CertificateVerificationException
    {
        String origPageKey;
        try (PDDocument document = Loader.loadPDF(origFile))
        {
            // get string representation of pages COSObject
            origPageKey = document.getDocumentCatalog().getCOSObject().getItem(COSName.PAGES).toString();
            System.out.println(origPageKey);
        }
        try (PDDocument document = Loader.loadPDF(signedFile))
        {
            // early detection of problems in the page structure
            int p = 0;
            PDPageTree pageTree = document.getPages();
            for (PDPage page : document.getPages())
            {
                assertEquals(p, pageTree.indexOf(page));
                ++p;
            }

            // PDFBOX-4261: check that object number stays the same
            assertEquals(origPageKey, document.getDocumentCatalog().getCOSObject().getItem(COSName.PAGES).toString());

            List<PDSignature> signatureDictionaries = document.getSignatureDictionaries();
            if (signatureDictionaries.isEmpty())
            {
                fail("no signature found");
            }
            for (PDSignature sig : document.getSignatureDictionaries())
            {
                byte[] contents = sig.getContents();

                // verify that getSignedContent() brings the same content
                // regardless whether from an InputStream or from a byte array
                byte[] totalFileContent = Files.readAllBytes(signedFile.toPath());
                byte[] signedFileContent1 = sig.getSignedContent(new ByteArrayInputStream(totalFileContent));
                byte[] signedFileContent2 = sig.getSignedContent(totalFileContent);
                assertArrayEquals(signedFileContent1, signedFileContent2);

                // verify that all getContents() methods returns the same content
                try (FileInputStream fis = new FileInputStream(signedFile))
                {
                    byte[] contents2 = sig.getContents(IOUtils.toByteArray(fis));
                    assertArrayEquals(contents, contents2);
                }
                byte[] contents3 = sig.getContents(new FileInputStream(signedFile));
                assertArrayEquals(contents, contents3);

                // inspiration:
                // http://stackoverflow.com/a/26702631/535646
                // http://stackoverflow.com/a/9261365/535646
                CMSSignedData signedData = new CMSSignedData(new CMSProcessableByteArray(signedFileContent1), contents);
                Store<X509CertificateHolder> certificatesStore = signedData.getCertificates();
                Collection<SignerInformation> signers = signedData.getSignerInfos().getSigners();
                SignerInformation signerInformation = signers.iterator().next();
                @SuppressWarnings("unchecked")
                Collection<X509CertificateHolder> matches = certificatesStore
                        .getMatches((Selector<X509CertificateHolder>) signerInformation.getSID());
                X509CertificateHolder certificateHolder = matches.iterator().next();
                assertArrayEquals(certificate.getEncoded(), certificateHolder.getEncoded());
                // CMSVerifierCertificateNotValidException means that the keystore wasn't valid at signing time
                if (!signerInformation.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificateHolder)))
                {
                    fail("Signature verification failed");
                }

                TimeStampToken timeStampToken = SigUtils.extractTimeStampTokenFromSignerInformation(signerInformation);
                if (checkTimeStamp)
                {
                    assertNotNull(timeStampToken);
                    SigUtils.validateTimestampToken(timeStampToken);

                    TimeStampTokenInfo timeStampInfo = timeStampToken.getTimeStampInfo();

                    // compare the hash of the signed content with the hash in the timestamp
                    byte[] tsMessageImprintDigest = timeStampInfo.getMessageImprintDigest();
                    String hashAlgorithm = timeStampInfo.getMessageImprintAlgOID().getId();
                    byte[] sigMessageImprintDigest = MessageDigest.getInstance(hashAlgorithm).digest(signerInformation.getSignature());
                    assertArrayEquals(sigMessageImprintDigest, tsMessageImprintDigest, "timestamp signature verification failed");

                    Store<X509CertificateHolder> tsCertStore = timeStampToken.getCertificates();

                    // get the certificate from the timeStampToken
                    @SuppressWarnings("unchecked") // TimeStampToken.getSID() is untyped
                    Collection<X509CertificateHolder> tsCertStoreMatches = tsCertStore.getMatches(timeStampToken.getSID());
                    X509CertificateHolder certHolderFromTimeStamp = tsCertStoreMatches.iterator().next();
                    X509Certificate certFromTimeStamp = new JcaX509CertificateConverter().getCertificate(certHolderFromTimeStamp);

                    SigUtils.checkTimeStampCertificateUsage(certFromTimeStamp);
                    SigUtils.verifyCertificateChain(tsCertStore, certFromTimeStamp, timeStampInfo.getGenTime());
                }
                else
                {
                    assertNull(timeStampToken);
                }
            }
        }
    }
}
