package net.dimaskama.mcef.impl;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefRequestContext;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class MCEFApiImpl implements MCEFApi {

    private static volatile InitializationImpl initialization;
    private static boolean stopping;
    private final CefApp cefApp;
    private final CefClient client;
    private final CompletableFuture<Void> terminated = new CompletableFuture<>();
    private final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();

    private MCEFApiImpl(InitializationImpl initialization) throws Exception {
        CefAppBuilder cefAppBuilder = new CefAppBuilder();
        cefAppBuilder.setAppHandler(new me.friwi.jcefmaven.MavenCefAppHandlerAdapter() {
            @Override public void stateHasChanged(CefApp.CefAppState state) {
                super.stateHasChanged(state);
                if (state == CefApp.CefAppState.TERMINATED) terminated.complete(null);
            }
        });
        cefAppBuilder.setInstallDir(MCEFModern.JCEF_PATH.toFile());
        cefAppBuilder.setProgressHandler(initialization);
        cefAppBuilder.addJcefArgs(
                "--autoplay-policy=no-user-gesture-required",

                "--enable-widevine-cdm"
        );


        CefSettings cefSettings = new CefSettings();
        cefSettings.user_agent_product = "MCEF-Modern/0";
        cefSettings.root_cache_path = MCEFModern.CACHE_PATH.toAbsolutePath().toString();
        cefAppBuilder.getCefSettings().user_agent_product = cefSettings.user_agent_product;
        cefAppBuilder.getCefSettings().root_cache_path = cefSettings.root_cache_path;
        cefApp = cefAppBuilder.build();

        client = cefApp.createClient();
    }

    public static synchronized Initialization initialize() {
        if (stopping) throw new IllegalStateException("MCEF is shutting down");
        if (initialization == null) initialization = new InitializationImpl();
        return initialization;
    }
    /** Keep the loader alive until startup and native teardown have both finished. */
    public static void shutdown() {
        InitializationImpl pending;
        synchronized (MCEFApiImpl.class) { stopping = true; pending = initialization; }
        if (pending == null) return;
        try {
            ((MCEFApiImpl) pending.getFuture().join()).close();
            MCEFModern.LOGGER.info("MCEF native shutdown completed");
        } catch (java.util.concurrent.CompletionException failure) {
            MCEFModern.LOGGER.warn("MCEF initialization ended before shutdown", failure.getCause());
        }
    }

    @Nullable
    public static Initialization getInitialization() {
        return initialization;
    }

    @Override
    public MCEFBrowser createBrowser(String url, boolean transparent) {
        if (closed.get()) throw new IllegalStateException("MCEF is closed");
        MCEFBrowserImpl browser = new MCEFBrowserImpl(
                client,
                url,
                transparent,
                CefRequestContext.getGlobalContext(),
                null
        );
        browser.setCloseAllowed();
        browser.createImmediately();
        return browser;
    }

    public void close() {
        if (closed.compareAndSet(false, true)) cefApp.dispose();
        terminated.join();
    }

    private static class InitializationImpl implements Initialization, IProgressHandler {

        private final CompletableFuture<MCEFApi> future;
        private volatile Stage stage = Stage.NOT_STARTED;
        private volatile float percentage = -1.0F;

        private InitializationImpl() {
            future = CompletableFuture.supplyAsync(() -> {
                try {
                    return new MCEFApiImpl(this);
                } catch (Throwable e) {
                    MCEFModern.LOGGER.error("Failed to initialize MCEF Modern", e);
                    stage = Stage.DONE;
                    percentage = -1;
                    throw new RuntimeException("Failed to initialize MCEF Modern", e);
                }
            });
        }

        @Override
        public Stage getStage() {
            return stage;
        }

        @Override
        public float getPercentage() {
            return percentage;
        }

        @Override
        public CompletableFuture<MCEFApi> getFuture() {
            return future;
        }

        @Override
        public void handleProgress(EnumProgress state, float percent) {
            stage = switch (state) {
                case LOCATING -> Stage.NOT_STARTED;
                case DOWNLOADING -> Stage.DOWNLOADING;
                case EXTRACTING -> Stage.EXTRACTING;
                case INSTALL -> Stage.INSTALL;
                case INITIALIZING -> Stage.INITIALIZING;
                case INITIALIZED -> Stage.DONE;
            };
            percentage = percent;
        }

    }

}
