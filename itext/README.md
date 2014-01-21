allin-cmd: iText
============

Java source code and command line tool to sign PDF with iText.

### Usage

````
Usage: java <javaoptions> allin_itext <allin_itext_args> signature pdftosign signedpdf <dn> <msisdn> <msg> <lang>
-v        - verbose output
-d        - debug mode
signature - timestamp, static, ondemand
pdftosign - PDF to be signed
signedpdf - signed PDF
<dn>      - optional distinguished name in ondemand
<msisdn>  - optional Mobile ID step-up in ondemand
<msg>     - optional Mobile ID message, mandatory if msisdn is set
<lang>    - optional Mobile ID language element (en, de, fr, it), mandatory if msisdn is set

Examples java allin_itext -v timestamp sample.pdf signed.pdf
         java allin_itext -v static sample.pdf signed.pdf
         java allin_itext -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'
         java allin_itext -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en
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

Windows  : `java -cp .;bcprov-jdk15on-150.jar;bcpkix-jdk15on-150.jar;itextpdf-5.4.5.jar;jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext`
Linux/OSX: `java -cp .:bcprov-jdk15on-150.jar:bcpkix-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext`

To get more debugging information you have to set java options. This looks as follows:

Windows  : `java -cp .;bcprov-jdk15on-150.jar;bcpkix-jdk15on-150.jar;itextpdf-5.4.5.jar;jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext -Djavax.net.debug=all -Djava.security.debug=certpath`
Linux/OSX: `java -cp .:bcprov-jdk15on-150.jar:bcpkix-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar swisscom/com/ais/itext/allin_itext -Djavax.net.debug=all -Djava.security.debug=certpath`
