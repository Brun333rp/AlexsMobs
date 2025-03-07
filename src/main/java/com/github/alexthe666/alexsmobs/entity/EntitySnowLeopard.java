package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;

public class EntitySnowLeopard extends AnimalEntity implements IAnimatedEntity, ITargetsDroppedItems {

    public static final Animation ANIMATION_ATTACK_R = Animation.create(13);
    public static final Animation ANIMATION_ATTACK_L = Animation.create(13);
    private int animationTick;
    private Animation currentAnimation;
    public float prevSneakProgress;
    public float sneakProgress;
    public float prevTackleProgress;
    public float tackleProgress;
    public float prevSitProgress;
    public float sitProgress;
    private static final DataParameter<Boolean> TACKLING = EntityDataManager.createKey(EntitySnowLeopard.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.createKey(EntitySnowLeopard.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SL_SNEAKING = EntityDataManager.createKey(EntitySnowLeopard.class, DataSerializers.BOOLEAN);
    private boolean hasSlowedDown = false;
    private int sittingTime = 0;
    private int maxSitTime = 75;

    protected EntitySnowLeopard(EntityType type, World worldIn) {
        super(type, worldIn);
        this.stepHeight = 2F;
    }


    public boolean canSpawn(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.snowLeopardSpawnRolls, this.getRNG(), spawnReasonIn);
    }

    public static <T extends MobEntity> boolean canSnowLeopardSpawn(EntityType<EntitySnowLeopard> snowleperd, IWorld worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.down());
        return (blockstate.isIn(BlockTags.BASE_STONE_OVERWORLD) || blockstate.isIn(Blocks.DIRT) || blockstate.isIn(Blocks.GRASS_BLOCK)) && worldIn.getLightSubtracted(p_223317_3_, 0) > 8;
    }

    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == AMItemRegistry.MOOSE_RIBS || stack.getItem() == AMItemRegistry.COOKED_MOOSE_RIBS;
    }

    public boolean onLivingFall(float distance, float damageMultiplier) {
        return false;
    }

    protected void updateFallState(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.addGoal(3, new SnowLeopardAIMelee(this));
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(7, new AnimalAIWanderRanged(this, 60, 1.0D, 14, 7));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, true, AMEntityRegistry.buildPredicateFromTag(EntityTypeTags.getCollection().get(AMTagRegistry.SNOW_LEOPARD_TARGETS))));
        this.targetSelector.addGoal(3, new CreatureAITargetItems(this, false, 30));
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.func_234295_eP_().createMutableAttribute(Attributes.MAX_HEALTH, 30D).createMutableAttribute(Attributes.ATTACK_DAMAGE, 6.0D).createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.35F).createMutableAttribute(Attributes.FOLLOW_RANGE, 64F);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SNOW_LEOPARD_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SNOW_LEOPARD_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SNOW_LEOPARD_HURT;
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(SITTING, Boolean.valueOf(false));
        this.dataManager.register(SL_SNEAKING, Boolean.valueOf(false));
        this.dataManager.register(TACKLING, Boolean.valueOf(false));
    }

    public boolean isSitting() {
        return this.dataManager.get(SITTING).booleanValue();
    }

    public void setSitting(boolean bar) {
        this.dataManager.set(SITTING, Boolean.valueOf(bar));
    }

    public boolean isTackling() {
        return this.dataManager.get(TACKLING).booleanValue();
    }

    public void setTackling(boolean bar) {
        this.dataManager.set(TACKLING, Boolean.valueOf(bar));
    }

    public boolean isSLSneaking() {
        return this.dataManager.get(SL_SNEAKING).booleanValue();
    }

    public void setSlSneaking(boolean bar) {
        this.dataManager.set(SL_SNEAKING, Boolean.valueOf(bar));
    }

    @Nullable
    @Override
    public AgeableEntity func_241840_a(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.SNOW_LEOPARD.create(serverWorld);
    }

    public void tick(){
        super.tick();
        this.prevSitProgress = sitProgress;
        this.prevSneakProgress = sneakProgress;
        this.prevTackleProgress = tackleProgress;
        if (this.isSitting() && sitProgress < 5F) {
            sitProgress += 0.5F;
        }
        if (!isSitting() && sitProgress > 0F) {
            sitProgress -= 0.5F;
        }
        if (this.isSLSneaking() && sneakProgress < 5F) {
            sneakProgress++;
        }
        if (!isSLSneaking() && sneakProgress > 0F) {
            sneakProgress--;
        }
        if (this.isTackling() && tackleProgress < 3F) {
            tackleProgress++;
        }
        if (!isTackling() && tackleProgress > 0F) {
            tackleProgress--;
        }
        if(isSLSneaking() && !hasSlowedDown){
            hasSlowedDown = true;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25F);
        }
        if(!isSLSneaking() && hasSlowedDown){
            hasSlowedDown = false;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35F);
        }
        if(isTackling()){
            this.renderYawOffset = this.rotationYaw;
        }
        if (isSitting() && ++sittingTime > maxSitTime) {
            this.setSitting(false);
            sittingTime = 0;
        }
        if(this.getAttackTarget() != null && this.isSitting()){
            this.setSitting(false);
        }
        if (!world.isRemote && this.getAnimation() == NO_ANIMATION && this.getAttackTarget() == null && !this.isSitting() && rand.nextInt(200) == 0) {
            maxSitTime = 2600 + rand.nextInt(2600);
            this.setSitting(true);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public void travel(Vector3d vec3d) {
        if (this.isSitting()) {
            if (this.getNavigator().getPath() != null) {
                this.getNavigator().clearPath();
            }
            vec3d = Vector3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_ATTACK_L, ANIMATION_ATTACK_R};
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isFood() && stack.getItem().getFood() != null && stack.getItem().getFood().isMeat();
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(5);
    }
}
