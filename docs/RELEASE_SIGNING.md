# Signering av Stova

Produksjonsrelease krev eigaren sin varige nøkkel. `assembleRelease` og `bundleRelease`
stoppar når `signing.properties` manglar, nokre verdiar er tomme eller nøkkelfila ikkje finst.
Debug og dei eigne benchmark-variantane bruker framleis debug-signering.

## Eksisterande nøkkel

Bruk den eksisterande Stova-nøkkelen dersom han finst. Ikkje lag ein ny nøkkel for å kome forbi
ein byggfeil. Eit nytt sertifikat kan ikkje oppdatere ei utgåve signert med den gamle nøkkelen.

## Førstegongsoppsett utført av eigaren

Dette steget er ikkje køyrt av agenten. Køyr lokalt i PowerShell; `keytool` spør etter passordet
utan at det står i chatten eller kommandolinja:

```powershell
New-Item -ItemType Directory -Force .signing | Out-Null
keytool -genkeypair -keystore .signing/stova-release.jks -storetype JKS -alias stova -keyalg RSA -keysize 4096 -validity 10000
Copy-Item signing.properties.example signing.properties
```

Fyll inn `storePassword` og `keyPassword` lokalt. `storeFile` er relativ til prosjektrota.
Bruk UTF-8. Java-properties krev dobbel bakstrek for ein bokstavleg bakstrek i ein verdi;
bruk gjerne `/` i filstiar. Ta vare på nøkkel og passord i eigaren si vanlege løysing for løyndomar.
Nøkkelen og `signing.properties` er git-ignorerte.

## Kontroller og bygg

```powershell
pwsh -NoProfile -File scripts/gradle.ps1 :app:verifyReleaseSigning
pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintRelease assembleRelease
```

`verifyReleaseSigning` kontrollerer at oppsett og fil finst; Gradle si signeringsoppgåve
kontrollerer at passordet og aliaset faktisk opnar nøkkelen. Verifiser deretter APK-en med
SDK-verktøyet `apksigner verify --print-certs`, og noter SHA-256 for sertifikat og APK.

Før første utgåve: mål det signerte bygget på Shield og prøv `install -r` over ei eldre utgåve
med same pakkenamn og sertifikat. Debug (`app.stova.debug`) og release (`app.stova`) er to
ulike appar. Ein debug-installasjon er derfor ikkje ein oppdateringstest av produksjonsrelease.
