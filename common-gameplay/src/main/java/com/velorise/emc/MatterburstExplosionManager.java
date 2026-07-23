package com.velorise.emc;
import com.velorise.emc.common.MatterburstProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public final class MatterburstExplosionManager {
    public static final String ENTITY_CORE_TAG="VeloriseEMCMatterburstCore";
    private static final TagKey<Block> IMMUNE=TagKey.create(Registries.BLOCK,ResourceLocation.tryParse("velorise_emc:matterburst_immune"));
    private static final Map<ResourceKey<Level>,List<Job>> JOBS=new HashMap<>(); private static final Map<ResourceKey<Level>,Long> LAST=new HashMap<>();
    private MatterburstExplosionManager(){}
    public static void start(ServerLevel level,Vec3 center,int core){int tier=Math.max(1,Math.min(4,core));impact(level,center,tier);JOBS.computeIfAbsent(level.dimension(),k->new ArrayList<>()).add(new Job(BlockPos.containing(center.x,center.y,center.z),tier));}
    public static void tick(ServerLevel level){long now=level.getGameTime();Long old=LAST.put(level.dimension(),now);if(old!=null&&old.longValue()==now)return;List<Job> jobs=JOBS.get(level.dimension());if(jobs==null)return;int budget=MatterburstProfile.maxBlocksPerTick();Iterator<Job> it=jobs.iterator();while(it.hasNext()&&budget>0){Job j=it.next();int used=j.process(level,Math.min(budget,Math.max(512,budget/Math.max(1,jobs.size()))));budget-=used;if(j.done)it.remove();}if(jobs.isEmpty())JOBS.remove(level.dimension());}
    public static Entity findTaggedExplosionSource(Object explosion){if(explosion==null)return null;for(String name:new String[]{"getExploder","getDirectSourceEntity","getIndirectSourceEntity","getSourceMob"})try{Object o=explosion.getClass().getMethod(name).invoke(explosion);if(o instanceof Entity e)return e;}catch(ReflectiveOperationException ignored){}return null;}
    private static void impact(ServerLevel level,Vec3 c,int tier){float r=MatterburstProfile.entityDamageRadius(tier),max=MatterburstProfile.maxDamage(tier);for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,new AABB(c,c).inflate(r),Entity::isAlive)){double d=Math.sqrt(e.distanceToSqr(c));if(d>r)continue;float scale=(float)Math.max(0,1-d/r);e.hurt(level.damageSources().generic(),Math.max(1,max*scale));Vec3 v=e.position().subtract(c);if(v.lengthSqr()>1e-6){v=v.normalize();e.push(v.x*scale*2.5,Math.max(.35,scale*1.4),v.z*scale*2.5);}}level.playSound(null,c.x,c.y,c.z,SoundEvents.GENERIC_EXPLODE,SoundSource.BLOCKS,4,.65F);level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,c.x,c.y,c.z,Math.max(1,tier*2),r*.08,r*.05,r*.08,0);}
    private static final class Job{final BlockPos c;final int tier,r,depth;int x,z,y;boolean done;Job(BlockPos c,int tier){this.c=c;this.tier=tier;r=MatterburstProfile.radius(tier);depth=MatterburstProfile.depth(tier);x=-r;z=-r;y=-depth;}int process(ServerLevel l,int b){int n=0;while(!done&&n<b){BlockPos p=c.offset(x,y,z);if(inside(p))carve(l,p);advance();n++;}return n;}boolean inside(BlockPos p){double dx=p.getX()-c.getX(),dy=p.getY()-c.getY()+depth*.18,dz=p.getZ()-c.getZ();double shape=(dx*dx+dz*dz)/(r*(double)r)+(dy*dy)/(depth*(double)depth);long q=p.asLong()*0x9E3779B97F4A7C15L+tier*0x632BE59BD9B4E019L;q^=q>>>30;q*=0xBF58476D1CE4E5B9L;q^=q>>>27;double noise=(((q&0xFFFFL)/32767.5)-1)*.16;return shape<=1+noise;}void carve(ServerLevel l,BlockPos p){if(p.getY()<=l.getMinBuildHeight()||p.getY()>=l.getMaxBuildHeight())return;l.getChunk(p);BlockState s=l.getBlockState(p);if(s.isAir()||s.is(IMMUNE)||s.is(Blocks.BEDROCK)||s.is(Blocks.END_PORTAL_FRAME)||s.is(Blocks.END_PORTAL)||s.is(Blocks.COMMAND_BLOCK)||s.is(Blocks.CHAIN_COMMAND_BLOCK)||s.is(Blocks.REPEATING_COMMAND_BLOCK)||s.is(Blocks.STRUCTURE_BLOCK)||s.is(Blocks.JIGSAW)||s.is(Blocks.BARRIER))return;l.removeBlock(p,false);}void advance(){if(++y<=Math.max(2,r/4))return;y=-depth;if(++z<=r)return;z=-r;if(++x>r)done=true;}}
}
