package com.sumirelabs.sum1rem0d.network;

import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.io.IOException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public final class ServerConfigHandler {
    private ServerConfigHandler() {}

    private static boolean canEdit(ServerPlayer player) {
        var server = player.level().getServer();
        var profile = new NameAndId(player.getGameProfile());
        return server.isSingleplayerOwner(profile) || server.getPlayerList().isOp(profile);
    }
    public static void send(ServerPlayer player, String message) {
        send(player, message, 0, false);
    }
    private static void send(ServerPlayer player, String message, long requestId, boolean applied) {
        var store = Sum1reM0d.config();
        PlatformNetwork.send(player, new ConfigStatePayload(store.revision(), canEdit(player), store.snapshot().toString(), message, requestId, applied));
    }
    public static void handle(ConfigRequestPayload request, ServerPlayer player) {
        if (com.sumirelabs.sum1rem0d.game.ChatPrivacyNetworkVerification.request(request, player)) return;
        if (com.sumirelabs.sum1rem0d.game.MultiplayerVerification.request(request, player)) return;
        if (!request.update()) { send(player, ""); return; }
        if (!canEdit(player)) { send(player, "変更にはOP権限が必要です。", request.requestId(), false); return; }
        try {
            if (request.json().length() > ConfigRequestPayload.MAX_LENGTH) throw new IllegalArgumentException("設定が大きすぎます。");
            Sum1reM0d.config().update(JsonParser.parseString(request.json()).getAsJsonObject(), request.revision());
            for (ServerPlayer recipient : player.level().getServer().getPlayerList().getPlayers()) {
                send(recipient, recipient == player ? "設定を保存しました。" : "サーバー設定が更新されました。", recipient == player ? request.requestId() : 0, recipient == player);
            }
        } catch (IllegalArgumentException | IllegalStateException | JsonParseException exception) {
            String detail = exception.getMessage();
            if (detail == null) detail = "値を確認し、画面を開き直してください。";
            send(player, "設定を適用できません: " + detail.substring(0, Math.min(180, detail.length())), request.requestId(), false);
        } catch (IOException exception) {
            Sum1reM0d.LOGGER.error("Cannot save server configuration", exception);
            send(player, "設定ファイルの保存に失敗しました。", request.requestId(), false);
        }
    }
}
