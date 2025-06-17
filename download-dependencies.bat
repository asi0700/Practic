@echo off
echo Downloading dependencies...

if not exist "lib" mkdir lib

echo Downloading MySQL Connector...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar' -OutFile 'lib\mysql-connector-j-8.0.33.jar'"

echo Downloading Apache Commons Lang...
curl -L "https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar" -o "lib\commons-lang3-3.12.0.jar"

echo Downloading JCalendar...
curl -L "https://repo1.maven.org/maven2/com/toedter/jcalendar/1.4/jcalendar-1.4.jar" -o "lib\jcalendar-1.4.jar"

echo Downloading Webcam Capture...
curl -L "https://repo1.maven.org/maven2/com/github/sarxos/webcam-capture/0.3.12/webcam-capture-0.3.12.jar" -o "lib\webcam-capture-0.3.12.jar"

echo Downloading SLF4J Simple Logger...
curl -L "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar" -o "lib\slf4j-simple-1.7.36.jar"

echo All dependencies downloaded successfully! 