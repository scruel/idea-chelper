package net.egork.chelper.tester;

import net.egork.chelper.task.test.NewTopCoderTest;

import java.util.Collection;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public interface TopCoderTestProvider {
    public Collection<NewTopCoderTest> createTests();
}
