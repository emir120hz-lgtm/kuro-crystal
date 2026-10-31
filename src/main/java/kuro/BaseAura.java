package kuro;

import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * BaseAura: hedef oyuncunun yakininda uygun kristal noktasi yoksa obsidyen koyar, uzerine kristal koyar ve patlatir.
 * Nisan insan gibi yumusak doner; "Duvar arkasi" aciksa nisan almadan (blok arkasindan) koyar/vurur.
 */
public final class BaseAura {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Set<Block> NO_SUPPORT = Set.of(
      Blocks.CRAFTING_TABLE, Blocks.NOTE_BLOCK, Blocks.RESPAWN_ANCHOR, Blocks.SMITHING_TABLE, Blocks.LOOM,
      Blocks.CARTOGRAPHY_TABLE, Blocks.GRINDSTONE, Blocks.ENCHANTING_TABLE, Blocks.BEACON, Blocks.TNT
   );

   private record Plan(BlockPos base, boolean needObs, BlockPos support, Direction face) {
   }

   private static Plan plan;
   private static int cd;
   private static int tick;

   private BaseAura() {
   }

   public static void reset() {
      plan = null;
      cd = 0;
   }

   public static void tick() {
      ClientPlayerEntity p = mc.player;
      if (cd > 0) {
         cd--;
      }

      if (mc.world == null || mc.currentScreen != null || Freecam.active) {
         plan = null;
         return;
      }

      if (Config.baHold >= 0.5F && !mc.options.useKey.isPressed()) {
         plan = null;
         return;
      }

      PlayerEntity target = null;
      double bd = (double)Config.baTargetRange * Config.baTargetRange;
      for (PlayerEntity o : mc.world.getPlayers()) {
         if (o != p && o.isAlive() && !o.isSpectator()) {
            double d = p.squaredDistanceTo(o);
            if (d < bd) {
               bd = d;
               target = o;
            }
         }
      }

      if (target == null) {
         plan = null;
         return;
      }

      float selfHp = p.getHealth() + p.getAbsorptionAmount();
      Vec3d eye = p.getEyePos();
      boolean walls = Config.baWalls >= 0.5F;

      // 1) kristal patlat
      EndCrystalEntity bc = null;
      float bestTd = 0.0F;
      for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class, p.getBoundingBox().expand(6.0), e -> true)) {
         if (Mods.distToBox(eye, c.getBoundingBox()) > Config.baBreakRange) {
            continue;
         }

         Vec3d cp = c.getPos();
         float td = Mods.dmg(cp, target);
         float sd = Mods.dmg(cp, p);
         if (td < Config.baMinDmg || sd > Config.baMaxSelf || sd >= selfHp - 1.0F) {
            continue;
         }

         if (td > bestTd) {
            bestTd = td;
            bc = c;
         }
      }

      if (bc != null) {
         if (!walls) {
            Legit.aim(bc.getBoundingBox().getCenter(), Config.baAim, Config.baNoise);
         }

         boolean ok = walls || (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == bc);
         if (ok && cd <= 0) {
            if (Legit.miss(Config.baMiss)) {
               cd = 2;
               return;
            }

            mc.interactionManager.attackEntity(p, bc);
            p.swingHand(Hand.MAIN_HAND);
            cd = Legit.rand(Config.baBreakMin, Config.baBreakMax);
         }

         return;
      }

      // 2) kristal koy (varsa mevcut obsidyene) / 3) obsidyen koy
      if (plan == null || tick++ % 2 == 0 || !valid(plan)) {
         plan = findPlan(p, target, eye, selfHp, walls);
      }

      if (plan == null) {
         return;
      }

      if (!plan.needObs) {
         Hand hand = Mods.crystalHand(p);
         if (hand == null) {
            return;
         }

         Vec3d hit = new Vec3d((double)plan.base.getX() + 0.5, (double)plan.base.getY() + 1.0, (double)plan.base.getZ() + 0.5);
         place(p, hand, plan.base, Direction.UP, hit, walls, Config.baPlaceMin, Config.baPlaceMax);
      } else {
         int slot = hotbarObsidian(p);
         if (slot < 0) {
            return;
         }

         if (p.getInventory().selectedSlot != slot) {
            p.getInventory().selectedSlot = slot;
            cd = Legit.rand(Config.baObsMin, Config.baObsMax);
            return;
         }

         Vec3d fc = Vec3d.ofCenter(plan.support).add(
            plan.face.getOffsetX() * 0.5, plan.face.getOffsetY() * 0.5, plan.face.getOffsetZ() * 0.5
         );
         place(p, Hand.MAIN_HAND, plan.support, plan.face, fc, walls, Config.baObsMin, Config.baObsMax);
      }
   }

   /** Verilen blok yuzune sag tikla (nisan al ya da duvar arkasindan dogrudan). */
   private static void place(ClientPlayerEntity p, Hand hand, BlockPos block, Direction face, Vec3d hit, boolean walls, float dMin, float dMax) {
      BlockHitResult bhr;
      if (walls) {
         bhr = new BlockHitResult(hit, face, block, false);
      } else {
         Legit.aim(hit, Config.baAim, Config.baNoise);
         if (!(mc.crosshairTarget instanceof BlockHitResult b)
            || b.getType() != HitResult.Type.BLOCK
            || !b.getBlockPos().equals(block)
            || b.getSide() != face) {
            return;
         }

         bhr = b;
      }

      if (cd > 0) {
         return;
      }

      if (Legit.miss(Config.baMiss)) {
         cd = 2;
         return;
      }

      mc.interactionManager.interactBlock(p, hand, bhr);
      p.swingHand(hand);
      cd = Legit.rand(dMin, dMax);
   }

   private static int hotbarObsidian(ClientPlayerEntity p) {
      for (int i = 0; i < 9; i++) {
         if (p.getInventory().getStack(i).isOf(Items.OBSIDIAN)) {
            return i;
         }
      }

      return -1;
   }

   private static boolean valid(Plan pl) {
      BlockState st = mc.world.getBlockState(pl.base);
      boolean base = st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK);
      if (pl.needObs ? !st.isAir() : !base) {
         return false;
      }

      return mc.world.isAir(pl.base.up());
   }

   private static boolean los(Vec3d eye, Vec3d point, BlockPos expected, ClientPlayerEntity p) {
      BlockHitResult r = mc.world.raycast(new RaycastContext(eye, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
      return r.getType() == HitResult.Type.BLOCK && r.getBlockPos().equals(expected);
   }

   private static Plan findPlan(ClientPlayerEntity p, PlayerEntity target, Vec3d eye, float selfHp, boolean walls) {
      double reach = Math.min(Config.baPlaceRange, 4.5F);
      double reach2 = reach * reach;
      int r = (int)Math.ceil(reach);
      BlockPos o = p.getBlockPos();
      Vec3d tp = target.getPos();
      boolean allowObs = Config.baObsidian >= 0.5F && hotbarObsidian(p) >= 0;
      Plan best = null;
      double bestScore = -999.0;

      for (int dx = -r; dx <= r; dx++) {
         for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
               BlockPos pos = o.add(dx, dy, dz);
               Vec3d cc = new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 1.0, (double)pos.getZ() + 0.5);
               if (eye.squaredDistanceTo(cc) > reach2 || cc.squaredDistanceTo(tp) > 64.0) {
                  continue;
               }

               BlockState st = mc.world.getBlockState(pos);
               boolean isBase = st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK);
               if (!isBase && !(allowObs && st.isAir())) {
                  continue;
               }

               BlockPos up = pos.up();
               if (!mc.world.isAir(up)) {
                  continue;
               }

               Box crystalBox = new Box((double)up.getX(), (double)up.getY(), (double)up.getZ(), (double)(up.getX() + 1), (double)(up.getY() + 2), (double)(up.getZ() + 1));
               if (!mc.world.getOtherEntities(null, crystalBox).isEmpty()) {
                  continue;
               }

               if (!isBase && !mc.world.getOtherEntities(null, new Box(pos)).isEmpty()) {
                  continue;
               }

               float td = Mods.dmg(cc, target);
               float sd = Mods.dmg(cc, p);
               if (td < Config.baMinDmg || sd > Config.baMaxSelf || sd >= selfHp - 1.0F) {
                  continue;
               }

               double score = (double)td - (double)sd * 0.7 - (isBase ? 0.0 : 1.5);
               if (score <= bestScore) {
                  continue;
               }

               if (isBase) {
                  Vec3d top = new Vec3d(cc.x, (double)pos.getY() + 0.997, cc.z);
                  if (walls || los(eye, top, pos, p)) {
                     best = new Plan(pos, false, null, null);
                     bestScore = score;
                  }

                  continue;
               }

               for (Direction d : Direction.values()) {
                  BlockPos nb = pos.offset(d);
                  BlockState ns = mc.world.getBlockState(nb);
                  if (ns.isAir() || !ns.isFullCube(mc.world, nb) || ns.hasBlockEntity() || NO_SUPPORT.contains(ns.getBlock())) {
                     continue;
                  }

                  Direction face = d.getOpposite();
                  Vec3d c = Vec3d.ofCenter(nb);
                  Vec3d fc = c.add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                  if (eye.squaredDistanceTo(fc) > reach2) {
                     continue;
                  }

                  Vec3d inner = c.add(face.getOffsetX() * 0.497, face.getOffsetY() * 0.497, face.getOffsetZ() * 0.497);
                  if (walls || los(eye, inner, nb, p)) {
                     best = new Plan(pos, true, nb, face);
                     bestScore = score;
                     break;
                  }
               }
            }
         }
      }

      return best;
   }
}
