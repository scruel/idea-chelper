package net.egork.chelper.tester;

import net.egork.chelper.task.test.Test;

import java.util.Collection;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public interface TestProvider {
    public Collection<Test> createTests();
}
