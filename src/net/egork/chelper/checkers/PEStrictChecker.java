package net.egork.chelper.checkers;

import net.egork.chelper.tester.Verdict;

/**
 * Created by Scruel on 2017/5/29.
 * Github : https://github.com/scruel
 */
public class PEStrictChecker implements Checker {
    //    public static final Verdict RTE;
    private static final Verdict PE;
    //    public static final Verdict PELF;
    private static final Verdict LFOK;

    static {
        //        RTE = new Verdict(Verdict.VerdictType.RTE, null);
        PE = new Verdict(Verdict.VerdictType.PE, null);
        LFOK = new Verdict(Verdict.VerdictType.OK, "LF may cause WA/PE");
    }

    private TokenChecker tk;

    public PEStrictChecker(String parameters) {
        tk = new TokenChecker(parameters);
    }

    public Verdict check(String input, String expectedOutput, String actualOutput) {
        if (expectedOutput == null) {
            return Verdict.UNDECIDED;
        }
        if (!expectedOutput.endsWith("\n")) {
            expectedOutput += "\n";
        }
        if (actualOutput.equals(expectedOutput)) {
            return Verdict.OK;
        }
        if (actualOutput.trim().equals(expectedOutput.trim())) {
            return LFOK;
        }
        Verdict v = tk.check(input, expectedOutput, actualOutput);
        if (v == Verdict.OK) {
            return PE;
        }
        return Verdict.WA;
    }
}

