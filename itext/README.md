allin-cmd: iText
============

Java source code and command line tool to sign PDF with iText.

### Usage

````
Usage: swisscom.com.ais.itext.allin_itext <options> signature pdftosign signedpdf [dn] [[msisdn]] [[msg]] [[lang]]
-v          - verbose output
-d          - debug mode
signature   - timestamp, sign
pdftosign   - PDF to be signed
signedpdf   - signed PDF
[dn]        - optional distinguished name for on-demand certificate signing
[[msisdn]]  - optional Mobile ID authentication when [dn] is present
[[msg]]     - optional Mobile ID message when [dn] is present
[[lang]]    - optional Mobile ID language (en, de, fr, it) when [dn] is present

Example: java swisscom.com.ais.itext.allin_itext -v timestamp sample.pdf signed.pdf
         java swisscom.com.ais.itext.allin_itext -v sign sample.pdf signed.pdf
         java swisscom.com.ais.itext.allin_itext -v sign sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'
         java swisscom.com.ais.itext.allin_itext -v sign sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en
```

### Dependencies

To run this java example you need to download the following external libraries from given sources:

1: http://mvnrepository.com/artifact/com.google.code.findbugs/jsr305

Version 2.0.2 has been successfully tested

2: http://sourceforge.net/projects/itext

Version 5.4.5 has been successfully tested

3: http://www.bouncycastle.org/latest_releases.html

bcprov-jdk15on-150.jar has been successfully tested
bcpkix-jdk15on-150.jar has been successfully tested

### Configuration

Refer to `allin_itext.properties` configuration file.

### Compile & Run

The source files can be compiled as follows. The following placeholder need to be replaced accordingly:
```
<SRC>   = Directory containing the java source files
<LIB>   = Directory containing the external libraries (jar files)
<CLASS> = Directory where to place the generated class files
<CFG>   = Location (path) of the allin_itext.properties
```

Compile the sources: `javac -d <CLASS> -cp ".:<LIB>/*" <SRC>/*.java`

Note: The class files are always generated in a directory hierarchy which reflects the given package structure: `<CLASS>/swisscom/com/ais/itext/*.class`

The compiled application can be run as follows.

Run the application (Unix/OSX):
`java -cp "<CLASS>:<LIB>/*" swisscom.com.ais.itext.allin_itext`

Run the application (Unix/OSX) with custom path to the properties file:
`java -DpropertyFile.path=<CFG> -cp "<CLASS>:<LIB>/*" swisscom.com.ais.itext.allin_itext`

Run the application (Unix/OSX) with DEBUG enabled:
`java -Djavax.net.debug=all -Djava.security.debug=certpath -cp ".:<CLASS>:<LIB>/*" swisscom.com.ais.itext.allin_itext`

If you're on Windows then use a semicolon ; instead of the colon : 

### Run executable JAR file

Alternatively you can run the executable JAR file that contains already the compiled java code. It is located in the `./jar` directory. 

Run the application (Unix/OSX): `java -cp "allin_itext.jar:<LIB>/*" swisscom.com.ais.itext.allin_itext`

Run the application (Unix/OSX) with custom path to the properties file:
`java -DpropertyFile.path=<CFG> -cp "allin_itext.jar:<LIB>/*" swisscom.com.ais.itext.allin_itext`

Run the application (Unix/OSX) with DEBUG enabled:
`java -Djavax.net.debug=all -Djava.security.debug=certpath -cp "allin_itext.jar:<LIB>/*" swisscom.com.ais.itext.allin_itext`


If you're on Windows then use a semicolon ; instead of the colon : 
