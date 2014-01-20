/**
 * Created:
 * 19.12.13 KW51 08:04
 * <p/>
 * Last Modification:
 * 10.01.2014 12:31
 * <p/>
 * Version:
 * 1.0.0
 * </p>
 * Copyright:
 * Copyright (C) 2013. All rights reserved.
 * </p>
 * License:
 * GNU General Public License version 2 or later; see LICENSE.md
 * </p>
 * Author:
 * Swisscom (Schweiz) AG
 */

package swisscom.com.ais.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.codec.Base64;

import javax.annotation.Nonnull;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

public class allin_pdf {

    private String inputFilePath;
    private String outputFilePath;
    private String pdfPassword;
    private String signReason;
    private String signLocation;
    private String signContact;
    private PdfSignatureAppearance pdfSignatureAppearance;
    private PdfSignature pdfSignature;
    private ByteArrayOutputStream byteArrayOutputStream;

    allin_pdf(@Nonnull String inputFilePath, @Nonnull String outputFilePath, String pdfPassword, String signReason, String signLocation, String signContact) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.pdfPassword = pdfPassword;
        this.signReason = signReason;
        this.signLocation = signLocation;
        this.signContact = signContact;
    }


    public String getInputFilePath() {
        return inputFilePath;
    }

    public byte[] getPdfHash(@Nonnull Calendar signDate, int estimatedSize, @Nonnull String hashAlgorithm, boolean isTimestampOnly)
            throws IOException, DocumentException, NoSuchAlgorithmException {

        PdfReader pdfReader = null;
        pdfReader = new PdfReader(inputFilePath, pdfPassword != null ? pdfPassword.getBytes() : null);
        AcroFields acroFields = pdfReader.getAcroFields();
        boolean hasSignature = acroFields.getSignatureNames().size() > 0;
        byteArrayOutputStream = new ByteArrayOutputStream();
        PdfStamper pdfStamper = PdfStamper.createSignature(pdfReader, byteArrayOutputStream, '\0', null, hasSignature);
        pdfStamper.setXmpMetadata(pdfReader.getMetadata());

        pdfSignatureAppearance = pdfStamper.getSignatureAppearance();
        pdfSignature = new PdfSignature(PdfName.ADOBE_PPKLITE, isTimestampOnly ? PdfName.ETSI_RFC3161 : PdfName.ADBE_PKCS7_DETACHED);
        pdfSignature.setReason(signReason);
        pdfSignature.setLocation(signLocation);
        pdfSignature.setContact(signContact);
        pdfSignature.setDate(new PdfDate(signDate));
        pdfSignatureAppearance.setCryptoDictionary(pdfSignature);

        HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
        exc.put(PdfName.CONTENTS, new Integer(estimatedSize * 2 + 2));

        pdfSignatureAppearance.preClose(exc);

        MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
        InputStream rangeStream = pdfSignatureAppearance.getRangeStream();
        int i;
        while ((i = rangeStream.read()) != -1)
            messageDigest.update((byte) i);
        return messageDigest.digest();
    }

    /**
     * @param externalSignature
     * @param estimatedSize
     * @throws IOException
     * @throws DocumentException
     */
    private void addSignatureToPdf(@Nonnull byte[] externalSignature, int estimatedSize) throws IOException, DocumentException {

        if (estimatedSize < externalSignature.length)
            throw new IOException("Not enough space for signature");

        PdfLiteral pdfLiteral = (PdfLiteral) pdfSignature.get(PdfName.CONTENTS);
        byte[] outc = new byte[(pdfLiteral.getPosLength() - 2) / 2];

        Arrays.fill(outc, (byte) 0);

        System.arraycopy(externalSignature, 0, outc, 0, externalSignature.length);
        PdfDictionary dic2 = new PdfDictionary();
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        pdfSignatureAppearance.close(dic2);
        OutputStream outputStream = new FileOutputStream(outputFilePath);
        byteArrayOutputStream.writeTo(outputStream);
        outputStream.close();
        byteArrayOutputStream = null;
    }

    /**
     * Decode hash to Base64 and sign PDF
     *
     * @param hash
     * @param estimatedSize
     * @throws IOException, DocumentException
     */
    public void sign(@Nonnull String hash, int estimatedSize) throws IOException, DocumentException {
        addSignatureToPdf(Base64.decode(hash), estimatedSize);
    }


}
