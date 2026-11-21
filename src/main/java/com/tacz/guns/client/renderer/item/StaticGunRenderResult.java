package com.tacz.guns.client.renderer.item;

/** Static gun handoff; selectedLod is meaningful only after a mesh has been selected. */
public final class StaticGunRenderResult {
    private final Status status;
    private final boolean selectedLod;

    public StaticGunRenderResult(Status status, boolean selectedLod) {
        this.status = status;
        this.selectedLod = selectedLod;
    }

    public Status status() {
        return status;
    }

    public boolean selectedLod() {
        return selectedLod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaticGunRenderResult)) return false;
        StaticGunRenderResult that = (StaticGunRenderResult) o;
        return java.util.Objects.equals(status, that.status) && java.util.Objects.equals(selectedLod, that.selectedLod);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, selectedLod);
    }

    @Override
    public String toString() {
        return "StaticGunRenderResult[" + "status=" + status + ", " + "selectedLod=" + selectedLod + "]";
    }

    public enum Status {
        SUBMITTED, PENDING, UNSUPPORTED
    }

    public static final StaticGunRenderResult PENDING = new StaticGunRenderResult(Status.PENDING, false);
    public static final StaticGunRenderResult UNSUPPORTED = new StaticGunRenderResult(Status.UNSUPPORTED, false);

    public boolean submitted() { return status == Status.SUBMITTED; }
}
