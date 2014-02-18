/**
 * Created:
 * 18.12.13 KW 51 10:42
 * </p>
 * Last Modification:
 * 17.02.2014 15:12
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
 * </p>
 * **********************************************************************************************************
 * This is a wrapper class for allin_soap class                                                             *
 * Only program arguments will be handled                                                                   *
 * At least allin_soap will be called with arguments                                                        *
 * **********************************************************************************************************
 */

package swisscom.com.ais.itext;

import javax.annotation.*;
import java.io.File;

public class allin_itext {

    /**
     * The value is used to decide if verbose information should be print
     */
    static boolean verboseMode = false;

    /**
     * The value is used to decide if debug information should be print
     */
    static boolean debugMode = false;

    /**
     * The signature type. E.g. timestamp, sign, ...
     */
    allin_include.Signature signature = null;

    /**
     * Path to pdf which get a signature
     */
    String pdfToSign = null;

    /**
     * Path to output document with generated signature
     */
    String signedPDF = null;

    /**
     * Reason for signing a document.
     */
    String signingReason = null;

    /**
     * Location where a document was signed
     */
    String signingLocation = null;

    /**
     * Person who signed the document
     */
    String signingContact = null;

    /**
     * Distinguished name contains information about signer. Needed for ondemand signature
     */
    String distinguishedName = null;

    /**
     * Mobile phone number to send a message when signing a document. Needed for signing with mobile id
     */
    String msisdn = null;

    /**
     * Message which will be send to mobile phone with mobile id. Needed for signing with mobile id.
     */
    String msg = null;

    /**
     * Language of the message which will be send to mobile phone with mobile id. Needed for signing with mobile id.
     */
    String language = null;

    /**
     * Path for properties file. Needed if standard path will not be used.
     */
    String propertyFilePath = null;

    /**
     * Prints usage of allin
     */
    public static void printUsage() {
        System.out.println("Usage: swisscom.com.ais.itext.allin_itext <options>");
        System.out.println("-v              - verbose output");
        System.out.println("-d              - debug mode");
        System.out.println("-mode           - timestamp, sign");
        System.out.println("-infile         - PDF to be signed");
        System.out.println("-outfile        - signed PDF");
        System.out.println("[-reason]       - optional singing reason");
        System.out.println("[-location]     - optional signign location");
        System.out.println("[-contact]      - optional person who signed document");
        System.out.println("[-dn]           - optional distinguished name for on-demand certificate signing");
        System.out.println("[[-msisdn]]     - optional Mobile ID authentication when [dn] is present");
        System.out.println("[[-msg]]        - optional Mobile ID message when [dn] is present");
        System.out.println("[[-lang]]       - optional Mobile ID language (en, de, fr, it) when [dn] is present");
        System.out.println("[-prop_file]    - optional path to properties file when standard path will not be used");
        System.out.println("");
        System.out.println("Example: java swisscom.com.ais.itext.allin_itext -v -mode=timestamp -infile='sample.pdf' -outfile='signed.pdf'");
        System.out.println("         java swisscom.com.ais.itext.allin_itext -v -mode=sign -infile='sample.pdf' -outfile='signed.pdf' -prop_file='/tmp/dss.properties'");
        System.out.println("         java swisscom.com.ais.itext.allin_itext -v -mode=sign -infile='sample.pdf' -outfile='signed.pdf' -dn='cn=Hans Muster,o=ACME,c=CH'");
        System.out.println("         java swisscom.com.ais.itext.allin_itext -v -mode=sign -infile='sample.pdf' -outfile='signed.pdf' -dn='cn=Hans Muster,o=ACME,c=CH' -msisdn='+41792080350' -msg='service.com: Sign?' -lang=en");
    }

    /**
     * Prints error message
     *
     * @param error Message that should print
     */
    public static void printError(@Nonnull String error) {
        System.err.println(error);
    }

    /**
     * Parse given parameters. If an error occurs application with exit with code 1. If debug and/or verbose mode is set
     * an error message will be shown
     * @param args
     */
    private void parseParameters(String[] args) {
        String param;

        if (args == null || args.length < 3) {
            printUsage();
            System.exit(1);
        }

        for (int i = 0; i < args.length; i++) {

            param = args[i].toLowerCase();

            if (param.contains("-mode=")) {
                String signatureString = null;
                try {
                    signatureString = args[i].substring(args[i].indexOf("=") + 1).trim().toUpperCase();
                    signature = allin_include.Signature.valueOf(signatureString);
                } catch (IllegalArgumentException e) {
                    if (debugMode || verboseMode) {
                        printError(signatureString + " is not a valid signature.");
                    }
                    printUsage();
                    System.exit(1);
                }
            } else if (param.contains("-infile=")) {
                pdfToSign = args[i].substring(args[i].indexOf("=") + 1).trim();
                File pdfToSignFile = new File(pdfToSign);

                if (!pdfToSignFile.isFile() || !pdfToSignFile.canRead()) {
                    if (debugMode || verboseMode) {
                        printError("File " + pdfToSign + " is not a file or can not be read.");
                    }
                    System.exit(1);
                }
            } else if (param.contains("-outfile=")) {
                signedPDF = args[i].substring(args[i].indexOf("=") + 1).trim();
                String errorMsg = null;
                if (signedPDF.equals(pdfToSign)) {
                    errorMsg = "Source file equals target file.";
                } else if (!new File(signedPDF).getParentFile().isDirectory()) {
                    errorMsg = "Can not create target file in given path.";
                } else if (new File(signedPDF).isFile()) {
                    errorMsg = "Target file exist.";
                }
                if (errorMsg != null) {
                    if (debugMode || verboseMode) {
                        printError(errorMsg);
                    }
                    System.exit(1);
                }
            } else if (param.contains("-reason")) {
                signingReason = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-location")) {
                signingLocation = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-contact")) {
                signingContact = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-dn=")) {
                distinguishedName = args[i].substring(args[i].indexOf("=") - 1).trim();
            } else if (param.contains("-msisdn=")) {
                msisdn = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-msg=")) {
                msg = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-lang=")) {
                language = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-prop_file=")) {
                propertyFilePath = args[i].substring(args[i].indexOf("=") + 1).trim();
                File propertyFile = new File(propertyFilePath);
                if (!propertyFile.isFile() || !propertyFile.canRead()) {
                    if (debugMode || verboseMode) {
                        printError("Property file path is set but file does not exist or can not read it.");
                    }
                    System.exit(1);
                }
            } else if (args[i].toLowerCase().contains("-v")) {
                verboseMode = true;
            } else if (param.contains("-d")) {
                debugMode = true;
            }
        }
    }

    /**
     * Check if needed parameters are given. If not method will print an error and exit with code 1
     */
    private void checkNecessaryParams() {

        if (pdfToSign == null) {
            if (debugMode || verboseMode) {
                printError("Input file does not exist.");
            }
            System.exit(1);
        }

        if (signedPDF == null) {
            if (debugMode || verboseMode) {
                printError("Output file does not exist.");
            }
            System.exit(1);
        }
    }

    /**
     * This method checks if there are unnecessary parameters. If there are some it will print the usage of parameters
     * and exit with code 1 (e.g. DN is given for signing with timestamp)
     */
    private void checkUnnecessaryParams() {

        if (signature.equals(allin_include.Signature.TIMESTAMP)) {
            if (distinguishedName != null || msisdn != null || msg != null || language != null) {
                if (debugMode || verboseMode) {
                    printUsage();
                }
                System.exit(1);
            }
        } else {
            if (!(distinguishedName == null && msisdn == null && msg == null && language == null ||
                    distinguishedName != null && msisdn == null && msg == null && language == null ||
                    distinguishedName != null && msisdn != null && msg != null && language != null)) {
                if (debugMode || verboseMode) {
                    printUsage();
                }
                System.exit(1);
            }
        }
    }

    /**
     * Parse given parameters, check if all necessary parameters exist and if there are not unnecessary parameters.
     * If there are problems with parameters application will abort with exit code 1.
     * After all checks are done signing process will start.
     *
     * @param params
     */
    private void runSigning(String[] params) {

        parseParameters(params);
        checkNecessaryParams();
        checkUnnecessaryParams();

        try {
            //parse signature
            if (signature.equals(allin_include.Signature.SIGN) && distinguishedName != null) {
                signature = allin_include.Signature.ONDEMAND;
            } else if (signature.equals(allin_include.Signature.SIGN) && distinguishedName == null) {
                signature = allin_include.Signature.STATIC;
            }

            //start signing
            allin_soap dss_soap = new allin_soap(verboseMode, debugMode, propertyFilePath);
            dss_soap.sign(signature, pdfToSign, signedPDF, signingReason, signingLocation, signingContact, distinguishedName, msisdn, msg, language);
        } catch (Exception e) {
            if (debugMode || verboseMode) {
                printError(e.getMessage().replaceAll("java.lang.Exception", "").length() > 0 ? e.getMessage() : "");
            }
            System.exit(1);
        }
    }

    /**
     * Main method to start allin. This will parse given parameters e.g. input file, output file etc. and start signature
     * process. Furthermore this method prints error message if signing failed. See usage part in README to know how to
     * use it.
     *
     * @param args Arguments that will be parsed. See useage part in README for more details.
     */
    public static void main(String[] args) {

        allin_itext allin = new allin_itext();
        allin.runSigning(args);

    }

}
