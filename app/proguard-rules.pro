-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# Reports
-printusage build/outputs/logs/R8-removed-code-report.txt
-printseeds build/outputs/logs/R8-entry-points-report.txt

## Okio
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java
-dontwarn org.codehaus.mojo.animal_sniffer.*

#-keep class cash.z.** { *; }