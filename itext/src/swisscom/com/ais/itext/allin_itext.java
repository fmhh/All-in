/**
 * Created:
 * 18.12.13 KW 51 10:42
 * </p>
 * Last Modification:
 * 22.01.2014 17:22
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
     * Prints usage of allin
     */
    public static void printUsage() {
        System.out.println("Usage: swisscom.com.ais.itext.allin_itext <options> signature pdftosign signedpdf [dn] [[msisdn]] [[msg]] [[lang]]");
        System.out.println("-v          - verbose output");
        System.out.println("-d          - debug mode");
        System.out.println("signature   - timestamp, sign");
        System.out.println("pdftosign   - PDF to be signed");
        System.out.println("signedpdf   - signed PDF");
        System.out.println("[dn]        - optional distinguished name for on-demand certificate signing");
        System.out.println("[[msisdn]]  - optional Mobile ID authentication when [dn] is present");
        System.out.println("[[msg]]     - optional Mobile ID message when [dn] is present");
        System.out.println("[[lang]]    - optional Mobile ID language (en, de, fr, it) when [dn] is present");
        System.out.println("");
        System.out.println("Example: java swisscom.com.ais.itext.allin_itext -v timestamp sample.pdf signed.pdf");
        System.out.println("         java swisscom.com.ais.itext.allin_itext -v sign sample.pdf signed.pdf");
        System.out.println("         java swisscom.com.ais.itext.allin_itext -v sign sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'");
        System.out.println("         java swisscom.com.ais.itext.allin_itext -v sign sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en");
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
     * Main method to start allin. This will parse given parameters e.g. input file, output file etc. and start signature
     * process. Furthermore this method prints error message if signing failed. See usage part in README to know how to
     * use it.
     *
     * @param args Arguments that will be parsed. See useage part in README for more details.
     */
    public static void main(String[] args) {

        allin_include.Signature signature = null;
        String pdfToSign = null;
        String signedPDF = null;
        String distinguishedName = null;
        String msisdn = null;
        String msg = null;
        String language = null;

        if (args == null || args.length < 3) {
            printUsage();
            System.exit(1);
        }

        if (args[0].trim().toLowerCase().equals("-v") || args[1].trim().toLowerCase().equals("-v")) {
            verboseMode = true;
        }

        if (args[0].trim().toLowerCase().equals("-d") || args[1].trim().toLowerCase().equals("-d")) {
            debugMode = true;
        }

        int argPointer = debugMode && verboseMode ? 2 : !debugMode && !verboseMode ? 0 : !debugMode && verboseMode || debugMode && !verboseMode ? 1 : -1;

        try {
            signature = allin_include.Signature.valueOf(args[argPointer].trim().toUpperCase());
            ++argPointer;
        } catch (IllegalArgumentException e) {
            if (debugMode || verboseMode) {
                printError(args[argPointer] + " is not a valid signature.");
            }
            printUsage();
            System.exit(1);
        }

        if (args.length < argPointer + 1) {
            if (debugMode || verboseMode) {
                printError("Could not find pdf to sign");
            }
            printUsage();
            System.exit(1);
        }

        pdfToSign = args[argPointer];
        File filePdfToSign = new File(pdfToSign);
        ++argPointer;

        if (!filePdfToSign.isFile() || !filePdfToSign.canRead()) {
            if (debugMode || verboseMode) {
                printError("File " + pdfToSign + " does not exist or is not a file or can not be read.");
            }
            System.exit(1);
        }

        if (args.length < argPointer + 1) {
            if (debugMode || verboseMode) {
                printError("Could not find output path for signing PDF");
            }
            printUsage();
            System.exit(1);
        }

        signedPDF = args[argPointer];
        if (signedPDF.equals(pdfToSign)) {
            if (debugMode || verboseMode) {
                printError("Source file equals target file");
            }
            System.exit(1);
        }
        
        if (new File(signedPDF).isFile()) {
	    if (debugMode || verboseMode) {
              printError("Target file exists");
            }
            System.exit(1);
        }
        ++argPointer;

        if (args.length >= argPointer + 1) {
            distinguishedName = args[argPointer];
            ++argPointer;
        }

        if (args.length >= argPointer + 1) {
            msisdn = args[argPointer];
            ++argPointer;
        }

        if (args.length >= argPointer + 1) {
            msg = args[argPointer];
            ++argPointer;
        }

        if (args.length >= argPointer + 1) {
            language = args[argPointer];
            ++argPointer;
        }

        if (msisdn != null && msg == null) {
            if (debugMode || verboseMode) {
                printError("Missing msg parameter");
            }
            printUsage();
            System.exit(1);
        }

        if (msisdn != null && language == null) {
            if (debugMode || verboseMode) {
                printError("Missing language parameter");
            }
            printUsage();
            System.exit(1);
        }

        try {
            //parse signature
            if (signature.equals(allin_include.Signature.SIGN) && distinguishedName != null) {
                signature = allin_include.Signature.ONDEMAND;
            } else if (signature.equals(allin_include.Signature.SIGN) && distinguishedName == null) {
                signature = allin_include.Signature.STATIC;
            }

            allin_soap dss_soap = new allin_soap(verboseMode, debugMode, System.getProperty("propertyFile.path"));
            dss_soap.sign(signature, pdfToSign, signedPDF, distinguishedName, msisdn, msg, language);
        } catch (Exception e) {
            if (debugMode || verboseMode) {
                printError(e.getMessage().replaceAll("java.lang.Exception", "").length() > 0 ? e.getMessage() : "");
            }
            System.exit(1);
        }
    }

}
