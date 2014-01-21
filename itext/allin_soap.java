/**
 * Created:
 * 03.12.13 KW49 14:51
 * </p>
 * Last Modification:
 * 21.01.2014 08:33
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
 * **********************************************************************************
 * Sign PDF using Swisscom All-in signing service                                   *
 * Tested with iText-5.4.5; Bouncy Castle 1.50 and JDK 1.7.0_45                     *
 * For examples see main method. You only need to change variables in this method.  *
 * **********************************************************************************
 */

package swisscom.com.ais.itext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

public class allin_soap {

    private static final String _CERTIFICATE_REQUEST_PROFILE = "urn:com:swisscom:advanced";
    private static final String _TIMESTAMP_URN = "urn:ietf:rfc:3161";
    private static final String _OCSP_URN = "urn:ietf:rfc:2560";
    private static final String _MOBILE_ID_TYPE = "urn:com:swisscom:auth:mobileid:v1.0";
    private static String _cfgPath = "allin_itext.properties";
    private Properties properties;
    private String _privateKeyName;
    private String _serverCertPath;
    private String _clientCertPath;
    private String _url;
    private int _timeout;
    private boolean _debug = false;
    private boolean _verboseMode = false;

    /**
     * Constructor
     * load properties file and set connection properties from properties file
     *
     * @param verboseOutput
     * @param debugMode
     * @param propertyFilePath
     * @throws Exception
     */

    public allin_soap(boolean verboseOutput, boolean debugMode, String propertyFilePath) throws Exception {

        this._verboseMode = verboseOutput;
        this._debug = debugMode;

        if (propertyFilePath != null) {
            _cfgPath = propertyFilePath;
        }

        properties = new Properties();

        try {
            properties.load(new FileReader(_cfgPath));
        } catch (IOException e) {
            throw new Exception("Could not load property file");
        }

        setConnectionProperties();
        checkFilesExists(new String[]{this._clientCertPath, this._privateKeyName, this._serverCertPath});

    }

    private void setConnectionProperties() {

        this._clientCertPath = properties.getProperty("CERT_FILE");
        this._privateKeyName = properties.getProperty("CERT_KEY");
        this._serverCertPath = properties.getProperty("SSL_CA");
        this._url = properties.getProperty("URL");
        try {
            this._timeout = Integer.parseInt(properties.getProperty("TIMEOUT_CON"));
            this._timeout *= 1000;
        } catch (NumberFormatException e) {
            this._timeout = 90 * 1000;
        }

    }

    /**
     * @param signatureType     TSA, OnDemand, StaticCert
     * @param fileIn
     * @param fileOut
     * @param distinguishedName
     * @param msisdn
     * @param msg
     * @param language
     */
    public void sign(@Nonnull allin_include.Signature signatureType, @Nonnull String fileIn,
                     @Nonnull String fileOut, String distinguishedName, String msisdn, String msg, String language) throws Exception {

        boolean addTimestamp = properties.getProperty("ADD_TSA").trim().toLowerCase().equals("true");
        boolean addOCSP = properties.getProperty("ADD_OCSP").trim().toLowerCase().equals("true");

        allin_include.HashAlgorithm hashAlgo = allin_include.HashAlgorithm.valueOf(properties.getProperty("DIGEST_METHOD").trim().toUpperCase());

        String claimedIdentityPropName = signatureType.equals(allin_include.Signature.ONDEMAND) ?
                "AP_ID_ONDEMAND" : signatureType.equals(allin_include.Signature.TSA) ? "AP_ID_TSA" : "AP_ID_STATIC";
        String claimedIdentity = properties.getProperty(claimedIdentityPropName);

        allin_pdf pdf = new allin_pdf(fileIn, fileOut, null, null, null, null);

        try {
            if (msisdn != null && msg != null && language != null && signatureType.equals(allin_include.Signature.ONDEMAND)) {
                if (_debug) {
                    System.out.println("Going to sign ondemand with mobile id");
                }
                signDocumentOnDemandCertMobileId(new allin_pdf[]{pdf}, Calendar.getInstance(), hashAlgo, _url, addTimestamp,
                        addOCSP, claimedIdentity, distinguishedName, msisdn, msg, language, (int) (Math.random() * 1000));
            } else if (signatureType.equals(allin_include.Signature.ONDEMAND)) {
                if (_debug) {
                    System.out.println("Going to sign with ondemand");
                }
                signDocumentOnDemandCert(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, _CERTIFICATE_REQUEST_PROFILE,
                        addTimestamp, addOCSP, distinguishedName, claimedIdentity, (int) (Math.random() * 1000));
            } else if (signatureType.equals(allin_include.Signature.TSA)) {
                if (_debug) {
                    System.out.println("Going to sign only with timestamp");
                }
                signDocumentTimestampOnly(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, claimedIdentity,
                        (int) (Math.random() * 1000));
            } else if (signatureType.equals(allin_include.Signature.STATIC)) {
                if (_debug) {
                    System.out.println("Going to sign with static cert");
                }
                signDocumentStaticCert(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, addTimestamp, addOCSP,
                        claimedIdentity, (int) (Math.random() * 1000));
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    /**
     * Sign document with on demand certificate and authenticate with mobile id
     *
     * @param pdfs
     * @param signDate
     * @param hashAlgo
     * @param serverURI
     * @param addTimestamp
     * @param addOcsp
     * @param claimedIdentity
     * @param distinguishedName
     * @param phoneNumber
     * @param certReqMsg
     * @param certReqMsgLang
     * @param requestId
     * @throws Exception
     */
    private void signDocumentOnDemandCertMobileId(@Nonnull allin_pdf pdfs[], @Nonnull Calendar signDate, @Nonnull allin_include.HashAlgorithm hashAlgo,
                                                  @Nonnull String serverURI, boolean addTimestamp, boolean addOcsp, @Nonnull String claimedIdentity,
                                                  @Nonnull String distinguishedName, @Nonnull String phoneNumber, @Nonnull String certReqMsg,
                                                  @Nonnull String certReqMsgLang, int requestId) throws Exception {
        String[] additionalProfiles;

        if (pdfs.length > 1) {
            additionalProfiles = new String[2];
            additionalProfiles[1] = allin_include.AdditionalProfiles.BATCH.getProfileName();
        } else {
            additionalProfiles = new String[1];
        }
        additionalProfiles[0] = allin_include.AdditionalProfiles.ON_DEMAND_CERTIFCATE.getProfileName();

        int estimatedSize = getEstimatedSize(addTimestamp, addOcsp, true);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), false);
        }

        SOAPMessage sigReqMsg = createRequestMessage(allin_include.RequestType.SignRequest, hashAlgo.getHashUri(), _CERTIFICATE_REQUEST_PROFILE,
                pdfHash, addTimestamp ? _TIMESTAMP_URN : null, addOcsp ? _OCSP_URN : null, additionalProfiles,
                claimedIdentity, allin_include.SignatureType.CMS.getSignatureType(), distinguishedName, _MOBILE_ID_TYPE, phoneNumber,
                certReqMsg, certReqMsgLang, null, requestId);

        signDocumentSync(sigReqMsg, serverURI, pdfs, estimatedSize, "Base64Signature");

    }

    /**
     * Sign document with on demand certificate
     *
     * @param pdfs
     * @param hashAlgo
     * @param signDate
     * @param serverURI
     * @param certRequestProfile
     * @param addTimeStamp
     * @param addOcsp
     * @param distinguishedName
     * @param claimedIdentity
     * @param requestId
     * @throws Exception
     */
    private void signDocumentOnDemandCert(@Nonnull allin_pdf[] pdfs, @Nonnull allin_include.HashAlgorithm hashAlgo, Calendar signDate, @Nonnull String serverURI,
                                          @Nonnull String certRequestProfile, boolean addTimeStamp, boolean addOcsp,
                                          @Nonnull String distinguishedName, @Nonnull String claimedIdentity, int requestId)
            throws Exception {

        String[] additionalProfiles;
        if (pdfs.length > 1) {
            additionalProfiles = new String[2];
            additionalProfiles[1] = allin_include.AdditionalProfiles.BATCH.getProfileName();
        } else {
            additionalProfiles = new String[1];
        }
        additionalProfiles[0] = allin_include.AdditionalProfiles.ON_DEMAND_CERTIFCATE.getProfileName();

        int estimatedSize = getEstimatedSize(addTimeStamp, addOcsp, true);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), false);
        }

        SOAPMessage sigReqMsg = createRequestMessage(allin_include.RequestType.SignRequest, hashAlgo.getHashUri(), certRequestProfile,
                pdfHash, addTimeStamp ? _TIMESTAMP_URN : null, addOcsp ? _OCSP_URN : null, additionalProfiles,
                claimedIdentity, allin_include.SignatureType.CMS.getSignatureType(), distinguishedName, null, null, null, null, null, requestId);

        signDocumentSync(sigReqMsg, serverURI, pdfs, estimatedSize, "Base64Signature");
    }

    /**
     * Sign document with static cert
     *
     * @param pdfs
     * @param hashAlgo
     * @param signDate
     * @param serverURI
     * @param addTimeStamp
     * @param addOCSP
     * @param claimedIdentity
     * @param requestId
     * @throws Exception
     */
    private void signDocumentStaticCert(@Nonnull allin_pdf[] pdfs, @Nonnull allin_include.HashAlgorithm hashAlgo, Calendar signDate, @Nonnull String serverURI,
                                        boolean addTimeStamp, boolean addOCSP, @Nonnull String claimedIdentity, int requestId)
            throws Exception {

        String[] additionalProfiles = null;
        if (pdfs.length > 1) {
            additionalProfiles = new String[1];
            additionalProfiles[0] = allin_include.AdditionalProfiles.BATCH.getProfileName();
        }

        int estimatedSize = getEstimatedSize(addTimeStamp, addOCSP, false);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), false);
        }

        SOAPMessage sigReqMsg = createRequestMessage(allin_include.RequestType.SignRequest, hashAlgo.getHashUri(), null,
                pdfHash, addTimeStamp ? _TIMESTAMP_URN : null, addOCSP ? _OCSP_URN : null, additionalProfiles,
                claimedIdentity, allin_include.SignatureType.CMS.getSignatureType(), null, null, null, null, null, null, requestId);

        signDocumentSync(sigReqMsg, serverURI, pdfs, estimatedSize, "Base64Signature");
    }

    /**
     * Sign document only with timestamp
     *
     * @param pdfs
     * @param hashAlgo
     * @param signDate
     * @param serverURI
     * @param claimedIdentity
     * @param requestId
     * @throws Exception
     */
    private void signDocumentTimestampOnly(@Nonnull allin_pdf[] pdfs, @Nonnull allin_include.HashAlgorithm hashAlgo, Calendar signDate,
                                           @Nonnull String serverURI, @Nonnull String claimedIdentity, int requestId)
            throws Exception {

        allin_include.SignatureType signatureType = allin_include.SignatureType.TIMESTAMP;

        String[] additionalProfiles;
        if (pdfs.length > 1) {
            additionalProfiles = new String[2];
            additionalProfiles[1] = allin_include.AdditionalProfiles.BATCH.getProfileName();
        } else {
            additionalProfiles = new String[1];
        }
        additionalProfiles[0] = allin_include.AdditionalProfiles.TIMESTAMP.getProfileName();

        int estimatedSize = getEstimatedSize(true, true, false);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), true);
        }

        SOAPMessage sigReqMsg = createRequestMessage(allin_include.RequestType.SignRequest, hashAlgo.getHashUri(), null,
                pdfHash, null, null, additionalProfiles, claimedIdentity, signatureType.getSignatureType(),
                null, null, null, null, null, null, requestId);

        signDocumentSync(sigReqMsg, serverURI, pdfs, estimatedSize, "RFC3161TimeStampToken");
    }

    /**
     * Sign document synchron
     *
     * @param sigReqMsg
     * @param serverURI
     * @param pdfs
     * @param estimatedSize
     * @param signNodeName
     * @throws Exception
     */
    private void signDocumentSync(@Nonnull SOAPMessage sigReqMsg, @Nonnull String serverURI, @Nonnull allin_pdf[] pdfs,
                                  int estimatedSize, String signNodeName) throws Exception {

        String sigResponse = sendRequest(sigReqMsg, serverURI);
        ArrayList<String> responseResult = getTextFromXmlText(sigResponse, "ResultMajor");
        boolean singingSuccess = sigResponse != null && responseResult != null && allin_include.RequestResult.Success.getResultUrn().equals(responseResult.get(0));

        if (_debug || _verboseMode) {
            //Getting pdf input file names for message output
            String pdfNames = "";
            for (int i = 0; i < pdfs.length; i++) {
                pdfNames = pdfNames.concat(new File(pdfs[i].getInputFilePath()).getName());
                if (pdfs.length > i + 1)
                    pdfNames = pdfNames.concat(", ");
            }

            if (!singingSuccess) {
                System.err.println("FAILED to sign " + pdfNames + " with following details:");
            } else {
                System.out.println("OK signing " + pdfNames + " with following details:");
            }

            if (sigResponse != null) {
                if (responseResult != null) {
                    for (String s : responseResult) {
                        if (s.length() > 0) {
                            if (!singingSuccess) {
                                System.err.println(" Result major: " + s);
                            } else {
                                System.out.println(" Result major: " + s);
                            }
                        }
                    }
                }

                ArrayList<String> resultMinor = getTextFromXmlText(sigResponse, "ResultMinor");
                if (resultMinor != null) {
                    for (String s : resultMinor) {
                        if (s.length() > 0) {
                            if (!singingSuccess) {
                                System.err.println(" Result minor: " + s);
                            } else {
                                System.out.println(" Result minor: " + s);
                            }
                        }
                    }
                }

                ArrayList<String> errorMsg = getTextFromXmlText(sigResponse, "ResultMessage");
                if (errorMsg != null) {
                    for (String s : errorMsg) {
                        if (s.length() > 0) {
                            if (!singingSuccess) {
                                System.err.println(" Result message: " + s);
                            } else {
                                System.out.println(" Result message: " + s);
                            }
                        }
                    }
                }
            }
        }

        if (!singingSuccess) {
            throw new Exception();
        }

        ArrayList<String> signHashes = getTextFromXmlText(sigResponse, signNodeName);
        signDocuments(signHashes, pdfs, estimatedSize);
    }

    /**
     * Sign document
     *
     * @param signatureList
     * @param pdfs
     * @param estimatedSize
     * @throws Exception
     */
    private void signDocuments(@Nonnull ArrayList<String> signatureList, @Nonnull allin_pdf[] pdfs, int estimatedSize) throws Exception {
        int counter = 0;

        for (String signatureHash : signatureList) {
            try {
                pdfs[counter].sign(signatureHash, estimatedSize);
            } catch (Exception e) {
                if (_debug) {
                    System.err.println("Could not add signature hash to document");
                }
                throw new Exception(e);
            }

            counter++;
        }
    }

    /**
     * Get nodes text content
     *
     * @param soapResponseText can be a full response as xml
     * @param nodeName
     * @return if nodes with searched node names exist it will return an array list containing text from value from nodes
     * @throws IOException, SAXException, ParserConfigurationException
     */
    @Nullable
    private ArrayList<String> getTextFromXmlText(String soapResponseText, String nodeName) throws IOException, SAXException, ParserConfigurationException {
        Element element = getNodeList(soapResponseText);

        return getNodesFromNodeList(element, nodeName);
    }

    /**
     * Get nodes text content
     *
     * @param element
     * @param nodeName
     * @return if nodes with searched node names exist it will return an array list containing text from value from nodes
     */
    @Nullable
    private ArrayList<String> getNodesFromNodeList(@Nonnull Element element, @Nonnull String nodeName) {
        ArrayList<String> returnlist = null;
        NodeList nl = element.getElementsByTagName(nodeName);

        for (int i = 0; i < nl.getLength(); i++) {
            if (nodeName.equals(nl.item(i).getNodeName())) {
                if (returnlist == null) {
                    returnlist = new ArrayList();
                }
                returnlist.add(nl.item(i).getTextContent());
            }

        }

        return returnlist;
    }

    /**
     * Get a XML string as an element
     *
     * @param xmlString
     * @return org.w3c.dom.Element from XML String
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private Element getNodeList(@Nonnull String xmlString) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        ByteArrayInputStream bis = new ByteArrayInputStream(xmlString.getBytes());
        Document doc = db.parse(bis);

        return doc.getDocumentElement();
    }

    /**
     * Create a SOAP message object. Will print the message if debug is set to true
     *
     * @param reqType                  type of request e.g. singing or pending
     * @param digestMethodAlgorithmURL
     * @param certRequestProfile       only necessary when on demand certificate is needed
     * @param hashList                 pdf hashes
     * @param timestampURN             if needed urn of timestamp
     * @param ocspURN                  if needed urn of ocsp
     * @param additionalProfiles
     * @param claimedIdentity
     * @param signatureType            e.g. cms or timestamp
     * @param distinguishedName
     * @param mobileIdType
     * @param phoneNumber              must start with e.g. +41 or +49
     * @param certReqMsg
     * @param certReqMsgLang
     * @param responseId
     * @return
     * @throws SOAPException
     * @throws IOException
     */
    private SOAPMessage createRequestMessage(@Nonnull allin_include.RequestType reqType, @Nonnull String digestMethodAlgorithmURL,
                                             String certRequestProfile, @Nonnull byte[][] hashList, String timestampURN, String ocspURN,
                                             String[] additionalProfiles, String claimedIdentity,
                                             @Nonnull String signatureType, String distinguishedName,
                                             String mobileIdType, String phoneNumber, String certReqMsg, String certReqMsgLang,
                                             String responseId, int requestId) throws SOAPException, IOException {

        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addAttribute(new QName("xmlns"), "urn:oasis:names:tc:dss:1.0:core:schema");
        envelope.addNamespaceDeclaration("dsig", "http://www.w3.org/2000/09/xmldsig#");
        envelope.addNamespaceDeclaration("sc", "urn:com:swisscom:dss:1.0:schema");
        envelope.addNamespaceDeclaration("ais", "http://service.ais.swisscom.com/");

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();

        SOAPElement signElement = soapBody.addChildElement("sign", "ais");

        SOAPElement requestElement = signElement.addChildElement(reqType.getRequestType());
        requestElement.addAttribute(new QName("Profile"), reqType.getUrn());
        requestElement.addAttribute(new QName("RequestID"), String.valueOf(requestId));
        SOAPElement inputDocumentsElement = requestElement.addChildElement("InputDocuments");

        SOAPElement digestValueElement;
        SOAPElement documentHashElement;
        SOAPElement digestMethodElement;

        for (int i = 0; i < hashList.length; i++) {
            documentHashElement = inputDocumentsElement.addChildElement("DocumentHash");
            if (hashList.length > 1)
                documentHashElement.addAttribute(new QName("ID"), String.valueOf(i));
            digestMethodElement = documentHashElement.addChildElement("DigestMethod", "dsig");
            digestMethodElement.addAttribute(new QName("Algorithm"), digestMethodAlgorithmURL);
            digestValueElement = documentHashElement.addChildElement("DigestValue", "dsig");

            String s = com.itextpdf.text.pdf.codec.Base64.encodeBytes(hashList[i]);
            digestValueElement.addTextNode(s);
        }

        if (timestampURN != null || additionalProfiles != null || ocspURN != null || certRequestProfile != null || claimedIdentity != null || signatureType != null) {
            SOAPElement optionalInputsElement = requestElement.addChildElement("OptionalInputs");

            SOAPElement additionalProfileelement;
            if (additionalProfiles != null) {
                for (String additionalProfile : additionalProfiles) {
                    additionalProfileelement = optionalInputsElement.addChildElement("AdditionalProfile");
                    additionalProfileelement.addTextNode(additionalProfile);
                }
            }

            if (claimedIdentity != null) {
                SOAPElement claimedIdentityElement = optionalInputsElement.addChildElement(new QName("ClaimedIdentity"));
                SOAPElement claimedIdNameElement = claimedIdentityElement.addChildElement("Name");
                claimedIdNameElement.addTextNode(claimedIdentity);
            }

            if (certRequestProfile != null) {
                SOAPElement certificateRequestElement = optionalInputsElement.addChildElement("CertificateRequest", "sc");
                if (!_CERTIFICATE_REQUEST_PROFILE.equals(certRequestProfile)) {
                    certificateRequestElement.addAttribute(new QName("Profile"), certRequestProfile);
                }
                if (distinguishedName != null) {
                    SOAPElement distinguishedNameElement = _CERTIFICATE_REQUEST_PROFILE.equals(certRequestProfile) ?
                            certificateRequestElement.addChildElement("DistinguishedName", "sc") :
                            certificateRequestElement.addChildElement("DistinguishedName");
                    distinguishedNameElement.addTextNode(distinguishedName);
                    if (phoneNumber != null) {
                        SOAPElement stepUpAuthorisationElement = certificateRequestElement.addChildElement("StepUpAuthorisation", "sc");

                        if (mobileIdType != null && phoneNumber != null) {
                            SOAPElement mobileIdElement = stepUpAuthorisationElement.addChildElement("MobileID", "sc");
                            mobileIdElement.addAttribute(new QName("Type"), _MOBILE_ID_TYPE);
                            SOAPElement msisdnElement = mobileIdElement.addChildElement("MSISDN", "sc");
                            msisdnElement.addTextNode(phoneNumber);
                            SOAPElement certReqMsgElement = mobileIdElement.addChildElement("Message", "sc");
                            certReqMsgElement.addTextNode(certReqMsg);
                            SOAPElement certReqMsgLangElement = mobileIdElement.addChildElement("Language", "sc");
                            certReqMsgLangElement.addTextNode(certReqMsgLang);
                        }
                    }
                }
            }

            if (signatureType != null) {
                SOAPElement signatureTypeElement = optionalInputsElement.addChildElement("SignatureType");
                signatureTypeElement.addTextNode(signatureType);
            }

            if (timestampURN != null && !signatureType.equals(_TIMESTAMP_URN)) {
                SOAPElement addTimeStampelemtn = optionalInputsElement.addChildElement("AddTimestamp");
                addTimeStampelemtn.addAttribute(new QName("Type"), timestampURN);
            }

            if (ocspURN != null && !signatureType.equals(_TIMESTAMP_URN)) {
                SOAPElement addOcspElement = optionalInputsElement.addChildElement("AddOcspResponse", "sc");
                addOcspElement.addAttribute(new QName("Type"), ocspURN);
            }

            if (responseId != null) {
                SOAPElement responseIdElement = optionalInputsElement.addChildElement("ResponseID");
                responseIdElement.addTextNode(responseId);
            }
        }

        soapMessage.saveChanges();

        if (_debug) {
            System.out.print("\nRequest SOAP Message:\n");
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            soapMessage.writeTo(ba);
            String msg = new String(ba.toByteArray());
            System.out.println(getPrettyFormatedXml(msg, 2) + "\n");
        }

        return soapMessage;
    }

    /**
     * Send request to a server. If debug is set to true it will print response message.
     *
     * @param soapMsg
     * @param urlPath
     * @return Server response as string
     * @throws Exception
     */
    @Nullable
    private String sendRequest(@Nonnull SOAPMessage soapMsg, @Nonnull String urlPath) throws Exception {

        URLConnection conn = new allin_connect(urlPath, _privateKeyName, _serverCertPath, _clientCertPath, _timeout, _debug, _verboseMode).getConnection();
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setRequestMethod("POST");
        }

        conn.setAllowUserInteraction(true);
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setDoOutput(true);

        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMsg.writeTo(baos);
        String msg = baos.toString();

        out.write(msg);
        out.flush();
        if (out != null) {
            out.close();
        }

        String line = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String response = "";
        while ((line = in.readLine()) != null) {
            response = response + line;
        }

        if (in != null) {
            in.close();
        }

        if (_debug) {
            System.out.print("\nSOAP response message:\n" + getPrettyFormatedXml(response, 2) + "\n");
        }

        return response;
    }

    /**
     * Calculate size of signature
     *
     * @param useTimestmap
     * @param useOcsp
     * @param certRequestProfile
     * @return calculated size of signature as int
     */
    private int getEstimatedSize(boolean useTimestmap, boolean useOcsp, boolean certRequestProfile) {
        int returnValue = 8192;
        returnValue = useTimestmap ? returnValue + 4192 : returnValue;
        returnValue = useOcsp ? returnValue + 4192 : returnValue;
        returnValue = certRequestProfile ? returnValue + 700 : returnValue;

        return returnValue;
    }

    /**
     * @param filePaths
     * @throws FileNotFoundException
     */
    private void checkFilesExists(@Nonnull String[] filePaths) throws FileNotFoundException {

        File file;
        for (String filePath : filePaths) {
            file = new File(filePath);
            if (!file.isFile() || !file.canRead()) {
                throw new FileNotFoundException("File not found or is not a file or not readable: " + file.getAbsolutePath());
            }
        }
    }

    public String getPrettyFormatedXml(String input, int indent) {

        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);

            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            return input;
        }
    }

}

