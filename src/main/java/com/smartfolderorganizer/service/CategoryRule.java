package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable rule defining category matching behavior, priority, enabled state, and extensions set.
 */
public final class CategoryRule implements Comparable<CategoryRule> {

    private final Category category;
    private final Set<String> extensions;
    private final int priority;
    private final boolean enabled;
    private final String description;

    private CategoryRule(Builder builder) {
        this.category = Objects.requireNonNull(builder.category, "category must not be null");
        this.priority = builder.priority;
        this.enabled = builder.enabled;
        this.description = builder.description != null ? builder.description : "";

        Set<String> cleanSet = new HashSet<>();
        if (builder.extensions != null) {
            for (String ext : builder.extensions) {
                if (ext != null && !ext.isBlank()) {
                    cleanSet.add(ext.toLowerCase().trim().replaceFirst("^\\.", ""));
                }
            }
        }
        this.extensions = Collections.unmodifiableSet(cleanSet);
    }

    public Category getCategory() {
        return category;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this rule matches the specified extension.
     *
     * @param extension file extension to test
     * @return true if enabled and extension matches
     */
    public boolean matches(String extension) {
        if (!enabled || extension == null || extension.isBlank()) {
            return false;
        }
        String cleanExt = extension.toLowerCase().trim().replaceFirst("^\\.", "");
        return extensions.contains(cleanExt);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int compareTo(CategoryRule other) {
        Objects.requireNonNull(other, "other CategoryRule must not be null");
        // Higher priority first
        int pCompare = Integer.compare(other.priority, this.priority);
        if (pCompare != 0) {
            return pCompare;
        }
        return this.category.name().compareTo(other.category.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryRule rule = (CategoryRule) o;
        return priority == rule.priority &&
                enabled == rule.enabled &&
                category == rule.category &&
                Objects.equals(extensions, rule.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, extensions, priority, enabled);
    }

    @Override
    public String toString() {
        return "CategoryRule{" +
                "category=" + category +
                ", extensions=" + extensions +
                ", priority=" + priority +
                ", enabled=" + enabled +
                ", description='" + description + '\'' +
                '}';
    }

    /**
     * Builder for constructing immutable {@link CategoryRule} instances.
     */
    public static final class Builder {
        private Category category;
        private Set<String> extensions = Collections.emptySet();
        private int priority = 100;
        private boolean enabled = true;
        private String description = "";

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder extensions(Set<String> extensions) {
            this.extensions = extensions != null ? extensions : Collections.emptySet();
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public CategoryRule build() {
            return new CategoryRule(this);
        }
    }
}
