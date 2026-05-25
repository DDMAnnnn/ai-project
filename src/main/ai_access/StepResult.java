package ai_access;

import java.util.Arrays;

public class StepResult {
    private final Observation observation;
    private final double reward;
    private final boolean done;
    private final boolean[] actionMask;
    private final ActionResult actionResult;

    public StepResult(Observation observation, double reward, boolean done,
            boolean[] actionMask, ActionResult actionResult) {
        this.observation = observation;
        this.reward = reward;
        this.done = done;
        this.actionMask = Arrays.copyOf(actionMask, actionMask.length);
        this.actionResult = actionResult;
    }

    public Observation getObservation() {
        return observation;
    }

    public double getReward() {
        return reward;
    }

    public boolean isDone() {
        return done;
    }

    public boolean[] getActionMask() {
        return Arrays.copyOf(actionMask, actionMask.length);
    }

    public ActionResult getActionResult() {
        return actionResult;
    }
}