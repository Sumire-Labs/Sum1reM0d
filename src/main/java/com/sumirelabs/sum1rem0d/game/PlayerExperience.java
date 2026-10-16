package com.sumirelabs.sum1rem0d.game;

import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Raw XP fields are journaled; bank arithmetic uses level + bar, never the stale lifetime counter.
 */
public record PlayerExperience(int level, float progress, int total) {
  public static PlayerExperience capture(ServerPlayer p) {
    return new PlayerExperience(p.experienceLevel, p.experienceProgress, p.totalExperience);
  }

  public static PlayerExperience at(long points) {
    var bar = ExperienceAccounting.bar(points);
    return new PlayerExperience(bar.level(), bar.progress(), bar.total());
  }

  public long points() {
    return ExperienceAccounting.points(level, progress);
  }

  public void apply(ServerPlayer p) {
    p.experienceLevel = level;
    p.experienceProgress = progress;
    p.totalExperience = total;
  }

  public void send(ServerPlayer p) {
    p.connection.send(new ClientboundSetExperiencePacket(progress, total, level));
  }

  public JsonObject json() {
    var j = new JsonObject();
    j.addProperty("level", level);
    j.addProperty("progress", progress);
    j.addProperty("total", total);
    return j;
  }

  public static PlayerExperience read(JsonObject j) {
    return new PlayerExperience(
        j.get("level").getAsInt(), j.get("progress").getAsFloat(), j.get("total").getAsInt());
  }
}
