/**
 * Creates SOAP requests with hash from a pdf document and send it to a server. If server sends a response with a signature
 * this will be add to a pdf.
 *
 * Created:
 * 03.12.13 KW49 14:51
 * </p>
 * Last Modification:
 * 22.01.2014 15:56
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

import com.itextpdf.text.pdf.codec.Base64;
import com.sun.istack.internal.NotNull;
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

    /**
     * Constant for certificate request profile
     */
    private static final String _CERTIFICATE_REQUEST_PROFILE = "urn:com:swisscom:advanced";

    /**
     * Constant for timestamp urn
     */
    private static final String _TIMESTAMP_URN = "urn:ietf:rfc:3161";

    /**
     * Constant for ocsp urn
     */
    private static final String _OCSP_URN = "urn:ietf:rfc:2560";

    /**
     * Constant for mobile id type
     */
    private static final String _MOBILE_ID_TYPE = "urn:com:swisscom:auth:mobileid:v1.0";

    /**
     * Path to configuration file. Can also set in constructor
     */
    private static String _cfgPath = "allin_itext.properties";

    /**
     * Properties from properties file
     */
    private Properties properties;

    /**
     * File path of private key
     */
    private String _privateKeyName;

    /**
     * File path of server certificate
     */
    private String _serverCertPath;

    /**
     * File patj of client certificate
     */
    private String _clientCertPath;

    /**
     * Url of dss server
     */
    private String _url;

    /**
     * Connection timeout in seconds
     */
    private int _timeout;

    /**
     * If set to true debug information will be print otherwise not
     */
    private boolean _debug = false;

    /**
     * If set to true verbose information will be print otherwise not
     */
    private boolean _verboseMode = false;

    /**
     * Constructor. Set parameter and load properties from file. Connection properties will be set and check if all needed
     * files exist
     *
     * @param verboseOutput    If true verbose information will be print out
     * @param debugMode        If true debug information will be print out
     * @param propertyFilePath Path of property file
     * @throws FileNotFoundException If a file do not exist. E.g. property file, certificate, input pdf etc
     */
    public allin_soap(boolean verboseOutput, boolean debugMode, @Nullable String propertyFilePath) throws FileNotFoundException {

        this._verboseMode = verboseOutput;
        this._debug = debugMode;

        if (propertyFilePath != null) {
            _cfgPath = propertyFilePath;
        }

        properties = new Properties();

        try {
            properties.load(new FileReader(_cfgPath));
        } catch (IOException e) {
            throw new FileNotFoundException(("Could not load property file"));
        }

        setConnectionProperties();
        checkFilesExistsAndIsFile(new String[]{this._clientCertPath, this._privateKeyName, this._serverCertPath});

    }

    /**
     * Set connection properties from property file. Also convert timeout from seconds to milliseconds. If timeout can not
     * be readed from properties file it will use standard value 90 seconds
     */
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
     * Read signing options from properties. Depending on parameters here will be decided which type of signature will be used.
     *
     * @param signatureType     Type of signature e.g. timestamp, ondemand or static
     * @param fileIn            File path of input pdf document
     * @param fileOut           File path of output pdf document which will be the signed one
     * @param distinguishedName Information about signer e.g. name, country etc.
     * @param msisdn            Mobile id for sending message to signer
     * @param msg               Message which will be send to signer if msisdn is set
     * @param language          Language of message
     * @throws Exception If parameters are not set or signing failed
     */
    public void sign(@Nonnull allin_include.Signature signatureType, @Nonnull String fileIn, @Nonnull String fileOut,
                     @Nullable String distinguishedName, @Nullable String msisdn, @Nullable String msg, @Nullable String language)
            throws Exception {

        boolean addTimestamp = properties.getProperty("ADD_TIMESTAMP").trim().toLowerCase().equals("true");
        boolean addOCSP = properties.getProperty("ADD_OCSP").trim().toLowerCase().equals("true");

        allin_include.HashAlgorithm hashAlgo = allin_include.HashAlgorithm.valueOf(properties.getProperty("DIGEST_METHOD").trim().toUpperCase());

        String claimedIdentity = properties.getProperty("CUSTOMER");
        String claimedIdentityPropName = signatureType.equals(allin_include.Signature.ONDEMAND) ?
                "KEY_ONDEMAND" : signatureType.equals(allin_include.Signature.STATIC) ? "KEY_STATIC" : null;
        if (claimedIdentityPropName != null) {
            claimedIdentity = claimedIdentity.concat(":" + properties.getProperty(claimedIdentityPropName));
        }

        allin_pdf pdf = new allin_pdf(fileIn, fileOut, null, null, null, null);

        try {
            long requestId = System.nanoTime();

            if (msisdn != null && msg != null && language != null && signatureType.equals(allin_include.Signature.ONDEMAND)) {
                if (_debug) {
                    System.out.println("Going to sign ondemand with mobile id");
                }
                signDocumentOnDemandCertMobileId(new allin_pdf[]{pdf}, Calendar.getInstance(), hashAlgo, _url, addTimestamp,
                        addOCSP, claimedIdentity, distinguishedName, msisdn, msg, language, requestId);
            } else if (signatureType.equals(allin_include.Signature.ONDEMAND)) {
                if (_debug) {
                    System.out.println("Going to sign with ondemand");
                }
                signDocumentOnDemandCert(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, _CERTIFICATE_REQUEST_PROFILE,
                        addTimestamp, addOCSP, distinguishedName, claimedIdentity, requestId);
            } else if (signatureType.equals(allin_include.Signature.TIMESTAMP)) {
                if (_debug) {
                    System.out.println("Going to sign only with timestamp");
                }
                signDocumentTimestampOnly(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, claimedIdentity,
                        requestId);
            } else if (signatureType.equals(allin_include.Signature.STATIC)) {
                if (_debug) {
                    System.out.println("Going to sign with static cert");
                }
                signDocumentStaticCert(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, addTimestamp, addOCSP,
                        claimedIdentity, requestId);
            } else {
                throw new Exception("Wrong or missing parameters. Can not find a signature type.");
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    /**
     * Create SOAP request message and sign document with on demand certificate and authenticate with mobile id
     *
     * @param pdfs              Pdf input files
     * @param signDate          Date when document(s) will be signed
     * @param hashAlgo          Hash algorithm to use for signature
     * @param serverURI         Server uri where to send the request
     * @param addTimestamp      If set to true timestamp will be added in signature otherwise not
     * @param addOcsp           If set to true ocsp information will be add in signature otherwise not
     * @param claimedIdentity   Signers identity
     * @param distinguishedName Information about signer e.g. name, country etc.
     * @param phoneNumber       Number of phone when mobile id is used
     * @param certReqMsg        Message which the signer get on his phone
     * @param certReqMsgLang    Language of message
     * @param requestId         An id for the request
     * @throws Exception If hash or request can not be generated or document can not be signed.
     */
    private void signDocumentOnDemandCertMobileId(@Nonnull allin_pdf pdfs[], @Nonnull Calendar signDate, @Nonnull allin_include.HashAlgorithm hashAlgo,
                                                  @Nonnull String serverURI, boolean addTimestamp, boolean addOcsp, @Nonnull String claimedIdentity,
                                                  @Nonnull String distinguishedName, @Nonnull String phoneNumber, @Nonnull String certReqMsg,
                                                  @Nonnull String certReqMsgLang, long requestId) throws Exception {
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
     * create SOAP request message and sign document with ondemand certificate but without mobile id
     *
     * @param pdfs               Pdf input files
     * @param hashAlgo           Hash algorithm to use for signature
     * @param signDate           Date when document(s) will be signed
     * @param serverURI          Server uri where to send the request
     * @param certRequestProfile Urn of certificate request profile
     * @param addTimeStamp       If set to true timestamp will be added in signature otherwise not
     * @param addOcsp            If set to true ocsp information will be add in signature otherwise not
     * @param distinguishedName  Information about signer e.g. name, country etc.
     * @param claimedIdentity    Signers identity
     * @param requestId          An id for the request
     * @throws Exception If hash or request can not be generated or document can not be signed.
     */
    private void signDocumentOnDemandCert(@Nonnull allin_pdf[] pdfs, @Nonnull allin_include.HashAlgorithm hashAlgo, Calendar signDate, @Nonnull String serverURI,
                                          @Nonnull String certRequestProfile, boolean addTimeStamp, boolean addOcsp,
                                          @Nonnull String distinguishedName, @Nonnull String claimedIdentity, long requestId)
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
     * Create SOAP request message and sign document with static certificate
     *
     * @param pdfs            Pdf input files
     * @param hashAlgo        Hash algorithm to use for signature
     * @param signDate        Date when document(s) will be signed
     * @param serverURI       Server uri where to send the request
     * @param addTimeStamp    If set to true timestamp will be added in signature otherwise not
     * @param addOCSP         If set to true ocsp information will be add in signature otherwise not
     * @param claimedIdentity Signers identity
     * @param requestId       An id for the request
     * @throws Exception If hash or request can not be generated or document can not be signed.
     */
    private void signDocumentStaticCert(@Nonnull allin_pdf[] pdfs, @Nonnull allin_include.HashAlgorithm hashAlgo, Calendar signDate, @Nonnull String serverURI,
                                        boolean addTimeStamp, boolean addOCSP, @Nonnull String claimedIdentity, long requestId)
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
     * Create SOAP request message and add a timestamp to pdf
     *
     * @param pdfs            Pdf input files
     * @param hashAlgo        Hash algorithm to use for signature
     * @param signDate        Date when document(s) will be signed
     * @param serverURI       Server uri where to send the request
     * @param claimedIdentity Signers identity
     * @param requestId       An id for the request
     * @throws Exception If hash or request can not be generated or document can not be signed.
     */
    private void signDocumentTimestampOnly(@Nonnull allin_pdf[] pdfs, @Nonnull allin_include.HashAlgorithm hashAlgo, Calendar signDate,
                                           @Nonnull String serverURI, @Nonnull String claimedIdentity, long requestId)
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
     * Send SOAP request to server and sign document if server send signature
     *
     * @param sigReqMsg     SOAP request message which will be send to the server
     * @param serverURI     Uri of server
     * @param pdfs          Pdf input file
     * @param estimatedSize Estimated size of external signature
     * @param signNodeName  Name of node where to find the signature
     * @throws Exception If hash can not be generated or document can not be signed.
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
                if (responseResult != null && _verboseMode) {
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
                if (resultMinor != null && _verboseMode) {
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
                if (errorMsg != null && _verboseMode) {
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
     * Add signature to pdf
     *
     * @param signatureList Arraylist with Base64 encoded signatures
     * @param pdfs          Pdf which will be signed
     * @param estimatedSize Estimated size of external signature
     * @throws Exception If adding signature to pdf failed.
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
     * Get text from a node from a xml text
     *
     * @param soapResponseText Text where to search
     * @param nodeName         Name of the node which text should be returned
     * @return If nodes with searched node names exist it will return an array list containing text from nodes
     * @throws IOException                  If any IO errors occur
     * @throws SAXException                 If any parse errors occur
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created which satisfies the configuration requested
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
                    returnlist = new ArrayList<String>();
                }
                returnlist.add(nl.item(i).getTextContent());
            }

        }

        return returnlist;
    }

    /**
     * Get a xml string as an xml element object
     *
     * @param xmlString String to convert e.g. a server request or response
     * @return org.w3c.dom.Element from XML String
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created which satisfies the configuration requested
     * @throws IOException                  If any IO errors occur
     * @throws SAXException                 If any parse errors occur
     */
    private Element getNodeList(@Nonnull String xmlString) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        ByteArrayInputStream bis = new ByteArrayInputStream(xmlString.getBytes());
        Document doc = db.parse(bis);

        return doc.getDocumentElement();
    }

    /**
     * Create SOAP message object for server request. Will print debug information if debug is set to true
     *
     * @param reqType                  Type of request message e.g. singing or pending request
     * @param digestMethodAlgorithmURL Uri of hash algorithm
     * @param certRequestProfile       Urn of certificate request profile. Only necessary when on demand certificate is needed
     * @param hashList                 Hashes from documents which should be signed
     * @param timestampURN             Urn of timestamp if timestamp should be added
     * @param ocspURN                  Urn of ocsp if ocsp information should be added
     * @param additionalProfiles       Urn of additional profiles e.g. ondemand certificate, timestamp signature, batch process etc.
     * @param claimedIdentity          Signers identity / profile
     * @param signatureType            Urn of signature type e.g. signature type cms or timestamp
     * @param distinguishedName        Information about signer e.g. name, country etc.
     * @param mobileIdType             Urn of mobile id type
     * @param phoneNumber              Mobile id for on demand certificates with mobile id request
     * @param certReqMsg               Message which will be send to phone number if set
     * @param certReqMsgLang           Language from message which will be send to mobile id
     * @param responseId               Only necessary when asking the signing status on server
     * @param requestId                Request id to identify signature in response
     * @return SOAP response from server. Depending on request profile it can be a signarure, signing status information or an error
     * @throws SOAPException If there is an error creating SOAP message
     * @throws IOException   If there is an error writing debug information
     */
    private SOAPMessage createRequestMessage(@Nonnull allin_include.RequestType reqType, @Nonnull String digestMethodAlgorithmURL,
                                             String certRequestProfile, @Nonnull byte[][] hashList, String timestampURN, String ocspURN,
                                             String[] additionalProfiles, String claimedIdentity,
                                             @Nonnull String signatureType, String distinguishedName,
                                             String mobileIdType, String phoneNumber, String certReqMsg, String certReqMsgLang,
                                             String responseId, long requestId) throws SOAPException, IOException {

        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.removeNamespaceDeclaration("SOAP-ENV");
        envelope.setPrefix("soap");
        envelope.addAttribute(new QName("xmlns"), "urn:oasis:names:tc:dss:1.0:core:schema");
        envelope.addNamespaceDeclaration("dsig", "http://www.w3.org/2000/09/xmldsig#");
        envelope.addNamespaceDeclaration("sc", "urn:com:swisscom:dss:1.0:schema");
        envelope.addNamespaceDeclaration("ais", "http://service.ais.swisscom.com/");

        //SOAP Header
        SOAPHeader soapHeader = envelope.getHeader();
        soapHeader.removeNamespaceDeclaration("SOAP-ENV");
        soapHeader.setPrefix("soap");

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        soapBody.removeNamespaceDeclaration("SOAP-ENV");
        soapBody.setPrefix("soap");

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

            String s = com.itextpdf.text.pdf.codec.Base64.encodeBytes(hashList[i], Base64.DONT_BREAK_LINES);
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
     * Creating connection object and send request to server. If debug is set to true it will print response message.
     *
     * @param soapMsg Message which will be send to server
     * @param urlPath Url of server where to send the request
     * @return Server response
     * @throws Exception If creating connection ,sending request or reading response failed
     */
    @Nullable
    private String sendRequest(@Nonnull SOAPMessage soapMsg, @Nonnull String urlPath) throws Exception {

        URLConnection conn = new allin_connect(urlPath, _privateKeyName, _serverCertPath, _clientCertPath, _timeout, _debug).getConnection();
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
     * @param useTimestmap       If timestamp should be add to a signature
     * @param useOcsp            If ocsp information should be add to a signature
     * @param certRequestProfile Is signature an ondemand signature
     * @return Calculated size of external signature as int
     */
    private int getEstimatedSize(boolean useTimestmap, boolean useOcsp, boolean certRequestProfile) {
        int returnValue = 8192;
        returnValue = useTimestmap ? returnValue + 4192 : returnValue;
        returnValue = useOcsp ? returnValue + 4192 : returnValue;
        returnValue = certRequestProfile ? returnValue + 700 : returnValue;

        return returnValue;
    }

    /**
     * Check if given files exist and are files
     *
     * @param filePaths Files to check
     * @throws FileNotFoundException If file will not be found or is not readable
     */
    private void checkFilesExistsAndIsFile(@Nonnull String[] filePaths) throws FileNotFoundException {

        File file;
        for (String filePath : filePaths) {
            file = new File(filePath);
            if (!file.isFile() || !file.canRead()) {
                throw new FileNotFoundException("File not found or is not a file or not readable: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Convert a xml text which is not formated to a pretty format
     *
     * @param input  Input text
     * @param indent Set indent from left
     * @return Pretty formated xml
     */
    public String getPrettyFormatedXml(@NotNull String input, int indent) {

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

