package com.sumirelabs.sum1rem0d.game;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.GameType;

/** Detached, non-operator survival actor; it is never added to the player list or a world. */
final class AutomationMachineActor extends ServerPlayer {
    static final String TAG="sum1rem0d.machine_actor";

    static AutomationMachineActor create(ServerLevel level,AutomationMachineEntity machine){
        var server=level.getServer();String world=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().toString();
        String identity="sum1rem0d:automation-actor:v1:"+world+":"+level.dimension().identifier()+":"+machine.getBlockPos().asLong();
        UUID actorId=UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
        UUID ownerId=machine.actorOwner();String profileName="Sum1reMachine";
        if(ownerId!=null){var owner=WorldState.get(server).playerRecords().get(ownerId.toString());if(owner!=null&&owner.lastKnownName!=null&&!owner.lastKnownName.isBlank())profileName=owner.lastKnownName;}
        return new AutomationMachineActor(server,level,new GameProfile(actorId,profileName),ownerId);
    }

    private AutomationMachineActor(MinecraftServer server,ServerLevel level,GameProfile profile,UUID ownerId){
        super(server,level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(PacketFlow.SERVERBOUND);
        this.connection=new ServerGamePacketListenerImpl(server,connection,this,CommonListenerCookie.createInitial(profile,false)){
            @Override public boolean isAcceptingMessages(){return true;}
            @Override public void send(Packet<?> packet){}
            @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
        };
        setGameMode(GameType.SURVIVAL);
        //? if >=26.2 {
        getAdvancements().clearTriggers();
        //?} else {
        /*getAdvancements().stopListening();
        *///?}
        addTag(TAG);
        if(ownerId!=null)addTag(TAG+".owner."+ownerId);
    }

    @Override public PermissionSet permissions(){return PermissionSet.NO_PERMISSIONS;}
}
