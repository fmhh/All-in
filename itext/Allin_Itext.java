import com.sun.istack.internal.NotNull;

import java.io.File;

/**
 *  * Created:
 * 18.12.13 KW 51 10:42
 * </p>
 * Last Modification:
 * 18.12.13 KW 51 23:42
 * </p>
 * **********************************************************************************************************
 * This is a wrapper class for Allin_soap class                                                             *
 * Only program arguments will be handled                                                                   *
 * At least Allin_soap will be called with arguments                                                        *
 * **********************************************************************************************************
 * Usage: java allin-itext.java <args> signature pdftosign signedpdf <dn> <msisdn> <msg> <lang>
 * -v        - verbose output
 * -d        - debug mode
 * signature - tsa, static, ondemand
 * pdftosign - PDF to be signed
 * signedpdf - signed PDF
 * <dn>      - optional distinguished name in ondemand
 * <msisdn>  - optional Mobile ID step-up in ondemand
 * <msg>     - optional Mobile ID message, mandatory if msisdn is set
 * <lang>    - optional Mobile ID language element (en, de, fr, it), mandatory if msisdn is set
 * <p/>
 * Examples java allin-itext.java -v tsa sample.pdf signed.pdf
 * java allin-itext.java -v static sample.pdf signed.pdf
 * java allin-itext.java -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'
 * java allin-itext.java -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en
 */

public class Allin_Itext {

    public static void printUsage() {
        System.out.println("Usage: java Allin-itext <args> signature pdftosign signedpdf <dn> <msisdn> <msg> <lang>");
        System.out.println("-v \t \t - verbose output");
        System.out.println("-d \t \t - debug mode");
        System.out.println("signature - tsa, static, ondemand");
        System.out.println("pdftosign - PDF to be signed");
        System.out.println("signedpdf - signed PDF");
        System.out.println("<dn> \t - optional distinguished name in ondemand");
        System.out.println("<msisdn> \t - optional Mobile ID step-up in ondemand");
        System.out.println("<msg> \t - optional Mobile ID message, mandatory if msisdn is set");
        System.out.println("<lang> \t - optional Mobile ID language element (en, de, fr, it), mandatory if msisdn is set");
        System.out.println("Examples java allin-itext -v tsa sample.pdf signed.pdf");
        System.out.println("java allin-itext -v static sample.pdf signed.pdf");
        System.out.println("java allin-itext -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'");
        System.out.println("java allin-itext -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en");
    }

    public static void printError(@NotNull String error) {
        System.out.println(error);
    }

    public static void main(String[] args) {

        boolean verboseOutput = false;
        boolean debugMode = false;
        Allin_Include.Signature signature = null;
        String pdfToSign = null;
        String signedPDF = null;
        String distinguishedName = null;
        String msisdn = null;
        String msg = null;
        String language = null;

        args = new String[]{"-v", "-d", "ondemand", "/Users/fritschka/Desktop/test_pdfs/leere_pdf.pdf",  "/Users/fritschka/Desktop/signed_pdfs/leeres_pdf_dss_ondemand_cert.pdf",
                "cn=Hans Muster,o=Abacus Research AG,c=CH"};

        if (args == null || args.length < 3) {
            printUsage();
            return;
        }

        if (args[0].trim().toLowerCase().equals("-v"))
            verboseOutput = true;

        if (args[0].trim().toLowerCase().equals("-d") || args[1].trim().toLowerCase().equals("-d"))
            debugMode = true;

        int argPointer = debugMode && verboseOutput ? 2 : !debugMode && !verboseOutput ? 0 : !debugMode && verboseOutput || debugMode && !verboseOutput ? 1 : -1;

        signature = Allin_Include.Signature.valueOf(args[argPointer].trim().toUpperCase());
        ++argPointer;

        if (args.length < argPointer + 1) {
            printError("Could not find pdf to sign");
            printUsage();
            return;
        }
        pdfToSign = args[argPointer];
        File filePdfToSign = new File(pdfToSign);
        ++argPointer;

        if (!filePdfToSign.exists() || !filePdfToSign.isFile() || !filePdfToSign.canRead()) {
            printError("File " + pdfToSign + "does not exist or is not a file or can not be read.");
            return;
        }

        if (args.length < argPointer + 1) {
            printError("Could not find output path for signing PDF");
            printUsage();
            return;
        }
        signedPDF = args[argPointer];
        ++argPointer;

        if (args.length >= argPointer + 1 && args[argPointer].replaceAll(" ", "").toLowerCase().contains("cn=")) {
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
            printError("Missing msg parameter");
            printUsage();
            return;
        }

        if (msisdn != null && language == null) {
            printError("Missing language parameter");
            printUsage();
            return;
        }

        Allin_Soap dss_soap = new Allin_Soap();
        try{
        dss_soap.sign(verboseOutput, debugMode, signature, pdfToSign, signedPDF, distinguishedName, msisdn, msg, language);
        } catch (Exception e){
            printError("Signing failed");
            printError(e.getMessage());
        }
    }

}
