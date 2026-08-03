$JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
$SRC = "src\main\java"
$OUT = "target\classes"
$M2 = "$env:USERPROFILE\.m2\repository"
$JFX = "$M2\org\openjfx"
$MP = "$JFX\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;$JFX\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar;$JFX\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar;$JFX\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar"
$CP = "$M2\org\xerial\sqlite-jdbc\3.45.1.0\sqlite-jdbc-3.45.1.0.jar;$M2\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"

New-Item -ItemType Directory -Force -Path $OUT

$JAVA_FILES = (Get-ChildItem -Path "$SRC\com\aigen\sims" -Filter "*.java").FullName
& "$JAVA_HOME\bin\javac.exe" -encoding UTF-8 -d "$OUT" -cp "$CP" --module-path "$MP" --add-modules javafx.controls,javafx.fxml $JAVA_FILES

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful. Launching AEGIS/GodHand GUI..."
    & "$JAVA_HOME\bin\java.exe" --module-path "$MP" --add-modules javafx.controls,javafx.fxml -cp "$CP;$OUT" com.aigen.sims.GodHandApp
} else {
    Write-Host "Compilation failed."
}
