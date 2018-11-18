package net.egork.chelper.parser;

import java.util.Collection;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public interface DescriptionReceiver {
    void receiveDescriptions(Collection<Description> descriptions);

    boolean isStopped();
}
