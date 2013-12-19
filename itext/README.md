To run this java example you need to download the following files from given sources:

1: http://mvnrepository.com/artifact/com.google.code.findbugs/jsr305 - version 2.0.2 is successfully tested
2: http://sourceforge.net/projects/itext/ - version 5.4.5 is successfully tested
3: http://www.bouncycastle.org/latest_releases.html - download bcprov-jdk15on-150.jar - this is successfully tested
</p>
After downloading this files you need to compile the java sources (maybe you have to change the location of jar-files):

javac -cp .:bcprov-jdk15on-149.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar Allin_Itext.java
</p>
Now you can run the program:

 Usage:java -cp libraries Allin_Itext <args> signature pdftosign signedpdf <dn> <msisdn> <msg> <lang>
 -v        - verbose output
 -d        - debug mode
 signature - tsa, static, ondemand
 pdftosign - PDF to be signed
 signedpdf - signed PDF
 <dn>      - optional distinguished name in ondemand
 <msisdn>  - optional Mobile ID step-up in ondemand
 <msg>     - optional Mobile ID message, mandatory if msisdn is set
 <lang>    - optional Mobile ID language element (en, de, fr, it), mandatory if msisdn is set
 <p/>
 Examples java -cp .:bcprov-jdk15on-149.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar Allin_Itext -v tsa sample.pdf signed.pdf
 java allin-itext.java -v static sample.pdf signed.pdf
 java allin-itext.java -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'
 java allin-itext.java -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en
 </p>