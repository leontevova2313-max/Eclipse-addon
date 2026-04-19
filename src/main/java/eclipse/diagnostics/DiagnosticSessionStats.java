package eclipse.diagnostics;

public final class DiagnosticSessionStats {
    public long startedAt;
    public int ticks;
    public int corrections;
    public int localVelocityPackets;
    public int disconnects;
    public int interactionPackets;
    public int blockInteractions;
    public int movementAnomalies;
    public int horizontalCollisionSamples;
    public int verticalCollisionSamples;
    public double maxHorizontalSpeed;
    public double maxAbsYVelocity;

    public void reset(long startedAt) {
        this.startedAt = startedAt;
        ticks = 0;
        corrections = 0;
        localVelocityPackets = 0;
        disconnects = 0;
        interactionPackets = 0;
        blockInteractions = 0;
        movementAnomalies = 0;
        horizontalCollisionSamples = 0;
        verticalCollisionSamples = 0;
        maxHorizontalSpeed = 0.0;
        maxAbsYVelocity = 0.0;
    }
}
