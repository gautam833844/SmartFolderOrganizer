package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central thread-safe registry holding active {@link CategoryRule} definitions sorted by priority.
 * <p>
 * Supports dynamic registration, removal, rule queries, and initialization of default enum category rules.
 * </p>
 */
public class CategoryRegistry {

    private final List<CategoryRule> rules = new CopyOnWriteArrayList<>();

    public CategoryRegistry() {
        loadDefaultRules();
    }

    /**
     * Initializes registry with default rules derived from the {@link Category} enum.
     */
    public final void loadDefaultRules() {
        rules.clear();
        int priority = 100;
        for (Category category : Category.values()) {
            if (category == Category.OTHERS) continue;
            CategoryRule defaultRule = CategoryRule.builder()
                    .category(category)
                    .extensions(category.getSupportedExtensions())
                    .priority(priority--)
                    .enabled(true)
                    .description("Default rule for " + category.getDisplayName())
                    .build();
            registerRule(defaultRule);
        }
    }

    /**
     * Registers a new category rule and maintains priority order.
     *
     * @param rule category rule to register (non-null)
     */
    public void registerRule(CategoryRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");
        rules.add(rule);
        sortRules();
    }

    /**
     * Removes an existing rule from the registry.
     *
     * @param rule rule to remove
     * @return true if removed
     */
    public boolean removeRule(CategoryRule rule) {
        if (rule == null) return false;
        return rules.remove(rule);
    }

    /**
     * Removes all rules associated with a specific Category.
     *
     * @param category category to remove rules for
     */
    public void removeRulesForCategory(Category category) {
        if (category == null) return;
        rules.removeIf(r -> r.getCategory() == category);
    }

    /**
     * Finds the highest priority enabled rule matching the specified extension.
     *
     * @param extension file extension to test
     * @return Optional containing matching CategoryRule if found
     */
    public Optional<CategoryRule> findRuleForExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return Optional.empty();
        }
        for (CategoryRule rule : rules) {
            if (rule.matches(extension)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets an unmodifiable view of all registered category rules.
     *
     * @return unmodifiable list of rules
     */
    public List<CategoryRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    private void sortRules() {
        List<CategoryRule> sorted = new ArrayList<>(rules);
        Collections.sort(sorted);
        rules.clear();
        rules.addAll(sorted);
    }
}
