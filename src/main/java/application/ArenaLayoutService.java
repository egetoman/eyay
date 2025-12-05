package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Bridge;
import kuroyale.domain.Tower;
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
            cachedLayouts.add(defaultLayout);
            activeLayoutId = defaultLayout.getId();
        } else {
            cachedLayouts.addAll(loaded);
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
        save(layout);
        return layout;
    }

    public void save(ArenaLayout layout) {
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
        return findById(activeLayoutId).orElseGet(ArenaLayout::defaultLayout);
    }

    public void setActiveLayout(String layoutId) {
        this.activeLayoutId = layoutId;
    }
}

