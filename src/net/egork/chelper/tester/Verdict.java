package net.egork.chelper.tester;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class Verdict {
    public static final Verdict SKIPPED = new Verdict(Verdict.VerdictType.SKIPPED, null);
    public static final Verdict UNDECIDED = new Verdict(Verdict.VerdictType.UNDECIDED, null);
    public static final Verdict OK = new Verdict(Verdict.VerdictType.OK, null);
    public static final Verdict WA = new Verdict(Verdict.VerdictType.WA, null);
    // PE - Presentation Error
    public static final Verdict PE = new Verdict(Verdict.VerdictType.PE, null);
    // OK but line separator may cause error.
    public static final Verdict LFOK = new Verdict(Verdict.VerdictType.OK, "LF may cause WA/PE");

    public final VerdictType type;
    public final String message;

    public Verdict(VerdictType type, String message) {
        this.type = type;
        this.message = message;
    }

    @Override
    public String toString() {
        return type + (message == null ? "" : " (" + message + ")");
    }

    public static enum VerdictType {
        OK("OK"),
        WA("Wrong Answer"),
        PE("Presentation Error"),
        RTE("RunTime Error"),
        UNDECIDED("Unknown"),
        SKIPPED("Skipped");
        private final String uiDescription;

        private VerdictType(String uiDescription) {
            this.uiDescription = uiDescription;
        }

        @Override
        public String toString() {
            return uiDescription;
        }
    }
}
