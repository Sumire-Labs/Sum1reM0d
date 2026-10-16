# Third-party components

Sum1reM0d's independently written code remains under BlueOak-1.0.0.

## json5-java 3.0.0

This distribution includes an unmodified copy of `de.marhali:json5-java:3.0.0`, copyright Marcel Haßlinger and contributors, under Apache-2.0. Portions originating from Google Gson are Apache-2.0; portions originating from SyntaxError404's json5 project carry MIT notices. All original source headers are preserved in the included official source archive.

- Upstream: https://github.com/marhali/json5-java/tree/v3.0.0
- License: `licenses/json5-java/Apache-2.0.txt` inside every distributed Mod JAR.
- MIT notices: `licenses/json5-java/MIT-notices.txt` inside every distributed Mod JAR.
- Corresponding unmodified official source archive: `licenses/json5-java/json5-java-3.0.0-sources.jar` inside every distributed Mod JAR and this repository.
- Maven: https://repo.maven.apache.org/maven2/de/marhali/json5-java/3.0.0/

Other components, including MCEF, retain their own license notices in their distribution modules.

## No Chat Reports — core chat report prevention

Source: https://github.com/Aizistral-Studios/No-Chat-Reports/tree/7c7f63552026f5faab3c63eda80c76fa725e2e40
Authors: Aizistral and No Chat Reports contributors; original system-message conversion by JFronny.
License: WTFPL version 2 (see licenses/No-Chat-Reports-WTFPL.txt).

Sum1reM0d adapts the system-chat conversion, profile-key suppression, secure-profile override and NCR server-discovery convention. It uses its own configurable implementation and does not bundle the full upstream mod or its encrypted-chat feature. Native send callbacks and Minecraft content filtering are preserved. Sum1reM0d's own license remains BlueOak-1.0.0.
