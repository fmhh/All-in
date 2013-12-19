/**
 * Created:
 * 03.12.13 KW49 14:51
 * </p>
 * Last Modification:
 * 13.12.13 KW 50 14:21
 * </p>
 * **********************************************************************************
 * Sign PDF using Swisscom DSS                                                      *
 * Tested with iText-5.4.5; Bouncy Castle 1.50 and JDK 1.7.0_45                     *
 * For examples see main method. You only need to change variables in this method.  *
 * **********************************************************************************
 */

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
import java.io.*;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

public class allin_soap {

    private static final String _CLAIMED_IDENTITY_FORMAT = "urn:com:swisscom:dss:v1.0:entity";
    private static final String _CERTIFICATE_REQUEST_PROFILE = "urn:com:swisscom:advanced";
    private static final String _TIMESTAMP_URN = "urn:ietf:rfc:3161";
    private static final String _OCSP_URN = "urn:ietf:rfc:2560";
    private static final String _MOBILE_ID_TYPE = "urn:com:swisscom:auth:mobileid:v1.0";
    private static final String _CFG_PATH = "allin-itext.cfg";
    private Properties properties;
    private String _privateKeyName;
    private String _serverCert;
    private String _clientCert;
    private String _keyStorePath;
    private String _keyStorePass;
    private String _trustStorePath;
    private String _trustStorePass;
    private String _url;
    private int _timeout;
    private boolean _debug = false;
    private boolean _verboseMode = false;

    /**
     * Constructor
     * load properties file and set connection properties from properties file
     */

    public allin_soap() {

        properties = new Properties();

        try {
            properties.load(new FileReader(_CFG_PATH));
        } catch (IOException e) {
            System.out.println("Could not find properties file");
            return;
        }
        setConnectionProperties();
    }

    private void setConnectionProperties(){
        this._clientCert = properties.getProperty("CERT_FILE");
        this._privateKeyName = properties.getProperty("CERT_KEY");
        this._serverCert = properties.getProperty("SSL_CA");
        this._url = properties.getProperty("URL");
        this._keyStorePath = properties.getProperty("KEYSTORE");
        this._trustStorePath = properties.getProperty("TRUSTSTORE");
        this._keyStorePass = properties.getProperty("KEYSTORE_PASSWORD");
        this._trustStorePass = properties.getProperty("TRUSTSTORE_PASSWORD");
        try {
        this._timeout = Integer.parseInt(properties.getProperty("TIMEOUT_CON"));
        } catch (NumberFormatException e){
            this._timeout = 90;
        }
    }

    /**
     *
     * @param verboseOutput
     * @param debugMode
     * @param signatureType TSA, OnDemand, StaticCert
     * @param fileIn
     * @param fileOut
     * @param distinguishedName
     * @param msisdn
     * @param msg
     * @param language
     * @throws Exception
     */
    public void sign(boolean verboseOutput, boolean debugMode, @Nonnull allin_include.Signature signatureType, @Nonnull String fileIn,
                     @Nonnull String fileOut, String distinguishedName, String msisdn, String msg, String language) throws Exception {

        this._debug = debugMode;
        this._verboseMode = verboseOutput;

        if (verboseOutput)
            System.out.println("Loaded properties file successful");

        boolean addTimestamp = properties.getProperty("ADD_TSA").trim().toLowerCase().equals("true");
        boolean addOCSP = properties.getProperty("ADD_OCSP").trim().toLowerCase().equals("true");

        allin_include.HashAlgorithm hashAlgo = allin_include.HashAlgorithm.valueOf(properties.getProperty("DIGEST_METHOD").trim().toUpperCase());

        String claimedIdentityPropName = signatureType.equals(allin_include.Signature.ONDEMAND) ? "AP_ID_ONDEMAND" : signatureType.equals(allin_include.Signature.TSA) ? "AP_ID_TSA" : "AP_ID_STATIC";
        String claimedIdentity = properties.getProperty(claimedIdentityPropName);

        allin_pdf pdf = new allin_pdf(fileIn, fileOut, null, null, null, null);

        if (msisdn != null && msg != null && language != null && signatureType.equals(allin_include.Signature.ONDEMAND)){
            if (verboseOutput)
                System.out.println("Going to sign ondemand with mobile id");
            signDocumentOnDemandCertMobileId(new allin_pdf[]{pdf}, Calendar.getInstance(), hashAlgo, _url, addTimestamp,
                    addOCSP, claimedIdentity, distinguishedName, msisdn, msg, language, (int) (Math.random() * 1000));
            System.out.println("Signing ondemand was successful");
        } else if (signatureType.equals(allin_include.Signature.ONDEMAND))                                                 {
            if (verboseOutput)
                System.out.println("Going to sign with ondemand");
            signDocumentOnDemandCert(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, _CERTIFICATE_REQUEST_PROFILE,
                    addTimestamp, addOCSP, distinguishedName, claimedIdentity, (int) (Math.random() * 1000));
            if (verboseOutput)
                System.out.println("Signing ondemand was successful");
        } else if (signatureType.equals(allin_include.Signature.TSA))                                                       {
            if (verboseOutput)
                System.out.println("Going to sign only with timestamp");
            signDocumentTimestampOnly(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, claimedIdentity,
                    (int) (Math.random() * 1000));
            System.out.println("Signing only with timestamp was successful");
        } else if (signatureType.equals(allin_include.Signature.STATIC))                                                     {
            if (verboseOutput)
                System.out.println("Going to sign with static cert");
            signDocumentStaticCert(new allin_pdf[]{pdf}, hashAlgo, Calendar.getInstance(), _url, addTimestamp, addOCSP,
                    claimedIdentity, (int) (Math.random() * 1000));
            if (verboseOutput)
                System.out.println("Singing with static cert was successful");
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
        } else
            additionalProfiles = new String[1];
        additionalProfiles[0] = allin_include.AdditionalProfiles.ON_DEMAND_CERTIFCATE.getProfileName();

        int estimatedSize = getEstimatedSize(addTimestamp, addOcsp, true);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), false);
        }

        SOAPMessage sigReqMsg = createRequestMessage(allin_include.RequestType.SignRequest, hashAlgo.getHashUri(), _CERTIFICATE_REQUEST_PROFILE,
                pdfHash, addTimestamp ? _TIMESTAMP_URN : null, addOcsp ? _OCSP_URN : null, additionalProfiles, _CLAIMED_IDENTITY_FORMAT,
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
                                         @Nonnull String distinguishedName, @Nonnull String claimedIdentity, int requestId) throws Exception {

        String[] additionalProfiles;
        if (pdfs.length > 1) {
            additionalProfiles = new String[2];
            additionalProfiles[1] = allin_include.AdditionalProfiles.BATCH.getProfileName();
        } else
            additionalProfiles = new String[1];
        additionalProfiles[0] = allin_include.AdditionalProfiles.ON_DEMAND_CERTIFCATE.getProfileName();

        int estimatedSize = getEstimatedSize(addTimeStamp, addOcsp, true);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), false);
        }

        SOAPMessage sigReqMsg = createRequestMessage(allin_include.RequestType.SignRequest, hashAlgo.getHashUri(), certRequestProfile,
                pdfHash, addTimeStamp ? _TIMESTAMP_URN : null, addOcsp ? _OCSP_URN : null, additionalProfiles, _CLAIMED_IDENTITY_FORMAT,
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
                                       boolean addTimeStamp, boolean addOCSP, @Nonnull String claimedIdentity, int requestId) throws Exception {

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
                pdfHash, addTimeStamp ? _TIMESTAMP_URN : null, addOCSP ? _OCSP_URN : null, additionalProfiles, _CLAIMED_IDENTITY_FORMAT,
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
                                          @Nonnull String serverURI, @Nonnull String claimedIdentity, int requestId) throws Exception {

        allin_include.SignatureType signatureType = allin_include.SignatureType.TIMESTAMP;

        String[] additionalProfiles;
        if (pdfs.length > 1) {
            additionalProfiles = new String[2];
            additionalProfiles[1] = allin_include.AdditionalProfiles.BATCH.getProfileName();
        } else
            additionalProfiles = new String[1];
        additionalProfiles[0] = allin_include.AdditionalProfiles.TIMESTAMP.getProfileName();

        int estimatedSize = getEstimatedSize(true, true, false);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), true);
        }

        SOAPMessage sigReqMsg = createRequestMessage(allin_include.RequestType.SignRequest, hashAlgo.getHashUri(), null,
                pdfHash, null, null, additionalProfiles, _CLAIMED_IDENTITY_FORMAT, claimedIdentity, signatureType.getSignatureType(),
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
    private void signDocumentSync(@Nonnull SOAPMessage sigReqMsg, @Nonnull String serverURI, @Nonnull allin_pdf[] pdfs, int estimatedSize, String signNodeName) throws Exception {

        String sigResponse = sendRequest(sigReqMsg, serverURI);

        ArrayList<String> responseResult = getTextFromXmlText(sigResponse, "ResultMajor");

        if (responseResult == null || !allin_include.RequestResult.Success.getResultUrn().equals(responseResult.get(0)))
            throw new Exception("Getting signatures failed. Result: " + responseResult);

        ArrayList<String> signHashes = getTextFromXmlText(sigResponse, signNodeName);
        signDocuments(signHashes, pdfs, estimatedSize);
    }

    /**
     * Sign document
     *
     * @param signatureList
     * @param pdfs
     * @param estimatedSize
     */
    private void signDocuments(@Nonnull ArrayList<String> signatureList, @Nonnull allin_pdf[] pdfs, int estimatedSize) {
        int counter = 0;
        for (String signatureHash : signatureList) {
            pdfs[counter].sign(signatureHash, estimatedSize);
            counter++;
        }
    }

    /**
     * Get nodes text content
     *
     * @param soapResponseText can be a full response as xml
     * @param nodeName
     * @return if nodes with searched node names exist it will return an array list containing text from value from nodes
     */
    @Nullable
    private ArrayList<String> getTextFromXmlText(String soapResponseText, String nodeName) throws Exception {

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
                if (returnlist == null)
                    returnlist = new ArrayList<>();
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
     * @param claimedIdentityFormat
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
                                             String[] additionalProfiles, String claimedIdentityFormat, String claimedIdentity,
                                             @Nonnull String signatureType, String distinguishedName,
                                             String mobileIdType, String phoneNumber, String certReqMsg, String certReqMsgLang,
                                             String responseId, int requestId) throws SOAPException, IOException {

        if (_verboseMode)
            System.out.println("Creating server request message");

        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addAttribute(new QName("xmlns"), "urn:oasis:names:tc:dss:1.0:core:schema");
        envelope.addNamespaceDeclaration("dsig", "http://www.w3.org/2000/09/xmldsig#");
        envelope.addNamespaceDeclaration("ns5", "urn:com:swisscom:dss:1.0:schema");
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
            if (additionalProfiles != null)
                for (String additionalProfile : additionalProfiles) {
                    additionalProfileelement = optionalInputsElement.addChildElement("AdditionalProfile");
                    additionalProfileelement.addTextNode(additionalProfile);
                }

            if (claimedIdentity != null && claimedIdentityFormat != null) {
                SOAPElement claimedIdentityElement = optionalInputsElement.addChildElement(new QName("ClaimedIdentity"));
                if (!_CERTIFICATE_REQUEST_PROFILE.equals(certRequestProfile))
                    claimedIdentityElement.addAttribute(new QName("Format"), claimedIdentityFormat);
                SOAPElement claimedIdNameElement = claimedIdentityElement.addChildElement("Name");
                claimedIdNameElement.addTextNode(claimedIdentity);
            }

            if (certRequestProfile != null) {
                SOAPElement certificateRequestElement = optionalInputsElement.addChildElement("CertificateRequest", "ns5");
                if (!_CERTIFICATE_REQUEST_PROFILE.equals(certRequestProfile))
                    certificateRequestElement.addAttribute(new QName("Profile"), certRequestProfile);
                if (distinguishedName != null) {
                    SOAPElement distinguishedNameElement = _CERTIFICATE_REQUEST_PROFILE.equals(certRequestProfile) ?
                            certificateRequestElement.addChildElement("DistinguishedName", "ns5") :
                            certificateRequestElement.addChildElement("DistinguishedName");
                    distinguishedNameElement.addTextNode(distinguishedName);
                    if (phoneNumber != null) {
                        SOAPElement stepUpAuthorisationElement = certificateRequestElement.addChildElement("StepUpAuthorisation", "ns5");

                        if (mobileIdType != null && phoneNumber != null) {
                            SOAPElement mobileIdElement = stepUpAuthorisationElement.addChildElement("MobileID", "ns5");
                            SOAPElement msisdnElement = mobileIdElement.addChildElement("MSISDN", "ns5");
                            msisdnElement.addTextNode(phoneNumber);
                            SOAPElement certReqMsgElement = mobileIdElement.addChildElement("Message", "ns5");
                            certReqMsgElement.addTextNode(certReqMsg);
                            SOAPElement certReqMsgLangElement = mobileIdElement.addChildElement("Language", "ns5");
                            certReqMsgLangElement.addTextNode(certReqMsgLang.toUpperCase());
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
                SOAPElement addOcspElement = optionalInputsElement.addChildElement("AddOcspResponse", "ns5");
                addOcspElement.addAttribute(new QName("Type"), ocspURN);
            }

            if (responseId != null) {
                SOAPElement responseIdElement = optionalInputsElement.addChildElement("ResponseID");
                responseIdElement.addTextNode(responseId);
            }
        }

        soapMessage.saveChanges();

        if (_debug) {
            System.out.print("Request SOAP Message = ");
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            soapMessage.writeTo(ba);
            String msg = new String(ba.toByteArray()).replaceAll("><", ">\n<");
            System.out.println(msg);
        }

        if (_verboseMode)
            System.out.println("Creating server request message was successful");

        return soapMessage;
    }

    /**
     * Send request to a server. If debug is set to true it will print response message.
     *
     * @param soapMsg
     * @param urlPath
     * @return Server response as string
     * @throws SOAPException
     * @throws IOException
     */
    @Nullable
    private String sendRequest(@Nonnull SOAPMessage soapMsg, @Nonnull String urlPath) throws IOException, SOAPException {

        if (_verboseMode)
            System.out.println("Creating connection object");

        URLConnection conn = new allin_connect(urlPath, _privateKeyName, _serverCert, _clientCert, _keyStorePath, _trustStorePath,
                _keyStorePass, _trustStorePass, _timeout, _debug).getConnection();
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setRequestMethod("POST");
        }

        conn.setAllowUserInteraction(true);
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setDoOutput(true);

        if (_verboseMode)
            System.out.println("Creating connection object successful");

        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMsg.writeTo(baos);
        String msg = baos.toString();

        if (_verboseMode)
            System.out.println("Get response from server");

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

        if (_debug)
            System.out.println("response : " + response.replaceAll("><", ">\n<"));
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

}

