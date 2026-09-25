package com.baton.client.mixin;

import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Mutable
	@Accessor("user")
	void baton$setUser(User user);

	@Mutable
	@Accessor("profileFuture")
	void baton$setProfileFuture(CompletableFuture<ProfileResult> future);

	@Mutable
	@Accessor("profileKeyPairManager")
	void baton$setProfileKeyPairManager(ProfileKeyPairManager manager);
}
