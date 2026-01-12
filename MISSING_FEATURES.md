# Missing Features Analysis

After reviewing the KU Royale Phase 1 document and comparing it with the current implementation, here are the features that appear to be missing or incorrect:

## 1. Area of Effect (AoE) Attacks for Troops ❌

**Issue:** Several troops are supposed to deal area damage but currently only attack single targets.

**Affected Cards:**
- **Bomber**: "Throws bombs that explode on impact" - should damage multiple enemies in an area
- **Valkyrie**: "Spins and damages all nearby enemies" - should damage all nearby enemies in melee range
- **Wizard**: "Shoots explosive fireballs" - should damage multiple enemies in an area

**Current Implementation:** These units only attack single targets (one enemy at a time).

**Required Fix:** 
- Bomber and Wizard should deal area damage when their projectiles hit (similar to spells but as part of their attack)
- Valkyrie should damage all enemies within melee range when attacking
- Need to add AoE detection similar to spell implementation

## 2. Match Duration Mismatch ⚠️

**Issue:** Document specifies 3 minutes (180 seconds), but code has 6 minutes (360 seconds).

**Current Code:**
```java
private static final double TOTAL_DURATION_SECONDS = 6 * 60; // 360 seconds
```

**Document Requirement:** 3 minutes (180 seconds)

**Required Fix:** Change to `3 * 60` or `180.0`

## 3. Area of Effect for Buildings ❌

**Issue:** Some buildings should have area damage but currently only attack single targets.

**Affected Buildings:**
- **Mortar**: "Long-range artillery" - Document mentions it has splash damage (5.0 tile radius)
- **Bomb Tower**: "Defensive tower with explosive shells" - Should have area damage (1.8 tile radius)

**Current Implementation:** These buildings only attack single targets.

**Required Fix:**
- Mortar should deal area damage in a 5.0 tile radius
- Bomb Tower should deal area damage in a 1.8 tile radius
- Need to modify building attack logic to find all enemies in radius and damage them

## 4. Starting Elixir ✅

**Status:** Correctly implemented
- Document: Start with 5 elixir
- Code: `DEFAULT_MAX_ELIXIR / 2 = 10 / 2 = 5` ✅

## 5. Elixir Regeneration Rate ✅

**Status:** Correctly implemented
- Document: 1 point every 2.8 seconds
- Code: `SINGLE_ELIXIR_PER_SECOND = 1.0 / 2.8` ✅

## 6. Double Elixir Phase ✅

**Status:** Correctly implemented
- Document mentions double elixir phase
- Code has `ElixirPhase.DOUBLE` and `ElixirPhase.TRIPLE` with proper multipliers ✅

## Summary of Required Fixes

### High Priority:
1. ✅ **Implement AoE attacks for Bomber, Valkyrie, and Wizard** - FIXED
2. ✅ **Fix match duration from 6 minutes to 3 minutes** - FIXED
3. ✅ **Implement AoE attacks for Mortar and Bomb Tower** - FIXED

### Implementation Approach (Completed):

For AoE troop attacks:
- ✅ Added `getSplashRadius()` method to detect if a card has AoE capability
- ✅ Modified `Unit.tick()` attack logic to:
  - For Bomber/Wizard: When attacking, find all enemies in radius and damage them (1.5 tile radius)
  - For Valkyrie: When attacking, find all enemies in melee range and damage them (1.0 tile radius)
- ✅ Used `findUnitsInRadius` similar to spells

For AoE building attacks:
- ✅ Modified building attack logic in `Unit.tick()` for Mortar and Bomb Tower
- ✅ When they attack, find all enemies in their splash radius and damage all of them
  - Mortar: 5.0 tile splash radius
  - Bomb Tower: 1.8 tile splash radius

## Files Modified:

1. ✅ `src/main/java/kuroyale/domain/Unit.java` - Added AoE attack logic with `getSplashRadius()` and `performAoEAttack()` methods
2. ✅ `src/main/java/kuroyale/domain/Match.java` - Fixed match duration from 6 minutes to 3 minutes
3. ✅ No changes needed to `CardCatalogRepository.java` - Using card ID detection for AoE units

## Notes:

- The document mentions "Area of Effect (AoE) Attacks" in section 5.4
- Bomber, Valkyrie, and Wizard are listed under "Area-of-Effect (AoE) Troops" in section 6.2.2
- Mortar has "Splash Radius: 5.0 tiles" in the document
- Bomb Tower has "Splash Radius: 1.8 tiles" in the document

