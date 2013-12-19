package ch.swisscom;

/**
 * Creator:
 * 19.12.13 KW51 08:04 fritschka
 * <p/>
 * Maintainer:
 * 19.12.13 KW51 08:04 fritschka
 * <p/>
 * Last Modification:
 * <p/>
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
public class Allin_Include {

    public enum HashAlgorithm {

        SHA256("SHA-256", "http://www.w3.org/2001/04/xmlenc#sha256"),
        SHA384("SHA-384", "http://www.w3.org/2001/04/xmldsig-more#sha384"),
        SHA512("SHA-512", "http://www.w3.org/2001/04/xmlenc#sha512"),
        RIPEMD160("RIPEMD-160", "http://www.w3.org/2001/04/xmlenc#ripemd160");

        private String hashAlgo;
        private String hashUri;

        HashAlgorithm(String hashAlgo, String hashUri) {
            this.hashAlgo = hashAlgo;
            this.hashUri = hashUri;
        }

        public String getHashAlgorythm() {
            return this.hashAlgo;
        }

        public String getHashUri() {
            return this.hashUri;
        }

    }

    public enum RequestResult {

        Pending("urn:oasis:names:tc:dss:1.0:profiles:asynchronousprocessing:resultmajor:Pending"),
        Success("urn:oasis:names:tc:dss:1.0:resultmajor:Success");

        private String resultUrn;

        RequestResult(String urn) {
            this.resultUrn = urn;
        }

        public String getResultUrn(){
            return this.resultUrn;
        }
    }

    public enum RequestType {

        SignRequest("SignRequest", "urn:com:swisscom:dss:v1.0"),
        PendingRequest("PendingRequest", "urn:com:swisscom:dss:v1.0");

        private String urn;
        private String requestType;

        RequestType(String reqType, String urn) {
            this.requestType = reqType;
            this.urn = urn;
        }

        public String getRequestType() {
            return this.requestType;
        }

        public String getUrn(){
            return this.urn;
        }

    }

    public enum Signature {
        TSA("tsa"),
        STATIC("static"),
        ONDEMAND("ondemand");

        private String signature;

        Signature(String signature) {
            this.signature = signature;
        }

        public String getSignature() {
            return this.signature;
        }
    }

    public enum SignatureType {

        CMS("urn:ietf:rfc:3369"),
        TIMESTAMP("urn:ietf:rfc:3161");

        private String signatureType;

        SignatureType(String signatureType) {
            this.signatureType = signatureType;
        }

        public String getSignatureType() {
            return this.signatureType;
        }

    }

    public enum AdditionalProfiles {

        ASYNCHRON("urn:oasis:names:tc:dss:1.0:profiles:asynchronousprocessing"),
        BATCH("urn:com:swisscom:dss:v1.0:profiles:batchprocessing"),
        ON_DEMAND_CERTIFCATE("urn:com:swisscom:dss:v1.0:profiles:ondemandcertificate"),
        TIMESTAMP("urn:oasis:names:tc:dss:1.0:profiles:timestamping");

        private String profile;

        AdditionalProfiles(String s) {
            this.profile = s;
        }

        public String getProfileName() {
            return this.profile;
        }

    }

}
