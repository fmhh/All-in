All-in: iText
============

Java source code and command line tool to sign PDF with iText.

### Usage

````
Usage: swisscom.com.ais.itext.allin_itext <options>
-v              - verbose output
-d              - debug mode
-mode           - timestamp, sign
-infile         - PDF to be signed
-outfile        - signed PDF
[-reason]       - optional signing reason
[-location]     - optional signing location
[-contact]      - optional signing contact
[-dn]           - optional distinguished name for on-demand certificate signing
[[-msisdn]]     - optional Mobile ID authentication when [dn] is present
[[-msg]]        - optional Mobile ID message when [dn] is present
[[-lang]]       - optional Mobile ID language (en, de, fr, it) when [dn] is present
[-prop_file]    - optional path to properties file when standard path will not be used

Example java swisscom.com.ais.itext.allin_itext -v -mode=timestamp -infile='sample.pdf' -outfile='signed.pdf'
        java swisscom.com.ais.itext.allin_itext -v -mode=sign -infile='sample.pdf' -outfile='signed.pdf' -reason='Ok' -location='Zuerich' -contact='Musterperson' -prop_file='/tmp/dss.properties'
        java swisscom.com.ais.itext.allin_itext -v -mode=sign -infile='sample.pdf' -outfile='signed.pdf' -dn='cn=Hans Muster,o=ACME,c=CH'
        java swisscom.com.ais.itext.allin_itext -v -mode=sign -infile='sample.pdf' -outfile='signed.pdf' -dn='cn=Hans Muster,o=ACME,c=CH' -msisdn='+41792080350' -msg='service.com: Sign?' -lang=en
```

### Dependencies

This java application has external dependencies (libraries). They are located in the `./lib` subfolder.
The latest version may be downloaded from the following source:

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
<SRC>   = Path to the ./src subfolder containing the *.java source files, e.g. ./All-in/itext/src/swisscom/com/ais/itext
<LIB>   = Path to the ./lib subfolder containing the libraries, e.g. ./All-in/itext/lib
<CLASS> = Path to the directory where class files will be created, e.g. ./All-in/itext/class
<CFG>   = Path to the allin_itext.properties file, e.g. ./All-in/itext/allin_itext.properties
<DOC>   = Path to the ./doc subfolder containing the JavaDoc, e.g. ./All-in/itext/doc
```

Compile the sources: `javac -d <CLASS> -cp "<LIB>/*" <SRC>/*.java`

Note: The class files are generated in a directory hierarchy which reflects the given package structure: `<CLASS>/swisscom/com/ais/itext/*.class`

The compiled application can be run as follows.

Run the application (Unix/OSX):
`java -cp "<CLASS>:<LIB>/*" swisscom.com.ais.itext.allin_itext`

Run the application (Unix/OSX) with custom path to the properties file:
`java -DpropertyFile.path=<CFG> -cp "<CLASS>:<LIB>/*" swisscom.com.ais.itext.allin_itext`

Run the application (Unix/OSX) with DEBUG enabled:
`java -Djavax.net.debug=all -Djava.security.debug=certpath -cp "<CLASS>:<LIB>/*" swisscom.com.ais.itext.allin_itext`

If you're on Windows then use a semicolon ; instead of the colon : 

#### Create & Run JAR file

Alternatively you may create a Java Archive (JAR) that contains the compiled class files.

Create a JAR: `jar cfe allin_itext.jar swisscom.com.ais.itext.allin_itext -C <CLASS> .`

Note that we have done that already for you. The latest JAR file is located in the `./jar` subfolder. 

Run the JAR (Unix/OSX): `java -cp "./jar/allin_itext.jar:<LIB>/*" swisscom.com.ais.itext.allin_itext`

Run the JAR (Unix/OSX) with custom path to the properties file:
`java -DpropertyFile.path=<CFG> -cp "./jar/allin_itext.jar:<LIB>/*" swisscom.com.ais.itext.allin_itext`

Run the JAR (Unix/OSX) with DEBUG enabled:
`java -Djavax.net.debug=all -Djava.security.debug=certpath -cp "./jar/allin_itext.jar:<LIB>/*" swisscom.com.ais.itext.allin_itext`


If you're on Windows then use a semicolon ; instead of the colon : 

### JavaDoc

The latest JavaDoc is located in the `./doc` subfolder.
Create the JavaDoc: `javadoc -d <DOC> -private -sourcepath <SRC> swisscom.com.ais.itext`
