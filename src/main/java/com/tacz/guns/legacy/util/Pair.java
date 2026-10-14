package com.tacz.guns.legacy.util;

import java.util.Objects;

/**
 * Stand-in for {@code it.unimi.dsi.fastutil.Pair}, which fastutil 7.1.0 (bundled with 1.12.2) does not have.
 * Same accessors as upstream ({@link #left()}, {@link #right()}, {@link #of(Object, Object)}).
 */
public interface Pair<L, R> {
    L left();

    R right();

    default L first() {
        return this.left();
    }

    default R second() {
        return this.right();
    }

    static <L, R> Pair<L, R> of(L left, R right) {
        return new ImmutablePair<>(left, right);
    }

    final class ImmutablePair<L, R> implements Pair<L, R> {
        private final L left;
        private final R right;

        ImmutablePair(L left, R right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public L left() {
            return this.left;
        }

        @Override
        public R right() {
            return this.right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pair)) {
                return false;
            }
            Pair<?, ?> other = (Pair<?, ?>) o;
            return Objects.equals(this.left, other.left()) && Objects.equals(this.right, other.right());
        }

        @Override
        public int hashCode() {
            return 19 * Objects.hashCode(this.left) + Objects.hashCode(this.right);
        }

        @Override
        public String toString() {
            return "<" + this.left + "," + this.right + ">";
        }
    }
}
