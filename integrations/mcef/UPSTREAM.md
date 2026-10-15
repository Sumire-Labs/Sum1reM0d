# MCEF Modern — Sum1reM0d adapter

Upstream: https://github.com/DimasKama/mcef-modern

Pinned source revision: `febab6f9278d218d0658637be5419881d54f1242`.

This module retains the upstream LGPL-2.1 license independently of the parent
Sum1reM0d project. The full modified source is included in this directory and
its source Jar. DimasKama, the CinemaMod Group, montoyo, and upstream contributors
retain their respective copyrights.

Changes: shared loader-independent directory/bootstrap and shutdown handling;
Minecraft-version-specific Gradle modules and generated source adaptation for
the pre-26.3 blaze3d packages and GLFW input symbols (see
`gradle/scripts/compatibility.gradle` in the complete project source archive);
Fabric and NeoForge entrypoints; loader-specific nested dependency packaging;
browser initialization uses normal Chromium web security and configures CEF
settings before building the CEF application. Minecraft rendering, input, and
the public MCEF browser API are retained from the pinned upstream revision.
Browser paint delivery retains only the latest pending frame, frees replaced
native buffers, and discards queued paints after close to prevent textures
being recreated after their browser has been released.
AWT is initialized from the client bootstrap and uses the adapter's loader
context before CEF starts its asynchronous initialization.
Repeated focus requests are suppressed before entering JNI, including the
setFocus(true) callback made by CefClient.onGotFocus.
Synthetic keyboard events expose the Windows scan code required by JCEF JNI,
emit Return's character event for form submission, and retain supplementary
Unicode characters as UTF-16 pairs.
The keyboard conversion follows the pinned [JCEF native implementation](https://github.com/chromiumembedded/java-cef/blob/d3de827/native/CefBrowser_N.cpp).

Shutdown stops further initialization requests, waits for initialization already
in progress, disposes the CEF application once, and waits for its TERMINATED
state callback before returning to Minecraft's close method. This prevents the
NeoForge module loader from closing while asynchronous CEF startup or teardown
still needs its classes. Startup remains asynchronous during normal play; exit
may wait for an installation or initialization that has already started.

This is a compatibility adapter, not an upstream NeoForge release. LGPL source
and license files must accompany redistribution of the adapter. JCEF and
Chromium have their own notices and licenses, supplied by their distributions.
