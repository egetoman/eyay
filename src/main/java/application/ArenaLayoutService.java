package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Bridge;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerType;
import kuroyale.infrastructure.ArenaRepository;

public class ArenaLayoutService {

    private final ArenaRepository repository;
    private final List<ArenaLayout> cachedLayouts;
    private String activeLayoutId;

    public ArenaLayoutService(ArenaRepository repository) {
        this.repository = repository;
        this.cachedLayouts = new ArrayList<>();
        List<ArenaLayout> loaded = repository.loadAll();
        if (loaded.isEmpty()) {
            ArenaLayout defaultLayout = ArenaLayout.defaultLayout();
            normalizeTowerStats(defaultLayout);
            cachedLayouts.add(defaultLayout);
            activeLayoutId = defaultLayout.getId();
        } else {
            cachedLayouts.addAll(loaded);
            boolean changed = false;
            for (ArenaLayout l : cachedLayouts) {
                changed |= normalizeTowerStats(l);
            }
            // Migrate old saved layouts (e.g., 250/140 tower damage) to current balance values.
            if (changed) {
                repository.saveAll(cachedLayouts);
            }
            activeLayoutId = cachedLayouts.get(0).getId();
        }
    }

    public List<ArenaLayout> getLayouts() {
        return Collections.unmodifiableList(cachedLayouts);
    }

    public Optional<ArenaLayout> findById(String id) {
        return cachedLayouts.stream().filter(layout -> layout.getId().equals(id)).findFirst();
    }

    public ArenaLayout createLayout(String name, List<Tower> towers, List<Bridge> bridges, int width, int height) {
        ArenaLayout layout = ArenaLayout.of(UUID.randomUUID().toString(), name, width, height, towers, bridges);
        normalizeTowerStats(layout);
        save(layout);
        return layout;
    }

    public void save(ArenaLayout layout) {
        normalizeTowerStats(layout);
        cachedLayouts.removeIf(existing -> existing.getId().equals(layout.getId()));
        cachedLayouts.add(layout);
        repository.saveAll(cachedLayouts);
        if (activeLayoutId == null) {
            activeLayoutId = layout.getId();
        }
    }

    public void delete(String layoutId) {
        cachedLayouts.removeIf(layout -> layout.getId().equals(layoutId));
        if (cachedLayouts.isEmpty()) {
            ArenaLayout defaultLayout = ArenaLayout.defaultLayout();
            cachedLayouts.add(defaultLayout);
            activeLayoutId = defaultLayout.getId();
        }
        if (layoutId.equals(activeLayoutId) && !cachedLayouts.isEmpty()) {
            activeLayoutId = cachedLayouts.get(0).getId();
        }
        repository.saveAll(cachedLayouts);
    }

    public ArenaLayout getActiveLayout() {
        ArenaLayout layout = findById(activeLayoutId).orElseGet(ArenaLayout::defaultLayout);
        normalizeTowerStats(layout);
        return layout;
    }

    public void setActiveLayout(String layoutId) {
        this.activeLayoutId = layoutId;
    }

    /**
     * Ensures tower stats are aligned with the current balance tuning.
     * Returns true if any tower was modified.
     */
    private boolean normalizeTowerStats(ArenaLayout layout) {
        if (layout == null) {
            return false;
        }
        boolean changed = false;
        for (Tower t : layout.getTowers()) {
            if (t == null) {
                continue;
            }
            if (t.getType() == null) {
                // Older saved layouts may omit tower type; infer from HP.
                int hp = t.getHp();
                t.setType(hp >= 3500 ? TowerType.KING : TowerType.CROWN);
                changed = true;
            }
            int desiredDamage = t.getType() == TowerType.KING ? 90 : 45;
            double desiredAttackSpeed = t.getType() == TowerType.KING ? 1.0 : 0.8;
            if (t.getDamage() != desiredDamage) {
                t.setDamage(desiredDamage);
                changed = true;
            }
            if (Double.compare(t.getAttackSpeed(), desiredAttackSpeed) != 0) {
                t.setAttackSpeed(desiredAttackSpeed);
                changed = true;
            }
        }
        return changed;
    }
}

