package ai_access;

// Represents the result of one AI action.
public class ActionResult {
    private final boolean legal;
    private final double reward;
    private final boolean done;
    private final String message;

    public ActionResult(boolean legal, double reward, boolean done, String message) {
        this.legal = legal;
        this.reward = reward;
        this.done = done;
        this.message = message;
    }

    public boolean isLegal() {
        return legal;
    }

    public double getReward() {
        return reward;
    }

    public boolean isDone() {
        return done;
    }

    public String getMessage() {
        return message;
    }
}