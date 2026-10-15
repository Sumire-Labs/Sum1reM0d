package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.network.EconomyRequestPayload;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** All mutations run on the server thread. Offers reference inventory slots, never escrow items. */
public final class Economy {
    private Economy() {}
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Invite> INVITES = new HashMap<>();
    private static long sequence;
    private record Invite(UUID sender, long expires, long revision) {}
    public static final class Session {
        final ServerPlayer a, b;
        final LinkedHashMap<Integer, ItemStack> offerA = new LinkedHashMap<>(), offerB = new LinkedHashMap<>();
        final long id = ++sequence;
        long coinsA, coinsB, revision = id, expires;
        boolean acceptedA, acceptedB, closed;
        Session(ServerPlayer a, ServerPlayer b) { this.a = a; this.b = b; expires = now(a) + Rules.i("player_trading", "timeout_seconds") * 20L; }
        ServerPlayer other(ServerPlayer p) { return p == a ? b : a; }
        LinkedHashMap<Integer, ItemStack> offer(ServerPlayer p) { return p == a ? offerA : offerB; }
        long coins(ServerPlayer p) { return p == a ? coinsA : coinsB; }
        void changed() { acceptedA = acceptedB = false; revision = ++sequence; sync(); }
        void sync() {
            for (var p : List.of(a,b)) {
                if (p.containerMenu instanceof TradeMenu menu) menu.refresh(this, p);
                Gameplay.sendState(p);
            }
        }
    }
    private static long now(ServerPlayer p) { return p.level().getServer().overworld().getGameTime(); }
    public static long balance(ServerPlayer p) {
        var d = WorldState.get(p.level().getServer()).player(p);
        if (!d.walletInitialized) { d.coins = Rules.i("economy", "starting_balance"); d.walletInitialized = true; }
        if (d.coins < 0) throw new IllegalStateException("Invalid saved coin balance");
        return d.coins;
    }
    private static void balance(ServerPlayer p, long value) { WorldState.get(p.level().getServer()).player(p).coins = value; }
    private static long maximum() { return Rules.i("economy", "maximum_balance"); }
    public static int value(ItemStack stack) { return stack.is(ModContent.coin()) ? 1 : stack.is(ModContent.coinRoll()) ? 10 : 0; }
    private static void message(ServerPlayer p, String text) { p.sendSystemMessage(Component.literal("[Sum1reコ↑イン↓] " + text)); }
    private static void saved(ServerPlayer p) {
        p.getInventory().setChanged(); p.inventoryMenu.broadcastChanges();
        p.level().getServer().getPlayerList().saveAll(); WorldState.get(p.level().getServer()).save(); Gameplay.sendState(p);
    }
    public static void depositHand(ServerPlayer p, InteractionHand hand) {
        if (!Rules.on("economy") || !Rules.b("economy", "deposit") || p.isCreative() || p.isSpectator()) return;
        var stack = p.getItemInHand(hand); int unit = value(stack); if (unit == 0) return;
        try { long next = CoinMath.credit(balance(p), (long)unit * stack.getCount(), maximum());
            // Hand may be offhand: the journal includes all inventory/equipment slots.
            var copy = completeInventory(p); int handSlot = hand == InteractionHand.MAIN_HAND ? p.getInventory().getSelectedSlot() : net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
            copy[handSlot] = ItemStack.EMPTY; commitInventories(Map.of(p,copy),Map.of(p,next)); saved(p); }
        catch (IllegalArgumentException | ArithmeticException | IllegalStateException e) { message(p, e.getMessage()); }
    }
    private static ItemStack[] inventory(ServerPlayer p) {
        var result = new ItemStack[36]; for (int i=0;i<36;i++) result[i] = p.getInventory().getItem(i).copy(); return result;
    }
    private static boolean insert(ItemStack[] contents, ItemStack original) {
        var stack = original.copy();
        for (var existing : contents) if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
            int n = Math.min(stack.getCount(), Math.max(0, existing.getMaxStackSize() - existing.getCount())); existing.grow(n); stack.shrink(n);
        }
        for (int i=0;i<contents.length && !stack.isEmpty();i++) if (contents[i].isEmpty()) {
            int n = Math.min(stack.getCount(), stack.getMaxStackSize()); contents[i] = stack.copyWithCount(n); stack.shrink(n);
        }
        return stack.isEmpty();
    }
    private static void apply(ServerPlayer p, ItemStack[] contents) { for (int i=0;i<contents.length;i++) p.getInventory().setItem(i, contents[i].copy()); }
    private static ItemStack[] completeInventory(ServerPlayer p) { var result = new ItemStack[p.getInventory().getContainerSize()]; for (int i=0;i<result.length;i++) result[i]=p.getInventory().getItem(i).copy(); return result; }
    private static void commitInventories(Map<ServerPlayer,ItemStack[]> inventories,Map<ServerPlayer,Long> balances) {
        var first=inventories.keySet().iterator().next(); var state=WorldState.get(first.level().getServer()); var before=state.checkpoint();
        String revision=UUID.randomUUID().toString();
        try {
            for (var entry:inventories.entrySet()) {
                var p=entry.getKey(); var items=completeInventory(p); System.arraycopy(entry.getValue(),0,items,0,entry.getValue().length);
                var record=new JsonObject(); record.addProperty("revision",revision);
                record.add("items",ItemStack.OPTIONAL_CODEC.listOf().encodeStart(p.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),Arrays.asList(items)).getOrThrow());
                state.coinInventories().put(p.getUUID().toString(),record); balance(p,balances.get(p));
            }
            // Both accounts and their final inventories are durable in one atomic world-state replacement.
            state.saveStrict();
        } catch (RuntimeException failure) { state.restore(before); throw new IllegalStateException("保存できないため取引を取り消しました",failure); }
        for (var entry:inventories.entrySet()) { apply(entry.getKey(),entry.getValue()); ((CoinInventoryRevision)entry.getKey()).sum1rem0d$coinRevision(revision); }
    }
    public static void recover(ServerPlayer p) {
        var record=WorldState.get(p.level().getServer()).coinInventories().get(p.getUUID().toString()); if(record==null)return;
        String revision=record.get("revision").getAsString(); var marker=(CoinInventoryRevision)p;
        if(revision.equals(marker.sum1rem0d$coinRevision()))return;
        var items=ItemStack.OPTIONAL_CODEC.listOf().parse(p.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),record.get("items")).getOrThrow();
        if(items.size()!=p.getInventory().getContainerSize())throw new IllegalStateException("Invalid coin recovery inventory size");
        apply(p,items.toArray(ItemStack[]::new)); marker.sum1rem0d$coinRevision(revision); p.getInventory().setChanged(); p.inventoryMenu.broadcastFullState();
    }
    public static void handle(EconomyRequestPayload request, ServerPlayer p) {
        // Both loader handlers dispatch on the logical server thread.
        if (!p.level().getServer().isSameThread()) { p.level().getServer().execute(() -> handle(request,p)); return; }
        if (request.action().equals("verify") && Boolean.getBoolean("sum1rem0d.economyVerify")) { EconomyMultiplayerVerification.acknowledge(p,request.amount()); return; }
        if (!Rules.on("economy") || !p.isAlive() || p.isSpectator()) return;
        try {
            switch (request.action()) {
                case "refresh" -> Gameplay.sendState(p);
                case "deposit" -> {
                    if (!Rules.b("economy","deposit") || p.isCreative() || SESSIONS.containsKey(p.getUUID())) return;
                    var copy = inventory(p); long amount = 0;
                    for (var stack : copy) if (value(stack) != 0) { amount = Math.addExact(amount, (long)value(stack)*stack.getCount()); stack.setCount(0); }
                    if(amount==0)return; long next = CoinMath.credit(balance(p),amount,maximum()); commitInventories(Map.of(p,copy),Map.of(p,next)); saved(p);
                }
                case "withdraw" -> {
                    if (!Rules.b("economy","withdraw") || p.isCreative() || SESSIONS.containsKey(p.getUUID())) return;
                    int unit = request.target().equals("roll") ? 10 : 1;
                    long amount = request.amount(); if (amount <= 0 || amount % unit != 0 || amount / unit > 64 || amount > balance(p)) throw new IllegalArgumentException("出金額は1スタック以内、残高以内にしてください");
                    var copy = inventory(p); if (!insert(copy,new ItemStack(unit==10?ModContent.coinRoll():ModContent.coin(),(int)(amount/unit)))) throw new IllegalArgumentException("持ち物に空きがありません");
                    commitInventories(Map.of(p,copy),Map.of(p,balance(p)-amount)); saved(p);
                }
                case "invite" -> invite(p, UUID.fromString(request.target()));
                case "join" -> join(p,request.revision());
                case "decline" -> { INVITES.remove(p.getUUID()); Gameplay.sendState(p); }
                case "cancel" -> cancel(p,"取引をキャンセルしました");
                case "offer" -> {
                    var s = SESSIONS.get(p.getUUID());
                    // Editing one's own offer can race the partner's edit. Only consent needs an exact revision.
                    if (s == null || s.closed || request.revision() < s.id || request.revision() > s.revision || !(p.containerMenu instanceof TradeMenu menu) || menu.session != s || !Rules.b("player_trading","coins")) return;
                    if (request.amount()<0 || request.amount()>balance(p)) throw new IllegalArgumentException("提示金額が残高を超えています");
                    if (p==s.a) s.coinsA=request.amount(); else s.coinsB=request.amount(); s.changed();
                }
                case "confirm" -> {
                    var s = current(p,request.revision()); if (s == null) return;
                    if (p==s.a) s.acceptedA=true; else s.acceptedB=true;
                    if (s.acceptedA && s.acceptedB) commit(s); else s.sync();
                }
                default -> {}
            }
        } catch (IllegalArgumentException | ArithmeticException | IllegalStateException e) { message(p, e.getMessage()==null?"取引できません":e.getMessage()); Gameplay.sendState(p); }
    }
    private static boolean near(ServerPlayer a,ServerPlayer b) {
        double distance=Rules.n("player_trading","maximum_distance"); return a!=b && a.level()==b.level() && a.distanceToSqr(b)<=distance*distance;
    }
    private static boolean active(ServerPlayer p) { return p.isAlive() && !p.isSpectator() && !p.isCreative() && p.connection!=null && p.connection.isAcceptingMessages(); }
    private static void invite(ServerPlayer p,UUID target) {
        if (!Rules.on("player_trading") || !active(p)) return;
        var other=p.level().getServer().getPlayerList().getPlayer(target);
        if (other==null || !active(other) || !near(p,other) || SESSIONS.containsKey(p.getUUID()) || SESSIONS.containsKey(target)) throw new IllegalArgumentException("相手が近くにいないか、取引中です");
        if (INVITES.containsKey(target) && INVITES.get(target).expires()>now(p)) throw new IllegalArgumentException("相手には処理待ちの取引依頼があります");
        INVITES.put(target,new Invite(p.getUUID(),now(p)+Rules.i("player_trading","timeout_seconds")*20L,++sequence));
        message(other,p.getGameProfile().name()+"から取引依頼。財布画面で承諾できます"); message(p,"取引依頼を送りました"); Gameplay.sendState(other);
    }
    private static void join(ServerPlayer p,long revision) {
        if(Boolean.getBoolean("sum1rem0d.economyVerify")) com.sumirelabs.sum1rem0d.Sum1reM0d.LOGGER.info("ECONOMY_MULTI join {} revision {}",p.getGameProfile().name(),revision);
        var invite=INVITES.get(p.getUUID()); if(invite==null || invite.revision()!=revision || invite.expires()<now(p)) return;
        var other=p.level().getServer().getPlayerList().getPlayer(invite.sender());
        if (!Rules.on("player_trading") || other==null || !active(other) || !active(p) || !near(p,other) || SESSIONS.containsKey(p.getUUID()) || SESSIONS.containsKey(other.getUUID())) return;
        INVITES.remove(p.getUUID()); startSession(other,p);
    }
    static void startSession(ServerPlayer other,ServerPlayer p) {
        var s=new Session(other,p); SESSIONS.put(p.getUUID(),s); SESSIONS.put(other.getUUID(),s);
        for(var actor:List.of(other,p)) actor.openMenu(new SimpleMenuProvider((id,inventory,who)->new TradeMenu(id,inventory,s),Component.literal("取引: "+s.other(actor).getGameProfile().name())));
        s.sync();
    }
    private static Session current(ServerPlayer p,long revision) {
        var s=SESSIONS.get(p.getUUID()); return s!=null && !s.closed && s.revision==revision && p.containerMenu instanceof TradeMenu menu && menu.session==s ? s:null;
    }
    public static void select(ServerPlayer p,int slot,boolean remove) {
        var s=SESSIONS.get(p.getUUID()); if (s==null || !Rules.b("player_trading","items")) return;
        var offer=s.offer(p);
        if(remove) { if(slot>=0 && slot<offer.size()) offer.remove(new ArrayList<>(offer.keySet()).get(slot)); }
        else if(slot>=0 && slot<36) {
            if(offer.containsKey(slot)) offer.remove(slot);
            else if(offer.size()<9 && !p.getInventory().getItem(slot).isEmpty()) offer.put(slot,p.getInventory().getItem(slot).copy());
        }
        s.changed();
    }
    private static boolean matches(ServerPlayer p,Map<Integer,ItemStack> offer) { return offer.entrySet().stream().allMatch(e->ItemStack.matches(p.getInventory().getItem(e.getKey()),e.getValue())); }
    private static void commit(Session s) {
        if (!valid(s) || !matches(s.a,s.offerA) || !matches(s.b,s.offerB)) { cancel(s.a,"持ち物または取引条件が変わりました"); return; }
        try {
            var next=CoinMath.exchange(balance(s.a),balance(s.b),s.coinsA,s.coinsB,maximum());
            var a=inventory(s.a);var b=inventory(s.b); s.offerA.keySet().forEach(i->a[i]=ItemStack.EMPTY);s.offerB.keySet().forEach(i->b[i]=ItemStack.EMPTY);
            for(var item:s.offerB.values()) if(!insert(a,item)) throw new IllegalArgumentException("受取側の持ち物に空きがありません");
            for(var item:s.offerA.values()) if(!insert(b,item)) throw new IllegalArgumentException("受取側の持ち物に空きがありません");
            // No callbacks or packet handlers can interleave this validated server-thread commit.
            commitInventories(Map.of(s.a,a,s.b,b),Map.of(s.a,next.first(),s.b,next.second()));
            finish(s,"取引が成立しました");saved(s.a);saved(s.b);
        } catch(IllegalArgumentException|ArithmeticException|IllegalStateException e) { s.changed(); message(s.a,e.getMessage());message(s.b,e.getMessage()); }
    }
    private static boolean valid(Session s) { return Rules.on("economy") && Rules.on("player_trading") && active(s.a) && active(s.b) && near(s.a,s.b) && now(s.a)<=s.expires && (Rules.b("player_trading","coins") || s.coinsA==0 && s.coinsB==0) && (Rules.b("player_trading","items") || s.offerA.isEmpty() && s.offerB.isEmpty()) && s.a.containerMenu instanceof TradeMenu && s.b.containerMenu instanceof TradeMenu; }
    private static void finish(Session s,String reason) {
        s.closed=true; SESSIONS.remove(s.a.getUUID());SESSIONS.remove(s.b.getUUID());
        for(var p:List.of(s.a,s.b)) { if(p.containerMenu instanceof TradeMenu menu && menu.session==s) p.closeContainer();message(p,reason);Gameplay.sendState(p); }
    }
    public static void cancel(ServerPlayer p,String reason) { var s=SESSIONS.get(p.getUUID()); if(s!=null) finish(s,reason); }
    public static void leaving(ServerPlayer p) { cancel(p,"相手が退出したため取引をキャンセルしました"); INVITES.remove(p.getUUID()); INVITES.entrySet().removeIf(e->e.getValue().sender().equals(p.getUUID())); }
    public static void tick(MinecraftServer server) {
        for(var s:new HashSet<>(SESSIONS.values())) if(s.a.level().getServer()==server) {
            if(!valid(s)) finish(s,"取引条件が満たされないためキャンセルしました");
            else if(!matches(s.a,s.offerA)||!matches(s.b,s.offerB)) { s.offerA.clear();s.offerB.clear();s.changed(); }
        }
        INVITES.entrySet().removeIf(e->e.getValue().expires()<server.overworld().getGameTime());
    }
    public static JsonObject state(ServerPlayer p) {
        var json=new JsonObject();json.addProperty("balance",balance(p)); if(Boolean.getBoolean("sum1rem0d.economyVerify")) json.addProperty("verificationPhase",EconomyMultiplayerVerification.phase());
        var invite=INVITES.get(p.getUUID());
        if(invite!=null) { var sender=p.level().getServer().getPlayerList().getPlayer(invite.sender()); if(sender!=null) {json.addProperty("inviteName",sender.getGameProfile().name());json.addProperty("inviteRevision",invite.revision());} }
        var players=new JsonArray(); if(Rules.on("player_trading")) for(var other:p.level().getServer().getPlayerList().getPlayers()) if(active(other)&&near(p,other)) {var entry=new JsonObject();entry.addProperty("id",other.getUUID().toString());entry.addProperty("name",other.getGameProfile().name());players.add(entry);} json.add("players",players);
        var s=SESSIONS.get(p.getUUID()); if(s!=null) {var trade=new JsonObject();trade.addProperty("revision",s.revision);trade.addProperty("other",s.other(p).getGameProfile().name());trade.addProperty("ownCoins",s.coins(p));trade.addProperty("otherCoins",s.coins(s.other(p)));trade.addProperty("accepted",p==s.a?s.acceptedA:s.acceptedB);trade.addProperty("otherAccepted",p==s.a?s.acceptedB:s.acceptedA);json.add("trade",trade);}return json;
    }
    private static void reward(ServerPlayer p,int amount) {
        if(!Rules.on("economy") || !Rules.on("coin_rewards") || !Gameplay.eligible(p) || amount<=0 || !Rules.roll(p.getRandom(),"coin_rewards"))return;
        long previous=balance(p);
        try { balance(p,CoinMath.credit(previous,amount,maximum()));WorldState.get(p.level().getServer()).saveStrict();Gameplay.sendState(p); }
        catch(IllegalArgumentException|ArithmeticException ignored) { balance(p,previous); }
        catch(IllegalStateException failure) { balance(p,previous); com.sumirelabs.sum1rem0d.Sum1reM0d.LOGGER.error("Cannot persist coin reward",failure); }
    }
    public static void killed(LivingEntity victim,DamageSource source) {
        if(!victim.isAlive() || victim.getHealth()<=0) { // die() can be called repeatedly by commands/mods; tag makes the reward once-only.
            if(victim.entityTags().contains("sum1rem0d.coin_reward"))return;
            victim.addTag("sum1rem0d.coin_reward");
            if(victim instanceof Enemy && source.getEntity() instanceof ServerPlayer p && Rules.on("coin_rewards") && Rules.b("coin_rewards","hostile_kills")) reward(p,Rules.i("coin_rewards","kill_coins"));
        }
    }
    public static boolean ore(BlockState state) { String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();return id.endsWith("_ore") || state.is(Blocks.ANCIENT_DEBRIS); }
    public static void placed(ServerLevel level,BlockPos pos,BlockState state) { if(ore(state)||state.is(Blocks.SPAWNER)) WorldState.get(level.getServer()).markCoinBlock(level,pos); }
    public static void mined(ServerPlayer p,BlockPos pos,BlockState block,ItemStack tool) {
        if(!ore(block)&&!block.is(Blocks.SPAWNER))return;
        var state=WorldState.get(p.level().getServer());boolean used=state.markCoinBlock(p.level(),pos);
        if(used || !Gameplay.eligible(p) || !tool.isCorrectToolForDrops(block) || !Rules.on("coin_rewards"))return;
        if(EnchantmentHelper.getItemEnchantmentLevel(p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),tool)>0)return;
        if(block.is(Blocks.SPAWNER)) {if(Rules.b("coin_rewards","spawners"))reward(p,Rules.i("coin_rewards","spawner_coins"));}
        else if(Rules.b("coin_rewards","ores"))reward(p,Rules.i("coin_rewards","ore_coins"));
    }
}
