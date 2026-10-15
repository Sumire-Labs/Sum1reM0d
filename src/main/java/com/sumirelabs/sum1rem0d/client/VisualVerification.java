package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;

/** Temporary client-only fixtures used by the opt-in native toast/visual check. */
public final class VisualVerification {
    private static boolean active;
    private static JsonObject originalConfig, originalState, fixture, state;
    private static GameStatePayload deferred;
    private static CameraType camera;
    private static JsonObject originalPersonal;
    private static boolean originalHideGui;
    private static long revision;
    private static boolean editable;
    private static BlockPos center, outside, nearOutside;
    private static int base, outsideBase, nearOutsideBase, worldBlockLight;
    private VisualVerification() {}
    public static boolean defer(GameStatePayload payload) { if (!active) return false; deferred = payload; return true; }
    public static void begin(Minecraft mc) {
        originalConfig = ClientRules.snapshot(); originalState = ClientGameState.snapshot(); revision = ClientRules.revision; editable = ClientRules.editable;
        camera = mc.options.getCameraType();
        originalPersonal = PersonalConfig.store().snapshot(); originalHideGui = mc.gui.hud.isHidden();
        center = mc.player.blockPosition().above().immutable(); outside = center.offset(13, 0, 0);
        nearOutside = center.offset(4, 0, 0);
        base = LightCoordsUtil.getLightCoords(mc.level, center); outsideBase = LightCoordsUtil.getLightCoords(mc.level, outside); nearOutsideBase = LightCoordsUtil.getLightCoords(mc.level, nearOutside); worldBlockLight = mc.level.getBrightness(LightLayer.BLOCK, center);
        fixture = originalConfig.deepCopy();
        for (var entry : fixture.getAsJsonObject("features").entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
        var afk = fixture.getAsJsonObject("features").getAsJsonObject("afk_white");
        afk.addProperty("enabled", true); afk.addProperty("whiteness", 1); afk.addProperty("dynamic_light", true); afk.addProperty("light_level", 15); afk.addProperty("light_radius", 12);
        state = originalState.deepCopy(); state.add("daily", new JsonObject()); var white = new JsonObject(); white.addProperty(mc.player.getUUID().toString(), true); state.add("white", white);
        active = true; mc.options.setCameraType(CameraType.FIRST_PERSON); apply();
    }
    private static void apply() {
        ClientRuntime.acceptRules(new ConfigStatePayload(revision, editable, fixture.toString(), "", 0, false));
        ClientRuntime.applyState(new GameStatePayload(state.toString())); AfkLights.tick();
    }
    public static void assertLight(Minecraft mc) {
        int actual = LightCoordsUtil.getLightCoords(mc.level, center);
        require(LightCoordsUtil.block(actual) == 15 && LightCoordsUtil.sky(actual) == LightCoordsUtil.sky(base), "Dynamic light raises block light without changing sky light");
        require(LightCoordsUtil.getLightCoords(mc.level, outside) == outsideBase, "Dynamic light stays within configured radius");
        require(mc.level.getBrightness(LightLayer.BLOCK, center) == worldBlockLight, "Dynamic light does not change the world's light engine");
        var options = fixture.getAsJsonObject("features").getAsJsonObject("afk_white"); options.addProperty("light_level", 7); options.addProperty("light_radius", 3); apply();
        require(LightCoordsUtil.block(LightCoordsUtil.getLightCoords(mc.level, center)) == Math.max(7, LightCoordsUtil.block(base)), "Configured light level is applied");
        require(LightCoordsUtil.getLightCoords(mc.level, nearOutside) == nearOutsideBase, "Configured smaller radius is applied");
        options.addProperty("light_level", 15); options.addProperty("light_radius", 12); apply();
    }
    public static void natural() { fixture.getAsJsonObject("features").getAsJsonObject("afk_white").addProperty("whiteness", 0); apply(); }
    public static void assertModels(Minecraft mc) {
        var previousConfig = fixture.deepCopy(); var previousState = state.deepCopy(); var worldBlock = mc.level.getBlockState(center);
        var shapes = mc.getModelManager().getBlockStateModelSet(); var models = mc.getModelManager().getBlockModelSet();
        var grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(); var snowy = grass.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY, true);
        var coal = net.minecraft.world.level.block.Blocks.COAL_ORE.defaultBlockState(); var diamond = net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState();
        var baseGrass = shapes.get(grass); var baseCoal = shapes.get(coal);
        try {
            var features = fixture.getAsJsonObject("features"); var daily = state.getAsJsonObject("daily");
            features.getAsJsonObject("upside_down_grass").addProperty("enabled", true); daily.addProperty("upside_down_grass", true); apply();
            require(faceTextures(mc, grass, net.minecraft.core.Direction.UP).contains("minecraft:block/dirt") && faceTextures(mc, grass, net.minecraft.core.Direction.DOWN).contains("minecraft:block/grass_block_top"), "Native grass quads swap top and bottom textures");
            require(VisualBlocks.replace(snowy).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY) && faceTextures(mc, snowy, net.minecraft.core.Direction.NORTH).contains("minecraft:block/grass_block_snow"), "Snowy grass retains the native snow side texture");
            var proxy = com.sumirelabs.sum1rem0d.game.ModContent.upsideDownGrass().defaultBlockState(); require(models.get(grass) == models.get(proxy), "Native block-item model lookup also uses reversed grass");
            daily.addProperty("upside_down_grass", false); apply(); require(shapes.get(grass) == baseGrass, "Inactive grass day restores native model"); daily.addProperty("upside_down_grass", true);
            features.getAsJsonObject("upside_down_grass").addProperty("enabled", false); apply(); require(shapes.get(grass) == baseGrass, "Disabling grass reversal restores native model despite stale daily flag");
            var ore = features.getAsJsonObject("diamond_disguise"); ore.addProperty("enabled", true); ore.addProperty("items", true); ore.addProperty("replacement", "minecraft:diamond_ore"); daily.addProperty("diamond_disguise", true); apply();
            var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL_ORE, 7);
            require(shapes.get(coal) == shapes.get(diamond) && models.get(coal) == models.get(diamond), "Native world and block-item model lookups disguise ore as diamond");
            require(net.minecraft.world.level.block.Blocks.COAL_ORE.getName().getString().equals(net.minecraft.world.level.block.Blocks.DIAMOND_ORE.getName().getString()) && stack.getHoverName().getString().equals(net.minecraft.world.level.block.Blocks.DIAMOND_ORE.getName().getString()), "Native block and item names display the configured replacement");
            require(itemParticle(mc, stack).equals(itemParticle(mc, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_ORE))), "Native ItemModelResolver resolves the disguised item's diamond texture");
            require(stack.is(net.minecraft.world.item.Items.COAL_ORE) && stack.getCount() == 7, "Disguise never changes real item type or quantity");
            ore.addProperty("items", false); apply(); require(stack.getHoverName().getString().equals(net.minecraft.network.chat.Component.translatable("block.minecraft.coal_ore").getString()) && itemParticle(mc, stack).equals("minecraft:block/coal_ore"), "Item disguise can be disabled separately from world models");
            ore.addProperty("items", true); ore.addProperty("replacement", "minecraft:emerald_ore"); apply(); require(shapes.get(coal) == shapes.get(net.minecraft.world.level.block.Blocks.EMERALD_ORE.defaultBlockState()) && stack.getHoverName().getString().equals(net.minecraft.world.level.block.Blocks.EMERALD_ORE.getName().getString()), "Config changes the actual replacement model and name");
            var targets = ore.get("target_blocks").deepCopy(); ore.addProperty("target_blocks", "minecraft:iron_ore"); apply(); require(shapes.get(coal) == baseCoal && shapes.get(net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState()) == shapes.get(net.minecraft.world.level.block.Blocks.EMERALD_ORE.defaultBlockState()), "Configured ore targets affect only the selected blocks");
            ore.add("target_blocks", targets); daily.addProperty("diamond_disguise", false); apply(); require(shapes.get(coal) == baseCoal, "Inactive ore-disguise day restores the original model"); daily.addProperty("diamond_disguise", true);
            ore.addProperty("enabled", false); apply(); require(shapes.get(coal) == baseCoal, "Disabling ore disguise restores the original native model");
            var furnace = features.getAsJsonObject("furnace_textures"); furnace.addProperty("enabled", true); furnace.addProperty("candidate_blocks", "minecraft:furnace,minecraft:blast_furnace,minecraft:smoker"); daily.addProperty("furnace_textures", true);
            var kinds = java.util.List.of(net.minecraft.world.level.block.Blocks.FURNACE, net.minecraft.world.level.block.Blocks.BLAST_FURNACE, net.minecraft.world.level.block.Blocks.SMOKER); var permutations = new java.util.HashSet<String>();
            for (int permutation = 0; permutation < 6; permutation++) {
                state.addProperty("furnacePermutation", permutation); apply(); var choices = new java.util.ArrayList<String>();
                for (var kind : kinds) {
                    var input = kind.defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, net.minecraft.core.Direction.WEST).setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, true); var replacement = VisualBlocks.replace(input);
                    require(replacement.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING) == net.minecraft.core.Direction.WEST && replacement.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT), "Furnace appearance preserves facing and lit state");
                    require(shapes.get(input) == shapes.get(replacement) && models.get(input) == models.get(replacement) && faceTextures(mc, input, net.minecraft.core.Direction.WEST).stream().anyMatch(name -> name.endsWith("_front_on")), "Actual furnace model and illuminated front texture use the selected appearance");
                    choices.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(replacement.getBlock()).toString());
                }
                require(new java.util.HashSet<>(choices).size() == 3, "Each furnace permutation uses all three candidate appearances"); permutations.add(choices.toString());
            }
            require(permutations.size() == 6, "All six furnace permutations are reachable through synchronized state");
            furnace.addProperty("candidate_blocks", "minecraft:furnace,minecraft:blast_furnace,minecraft:smoker,minecraft:stone"); var extraChoices = new java.util.HashSet<String>();
            for (int sample = 0; sample < 32; sample++) {
                state.addProperty("day", 10 + sample); state.addProperty("furnacePermutation", sample); apply();
                var input = kinds.getFirst().defaultBlockState(); var replacement = VisualBlocks.replace(input); extraChoices.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(replacement.getBlock()).toString().replace("sum1rem0d:visual_", "minecraft:"));
                require(shapes.get(input) == shapes.get(replacement), "Custom candidate native model follows selected appearance");
            }
            require(extraChoices.equals(java.util.Set.of("minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:stone")), "Candidate lists longer than three reach every configured appearance");
            furnace.addProperty("candidate_blocks", ""); apply(); require(VisualBlocks.replace(kinds.getFirst().defaultBlockState()).is(kinds.getFirst()), "Empty candidate list retains vanilla appearance");
            furnace.addProperty("enabled", false); apply(); require(VisualBlocks.replace(kinds.getFirst().defaultBlockState()).is(kinds.getFirst()), "Disabled furnace appearance preserves original state");
            require(mc.level.getBlockState(center).equals(worldBlock), "Visual changes do not mutate world blocks");
        } finally { fixture = previousConfig; state = previousState; apply(); }
    }
    private static java.util.Set<String> faceTextures(Minecraft mc, net.minecraft.world.level.block.state.BlockState block, net.minecraft.core.Direction face) {
        var parts = new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>(); mc.getModelManager().getBlockStateModelSet().get(block).collectParts(net.minecraft.util.RandomSource.create(42), parts);
        var result = new java.util.HashSet<String>(); for (var part : parts) for (var quad : part.getQuads(face)) result.add(quad.materialInfo().sprite().contents().name().toString()); return result;
    }
    private static String itemParticle(Minecraft mc, net.minecraft.world.item.ItemStack stack) {
        var render = new net.minecraft.client.renderer.item.ItemStackRenderState(); new net.minecraft.client.renderer.item.ItemModelResolver(mc.getModelManager()).updateForTopItem(render, stack, net.minecraft.world.item.ItemDisplayContext.GUI, mc.level, null, 0);
        return render.pickParticleMaterial(net.minecraft.util.RandomSource.create(42)).sprite().contents().name().toString();
    }
    public static void assertNames(Minecraft mc) {
        var options = fixture.getAsJsonObject("features").getAsJsonObject("name_swap"); options.addProperty("enabled", true); options.addProperty("nametag", true); options.addProperty("tab", true);
        var originalAliases = state.has("aliases") ? state.getAsJsonObject("aliases").deepCopy() : new JsonObject();
        var aliases = new JsonObject(); aliases.addProperty(mc.player.getUUID().toString(), "SumireAlias"); state.add("aliases", aliases); apply();
        require(mc.player.getDisplayName().getString().contains("SumireAlias") && mc.player.getDisplayName().getStyle().getInsertion().equals(mc.player.getGameProfile().name()), "Client alias preserves the real name's rich style");
        var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        require(info != null && info.getTabListDisplayName().getString().contains("SumireAlias") && info.getProfile().id().equals(mc.player.getUUID()), "Tab alias does not mutate the authenticated profile");
        options.addProperty("enabled", false); state.add("aliases", originalAliases); apply();
    }
    public static void assertUrlEditor(Minecraft mc) {
        var option = ConfigSchema.features().stream().filter(feature -> feature.id().equals("youtube")).findFirst().orElseThrow().options().stream().filter(value -> value.key().equals("random_urls")).findFirst().orElseThrow();
        String initial = "https://www.youtube.com/watch?v=jNQXAC9IVRw&list=example";
        String updated = "https://www.youtube.com/watch?v=jNQXAC9IVRw&t=2";
        var accepted = new java.util.concurrent.atomic.AtomicReference<String>(); var parent = mc.gui.screen();
        var editor = new ResourceEntryScreen(parent, true, option, initial, accepted::set); mc.gui.setScreen(editor);
        try {
            var field = editor.children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.EditBox).map(child -> (net.minecraft.client.gui.components.EditBox) child).findFirst().orElseThrow();
            require(field.getValue().equals(initial), "URL editor preserves all query parameters when loading an entry"); field.setValue(updated);
            var save = editor.children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().equals("決定")).map(child -> (net.minecraft.client.gui.components.Button) child).findFirst().orElseThrow();
            save.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
            require(updated.equals(accepted.get()), "URL editor saves equals signs and query parameters intact");
        } finally { mc.gui.setScreen(parent); }
    }
    public static void partial() { fixture.getAsJsonObject("features").getAsJsonObject("afk_white").addProperty("whiteness", .5); apply(); }
    public static void assertCamera(Minecraft mc) {
        float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
        var options = fixture.getAsJsonObject("features").getAsJsonObject("camera_lock");
        try {
            options.addProperty("enabled", true);
            state.addProperty("originalYaw", 37); state.addProperty("originalPitch", 12);
            state.addProperty("cameraYaw", 217); state.addProperty("cameraPitch", 12); state.addProperty("cameraTicks", 2); apply();
            ClientRuntime.tickCamera(mc);
            require(mc.player.getYRot() == 217 && mc.player.getXRot() == 12, "Camera turns backward and preserves pitch");
            mc.player.turn(20, 10);
            require(mc.player.getYRot() == 217 && mc.player.getXRot() == 12, "Native Entity.turn cannot rotate a locked local player");
            ClientRuntime.tickCamera(mc); ClientRuntime.tickCamera(mc);
            require(mc.player.getYRot() == 37 && mc.player.getXRot() == 12 && !ClientRuntime.cameraLocked(), "Expiry restores the original orientation");
            state.addProperty("cameraTicks", 100); apply(); ClientRuntime.tickCamera(mc);
            options.addProperty("enabled", false); apply(); mc.player.turn(20, 10);
            require(mc.player.getYRot() != 217 && !ClientRuntime.cameraLocked(), "Disabling immediately permits native mouse rotation despite stale event ticks");
            ClientRuntime.tickCamera(mc);
            require(mc.player.getYRot() == 37 && mc.player.getXRot() == 12, "Disabling restores the original orientation");
        } finally {
            options.addProperty("enabled", false); state.addProperty("cameraTicks", 0); apply(); ClientRuntime.tickCamera(mc);
            mc.player.setYRot(yaw); mc.player.setXRot(pitch);
        }
    }
    public static void redScreen() {
        var options = fixture.getAsJsonObject("features").getAsJsonObject("sheep_red");
        options.addProperty("enabled", true); options.addProperty("opacity", 1); state.addProperty("redTicks", 60); apply();
    }
    public static void disableRed() { fixture.getAsJsonObject("features").getAsJsonObject("sheep_red").addProperty("enabled", false); apply(); }
    public static void deathScreen(Minecraft mc) {
        var options = fixture.getAsJsonObject("features").getAsJsonObject("death_message"); options.addProperty("enabled", true); options.addProperty("text", "You Suck"); apply();
        var cause = net.minecraft.network.chat.Component.literal("Native death-screen fixture");
        var normal = new net.minecraft.client.gui.screens.DeathScreen(cause, false, mc.player);
        var hardcore = new net.minecraft.client.gui.screens.DeathScreen(cause, true, mc.player);
        require(normal.getTitle().getString().equals("You Suck") && hardcore.getTitle().getString().equals("You Suck"), "Normal and hardcore native death screens use configured title");
        options.addProperty("text", "Custom Sumire"); apply(); require(new net.minecraft.client.gui.screens.DeathScreen(cause, false, mc.player).getTitle().getString().equals("Custom Sumire"), "Config changes the native death title");
        options.addProperty("enabled", false); apply(); require(!new net.minecraft.client.gui.screens.DeathScreen(cause, false, mc.player).getTitle().getString().equals("You Suck"), "Disabling death message restores vanilla title");
        options.addProperty("enabled", true); options.addProperty("text", "You Suck"); apply(); mc.gui.setScreen(normal);
    }
    public static void thirdPerson(Minecraft mc) { fixture.getAsJsonObject("features").getAsJsonObject("afk_white").addProperty("whiteness", 1); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); apply(); }
    public static void disableLight(Minecraft mc) {
        fixture.getAsJsonObject("features").getAsJsonObject("afk_white").addProperty("dynamic_light", false); apply();
        require(LightCoordsUtil.getLightCoords(mc.level, center) == base, "Disabling dynamic light restores vanilla lighting");
        fixture.getAsJsonObject("features").getAsJsonObject("afk_white").addProperty("enabled", false); apply();
        require(!AfkLights.white(mc.player.getUUID()), "Disabling AFK white immediately clears rendering even with stale synchronized UUIDs");
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }
    public static void hud(Minecraft mc, double value, double maximum, boolean visible) throws Exception {
        var options = fixture.getAsJsonObject("features").getAsJsonObject("cleanliness"); options.addProperty("enabled", true); options.addProperty("maximum", maximum);
        state.addProperty("cleanliness", value);
        var personal = PersonalConfig.store().snapshot(); personal.getAsJsonObject("features").getAsJsonObject("cleanliness").addProperty("hud", visible); PersonalConfig.store().update(personal, PersonalConfig.store().revision());
        if (mc.gui.hud.isHidden()) mc.gui.hud.toggle(); apply();
    }
    public static void assertHud(Minecraft mc, int barWidth, int color, boolean visible) {
        var renderState = new net.minecraft.client.renderer.state.gui.GuiRenderState();
        var graphics = new net.minecraft.client.gui.GuiGraphicsExtractor(mc, renderState, 0, 0); ClientRuntime.overlay(graphics);
        var rectangles = new java.util.ArrayList<net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState>();
        renderState.forEachElement(element -> { if (element instanceof net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState rectangle) rectangles.add(rectangle); }, net.minecraft.client.renderer.state.gui.GuiRenderState.TraverseRange.ALL);
        int[] textCount = {0}; renderState.forEachText(text -> textCount[0]++);
        if (!visible) { require(rectangles.isEmpty() && textCount[0] == 0, "Personal HUD toggle suppresses both meter geometry and text"); return; }
        require(rectangles.size() == 2 && textCount[0] == 1, "Visible cleanliness HUD extracts its background, bar and numeric label");
        // Native fill normalizes corners in descending order. Compare extents,
        // independently of the winding used by the rectangle's vertices.
        require(rectangles.stream().anyMatch(rectangle -> Math.min(rectangle.x0(), rectangle.x1()) == 8 && Math.max(rectangle.x0(), rectangle.x1()) >= 100 && Math.min(rectangle.y0(), rectangle.y1()) == 8 && Math.abs(rectangle.y1() - rectangle.y0()) == 15), "Cleanliness meter sits above inventory controls and fits its numeric label");
        require(rectangles.stream().anyMatch(rectangle -> Math.min(rectangle.x0(), rectangle.x1()) == 10 && Math.max(rectangle.x0(), rectangle.x1()) == 10 + barWidth && Math.abs(rectangle.y1() - rectangle.y0()) == 2 && rectangle.col1() == color), "Cleanliness meter width and color reflect value and maximum");
    }
    public static void restore(Minecraft mc) {
        if (!active) return;
        active = false;
        ClientRuntime.acceptRules(new ConfigStatePayload(revision, editable, originalConfig.toString(), "", 0, false));
        ClientRuntime.accept(deferred == null ? new GameStatePayload(originalState.toString()) : deferred); deferred = null;
        mc.options.setCameraType(camera); if (mc.gui.hud.isHidden() != originalHideGui) mc.gui.hud.toggle(); AfkLights.tick();
        try { PersonalConfig.store().update(originalPersonal, PersonalConfig.store().revision()); }
        catch (Exception failure) { throw new IllegalStateException("Cannot restore personal visual fixture settings", failure); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
