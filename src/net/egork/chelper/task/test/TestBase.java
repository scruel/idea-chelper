package net.egork.chelper.task.test;

import net.egork.chelper.util.OutputWriter;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Scruel on 2017/9/8.
 * Github : https://github.com/scruel
 */
public interface TestBase {
    /**
     * Checks if current element is active.
     *
     * @return is active
     */
    boolean isActive();

    /**
     * Returns new element with expected active status.
     *
     * @return {@link TestBase} instance.
     */
    @NotNull
    TestBase setActive(boolean active);

    /**
     * Returns current index of the element.
     *
     * @return index
     */
    int getIndex();

    /**
     * Returns new element with expected index.
     *
     * @return {@link TestBase} instance.
     */
    @NotNull
    TestBase setIndex(int index);

    /**
     * save this test instance into stream.
     */
    void saveTest(OutputWriter out);
}
