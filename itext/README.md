allin-cmd: iText
============

Java source code and command line tool to sign PDF with iText.

### Usage

````
Usage: java <javaoptions> swisscom/com/ais/itext/allin_itext <allin_itext_args> signature pdftosign signedpdf <dn> <msisdn> <msg> <lang>
-v          - verbose output
-d          - debug mode
signature   - timestamp, sign
pdftosign   - PDF to be signed
signedpdf   - signed PDF
[dn]        - optional distinguished name for on-demand certificate signing
[[msisdn]]  - optional Mobile ID authentication when [dn] is present
[[msg]]     - optional Mobile ID message when [dn] is present
[lang]      - optional Mobile ID language (en, de, fr, it) when [dn] is present

Examples java swisscom/com/ais/itext/allin_itext -v timestamp sample.pdf signed.pdf
         java swisscom/com/ais/itext/allin_itext -v sign sample.pdf signed.pdf
         java swisscom/com/ais/itext/allin_itext -v sign sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'
         java swisscom/com/ais/itext/allin_itext -v sign sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en
```

### Dependencies

To run this java example you need to download the following files from given sources:

1: http://mvnrepository.com/artifact/com.google.code.findbugs/jsr305

Version 2.0.2 is successfully tested

2: http://sourceforge.net/projects/itext

Version 5.4.5 is successfully tested

3: http://www.bouncycastle.org/latest_releases.html

bcprov-jdk15on-150.jar is successfully tested
bcpkix-jdk15on-150.jar is successfully tested

### Configuration

Refer to `allin_itext.properties` configuration file for related settings.

### Compiling

After downloading this files you need to compile the java sources (maybe you have to change the location of jar-files).
It is necessary to move the java sources in following directory structure: ./swisscom/com/ais/itext/. To compile and run
the java source files you need to be at the directory where `swisscom/com/ais/itext/s` exist:

Linux/OSX: `javac -cp .:bcprov-jdk15on-150.jar:bcpkix-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext.java`
Windows  : `javac -cp .;bcprov-jdk15on-150.jar;bcpkix-jdk15on-150.jar;itextpdf-5.4.5.jar;jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext.java`

### Running

Now you can run the program with:

Linux/OSX: `java -cp .:bcprov-jdk15on-150.jar:bcpkix-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext`
Windows  : `java -cp .;bcprov-jdk15on-150.jar;bcpkix-jdk15on-150.jar;itextpdf-5.4.5.jar;jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext`

To get more debugging information you have to set java options. This looks as follows:

Linux/OSX: `java -cp .:bcprov-jdk15on-150.jar:bcpkix-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext -Djavax.net.debug=all -Djava.security.debug=certpath`
Windows  : `java -cp .;bcprov-jdk15on-150.jar;bcpkix-jdk15on-150.jar;itextpdf-5.4.5.jar;jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext -Djavax.net.debug=all -Djava.security.debug=certpath`
