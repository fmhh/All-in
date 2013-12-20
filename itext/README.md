allin-cmd: iText
============

Java source code and command line to sign PDF with iText.

### Usage

````
Usage: allin_itext.sh <allin_itext_args> signature pdftosign signedpdf <dn> <msisdn> <msg> <lang>
-v        - verbose output
-d        - debug mode
signature - tsa, static, ondemand
pdftosign - PDF to be signed
signedpdf - signed PDF
<dn>      - optional distinguished name in ondemand
<msisdn>  - optional Mobile ID step-up in ondemand
<msg>     - optional Mobile ID message, mandatory if msisdn is set
<lang>    - optional Mobile ID language element (en, de, fr, it), mandatory if msisdn is set

Examples ./allin_itext.sh -v tsa sample.pdf signed.pdf
         ./allin_itext.sh -v static sample.pdf signed.pdf
         ./allin_itext.sh -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'
         ./allin_itext.sh -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en
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

### Compiling

After downloading this files you need to compile the java sources (maybe you have to change the location of jar-files):

`javac -cp .:bcprov-jdk15on-150.jar:bcpkix-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar allin_itext.java`

### Running

Now you can run the program with java `java -cp .:bcprov-jdk15on-150.jar:bcpkix-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar allin_itext` or by using the launcher script.

### Configuration

Refer to `allin_itext.cfg` configuration file for related settings.

### Known issues

n/a